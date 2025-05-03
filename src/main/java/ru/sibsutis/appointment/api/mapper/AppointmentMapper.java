package ru.sibsutis.appointment.api.mapper;

import org.mapstruct.Mapper;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.core.model.Appointment;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    Appointment toEntity(AppointmentRequestDto dto);
    AppointmentResponseDto toDto(Appointment appointment);
}
