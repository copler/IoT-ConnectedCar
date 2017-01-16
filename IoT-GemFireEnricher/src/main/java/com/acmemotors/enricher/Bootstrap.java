package com.acmemotors.enricher;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Bootstrap {

    @PostConstruct
    public void setup() {
        System.out.println("OK");
    }

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }

}
