package ru.sibsutis.appointment.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
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

        Doctor doctor;
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (dto.doctorId() == null) {
            log.info("Doctor ID is null -> try to find optimal doctor.");
            doctor = findOptimalDoctor(clinic.getId());
        } else {
            doctor = doctorRepository.findById(dto.doctorId())
                    .orElseThrow(() -> new EntityNotFoundException("Doctor not found"));
        }

        if (dto.startTime() == null) {
            log.info("Start time is null -> try to find nearest available time.");
            startTime = findNearestAvailableSlot(doctor);
            endTime = startTime.plusMinutes(30);
        } else {
            startTime = dto.startTime();
            endTime = dto.endTime();
        }

        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        TelegramUser telegramUser = telegramUserRepository.findByUsername(dto.telegramUsername());

        LocalDate date = startTime.toLocalDate();
        String preferredTime = formatTimeSlot(startTime, endTime);

        String slotKey = "slots:doctor:%s:date:%s".formatted(
                doctor.getId(),
                date
        );

        Boolean isSlotFree = redisTemplate.opsForHash().putIfAbsent(
                slotKey,
                preferredTime,
                "locked"
        );

        if (Boolean.FALSE.equals(isSlotFree)) {
            redisTemplate.opsForHash().delete(slotKey, preferredTime);
            throw new SlotAlreadyBookedException("На это время уже забронировано!");
        }

        try {
            Appointment appointment = appointmentMapper.toEntity(dto);
            appointment.setClinic(clinic);
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setTelegramUser(telegramUser);
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setStatus(AppointmentStatus.PENDING);

            appointment = appointmentRepository.save(appointment);

            redisTemplate.opsForHash().put(
                    slotKey,
                    preferredTime,
                    appointment.getId().toString()
            );

            String loadKey = "doctor_load:%s".formatted(doctor.getId());
            redisTemplate.opsForZSet().incrementScore(loadKey, "total", 1);

            return appointmentMapper.toDto(appointment);

        } catch (Exception e) {
            redisTemplate.opsForHash().delete(slotKey, preferredTime);
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
        String loadKey = "doctor_load:" + app.getDoctor().getId();
        redisTemplate.opsForZSet().incrementScore(loadKey, "total", -1);

        return new SuccessResponseDto(200, "Бронь успешно отменена");
    }

    public Doctor findOptimalDoctor(UUID clinicId) {
        List<Doctor> doctors = doctorRepository.findAllByClinicId(clinicId);

        return doctors.stream()
                .min((d1, d2) -> {
                    Double load1 = redisTemplate.opsForZSet().score("doctor_load:" + d1.getId(), "total");
                    Double load2 = redisTemplate.opsForZSet().score("doctor_load:" + d2.getId(), "total");
                    return Double.compare(load1 != null ? load1 : 0, load2 != null ? load2 : 0);
                })
                .orElseThrow(() -> new EntityNotFoundException("Нет доступных врачей"));
    }

    public LocalDateTime findNearestAvailableSlot(Doctor doctor) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 14; i++) {
            LocalDateTime date = adjustDateTime(now.plusDays(i), doctor, i == 0);

            String slotKey = "slots:doctor:" + doctor.getId() + ":date:" + date.toLocalDate();
            LocalTime tempTime = date.toLocalTime();

            while (!tempTime.isBefore(doctor.getEndTime())) {
                String slotTime = formatTimeSlot(tempTime, tempTime.plusMinutes(30));
                String status = (String) redisTemplate.opsForHash().get(slotKey, slotTime);

                if (status == null) {
                    LocalDateTime slotDateTime = tempTime.atDate(date.toLocalDate());

                    boolean isSlotFreeInDb = appointmentRepository.findByStartTime(slotDateTime).isEmpty();

                    if (isSlotFreeInDb) {
                        return slotDateTime;
                    } else {
                        // Обновляем Redis, если данные устарели
                        redisTemplate.opsForHash().put(slotKey, slotTime, "locked");
                    }
                }
                tempTime = tempTime.plusMinutes(30);
            }
        }

        throw new EntityNotFoundException("Нет свободных слотов на ближайшие 2 недели.");
    }

     private LocalDateTime adjustDateTime(LocalDateTime date, Doctor doctor, boolean isCurrentDay) {
        if (isCurrentDay) {
            return date.withMinute(date.getMinute() <= 30 ? 30 : 0)
                    .withSecond(0)
                    .withNano(0)
                    .plusHours(date.getMinute() > 30 ? 1 : 0);
        }
        return date.with(doctor.getStartTime());
    }

    private String formatTimeSlot(LocalDateTime start, LocalDateTime end) {
        return formatTimeSlot(start.toLocalTime(), end.toLocalTime());
    }

    private String formatTimeSlot(LocalTime start, LocalTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "%s-%s".formatted(
                start.format(formatter),
                end.format(formatter)
        );
    }
}
