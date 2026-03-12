package ru.sibsutis.appointment.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .requestInterceptor((request, body, execution) -> {
                    // Можно добавить общую логику (логирование, метрики)
                    return execution.execute(request, body);
                });
    }
}