package ru.sibsutis.appointment.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.core.model.Appointment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    Appointment toEntity(AppointmentRequestDto dto);
    AppointmentResponseDto toDto(Appointment appointment);
    List<AppointmentResponseDto> toDto(List<Appointment> appointments);
}
