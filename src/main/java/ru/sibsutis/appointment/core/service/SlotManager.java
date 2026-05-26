package ru.sibsutis.appointment.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlotManager {

    private final StringRedisTemplate redisTemplate;
    private static final String SLOT_KEY_PREFIX = "slot:doctor:%s:date:%s";

    public boolean tryBookSlot(UUID doctorId, LocalDate date, String timeSlot, UUID appointmentId) {
        String key = String.format(SLOT_KEY_PREFIX, doctorId, date);
        Boolean success = redisTemplate.opsForHash().putIfAbsent(key, timeSlot, appointmentId.toString());
        return Boolean.TRUE.equals(success);
    }

    public void releaseSlot(UUID doctorId, LocalDate date, String timeSlot) {
        String key = String.format(SLOT_KEY_PREFIX, doctorId, date);
        redisTemplate.opsForHash().delete(key, timeSlot);
    }

    public Set<String> getBookedSlots(UUID doctorId, LocalDate date) {
        String key = String.format(SLOT_KEY_PREFIX, doctorId, date);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null) return Collections.emptySet();
        return entries.keySet().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public boolean isSlotFree(UUID doctorId, LocalDate date, String timeSlot) {
        String key = String.format(SLOT_KEY_PREFIX, doctorId, date);
        return Boolean.FALSE.equals(redisTemplate.opsForHash().hasKey(key, timeSlot));
    }
}