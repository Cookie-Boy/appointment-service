package ru.sibsutis.appointment.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentRequestDto(
        UUID id,
        UUID clinicId,
        UUID doctorId,
        UUID patientId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Map<String, Object> metadata
) {}