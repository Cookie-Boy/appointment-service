package ru.sibsutis.appointment.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sibsutis.appointment.api.dto.ClinicDto;
import ru.sibsutis.appointment.api.dto.DoctorDto;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManagementServiceClient {
    private final RestClient restClient;
    private final TokenProvider tokenProvider;

    public DoctorDto getDoctorById(UUID doctorId) {
        String token = tokenProvider.getFreshToken();
        log.info("Fresh token for calling management-service: {}", token);
        return restClient.get()
                .uri("/api/management/doctors/{doctorId}", doctorId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<DoctorDto> getAllDoctors() {
        String token = tokenProvider.getFreshToken();
        return restClient.get()
                .uri("/api/management/doctors")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<DoctorDto> getDoctorsByClinic(UUID clinicId) {
        String token = tokenProvider.getFreshToken();
        return restClient.get()
                .uri("/api/management/doctors/clinic/{clinicId}", clinicId.toString())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ClinicDto getClinicById(UUID clinicId) {
        String token = tokenProvider.getFreshToken();
        log.info("Trying to fetch clinic from Management-service...");
        return restClient.get()
                .uri("/api/management/clinics/{clinicId}", clinicId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<ClinicDto> getAllClinics() {
        String token = tokenProvider.getFreshToken();
        return restClient.get()
                .uri("/api/management/clinics")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
