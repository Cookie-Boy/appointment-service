package ru.sibsutis.appointment.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.api.dto.SuccessResponseDto;
import ru.sibsutis.appointment.core.service.AppointmentService;

import java.util.UUID;

@Slf4j
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

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponseDto> cancelAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
