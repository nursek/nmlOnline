package com.mg.nmlonline.domain.exception;

/** Sous-classe d'IllegalStateException : mappée en 400, et seule exception qui commite l'expiration (noRollbackFor). */
public class OfferExpiredException extends IllegalStateException {

    public OfferExpiredException(String message) {
        super(message);
    }
}
