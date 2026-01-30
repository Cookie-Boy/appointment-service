package ru.sibsutis.appointment.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum Specialization {
    @JsonProperty("Терапевт") GENERAL_PRACTITIONER("Терапевт"),
    @JsonProperty("Хирург") SURGEON("Хирург"),
    @JsonProperty("Стоматолог") DENTIST("Стоматолог"),
    @JsonProperty("Дерматолог") DERMATOLOGIST("Дерматолог"),
    @JsonProperty("Офтальмолог") OPHTHALMOLOGIST("Офтальмолог"),
    @JsonProperty("Кардиолог") CARDIOLOGIST("Кардиолог"),
    @JsonProperty("Онколог") ONCOLOGIST("Онколог"),
    @JsonProperty("Экзотические животные") EXOTIC_PETS("Экзотические животные"),
    @JsonProperty("Кошки") FELINE("Кошки"),
    @JsonProperty("Собаки") CANINE("Собаки");

    private final String name;

    Specialization(String name) {
        this.name = name;
    }

    @JsonCreator
    public static Specialization fromName(String name) {
        for (Specialization specialization : Specialization.values()) {
            if (specialization.name.equalsIgnoreCase(name)) {
                return specialization;
            }
        }
        throw new IllegalArgumentException("Неизвестная специализация: " + name);
    }
}
