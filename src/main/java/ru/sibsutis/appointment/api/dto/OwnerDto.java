package ru.sibsutis.appointment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDto {
    private String id;
    private String tgChatId;
    private String firstName;
    private String lastName;
    private String phone;
}
