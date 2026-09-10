package com.mg.nmlonline.domain.exception;

/** Mappée en HTTP 409 par GlobalExceptionHandler. */
public class PlayerActionUndoException extends RuntimeException {

    public PlayerActionUndoException(String message) {
        super(message);
    }
}
