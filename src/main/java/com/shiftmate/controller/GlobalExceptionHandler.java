package com.shiftmate.controller;

import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception handler for all REST controllers. Translates application
 * exceptions into consistent JSON error responses rather than Spring's default HTML
 * error pages, so the React frontend can handle them programmatically.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles requests for resources that do not exist.
     *
     * @param ex the exception
     * @return a 404 response with an error message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    /**
     * Handles violations of application business rules such as duplicate emails
     * or invalid state transitions.
     *
     * @param ex the exception
     * @return a 409 response with an error message
     */
    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    /**
     * Handles bean validation failures on request bodies. Returns a map of field
     * names to their validation error messages so the frontend can highlight
     * individual form fields.
     *
     * @param ex the validation exception
     * @return a 400 response with a field-level error map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return Map.of("error", "Validation failed", "fields", fieldErrors);
    }

    /**
     * Handles optimistic locking failures caused by concurrent modifications to
     * the same record. The client should refresh and retry.
     *
     * @param ex the exception
     * @return a 409 response with a user-friendly message
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConcurrency(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return Map.of("error", "This record was modified by another user. Please refresh and try again.");
    }

    /**
     * Handles bad request parameters such as malformed ISO week strings.
     *
     * @param ex the exception
     * @return a 400 response with the error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadArgument(IllegalArgumentException ex) {
        log.warn("Bad request argument: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    /**
     * Catch-all handler for unexpected exceptions. Logs the full stack trace and
     * returns a generic error message to avoid leaking implementation details.
     *
     * @param ex the exception
     * @return a 500 response with a generic error message
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return Map.of("error", "An unexpected error occurred.");
    }
}
