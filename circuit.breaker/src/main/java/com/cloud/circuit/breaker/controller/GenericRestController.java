package com.cloud.circuit.breaker.controller;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

@Component
@RestController
@Slf4j
@EnableAutoConfiguration
@ComponentScan
@EnableWebFlux
@AutoConfiguration
@Configuration
public class GenericRestController {
    @PostMapping("/**")
    @CircuitBreaker(name = "post", fallbackMethod = "awsFallbackMethod")
    public Mono<ResponseEntity<String>> post(RequestEntity<String> req) {
        Mono<ResponseEntity<String>> user = null;
        HttpHeaders h = req.getHeaders();
//            log.info("pg-api-gateway hit: {}", new JSONObject(req));
        Consumer<HttpHeaders> consumer = it -> it.addAll(h);
        user = WebClient.builder().
                build().post().uri(UriComponentsBuilder.newInstance()
                        .host("apigw.pg2nonprod.paytm.com")
                        .path(req.getUrl().getPath())
                        .scheme("https")
                        .build().toUri()
                )
                .headers(consumer)
                .headers(headers -> headers.remove(HttpHeaders.HOST))
                .header("Host", "apigw.pg2nonprod.paytm.com")
                .body(BodyInserters.fromValue(req.getBody()))
                .retrieve().toEntity(String.class)
                .onErrorReturn(WebClientResponseException.TooManyRequests.class, ResponseEntity.status(HttpResponseStatus.TOO_MANY_REQUESTS.code()).body("Too Many Request"))
                .onErrorReturn(WebClientResponseException.GatewayTimeout.class, ResponseEntity.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build())
                .onErrorReturn(WebClientResponseException.InternalServerError.class, ResponseEntity.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).build())
                .onErrorReturn(WebClientResponseException.BadGateway.class, ResponseEntity.status(HttpResponseStatus.BAD_GATEWAY.code()).build())
                .onErrorReturn(WebClientResponseException.BadRequest.class, ResponseEntity.status(HttpResponseStatus.BAD_REQUEST.code()).build())
                .onErrorReturn(WebClientResponseException.ServiceUnavailable.class, ResponseEntity.status(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).build())
                .onErrorReturn(WebClientResponseException.MethodNotAllowed.class, ResponseEntity.status(HttpResponseStatus.METHOD_NOT_ALLOWED.code()).build())

