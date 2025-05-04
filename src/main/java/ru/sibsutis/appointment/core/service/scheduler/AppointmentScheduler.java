package ru.sibsutis.appointment.core.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.sibsutis.appointment.core.exception.NotificationException;
import ru.sibsutis.appointment.core.model.Appointment;
import ru.sibsutis.appointment.core.model.AppointmentStatus;
import ru.sibsutis.appointment.core.repository.AppointmentRepository;
import ru.sibsutis.appointment.core.service.TelegramWebhookBotService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

    private final AppointmentRepository appointmentRepository;
    private final TelegramWebhookBotService botService;
    private final RedisTemplate<String, String> redisTemplate;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Scheduled(fixedRate = 60_000)
    public void checkUpcomingAppointments() {
        log.info("<checkUpcomingAppointments started>");
        LocalDateTime now = LocalDateTime.now();
        checkAndNotify(now.plusHours(1));
        checkAndConfirm(now);
    }

    private void checkAndNotify(LocalDateTime targetTime) {
        log.info("checkAndNotify started: {}", targetTime);
        List<Appointment> appointments = appointmentRepository
                .findByStartTimeBetween(targetTime.minusMinutes(3), targetTime.plusMinutes(3));

        appointments.forEach(app -> {
            if (app.getStatus() == AppointmentStatus.CONFIRMED || app.getTelegramUser() == null) return;

            String lockKey = "hourly_notification:" + app.getId();
            try {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "sent", 55, TimeUnit.MINUTES);

                if (Boolean.TRUE.equals(acquired)) {
                    botService.sendNotification(
                            app.getTelegramUser().getChatId(),
                            "До вашего приёма остался 1 час ⏳" +
                                    "\nВрач: " + app.getDoctor().getFirstName() + " " + app.getDoctor().getLastName() +
                                    "\nВремя: " + app.getStartTime().format(TIME_FORMATTER));
                }
            } catch (Exception e) {
                throw new NotificationException("Не удалось отправить уведомление:\n" + e);
            }
            log.info("Sent notification for appointment: {}", app.getId());
        });
    }

    private void checkAndConfirm(LocalDateTime now) {
        log.info("checkAndConfirm started: {}", now);
        List<Appointment> appointments = appointmentRepository
                .findByStartTimeBetween(now.minusMinutes(5), now.plusMinutes(5));

        appointments.forEach(app -> {
            if (app.getStatus() != AppointmentStatus.CONFIRMED) {
                app.setStatus(AppointmentStatus.CONFIRMED);
                appointmentRepository.save(app);
                cleanRedisSlot(app);
                log.info("Confirmed appointment: {}", app.getId());

                if (app.getTelegramUser() != null) {
                    botService.sendNotification(
                            app.getTelegramUser().getChatId(),
                            "Ваш прием уже начинается!" +
                                    "\nВрач: " + app.getDoctor().getFirstName() + " " + app.getDoctor().getLastName() +
                                    "\nВремя: " + app.getStartTime().format(TIME_FORMATTER)
                    );
                    log.info("Sent notification for appointment: {}", app.getId());
                }
            }
        });
    }

    private void cleanRedisSlot(Appointment app) {
        String slotKey = "slots:doctor:%s:date:%s"
                .formatted(
                        app.getDoctor().getId(),
                        app.getStartTime().toLocalDate()
                );

        String slotTime = "%s-%s".formatted(
                app.getStartTime().format(TIME_FORMATTER),
                app.getEndTime().format(TIME_FORMATTER)
        );

        redisTemplate.opsForHash().delete(slotKey, slotTime);
    }

    @Scheduled(cron = "0 0 3 * * ?") // Ежедневно в 3:00
    public void cleanupOldAppointments() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Appointment> oldAppointments = appointmentRepository
                .findByEndTimeBeforeAndStatus(weekAgo, AppointmentStatus.CONFIRMED);

        oldAppointments.forEach(this::cleanRedisSlot);
        log.info("Cleaned {} old appointments", oldAppointments.size());
    }
}