package com.banduairlines;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BanduAirlinesApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));   // antes de arrancar Spring/Hibernate
        SpringApplication.run(BanduAirlinesApplication.class, args);
    }
}
