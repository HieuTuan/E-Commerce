package com.mypkga.commerceplatformfull;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CommercePlatformFullApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommercePlatformFullApplication.class, args);
    }

}
