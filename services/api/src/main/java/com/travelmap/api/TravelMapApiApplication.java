package com.travelmap.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelMapApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelMapApiApplication.class, args);
    }
}
