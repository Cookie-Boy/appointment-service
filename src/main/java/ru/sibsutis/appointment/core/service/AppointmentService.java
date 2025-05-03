package ru.sibsutis.appointment.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
//import ru.sibsutis.appointment.api.mapper.AppointmentMapper;
//import ru.sibsutis.appointment.core.model.Appointment;
//import ru.sibsutis.appointment.core.repository.AppointmentRepository;

@Service
@RequiredArgsConstructor
public class AppointmentService {

//    private final AppointmentRepository appointmentRepository;
//    private final AppointmentMapper appointmentMapper;

    public AppointmentResponseDto createAppointment(AppointmentRequestDto dto) {
//        Appointment appointment = appointmentMapper.toEntity(dto);
//        appointment = appointmentRepository.save(appointment);
//        return appointmentMapper.toDto(appointment);
        return null;
    }
}
