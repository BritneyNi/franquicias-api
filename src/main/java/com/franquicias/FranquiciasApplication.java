package com.franquicias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the franquicias API.
 *
 * <p>Reactive stack: Spring WebFlux + R2DBC, so requests are handled on non blocking event loop
 * threads and database calls never occupy a thread.
 */
@SpringBootApplication
public class FranquiciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FranquiciasApplication.class, args);
    }
}
