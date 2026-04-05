package com.remote.resetbutton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RestconfProperties.class)
public class RemoteResetButtonApplication {

    public static void main(String[] args) {
        SpringApplication.run(RemoteResetButtonApplication.class, args);
    }
}