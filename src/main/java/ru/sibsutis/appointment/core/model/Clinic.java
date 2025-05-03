package ru.sibsutis.appointment.core.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Doctor> doctors = new ArrayList<>();
}