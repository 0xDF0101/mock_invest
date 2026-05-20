package com.mockinvest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestClient yahooRestClient() {
        return RestClient.builder()
                .baseUrl("https://query1.finance.yahoo.com")
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    @Bean
    public RestClient yahooSearchClient() {
        return RestClient.builder()
                .baseUrl("https://query2.finance.yahoo.com")
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }
}
