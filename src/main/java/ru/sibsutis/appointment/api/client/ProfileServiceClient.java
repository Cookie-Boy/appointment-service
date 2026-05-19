package ru.sibsutis.appointment.api.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.sibsutis.appointment.api.dto.OwnerDto;
import ru.sibsutis.appointment.api.dto.PetDto;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileServiceClient {
    private final RestClient restClient;
    private final TokenProvider tokenProvider;

    public OwnerDto getOwnerById(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId cannot be null or empty");
        }

        String token = tokenProvider.getFreshToken();
        return restClient.get()
                .uri("/api/profile/owners/{ownerId}", ownerId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public PetDto getPetById(String petId) {
        if (petId == null || petId.isBlank()) {
            throw new IllegalArgumentException("petId cannot be null or empty");
        }

        String token = tokenProvider.getFreshToken();
        return restClient.get()
                .uri("/api/profile/pets/{petId}", petId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<OwnerDto> getAllOwners() {
        String token = tokenProvider.getFreshToken();
        log.info("Fresh token: {}", token);
        return restClient.get()
                .uri("/api/profile/owners")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}

