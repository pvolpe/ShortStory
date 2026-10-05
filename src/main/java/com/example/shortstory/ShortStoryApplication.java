package com.example.shortstory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class ShortStoryApplication {

    public static void main(String[] args) {
        // SQLite won't create missing parent directories for the database file
        new File("data").mkdirs();
        SpringApplication.run(ShortStoryApplication.class, args);
    }
}
