package com.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "api/user/v1/login",
                                "api/user/v1",
                                "/api/user/v1/register",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/api/user/v1/login-info")
                        .authenticationSuccessHandler(
                                new RedirectServerAuthenticationSuccessHandler("/api/user/v1/welcome")
                        )
                        .authenticationFailureHandler(
                                new RedirectServerAuthenticationFailureHandler("spi/user/v1/login?error=true")
                        )
                )

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}