package ru.sibsutis.appointment.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorLoadService {

    private final StringRedisTemplate redisTemplate;
    private static final String DAILY_LOAD_KEY = "doctor:daily:load:%s:%s";

    public void incrementDailyLoad(UUID doctorId, LocalDate date) {
        String key = String.format(DAILY_LOAD_KEY, doctorId, date);
        redisTemplate.opsForValue().increment(key);
    }

    public void decrementDailyLoad(UUID doctorId, LocalDate date) {
        String key = String.format(DAILY_LOAD_KEY, doctorId, date);
        Long current = redisTemplate.opsForValue().decrement(key);
        if (current != null && current < 0) {
            redisTemplate.delete(key);
        }
    }

    public long getDailyLoad(UUID doctorId, LocalDate date) {
        String key = String.format(DAILY_LOAD_KEY, doctorId, date);
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0 : Long.parseLong(val);
    }
}