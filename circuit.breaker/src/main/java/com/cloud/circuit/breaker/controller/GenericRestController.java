package com.cloud.circuit.breaker.controller;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@Component
@RestController
@Slf4j
@EnableWebFlux
@EnableAutoConfiguration
@ComponentScan
@AutoConfiguration
public class GenericRestController {
    @PostMapping("/**")
    @CircuitBreaker(name = "post", fallbackMethod = "awsFallbackMethod")
    public CompletableFuture<ResponseEntity<String>> post(RequestEntity<String> req) {
        try {
            URL url = new URL(String.valueOf(req.getUrl()));
            Mono<String> user = WebClient.builder().build().post().uri(UriComponentsBuilder.newInstance()
                            .host("dummy.restapiexample.com")
                            .path("/api/v1/create")
                            .scheme("https")
                            .build().toUri()
                    )
                    .body(BodyInserters.fromValue(req.getBody())).retrieve().bodyToMono(String.class);
            return user.map(u -> ResponseEntity.ok(u))
                    .defaultIfEmpty(ResponseEntity.notFound().build()).toFuture();

        } catch (MalformedURLException e) {
        }
        return null;
    }

    public CompletableFuture<ResponseEntity<String>> awsFallbackMethod(RequestEntity<String> req, Throwable t) {
        StopWatch s = new StopWatch();
        s.start();
        Mono<String> user = WebClient.builder().build().get().uri(UriComponentsBuilder.newInstance()
                        .host("www.boredapi.com")
                        .path("/api/activity")
                        .scheme("https")
                        .build().toUri()
                )
                .retrieve().bodyToMono(String.class);
        log.info("time taken: {}",s.getTotalTimeNanos());
        s.stop();
        s=null;
        return user.map(u -> ResponseEntity.ok(u))
                .defaultIfEmpty(ResponseEntity.notFound().build()).toFuture();
    }

}
