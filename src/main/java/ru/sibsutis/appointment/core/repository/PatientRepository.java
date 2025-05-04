package ru.sibsutis.appointment.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sibsutis.appointment.core.model.Patient;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
}