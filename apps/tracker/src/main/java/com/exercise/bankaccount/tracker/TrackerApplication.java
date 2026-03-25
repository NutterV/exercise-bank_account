package com.exercise.bankaccount.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.jms.annotation.EnableJms;

/**
 * Boots the balance tracker application and enables JMS-driven transaction consumption.
 */
@SpringBootApplication
@EnableJms
@ConfigurationPropertiesScan(basePackages = "com.exercise.bankaccount")
public class TrackerApplication {
	/**
	 * Starts the tracker Spring application.
	 *
	 * @param args standard Spring Boot startup arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(TrackerApplication.class, args);
	}
}
