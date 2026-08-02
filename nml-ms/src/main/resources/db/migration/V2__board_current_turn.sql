-- Ajout du tour courant sur le Board (source unique de vérité pour tout le plateau).
-- Initialisé à 1 : valeur par défaut des parties existantes.
-- CHECK >0 : protège contre setCurrentTurn(0) et les imports négatifs.
ALTER TABLE public.boards
    ADD COLUMN current_turn integer NOT NULL DEFAULT 1,
    ADD CONSTRAINT boards_current_turn_positive CHECK (current_turn > 0);