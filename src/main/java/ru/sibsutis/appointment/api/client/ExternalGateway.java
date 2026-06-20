package ru.sibsutis.appointment.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.sibsutis.appointment.api.dto.ClinicDto;
import ru.sibsutis.appointment.api.dto.DoctorDto;
import ru.sibsutis.appointment.api.dto.OwnerDto;
import ru.sibsutis.appointment.api.dto.PetDto;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalGateway {
    private final BotServiceClient botServiceClient;
    private final ManagementServiceClient managementServiceClient;
    private final ProfileServiceClient profileServiceClient;

    public ResponseEntity<?> sendNotification(Long userId, String text) {
        return botServiceClient.sendNotification(userId, text);
    }

    public DoctorDto getDoctorById(UUID doctorId) {
        return managementServiceClient.getDoctorById(doctorId);
    }

    public List<DoctorDto> getAllDoctors() {
        return managementServiceClient.getAllDoctors();
    }

    public ClinicDto getClinicById(UUID doctorId) {
        return managementServiceClient.getClinicById(doctorId);
    }

    public List<ClinicDto> getAllClinics() {
        return managementServiceClient.getAllClinics();
    }

    public OwnerDto getOwnerById(String ownerId) {
        return profileServiceClient.getOwnerById(ownerId);
    }

    public PetDto getPetById(String petId) {
        return profileServiceClient.getPetById(petId);
    }

    public List<OwnerDto> getAllOwners() {
        return profileServiceClient.getAllOwners();
    }
}
