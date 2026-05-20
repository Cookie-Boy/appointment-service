package ru.sibsutis.appointment.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentRequestDto(
        UUID doctorId,
        String ownerId,
        String petId,
        String tgUserName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Map<String, Object> metadata
) {}