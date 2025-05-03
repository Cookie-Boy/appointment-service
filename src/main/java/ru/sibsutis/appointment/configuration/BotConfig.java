package ru.sibsutis.appointment.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.webhook.TelegramBotsWebhookApplication;
import org.telegram.telegrambots.webhook.WebhookOptions;
import ru.sibsutis.appointment.core.service.TelegramWebhookBotService;

@Configuration
public class BotConfig {
    @Bean
    public TelegramBotsWebhookApplication telegramBotsApi(TelegramWebhookBotService botService) throws TelegramApiException {
        TelegramBotsWebhookApplication webhookApplication =
                new TelegramBotsWebhookApplication(
                        WebhookOptions.builder()
                                .enableRequestLogging(true)
                                .build()
                );

        webhookApplication.registerBot(botService);
        return webhookApplication;
    }
}
