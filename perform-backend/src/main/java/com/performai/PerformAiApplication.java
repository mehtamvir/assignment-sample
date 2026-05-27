/**
 * Perform AI Backend Application
 *
 * Entry point for the Perform AI Spring Boot application.
 * Enables asynchronous processing support via {@code @EnableAsync}
 * to allow background job execution for analysis tasks.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class. Bootstraps the Spring context and enables
 * the async executor required for background analysis processing.
 */
@SpringBootApplication
@EnableAsync
public class PerformAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformAiApplication.class, args);
    }
}
