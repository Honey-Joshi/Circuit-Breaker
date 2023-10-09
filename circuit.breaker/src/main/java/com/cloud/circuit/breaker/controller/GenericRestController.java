package com.cloud.circuit.breaker.controller;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
@RestController
@Slf4j
@Async("threadPoolTaskExecutor")
@EnableAutoConfiguration
@ComponentScan
@AutoConfiguration
@Configuration
public class GenericRestController {
    @PostMapping("/**")
    @CircuitBreaker(name = "post", fallbackMethod = "awsFallbackMethod")
    public CompletableFuture<ResponseEntity<String>> post(RequestEntity<String> req) {
        Mono<String> user;
        try {
            HttpHeaders h = req.getHeaders();
//            log.info("pg-api-gateway hit: {}",new JSONObject(req));
            Consumer<HttpHeaders> consumer = it -> it.addAll(h);
            user = WebClient.builder().
                    clientConnector(new ReactorClientHttpConnector(HttpClient.create().secure(t -> {
                        try {
                            t.sslContext(SslContextBuilder
                                    .forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .build());
                        } catch (SSLException e) {
                            throw new RuntimeException(e);
                        }
                    }))).


                    build().post().uri(UriComponentsBuilder.newInstance()
                            .host("apigw.pg2nonprod.paytm.com")
                            .path(req.getUrl().getPath())
                            .scheme("https")
                            .build().toUri()
                    )
                    .headers(consumer)
                    .headers(headers -> headers.remove(HttpHeaders.HOST))
                    .header("Host","apigw.pg2nonprod.paytm.com")
                    .body(BodyInserters.fromValue(req.getBody())).retrieve().bodyToMono(String.class);
            return user.map(u -> ResponseEntity.ok(u))
                    .defaultIfEmpty(ResponseEntity.notFound().build()).toFuture();

        } catch (Exception e) {
            return null;
        }
    }

    public CompletableFuture<ResponseEntity<String>> awsFallbackMethod(RequestEntity<String> req, Throwable th) {

        try {

            HttpHeaders h = req.getHeaders();
//            h.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
//            h.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");

            Consumer<HttpHeaders> consumer = it -> it.addAll(h);
            log.info("amazon-api-gateway hit {}",new JSONObject(req));
            Mono<String> user = WebClient.builder().




            clientConnector(new ReactorClientHttpConnector(HttpClient.create().secure(t -> {
                try {
                    t.sslContext(SslContextBuilder
                            .forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build());
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                }
            })))




                    .build().post().uri(UriComponentsBuilder.newInstance()
                            .host("dev-qa.pg2nonprod.paytm.com")
                            .path(req.getUrl().getPath())
                            .scheme("https")
                            .build().toUri()
                    )
                    .headers(consumer)
                    .headers(headers -> headers.remove(HttpHeaders.HOST))
                    .header("Host","dev-qa.pg2nonprod.paytm.com")
                    .body(BodyInserters.fromValue(req.getBody())).retrieve().bodyToMono(String.class);
            return user.map(u -> ResponseEntity.ok(u))
                    .defaultIfEmpty(ResponseEntity.notFound().build()).toFuture();
        } catch (Exception e) {
            return null;
        }

    }
}
