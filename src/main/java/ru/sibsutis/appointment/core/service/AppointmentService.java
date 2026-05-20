package ru.sibsutis.appointment.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.sibsutis.appointment.api.client.ManagementServiceClient;
import ru.sibsutis.appointment.api.client.ProfileServiceClient;
import ru.sibsutis.appointment.api.dto.*;
import ru.sibsutis.appointment.api.mapper.AppointmentMapper;
import ru.sibsutis.appointment.core.exception.BookingException;
import ru.sibsutis.appointment.core.exception.SlotAlreadyBookedException;
import ru.sibsutis.appointment.core.model.*;
import ru.sibsutis.appointment.core.repository.AppointmentRepository;

import java.time.*;
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
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto) {
        DoctorDto doctor;
        OffsetDateTime startTime;
        OffsetDateTime endTime;

        if (dto.doctorId() == null) {
            log.info("Doctor ID is null -> try to find optimal doctor.");
            doctor = findOptimalDoctor();
        } else {
            doctor = managementServiceClient.getDoctorById(dto.doctorId());
        }

        // Определяем часовой пояс пользователя из метаданных
        ZoneId userZone = ZoneId.systemDefault();
        if (dto.metadata() != null && dto.metadata().get("originalTimezone") != null) {
            try {
                userZone = ZoneId.of(dto.metadata().get("originalTimezone").toString());
            } catch (Exception e) {
                log.warn("Invalid timezone in metadata, using system default: {}", e.getMessage());
            }
        }

        if (dto.startTime() == null) {
            log.info("Start time is null -> try to find nearest available time.");
            startTime = findNearestAvailableSlot(doctor, userZone);
            endTime = startTime.plusMinutes(30);
        } else {
            startTime = dto.startTime();
            endTime = dto.endTime();
        }

        LocalDate date = startTime.toLocalDate();
        String preferredTime = formatTimeSlot(startTime, endTime);

        String slotKey = "slots:doctor:%s:date:%s".formatted(doctor.id(), date);

        Boolean isSlotFree = redisTemplate.opsForHash().putIfAbsent(slotKey, preferredTime, "locked");
        if (Boolean.FALSE.equals(isSlotFree)) {
            redisTemplate.opsForHash().delete(slotKey, preferredTime);
            throw new SlotAlreadyBookedException("На это время уже забронировано!");
        }

        try {
            OwnerDto owner = profileServiceClient.getOwnerById(dto.ownerId());
            String ownerFullName = owner.getLastName() + " " + owner.getFirstName();

            PetDto pet = profileServiceClient.getPetById(dto.petId());
            String petFullName = String.format("%s (%s)", pet.getName(), pet.getBreed());

            String doctorFullName = doctor.lastName() + " " + doctor.firstName();

            Appointment appointment = appointmentMapper.toEntity(dto);
            appointment.setDoctorFullName(doctorFullName);
            appointment.setOwnerFullName(ownerFullName);
            appointment.setPetFullName(petFullName);
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment = appointmentRepository.save(appointment);

            redisTemplate.opsForHash().put(slotKey, preferredTime, appointment.getId().toString());

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

    public List<AppointmentResponseDto> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointmentMapper.toDto(appointments);
    }

    public List<AppointmentResponseDto> getOwnerAppointments(String ownerId) {
        List<Appointment> appointments = appointmentRepository.findByOwnerId(ownerId);
        return appointmentMapper.toDto(appointments);
    }

    public List<AppointmentResponseDto> getDoctorAppointments(UUID doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        return appointmentMapper.toDto(appointments);
    }

    public List<TimeSlotDto> getAvailableSlots(String doctorId, LocalDate date) {
        log.info("Inside service method");
        Set<TimeSlotDto> availableSlots = new HashSet<>();

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

            // Исправлено условие: пока текущий слот не достиг конца дня с учётом 30-минутных интервалов
            while (currentSlot.isBefore(endOfDay.minusMinutes(30))) {
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

        return availableSlots.stream()
                .sorted(Comparator.comparing(TimeSlotDto::getStartTime))
                .toList();
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

    private OffsetDateTime findNearestAvailableSlot(DoctorDto doctor, ZoneId userZone) {
        // Текущий момент в зоне пользователя
        ZonedDateTime nowInUserZone = ZonedDateTime.now(userZone);
        LocalDate today = nowInUserZone.toLocalDate();

        for (int i = 0; i < 14; i++) {
            LocalDate candidateDate = today.plusDays(i);

            LocalDateTime startOfDayLocal = candidateDate.atTime(doctor.startWorkingDay());
            ZonedDateTime startOfWork = startOfDayLocal.atZone(userZone);

            ZonedDateTime startSearch;
            if (i == 0) {
                LocalTime rounded = nowInUserZone.toLocalTime();
                int minute = rounded.getMinute();
                if (minute <= 30) {
                    rounded = rounded.withMinute(30).withSecond(0).withNano(0);
                } else {
                    rounded = rounded.withHour(rounded.getHour() + 1).withMinute(0).withSecond(0).withNano(0);
                }
                startSearch = nowInUserZone.with(rounded);
                if (startSearch.isBefore(startOfWork)) {
                    startSearch = startOfWork;
                }
            } else {
                startSearch = startOfWork;
            }

            ZonedDateTime endOfWork = candidateDate.atTime(doctor.endWorkingDay()).atZone(userZone);
            ZonedDateTime current = startSearch;

            String slotKey = "slots:doctor:" + doctor.id() + ":date:" + candidateDate;
            while (current.isBefore(endOfWork)) {
                LocalTime startTime = current.toLocalTime();
                LocalTime endTime = startTime.plusMinutes(30);
                String slotTime = formatTimeSlot(startTime, endTime);
                String status = (String) redisTemplate.opsForHash().get(slotKey, slotTime);

                if (status == null) {
                    return current.toOffsetDateTime();
                }
                current = current.plusMinutes(30);
            }
        }
        throw new EntityNotFoundException("Нет свободных слотов на ближайшие 2 недели.");
    }

    private String formatTimeSlot(OffsetDateTime start, OffsetDateTime end) {
        return formatTimeSlot(start.toLocalTime(), end.toLocalTime());
    }

    private String formatTimeSlot(LocalDateTime start, LocalDateTime end) {
        return formatTimeSlot(start.toLocalTime(), end.toLocalTime());
    }

    private String formatTimeSlot(LocalTime start, LocalTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "%s-%s".formatted(start.format(formatter), end.format(formatter));
    }
}