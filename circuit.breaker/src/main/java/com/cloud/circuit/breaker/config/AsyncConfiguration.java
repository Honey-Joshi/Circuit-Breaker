package com.cloud.circuit.breaker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "threadPoolTaskExecutor")
    public Executor aSyncExecutor() {
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(8);
        threadPoolTaskExecutor.setMaxPoolSize(8);
        threadPoolTaskExecutor.setQueueCapacity(1200);
        threadPoolTaskExecutor.setThreadNamePrefix("Circuit_Breaker_Thread_");
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }
}
