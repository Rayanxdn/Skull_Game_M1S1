package fr.miage.skull.frontend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApiConfig {

    @Bean
    public RestClient playerRestClient(@Value("${api.player-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
