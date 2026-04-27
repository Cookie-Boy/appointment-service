package ru.sibsutis.appointment.api.client;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenProvider {
    private final OAuth2AuthorizedClientManager clientManager;
    private final String clientRegistrationId = "appointment-service";

    public String getFreshToken() {
        OAuth2AuthorizedClient client = clientManager.authorize(
                OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                        .principal("service-account")
                        .build()
        );

        return client != null ? client.getAccessToken().getTokenValue() : "token-is-null";
    }
}
