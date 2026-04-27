package com.thomson.exception;

public class FlightNotFoundException extends RuntimeException {
    public FlightNotFoundException(Long id) {
        super("Flight not found with id: " + id);
    }
}
