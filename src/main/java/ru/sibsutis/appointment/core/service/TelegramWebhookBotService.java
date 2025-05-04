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
import ru.sibsutis.appointment.core.model.Appointment;
import ru.sibsutis.appointment.core.model.AppointmentStatus;
import ru.sibsutis.appointment.core.model.TelegramUser;
import ru.sibsutis.appointment.core.repository.TelegramUserRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramWebhookBotService implements TelegramWebhookBot {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${bot.name}")
    private String name;

    @Value("${bot.path}")
    private String path;

    @Value("${bot.webhook.url}")
    private String url;

    private final TelegramClient telegramClient;

    private final TelegramUserRepository telegramUserRepository;

    private final AppointmentService appointmentService;

    @Autowired
    public TelegramWebhookBotService(@Value("${bot.token}") String token,
                                     TelegramUserRepository telegramUserRepository,
                                     AppointmentService appointmentService) {
        this.telegramClient = new OkHttpTelegramClient(token);
        this.telegramUserRepository = telegramUserRepository;
        this.appointmentService = appointmentService;
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

    @Override
    public BotApiMethod<?> consumeUpdate(Update update) {
        log.info("Got the update");
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String messageText = update.getMessage().getText();

            if ("/start".equals(messageText)) {
                return handleStartCommand(chatId, update);
            } else if ("/schedule".equals(messageText)) {
                return handleScheduleCommand(chatId);
            }

            String text = "Привет! Вы отправили: " + update.getMessage().getText();
            return new SendMessage(chatId, text);
        }
        return null;
    }

    private BotApiMethod<?> handleStartCommand(String chatId, Update update) {
        String username = update.getMessage().getFrom().getUserName();

        telegramUserRepository.save(TelegramUser.builder()
                .username(username)
                .chatId(chatId)
                .build());

        return new SendMessage(chatId, "Привет! Я бот, который поможет тебе забронировать прием у врача.\n"
                + "Доступные команды:\n"
                + "/schedule - показать все записи\n"
                + "/book - создать новую запись");
    }

    private BotApiMethod<?> handleScheduleCommand(String chatId) {
        return telegramUserRepository.findByChatId(chatId)
                .map(user -> {
                    List<Appointment> appointments = appointmentService.getAllAppointments(user);
                    if (appointments.isEmpty()) {
                        return new SendMessage(chatId, "У вас нет запланированных приёмов.");
                    }

                    String schedule = appointments.stream()
                            .map(this::formatAppointment)
                            .collect(Collectors.joining("\n\n"));

                    return new SendMessage(chatId, "Ваши записи:\n\n" + schedule);
                })
                .orElseGet(() -> new SendMessage(chatId, "Сначала зарегистрируйтесь с помощью /start"));
    }

    private String formatAppointment(Appointment appointment) {
        return String.format(
                """
                        📅 %s в %s
                        👨⚕️ Врач: %s %s
                        🏥 Клиника: %s
                        🔖 Статус: %s""",
                appointment.getStartTime().format(DATE_FORMATTER),
                appointment.getStartTime().format(TIME_FORMATTER),
                appointment.getDoctor().getFirstName(),
                appointment.getDoctor().getLastName(),
                appointment.getClinic().getName(),
                getStatusEmoji(appointment.getStatus())
        );
    }

    private String getStatusEmoji(AppointmentStatus status) {
        return switch (status) {
            case CONFIRMED -> "✅ Подтверждено";
            case PENDING -> "⏳ Ожидает подтверждения";
            case CANCELLED -> "❌ Отменено";
        };
    }
}
