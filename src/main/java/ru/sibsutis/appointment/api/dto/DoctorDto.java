package ru.sibsutis.appointment.api.dto;

import lombok.Builder;
import ru.sibsutis.appointment.core.model.Specialization;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record DoctorDto(
        UUID id,
        String firstName,
        String lastName,
        String middleName,
        Specialization specialization,
        String licenseNumber,
        String phoneNumber,
        String email,
        LocalDate hireDate,
        LocalTime startWorkingDay,
        LocalTime endWorkingDay,
        String bio
) {
}