package com.beancounter.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * Read a google sheet and create an output file.
 *
 * @author mikeh
 * @since 2019-02-08
 */
@SpringBootApplication(scanBasePackages = {"com.beancounter.shell", "com.beancounter.client"})
@EnableConfigurationProperties
public class ShellRunner {
  public static void main(String[] args) {
    SpringApplication.run(ShellRunner.class, args);
  }
}
