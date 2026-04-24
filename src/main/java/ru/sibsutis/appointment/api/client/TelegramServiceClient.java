package ru.sibsutis.appointment.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramServiceClient {
    private final RestClient restClient;
    private final TokenProvider tokenProvider;

    public ResponseEntity<?> sendNotification(String username, String text) {
        String token = tokenProvider.getFreshToken();
        log.info("Fresh token: {}", token);
        return restClient.post()
                .uri("/notify/{username}", username)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .body(text)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
