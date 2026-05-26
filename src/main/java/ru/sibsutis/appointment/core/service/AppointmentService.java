package ru.sibsutis.appointment.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sibsutis.appointment.api.client.ExternalGateway;
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
    private final ExternalGateway externalGateway;
    private final SlotManager slotManager;
    private final DoctorLoadService doctorLoadService;

    @Transactional
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto) {
        ZoneId userZone = extractUserZone(dto);

        DoctorDto doctor;
        OffsetDateTime startTime;
        OffsetDateTime endTime;

        if (dto.doctorId() == null) {
            log.info("Doctor ID not provided, searching for best doctor and time.");
            if (dto.startTime() == null) {
                SlotSearchResult result = findAnyAvailableSlot(userZone);
                doctor = result.doctor;
                startTime = result.startTime;
                endTime = startTime.plusMinutes(30);
            } else {
                startTime = dto.startTime();
                endTime = dto.endTime();
                doctor = findBestDoctorForSlot(startTime, userZone);
            }
        } else {
            doctor = externalGateway.getDoctorById(dto.doctorId());
            if (dto.startTime() == null) {
                startTime = findNearestAvailableSlot(doctor, userZone);
                endTime = startTime.plusMinutes(30);
            } else {
                startTime = dto.startTime();
                endTime = dto.endTime();
            }
        }

        LocalDate date = startTime.toLocalDate();
        String timeSlot = formatTimeSlot(startTime, endTime);

        boolean booked = slotManager.isSlotFree(doctor.id(), date, timeSlot);
        if (!booked) {
            throw new SlotAlreadyBookedException("Это время уже занято другим пациентом.");
        }

        try {
            OwnerDto owner = externalGateway.getOwnerById(dto.ownerId());
            PetDto pet = externalGateway.getPetById(dto.petId());

            Appointment appointment = appointmentMapper.toEntity(dto);
            appointment.setDoctorId(doctor.id());
            appointment.setDoctorFullName(doctor.lastName() + " " + doctor.firstName());
            appointment.setOwnerFullName(owner.getLastName() + " " + owner.getFirstName());
            appointment.setPetFullName(pet.getName() + " (" + pet.getBreed() + ")");
            appointment.setStartTime(startTime);
            appointment.setEndTime(endTime);
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment = appointmentRepository.save(appointment);

            slotManager.tryBookSlot(doctor.id(), date, timeSlot, appointment.getId());
            doctorLoadService.incrementDailyLoad(doctor.id(), date);

            return appointmentMapper.toDto(appointment);

        } catch (Exception e) {
            slotManager.releaseSlot(doctor.id(), date, timeSlot);
            throw new BookingException("Ошибка бронирования: " + e.getMessage());
        }
    }

    @Transactional
    public SuccessResponseDto cancelAppointment(UUID id) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронь не найдена"));

        app.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(app);

        LocalDate date = app.getStartTime().toLocalDate();
        String timeSlot = formatTimeSlot(app.getStartTime(), app.getEndTime());
        slotManager.releaseSlot(app.getDoctorId(), date, timeSlot);
        doctorLoadService.decrementDailyLoad(app.getDoctorId(), date);

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
        List<DoctorDto> doctors = (doctorId != null && !doctorId.isEmpty())
                ? List.of(externalGateway.getDoctorById(UUID.fromString(doctorId)))
                : externalGateway.getAllDoctors();

        Set<TimeSlotDto> availableSlots = new HashSet<>();
        for (DoctorDto doctor : doctors) {
            Set<String> booked = slotManager.getBookedSlots(doctor.id(), date);
            LocalTime current = doctor.startWorkingDay();
            LocalTime end = doctor.endWorkingDay();

            while (current.isBefore(end.minusMinutes(30))) {
                String slot = formatTimeSlot(current, current.plusMinutes(30));
                if (!booked.contains(slot)) {
                    availableSlots.add(TimeSlotDto.builder()
                            .startTime(current.toString())
                            .endTime(current.plusMinutes(30).toString())
                            .build());
                }
                current = current.plusMinutes(30);
            }
        }
        return availableSlots.stream()
                .sorted(Comparator.comparing(TimeSlotDto::getStartTime))
                .collect(Collectors.toList());
    }

    private ZoneId extractUserZone(AppointmentRequestDto dto) {
        if (dto.metadata() != null && dto.metadata().get("originalTimezone") != null) {
            try {
                return ZoneId.of(dto.metadata().get("originalTimezone").toString());
            } catch (Exception e) {
                log.warn("Invalid timezone in metadata, using system default");
            }
        }
        return ZoneId.systemDefault();
    }

    private DoctorDto findBestDoctorForSlot(OffsetDateTime desiredStartTime, ZoneId userZone) {
        LocalDate targetDate = desiredStartTime.toLocalDate();
        String timeSlot = formatTimeSlot(desiredStartTime, desiredStartTime.plusMinutes(30));

        List<DoctorDto> doctors = externalGateway.getAllDoctors();
        DoctorDto bestDoctor = null;
        long minLoad = Long.MAX_VALUE;

        for (DoctorDto doctor : doctors) {
            // Проверяем, работает ли врач в это время
            LocalTime desiredLocal = desiredStartTime.atZoneSameInstant(userZone).toLocalTime();
            if (desiredLocal.isBefore(doctor.startWorkingDay()) || desiredLocal.isAfter(doctor.endWorkingDay().minusMinutes(30))) {
                continue;
            }

            if (slotManager.isSlotFree(doctor.id(), targetDate, timeSlot)) {
                long load = doctorLoadService.getDailyLoad(doctor.id(), targetDate);
                if (load < minLoad) {
                    minLoad = load;
                    bestDoctor = doctor;
                }
            }
        }

        if (bestDoctor == null) {
            throw new EntityNotFoundException("Нет свободных врачей на указанное время.");
        }
        return bestDoctor;
    }

    private SlotSearchResult findAnyAvailableSlot(ZoneId userZone) {
        ZonedDateTime now = ZonedDateTime.now(userZone);
        List<DoctorDto> doctors = externalGateway.getAllDoctors();

        for (int i = 0; i < 14; i++) {
            LocalDate candidateDate = now.toLocalDate().plusDays(i);
            for (DoctorDto doctor : doctors) {
                ZonedDateTime startOfDay = candidateDate.atTime(doctor.startWorkingDay()).atZone(userZone);
                ZonedDateTime endOfDay = candidateDate.atTime(doctor.endWorkingDay()).atZone(userZone);
                ZonedDateTime current = (i == 0) ? startOfDay.isBefore(now) ? now.withSecond(0).withNano(0) : startOfDay : startOfDay;

                while (current.isBefore(endOfDay.minusMinutes(30))) {
                    String timeSlot = formatTimeSlot(current.toLocalTime(), current.plusMinutes(30).toLocalTime());
                    if (slotManager.isSlotFree(doctor.id(), candidateDate, timeSlot)) {
                        return new SlotSearchResult(doctor, current.toOffsetDateTime());
                    }
                    current = current.plusMinutes(30);
                }
            }
        }
        throw new EntityNotFoundException("Нет свободных слотов в ближайшие 2 недели.");
    }

    private OffsetDateTime findNearestAvailableSlot(DoctorDto doctor, ZoneId userZone) {
        ZonedDateTime nowInUserZone = ZonedDateTime.now(userZone);
        LocalDate today = nowInUserZone.toLocalDate();

        for (int i = 0; i < 14; i++) {
            LocalDate candidateDate = today.plusDays(i);

            ZonedDateTime startOfWork = candidateDate.atTime(doctor.startWorkingDay()).atZone(userZone);
            ZonedDateTime endOfWork = candidateDate.atTime(doctor.endWorkingDay()).atZone(userZone);

            ZonedDateTime startSearch;
            if (i == 0) {
                LocalTime currentTime = nowInUserZone.toLocalTime();
                LocalTime rounded;
                if (currentTime.getMinute() <= 30) {
                    rounded = currentTime.withMinute(30).withSecond(0).withNano(0);
                } else {
                    rounded = currentTime.withHour(currentTime.getHour() + 1).withMinute(0).withSecond(0).withNano(0);
                }
                startSearch = nowInUserZone.with(rounded);
                if (startSearch.isBefore(startOfWork)) {
                    startSearch = startOfWork;
                }
            } else {
                startSearch = startOfWork;
            }

            ZonedDateTime current = startSearch;
            while (current.isBefore(endOfWork)) {
                LocalTime slotStart = current.toLocalTime();
                LocalTime slotEnd = slotStart.plusMinutes(30);
                String timeSlot = formatTimeSlot(slotStart, slotEnd);

                if (slotManager.isSlotFree(doctor.id(), candidateDate, timeSlot)) {
                    return current.toOffsetDateTime();
                }
                current = current.plusMinutes(30);
            }
        }
        throw new EntityNotFoundException("Нет свободных слотов для врача " + doctor.id() + " на ближайшие 2 недели.");
    }

    private String formatTimeSlot(OffsetDateTime start, OffsetDateTime end) {
        return formatTimeSlot(start.toLocalTime(), end.toLocalTime());
    }

    private String formatTimeSlot(LocalTime start, LocalTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + "-" + end.format(formatter);
    }

    private record SlotSearchResult(DoctorDto doctor, OffsetDateTime startTime) {
    }
}