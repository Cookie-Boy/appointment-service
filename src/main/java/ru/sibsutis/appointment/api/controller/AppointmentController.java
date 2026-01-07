package ru.sibsutis.appointment.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.api.dto.SuccessResponseDto;
import ru.sibsutis.appointment.core.service.AppointmentService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/appointment")
    public ResponseEntity<AppointmentResponseDto> createAppointment(@RequestBody AppointmentRequestDto dto) {
        AppointmentResponseDto response = appointmentService.bookAppointment(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointment/{patientId}")
    public ResponseEntity<List<AppointmentResponseDto>> getPatientAppointments(@PathVariable UUID patientId) {
        List<AppointmentResponseDto> response = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointment/{tgUserId}")
    public ResponseEntity<List<AppointmentResponseDto>> getTgUserAppointments(@PathVariable UUID tgUserId) {
        List<AppointmentResponseDto> response = appointmentService.getTgUserAppointments(tgUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/appointment/{id}")
    public ResponseEntity<SuccessResponseDto> cancelAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
