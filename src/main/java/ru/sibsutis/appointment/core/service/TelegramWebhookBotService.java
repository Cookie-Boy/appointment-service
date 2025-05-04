package ru.sibsutis.appointment.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.webhook.TelegramWebhookBot;
import ru.sibsutis.appointment.core.model.TelegramUser;
import ru.sibsutis.appointment.core.repository.TelegramUserRepository;

@Slf4j
@Service
public class TelegramWebhookBotService implements TelegramWebhookBot {

    @Value("${bot.name}")
    private String name;

    @Value("${bot.path}")
    private String path;

    @Value("${bot.webhook.url}")
    private String url;

    private final TelegramClient telegramClient;

    private final TelegramUserRepository telegramUserRepository;

    @Autowired
    public TelegramWebhookBotService(@Value("${bot.token}") String token,
                                     TelegramUserRepository telegramUserRepository) {
        this.telegramClient = new OkHttpTelegramClient(token);
        this.telegramUserRepository = telegramUserRepository;
    }

    @Override
    public void runDeleteWebhook() {
        try {
            telegramClient.execute(new DeleteWebhook());
        } catch (TelegramApiException e) {
            log.info("Error deleting webhook");
        }
    }

    @Override
    public void runSetWebhook() {
        try {
            telegramClient.execute(new SetWebhook(url + path));
        } catch (TelegramApiException e) {
            log.info("Error setting webhook: {}", String.valueOf(e));
        }
    }

    @Override
    public BotApiMethod<?> consumeUpdate(Update update) {
        log.info("Got the update");
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            if (update.hasMessage() && update.getMessage().getText().equals("/start")) {
                String username = update.getMessage().getFrom().getUserName();

                telegramUserRepository.save(TelegramUser.builder()
                        .username(username)
                        .chatId(chatId)
                        .build());

                return new SendMessage(chatId, "Привет! Я бот, который поможет тебе забронировать прием у врача.");
            }

            String text = "Привет! Вы отправили: " + update.getMessage().getText();
            return new SendMessage(chatId, text);
        }
        return null;
    }

    public void sendNotification(String chatId, String message) {
        SendMessage sendMessage = new SendMessage(chatId, message);
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки уведомления", e);
        }
    }

    @Override
    public String getBotPath() {
        return path;
    }
}
