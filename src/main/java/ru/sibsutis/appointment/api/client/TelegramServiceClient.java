package ru.sibsutis.appointment.api.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TelegramServiceClient {

    private final OAuth2AuthorizedClientManager clientManager;
    private final RestClient restClient;

    private final String clientRegistrationId = "appointment";

    @Value("${endpoints.telegram-bot.url}")
    private String notifyUrl;

    @Autowired
    public TelegramServiceClient(OAuth2AuthorizedClientManager clientManager,
                                 RestClient.Builder builder) {
        this.clientManager = clientManager;
        this.restClient = builder.build();
    }

    private String getFreshToken() {
        OAuth2AuthorizedClient client = clientManager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                        .principal("service-account")
                        .build()
        );

        return client != null ? client.getAccessToken().getTokenValue() : "token-is-null";
    }

    public ResponseEntity<?> sendNotification(String tgUserName, String text) {
        String token = getFreshToken();
        log.info("Fresh token: {}", token);
        return restClient.post()
                .uri(notifyUrl, tgUserName)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .body(text)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
