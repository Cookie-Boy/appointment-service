package ru.sibsutis.appointment.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentRequestDto(
        UUID doctorId,
        String ownerId,
        String petId,
        String tgUserName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Map<String, Object> metadata
) {}