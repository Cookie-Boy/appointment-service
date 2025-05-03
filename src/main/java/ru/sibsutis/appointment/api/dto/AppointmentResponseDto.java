package ru.sibsutis.appointment.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        UUID clinicId,
        String clinicName,       // Дополнительные данные для удобства
        UUID doctorId,
        String doctorFullName,  // Дополнительные данные
        UUID patientId,
        String patientFullName, // Дополнительные данные
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Map<String, Object> metadata
) {}