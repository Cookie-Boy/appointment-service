package ru.sibsutis.appointment.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sibsutis.appointment.core.model.TelegramUser;

import java.util.UUID;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, UUID> {
    TelegramUser findByUsername(String username);
}