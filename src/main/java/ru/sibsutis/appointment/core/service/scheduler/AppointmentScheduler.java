package ru.sibsutis.appointment.core.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import ru.sibsutis.appointment.api.client.TelegramServiceClient;
import ru.sibsutis.appointment.core.exception.NotificationException;
import ru.sibsutis.appointment.core.model.Appointment;
import ru.sibsutis.appointment.core.model.AppointmentStatus;
import ru.sibsutis.appointment.core.repository.AppointmentRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final RedisTemplate<String, String> redisTemplate;
    private final AppointmentRepository appointmentRepository;
    private final TelegramServiceClient telegramServiceClient;

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

        for (Appointment app : appointments) {
            if (app.getStatus() == AppointmentStatus.CONFIRMED || app.getTgUserName() == null) return;

            String lockKey = "hourly_notification:" + app.getId();

            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "sent", 55, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(acquired)) {
                log.warn("Уведомление уже было отправлено (lock существует) для appointmentId: {}", app.getId());
                continue;
            }

            try {
                ResponseEntity<?> result = telegramServiceClient.sendNotification(
                        app.getTgUserName(),
                        "До вашего приёма остался 1 час ⏳" +
                                "\nВрач: " + app.getDoctor().getFirstName() + " " + app.getDoctor().getLastName() +
                                "\nВремя: " + app.getStartTime().format(TIME_FORMATTER));

                if (result.getStatusCode().is2xxSuccessful()) {
                    log.info("Уведомление успешно отправлено для chatId: {}", app.getTgUserName());
                } else {
                    throw new RestClientException("Ошибка отправки уведомления. Статус: " + result.getStatusCode()
                            + "Тело ответа: " + result.getBody());
                }

            } catch (RestClientException e) {
                log.error("Ошибка при отправке HTTP-запроса уведомления для chatId: {}",
                        app.getTgUserName(), e);
            } catch (Exception e) {
                log.error("Непредвиденная ошибка при отправке уведомления", e);
                throw new NotificationException("Ошибка отправки уведомления: " + e);
            }
        }
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

                if (app.getTgUserName() != null) {
                    telegramServiceClient.sendNotification(
                            app.getTgUserName(),
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