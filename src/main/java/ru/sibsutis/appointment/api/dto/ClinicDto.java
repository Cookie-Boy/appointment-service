package ru.sibsutis.appointment.api.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClinicDto (
        UUID id,
        String name,
        String address,
        String phone
) {
}
