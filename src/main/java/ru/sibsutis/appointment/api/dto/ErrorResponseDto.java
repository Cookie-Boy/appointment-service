package ru.sibsutis.appointment.api.dto;

public record ErrorResponseDto(
        int code,
        String message
) {}
