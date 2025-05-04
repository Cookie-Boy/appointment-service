package ru.sibsutis.appointment.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "telegram_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String chatId;
}