package com.beancounter.position;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication(scanBasePackages = "com.beancounter")
@EnableFeignClients
public class PositionBoot {
  public static void main(String[] args) {
    SpringApplication.run(PositionBoot.class, args);
  }

}
