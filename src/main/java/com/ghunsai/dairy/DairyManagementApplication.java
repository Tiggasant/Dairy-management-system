package com.ghunsai.dairy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DairyManagementApplication {

    public static void main(String[] args) {
        // "Today's report" and bill timestamps must follow the shop's clock, not the server's.
        String zone = System.getenv("APP_TIMEZONE");
        TimeZone.setDefault(TimeZone.getTimeZone(zone == null || zone.isBlank() ? "Asia/Kolkata" : zone));
        SpringApplication.run(DairyManagementApplication.class, args);
    }
}
