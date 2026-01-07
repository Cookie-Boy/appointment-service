package ru.sibsutis.appointment.api.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TelegramServiceClient {
    private final RestClient restClient;
    private final String SERVICE_NAME = "telegram_service_bot";

    @Value("${clients.telegram-bot.url.notify}")
    private String notifyUri;

    @Autowired
    public TelegramServiceClient(RestClient.Builder builder,
                                 @Value("${clients.telegram-bot.url.base}") String url) {
        this.restClient = builder
                .baseUrl(url)
                .defaultHeader("Telegram-Bot-Service", SERVICE_NAME)
                .build();
    }

    public ResponseEntity<?> sendNotification(String chatId, String text) {
        return restClient.post()
                .uri(notifyUri, chatId)
                .body(text)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
