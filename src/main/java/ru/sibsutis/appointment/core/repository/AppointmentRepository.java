package ru.sibsutis.appointment.core.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.sibsutis.appointment.core.model.Appointment;
import ru.sibsutis.appointment.core.model.AppointmentStatus;
import ru.sibsutis.appointment.core.model.TelegramUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findById(@Param("id") UUID id);

    Optional<Appointment> findByStartTime(LocalDateTime startTime);
    List<Appointment> findByTelegramUser(TelegramUser telegramUser);
    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Appointment> findByEndTimeBeforeAndStatus(LocalDateTime endTime, AppointmentStatus status);
}