                .onErrorReturn(WebClientResponseException.class, ResponseEntity.status(HttpResponseStatus.NOT_FOUND.code()).build())
                .doOnNext(response -> log.info("Response from Custom-API-GW: {}", new JSONObject(response)))
                .timeout(Duration.ofMillis(29500));
        return user;
    }

    public Mono<ResponseEntity<String>> awsFallbackMethod(RequestEntity<String> req, Throwable th) {
        Mono<ResponseEntity<String>> user = null;
        HttpHeaders h = req.getHeaders();
        Consumer<HttpHeaders> consumer = it -> it.addAll(h);
        log.info("amazon-api-gateway hit {}", th.getStackTrace());
        user = WebClient.builder()
                .build().post().uri(UriComponentsBuilder.newInstance()
                        .host("dev-qa.pg2nonprod.paytm.com")
                        .path(req.getUrl().getPath())
                        .scheme("https")
                        .build().toUri()
                )
                .headers(consumer)
                .headers(headers -> headers.remove(HttpHeaders.HOST))
                .header("Host", "dev-qa.pg2nonprod.paytm.com")
                .body(BodyInserters.fromValue(req.getBody()))
                .retrieve().toEntity(String.class)
                .onErrorReturn(WebClientResponseException.TooManyRequests.class, ResponseEntity.status(HttpResponseStatus.TOO_MANY_REQUESTS.code()).build())
                .onErrorReturn(WebClientResponseException.GatewayTimeout.class, ResponseEntity.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build())
                .onErrorReturn(WebClientResponseException.InternalServerError.class, ResponseEntity.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).build())
                .onErrorReturn(WebClientResponseException.BadGateway.class, ResponseEntity.status(HttpResponseStatus.BAD_GATEWAY.code()).build())
                .onErrorReturn(WebClientResponseException.BadRequest.class, ResponseEntity.status(HttpResponseStatus.BAD_REQUEST.code()).build())
                .onErrorReturn(WebClientResponseException.ServiceUnavailable.class, ResponseEntity.status(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).build())
                .doOnNext(response -> log.info("Response from Amazon-API-GW: {}", response))
                .timeout(Duration.ofSeconds(29));
        return user;
    }





    @GetMapping("/**")
    @CircuitBreaker(name = "get", fallbackMethod = "awsFallbackMethodForGet")
    @Retry(name = "get", fallbackMethod = "awsFallbackMethodForGet")
    public Mono<ResponseEntity<String>> get(RequestEntity<String> req) {
        Mono<ResponseEntity<String>> user = null;
        HttpHeaders h = req.getHeaders();
        Consumer<HttpHeaders> consumer = it -> it.addAll(h);
        user = WebClient.builder().
                build().get().uri(UriComponentsBuilder.newInstance()
                        .host("apigw.pg2nonprod.paytm.com")
                        .path(req.getUrl().getPath())
                        .scheme("https")
                        .build().toUri()
                )
                .headers(consumer)
                .headers(headers -> headers.remove(HttpHeaders.HOST))
                .header("Host", "apigw.pg2nonprod.paytm.com")
//                .body(BodyInserters.fromValue(req.getBody()))
                .retrieve().toEntity(String.class)
                .onErrorReturn(WebClientResponseException.TooManyRequests.class, ResponseEntity.status(HttpResponseStatus.TOO_MANY_REQUESTS.code()).body("Too Many Request"))
                .onErrorReturn(WebClientResponseException.GatewayTimeout.class, ResponseEntity.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build())
                .onErrorReturn(WebClientResponseException.InternalServerError.class, ResponseEntity.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).build())
                .onErrorReturn(WebClientResponseException.BadGateway.class, ResponseEntity.status(HttpResponseStatus.BAD_GATEWAY.code()).build())
                .onErrorReturn(WebClientResponseException.BadRequest.class, ResponseEntity.status(HttpResponseStatus.BAD_REQUEST.code()).build())
                .onErrorReturn(WebClientResponseException.ServiceUnavailable.class, ResponseEntity.status(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).build())
                .onErrorReturn(WebClientResponseException.MethodNotAllowed.class, ResponseEntity.status(HttpResponseStatus.METHOD_NOT_ALLOWED.code()).build())

                .onErrorReturn(WebClientResponseException.class, ResponseEntity.status(HttpResponseStatus.NOT_FOUND.code()).build())
                .doOnNext(response -> log.info("Response from Custom-API-GW: {}", new JSONObject(response)))
                .timeout(Duration.ofMillis(29500));
        return user;
    }

    public Mono<ResponseEntity<String>> awsFallbackMethodForGet(RequestEntity<String> req, Throwable th) {
        Mono<ResponseEntity<String>> user = null;
        HttpHeaders h = req.getHeaders();
        Consumer<HttpHeaders> consumer = it -> it.addAll(h);
        log.info("amazon-api-gateway hit {}", th.getStackTrace());
        user = WebClient.builder()
                .build().get().uri(UriComponentsBuilder.newInstance()
                        .host("dev-qa.pg2nonprod.paytm.com")
                        .path(req.getUrl().getPath())
                        .scheme("https")
                        .build().toUri()
                )
                .headers(consumer)
                .headers(headers -> headers.remove(HttpHeaders.HOST))
                .header("Host", "dev-qa.pg2nonprod.paytm.com")
//                .body(BodyInserters.fromValue(req.getBody()))
                .retrieve().toEntity(String.class)
                .onErrorReturn(WebClientResponseException.TooManyRequests.class, ResponseEntity.status(HttpResponseStatus.TOO_MANY_REQUESTS.code()).build())
                .onErrorReturn(WebClientResponseException.GatewayTimeout.class, ResponseEntity.status(HttpResponseStatus.GATEWAY_TIMEOUT.code()).build())
                .onErrorReturn(WebClientResponseException.InternalServerError.class, ResponseEntity.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).build())
                .onErrorReturn(WebClientResponseException.BadGateway.class, ResponseEntity.status(HttpResponseStatus.BAD_GATEWAY.code()).build())
                .onErrorReturn(WebClientResponseException.BadRequest.class, ResponseEntity.status(HttpResponseStatus.BAD_REQUEST.code()).build())
                .onErrorReturn(WebClientResponseException.ServiceUnavailable.class, ResponseEntity.status(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).build())
                .doOnNext(response -> log.info("Response from Amazon-API-GW: {}", response))
                .timeout(Duration.ofSeconds(29));
        return user;
    }
}
