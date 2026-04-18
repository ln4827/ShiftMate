package com.shiftmate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a request would violate a business rule, such as assigning a duplicate
 * email or transitioning an entity to an invalid state. Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessRuleException extends RuntimeException {

    /**
     * @param message a human-readable description of the violated rule
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
