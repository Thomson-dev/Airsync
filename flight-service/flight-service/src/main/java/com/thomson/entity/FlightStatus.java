package com.thomson.entity;

/**
 * Lifecycle states of a flight.
 */
public enum FlightStatus {
    /** Flight is created and accepting bookings */
    SCHEDULED,

    /** Flight has been cancelled — no further bookings allowed */
    CANCELLED,

    /** Flight has departed and the journey is done */
    COMPLETED
}
