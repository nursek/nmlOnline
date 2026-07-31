-- Ajout du tour courant sur le Board (source unique de vérité pour tout le plateau).
-- Initialisé à 1 : valeur par défaut des parties existantes.
ALTER TABLE public.boards ADD COLUMN current_turn integer NOT NULL DEFAULT 1;