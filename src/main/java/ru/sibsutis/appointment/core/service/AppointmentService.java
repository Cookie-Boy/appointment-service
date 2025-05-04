package ru.sibsutis.appointment.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.sibsutis.appointment.api.dto.AppointmentRequestDto;
import ru.sibsutis.appointment.api.dto.AppointmentResponseDto;
import ru.sibsutis.appointment.api.dto.SuccessResponseDto;
import ru.sibsutis.appointment.api.mapper.AppointmentMapper;
import ru.sibsutis.appointment.core.exception.BookingException;
import ru.sibsutis.appointment.core.exception.SlotAlreadyBookedException;
import ru.sibsutis.appointment.core.model.*;
import ru.sibsutis.appointment.core.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TelegramUserRepository telegramUserRepository;

    private final AppointmentMapper appointmentMapper;

    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto) {
        Clinic clinic = clinicRepository.findById(dto.clinicId())
                .orElseThrow(() -> new EntityNotFoundException("Clinic not found"));

        Doctor doctor = doctorRepository.findById(dto.doctorId())
                .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));

        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        TelegramUser telegramUser = telegramUserRepository.findByUsername(dto.telegramUsername());

        LocalDate date = dto.startTime().toLocalDate();
        String slotTime = formatTimeSlot(dto.startTime(), dto.endTime());

        String slotKey = "slots:doctor:%s:date:%s".formatted(
                doctor.getId(),
                date
        );

        Boolean isSlotFree = redisTemplate.opsForHash().putIfAbsent(
                slotKey,
                slotTime,
                "locked"
        );

        if (Boolean.FALSE.equals(isSlotFree)) {
            throw new SlotAlreadyBookedException("На это время уже забронировано!");
        }

        try {
            Appointment appointment = appointmentMapper.toEntity(dto);
            appointment.setClinic(clinic);
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setTelegramUser(telegramUser);
            appointment.setStatus(AppointmentStatus.PENDING);

            appointment = appointmentRepository.save(appointment);

            redisTemplate.opsForHash().put(
                    slotKey,
                    slotTime,
                    appointment.getId().toString()
            );

            String loadKey = "doctor_load:%s".formatted(doctor.getId());
            redisTemplate.opsForZSet().incrementScore(loadKey, "total", 1);

            return appointmentMapper.toDto(appointment);

        } catch (Exception e) {
            redisTemplate.opsForHash().delete(slotKey, slotTime);
            throw new BookingException("Ошибка бронирования: " + e.getMessage());
        }
    }

    @Transactional
    public SuccessResponseDto cancelAppointment(UUID id) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронь не найдена"));

        app.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(app);
        redisTemplate.opsForHash().delete(
                "slots:doctor:" + app.getDoctor().getId() + ":date:" + app.getStartTime().toLocalDate(),
                formatTimeSlot(app.getStartTime(), app.getEndTime())
        );

        return new SuccessResponseDto(200, "Бронь успешно отменена");
    }

    private String formatTimeSlot(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "%s-%s".formatted(
                start.toLocalTime().format(formatter),
                end.toLocalTime().format(formatter)
        );
    }
}
