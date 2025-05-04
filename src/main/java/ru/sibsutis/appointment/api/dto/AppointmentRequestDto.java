package ru.sibsutis.appointment.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentRequestDto(
        UUID clinicId,
        UUID doctorId,
        UUID patientId,
        String telegramUsername,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Map<String, Object> metadata
) {}