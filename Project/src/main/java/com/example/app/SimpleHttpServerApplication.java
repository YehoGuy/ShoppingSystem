package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(

)
public class SimpleHttpServerApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(SimpleHttpServerApplication.class, args);
        } catch (Exception e) {
            System.out.println("probably some db error");
        }
    }
}
