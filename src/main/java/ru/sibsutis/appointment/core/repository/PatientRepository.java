package ru.sibsutis.appointment.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sibsutis.appointment.core.model.Patient;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
}