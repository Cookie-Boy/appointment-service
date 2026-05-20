package ru.sibsutis.appointment.api.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        UUID doctorId,
        String doctorFullName,
        String ownerId,
        String ownerFullName,
        String petId,
        String petFullName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Map<String, Object> metadata
) {}