-- Part de dotation consommée par un achat : indispensable au remboursement exact d'une annulation.
ALTER TABLE public.player_actions
    ADD COLUMN starting_money_spent double precision NOT NULL DEFAULT 0;
