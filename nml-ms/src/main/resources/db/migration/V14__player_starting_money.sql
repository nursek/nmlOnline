-- Dotation de départ : seule la part dépensée ouvre droit au transfert entre joueurs.
ALTER TABLE public.players
    ADD COLUMN starting_money_total double precision NOT NULL DEFAULT 0;
ALTER TABLE public.players
    ADD COLUMN starting_money_remaining double precision NOT NULL DEFAULT 0;
