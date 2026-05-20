package ru.sibsutis.appointment;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class AppointmentApplication {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Novosibirsk"));
		System.out.println("Часовой пояс установлен: " + ZoneId.systemDefault());
	}

	public static void main(String[] args) {
		SpringApplication.run(AppointmentApplication.class, args);
	}

}
