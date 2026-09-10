package com.mg.nmlonline.domain.exception;

/** Annulation refusée : pré-requis non satisfaits (action liée déjà consommée, tour terminé…). Mappée en HTTP 409. */
public class PlayerActionUndoException extends RuntimeException {

    public PlayerActionUndoException(String message) {
        super(message);
    }
}
