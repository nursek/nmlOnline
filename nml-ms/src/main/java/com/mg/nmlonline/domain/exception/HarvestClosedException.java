package com.mg.nmlonline.domain.exception;

/** Mappée en HTTP 409 par GlobalExceptionHandler. */
public class HarvestClosedException extends IllegalStateException {

    public HarvestClosedException(String message) {
        super(message);
    }
}
