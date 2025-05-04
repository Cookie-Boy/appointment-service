package ru.sibsutis.appointment.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
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

    private final RestTemplate restTemplate;

    @PostMapping("/webhook")
    public ResponseEntity<?> forwardToJetty(@RequestBody String payload,
                                            HttpHeaders headers) {
        String jettyUrl = "http://localhost:9091/webhook";

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(jettyUrl, request, String.class);
    }

    @PostMapping("/appointment")
    public ResponseEntity<AppointmentResponseDto> createAppointment(@RequestBody AppointmentRequestDto dto) {
        AppointmentResponseDto response = appointmentService.bookAppointment(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointment/{patientId}")
    public ResponseEntity<List<AppointmentResponseDto>> getAllAppointments(@PathVariable UUID patientId) {
        List<AppointmentResponseDto> response = appointmentService.getAllAppointments(patientId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/appointment/{id}")
    public ResponseEntity<SuccessResponseDto> cancelAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
