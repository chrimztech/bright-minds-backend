package com.brightminds.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrightMindsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BrightMindsApplication.class, args);
    }
}
