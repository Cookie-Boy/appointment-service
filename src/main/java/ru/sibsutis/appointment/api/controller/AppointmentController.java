package ru.sibsutis.appointment.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.api.dto.SuccessResponseDto;
import ru.sibsutis.appointment.api.dto.TimeSlotDto;
import ru.sibsutis.appointment.core.service.AppointmentService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
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

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<AppointmentResponseDto>> getOwnerAppointments(@PathVariable String ownerId) {
        List<AppointmentResponseDto> response = appointmentService.getOwnerAppointments(ownerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tgUser/{tgUserName}")
    public ResponseEntity<List<AppointmentResponseDto>> getTgUserAppointments(@PathVariable String tgUserName) {
        List<AppointmentResponseDto> response = appointmentService.getTgUserAppointments(tgUserName);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponseDto> cancelAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @RequestParam(required = false) String doctorId,
            @RequestParam String date
    ) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Date parameter is required");
        }

        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd");
        }

        List<TimeSlotDto> availableSlots = appointmentService.getAvailableSlots(doctorId, requestedDate);
        return ResponseEntity.ok(availableSlots);
    }
}