package com.vanh.itam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // Kích hoạt @Scheduled jobs (audit auto-expire, audit reminder)
@EnableAsync        // Kích hoạt @Async cho email gửi qua SendGrid (non-blocking)
public class ItamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItamApplication.class, args);
    }
}
