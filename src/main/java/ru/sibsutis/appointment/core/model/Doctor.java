package ru.sibsutis.appointment.core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "doctors")
@Getter
@Setter
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(max = 30, message = "Имя слишком длинное, ограничение 30 символов")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(max = 30, message = "Фамилия слишком длинная, ограничение 30 символов")
    @Column(nullable = false)
    private String lastName;

    @Size(max = 30, message = "Отчество слишком длинное, ограничение 30 символов")
    private String middleName;

    @NotNull(message = "Специализация не может быть null")
    @Enumerated(EnumType.STRING)
    private Specialization specialization;

    @NotBlank(message = "Номер лицензии не может быть пустым")
    @Pattern(regexp = "^[A-Za-z0-9-]{6,20}$", message = "Некорректный формат номера лицензии")
    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @NotBlank(message = "Номер телефона не может быть пустым")
    @Pattern(regexp = "^\\+?[0-9\\-\\s()]{7,20}$", message = "Некорректный формат номера телефона")
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Column(nullable = false, unique = true)
    private String email;

    @PastOrPresent(message = "Дата приема на работу не может быть в будущем")
    @Column()
    private LocalDate hireDate;

    @Column()
    private LocalTime startWorkingDay;

    @Column()
    private LocalTime endWorkingDay;

    @Size(max = 1000, message = "Биография слишком длинная, ограничение 1000 символов")
    private String bio;

    @PrePersist
    private void onCreate() {
        if (hireDate == null) {
            hireDate = LocalDate.now();
        }
        if (startWorkingDay == null) {
            startWorkingDay = LocalTime.of(10, 0);
        }
        if (endWorkingDay == null) {
            endWorkingDay = LocalTime.of(18, 0);
        }
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", specialization=" + specialization +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", hireDate=" + hireDate +
                ", startWorkingDay=" + startWorkingDay +
                ", endWorkingDay=" + endWorkingDay +
                ", bio='" + bio + '\'' +
                '}';
    }
}