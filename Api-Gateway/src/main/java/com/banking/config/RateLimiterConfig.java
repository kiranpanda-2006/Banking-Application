package com.banking.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver keyResolver() {
        return exchange -> {

            if (exchange.getRequest().getRemoteAddress() == null) {
                return Mono.just("unknown");
            }

            String hostAddress = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();

            return Mono.just(hostAddress);
        };
    }
}