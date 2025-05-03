package ru.sibsutis.appointment.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sibsutis.appointment.core.model.Clinic;

import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
}