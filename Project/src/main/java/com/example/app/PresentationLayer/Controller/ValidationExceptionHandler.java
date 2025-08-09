package com.example.app.PresentationLayer.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/**
 * Catches JSR-303 validation failures on @RequestParam/@PathVariable and
 * turns them into 400 Bad Request.
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        // Return the first violation message, or a generic summary
        String msg = ex.getConstraintViolations()
                       .stream()
                       .findFirst()
                       .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                       .orElse("Validation error");
        return ResponseEntity.badRequest().body(msg);
    }
}
