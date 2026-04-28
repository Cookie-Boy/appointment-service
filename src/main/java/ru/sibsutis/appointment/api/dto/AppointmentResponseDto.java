package ru.sibsutis.appointment.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        UUID doctorId,
        UUID ownerId,
        UUID petId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Map<String, Object> metadata
) {}