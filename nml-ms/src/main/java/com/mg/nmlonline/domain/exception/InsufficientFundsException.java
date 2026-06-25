package com.mg.nmlonline.domain.exception;

/**
 * Exception métier levée lorsqu'un joueur ne dispose pas des fonds suffisants
 * pour effectuer un achat ou une opération coûteuse.
 * Mappée sur une réponse HTTP 402 (Payment Required) par le gestionnaire global.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
