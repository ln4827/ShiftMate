package com.shiftmate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist or is not accessible within
 * the current tenant scope. Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * @param message a human-readable description of what was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor that builds a standard "X with id Y not found" message.
     *
     * @param resource the name of the resource type (e.g. "Employee")
     * @param id       the ID that was not found
     */
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " not found.");
    }
}
