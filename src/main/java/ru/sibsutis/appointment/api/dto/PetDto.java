package ru.sibsutis.appointment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetDto {

    private String id;
    private String ownerId;
    private String name;
    private String species;
    private Integer age;
    private String breed;
    private String chipNumber;
    private String qrCode;
    private MedicalRecordDto medicalRecord;
    private CollarDto collar;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalRecordDto {
        private List<VaccinationDto> vaccinations;
        private List<String> allergies;
        private List<String> chronicDiseases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VaccinationDto {
        private String name;
        private LocalDate date;
        private LocalDate nextDueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollarDto {
        private Boolean active;
        private HomeInfoDto homeInfo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomeInfoDto {
        private Boolean alerting;
        private Integer radius;
        private Double lat;
        private Double lon;
    }
}