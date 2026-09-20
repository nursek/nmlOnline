-- Tour dont les revenus ont déjà été versés (raccourci admin sans incrément) : évite un second versement.
ALTER TABLE public.boards
    ADD COLUMN revenue_claimed_turn integer;
