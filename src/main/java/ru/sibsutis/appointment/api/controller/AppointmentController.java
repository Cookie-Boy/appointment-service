package ru.sibsutis.appointment.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.core.service.AppointmentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createAppointment(@RequestBody AppointmentRequestDto dto) {
        AppointmentResponseDto response = appointmentService.bookAppointment(dto);
        return ResponseEntity.ok(response);
    }
}
