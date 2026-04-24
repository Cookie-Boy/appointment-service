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

    public static PetDto fromPet(Pet pet) {
        if (pet == null) return null;

        PetResponseBuilder builder = PetDto.builder()
                .id(pet.getId())
                .ownerId(pet.getOwnerId())
                .name(pet.getName())
                .species(pet.getSpecies())
                .age(pet.getAge())
                .breed(pet.getBreed())
                .chipNumber(pet.getChipNumber())
                .qrCode(pet.getQrCode());

        if (pet.getMedicalRecord() != null) {
            Pet.MedicalRecord record = pet.getMedicalRecord();
            MedicalRecordDto recordDto = MedicalRecordDto.builder()
                    .allergies(record.getAllergies())
                    .chronicDiseases(record.getChronicDiseases())
                    .build();
            if (record.getVaccinations() != null) {
                recordDto.setVaccinations(record.getVaccinations().stream()
                        .map(v -> VaccinationDto.builder()
                                .name(v.getName())
                                .date(v.getDate())
                                .nextDueDate(v.getNextDueDate())
                                .build())
                        .toList());
            }
            builder.medicalRecord(recordDto);
        }

        if (pet.getCollar() != null) {
            Pet.Collar collar = pet.getCollar();
            CollarDto collarDto = CollarDto.builder()
                    .active(collar.getActive())
                    .build();
            if (collar.getHomeInfo() != null) {
                collarDto.setHomeInfo(HomeInfoDto.builder()
                        .alerting(collar.getHomeInfo().getAlerting())
                        .radius(collar.getHomeInfo().getRadius())
                        .lat(collar.getHomeInfo().getLat())
                        .lon(collar.getHomeInfo().getLon())
                        .build());
            }
            builder.collar(collarDto);
        }

        return builder.build();
    }
}