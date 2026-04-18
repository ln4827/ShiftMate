package com.shiftmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ShiftMate application.
 *
 * <p>ShiftMate is a restaurant staff and shift scheduling system developed for
 * ISTE-432 Database Application Development at RIT Croatia.
 *
 * @author Lucija Nesnidal
 * @author Karmen Penga
 */
@SpringBootApplication
public class ShiftMateApplication {

    /**
     * @param args command-line arguments passed to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(ShiftMateApplication.class, args);
    }
}
