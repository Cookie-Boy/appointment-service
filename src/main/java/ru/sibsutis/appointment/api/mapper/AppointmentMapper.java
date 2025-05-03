package ru.sibsutis.appointment.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.core.model.Appointment;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    Appointment toEntity(AppointmentRequestDto dto);

    @Mapping(source = "clinic.id", target = "clinicId")
    @Mapping(source = "clinic.name", target = "clinicName")
    @Mapping(source = "doctor.id", target = "doctorId")
    @Mapping(
            expression = "java(appointment.getDoctor().getFirstName() + \" \" + appointment.getDoctor().getLastName())",
            target = "doctorFullName"
    )
    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(
            expression = "java(appointment.getPatient().getFirstName() + \" \" + appointment.getPatient().getLastName())",
            target = "patientFullName"
    )
    AppointmentResponseDto toDto(Appointment appointment);
}
