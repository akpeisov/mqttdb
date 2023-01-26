package kz.home.mqttdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MqttdbApplication {

	public static void main(String[] args) {
		SpringApplication.run(MqttdbApplication.class, args);
	}

}
