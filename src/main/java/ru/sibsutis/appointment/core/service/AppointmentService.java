package ru.sibsutis.appointment.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.sibsutis.appointment.api.client.ManagementServiceClient;
import ru.sibsutis.appointment.api.client.ProfileServiceClient;
import ru.sibsutis.appointment.api.dto.*;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    private final ProfileServiceClient profileServiceClient;
    private final ManagementServiceClient managementServiceClient;

    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto) {
        DoctorDto doctor;
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (dto.doctorId() == null) {
            log.info("Doctor ID is null -> try to find optimal doctor.");
            doctor = findOptimalDoctor();
        } else {
            doctor = managementServiceClient.getDoctorById(dto.doctorId());
        }

        if (dto.startTime() == null) {
            log.info("Start time is null -> try to find nearest available time.");
            startTime = findNearestAvailableSlot(doctor);
            endTime = startTime.plusMinutes(30);
        } else {
            startTime = dto.startTime();
            endTime = dto.endTime();
        }

        OwnerDto owner = profileServiceClient.getOwnerById(dto.ownerId());

        LocalDate date = startTime.toLocalDate();
        String preferredTime = formatTimeSlot(startTime, endTime);

        String slotKey = "slots:doctor:%s:date:%s".formatted(
                doctor.id(),
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
            appointment.setDoctorId(doctor.id());
            appointment.setOwnerId(owner.getId());
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setTgUserName(dto.tgUserName());
            appointment.setStatus(AppointmentStatus.PENDING);

            appointment = appointmentRepository.save(appointment);

            redisTemplate.opsForHash().put(
                    slotKey,
                    preferredTime,
                    appointment.getId().toString()
            );

            String loadKey = "doctor_load:%s".formatted(doctor.id());
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

        log.info("App startTime: {}, endTime: {}", app.getStartTime(), app.getEndTime());

        app.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(app);

        redisTemplate.opsForHash().delete(
                "slots:doctor:" + app.getDoctorId() + ":date:" + app.getStartTime().toLocalDate(),
                formatTimeSlot(app.getStartTime(), app.getEndTime())
        );
        String loadKey = "doctor_load:" + app.getDoctorId();
        redisTemplate.opsForZSet().incrementScore(loadKey, "total", -1);

        return new SuccessResponseDto(200, "Бронь успешно отменена");
    }

    public List<AppointmentResponseDto> getOwnerAppointments(String ownerId) {
        List<Appointment> appointments = appointmentRepository.findByOwnerId(ownerId);
        return appointmentMapper.toDto(appointments);
    }

    public List<AppointmentResponseDto> getTgUserAppointments(String tgUserName) {
        List<Appointment> appointments = appointmentRepository.findByTgUserName(tgUserName);
        return appointmentMapper.toDto(appointments);
    }

    public List<TimeSlotDto> getAvailableSlots(String doctorId, LocalDate date) {
        log.info("Inside service method");
        List<TimeSlotDto> availableSlots = new ArrayList<>();

        List<DoctorDto> doctorsToCheck;
        if (doctorId != null && !doctorId.isEmpty()) {
            UUID doctorUUID = UUID.fromString(doctorId);
            DoctorDto doctor = managementServiceClient.getDoctorById(doctorUUID);
            doctorsToCheck = List.of(doctor);
        } else {
            doctorsToCheck = managementServiceClient.getAllDoctors();
        }

        for (DoctorDto doctor : doctorsToCheck) {
            log.info("Doctor: {}", doctor.id());
            String slotKey = "slots:doctor:%s:date:%s".formatted(doctor.id(), date);

            Map<Object, Object> bookedSlots = redisTemplate.opsForHash().entries(slotKey);
            Set<String> bookedTimeSlots = bookedSlots.keySet().stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());

            LocalTime currentSlot = doctor.startWorkingDay();
            LocalTime endOfDay = doctor.endWorkingDay();

            while (!currentSlot.isAfter(endOfDay.minusMinutes(30))) {
                String slotTime = formatTimeSlot(currentSlot, currentSlot.plusMinutes(30));

                if (!bookedTimeSlots.contains(slotTime)) {
                    availableSlots.add(TimeSlotDto.builder()
                            .startTime(currentSlot.toString())
                            .endTime(currentSlot.plusMinutes(30).toString())
                            .build());
                }

                currentSlot = currentSlot.plusMinutes(30);
            }
        }

        availableSlots.sort(Comparator.comparing(TimeSlotDto::getStartTime));
        return availableSlots;
    }

    private DoctorDto findOptimalDoctor() {
        List<DoctorDto> doctors = managementServiceClient.getAllDoctors();

        return doctors.stream()
                .min((d1, d2) -> {
                    Double load1 = redisTemplate.opsForZSet().score("doctor_load:" + d1.id(), "total");
                    Double load2 = redisTemplate.opsForZSet().score("doctor_load:" + d2.id(), "total");
                    return Double.compare(load1 != null ? load1 : 0, load2 != null ? load2 : 0);
                })
                .orElseThrow(() -> new EntityNotFoundException("Нет доступных врачей"));
    }

    private LocalDateTime findNearestAvailableSlot(DoctorDto doctor) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 14; i++) {
            LocalDateTime date = adjustDateTime(now.plusDays(i), doctor, i == 0);

            String slotKey = "slots:doctor:" + doctor.id() + ":date:" + date.toLocalDate();
            LocalTime tempTime = date.toLocalTime();

            while (!tempTime.isBefore(doctor.endWorkingDay())) {
                String slotTime = formatTimeSlot(tempTime, tempTime.plusMinutes(30));
                String status = (String) redisTemplate.opsForHash().get(slotKey, slotTime);

                if (status == null) {
                    return tempTime.atDate(date.toLocalDate());
                }
                tempTime = tempTime.plusMinutes(30);
            }
        }

        throw new EntityNotFoundException("Нет свободных слотов на ближайшие 2 недели.");
    }

     private LocalDateTime adjustDateTime(LocalDateTime date, DoctorDto doctor, boolean isCurrentDay) {
        if (isCurrentDay) {
            return date.withMinute(date.getMinute() <= 30 ? 30 : 0)
                    .withSecond(0)
                    .withNano(0)
                    .plusHours(date.getMinute() > 30 ? 1 : 0);
        }
        return date.with(doctor.startWorkingDay());
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
