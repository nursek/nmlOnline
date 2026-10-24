ALTER TABLE public.players
    ADD COLUMN eliminated boolean NOT NULL DEFAULT false,
    ADD COLUMN elimination_cause character varying(40),
    ADD COLUMN eliminated_turn integer;

ALTER TABLE public.players
    ADD CONSTRAINT players_elimination_cause_check
    CHECK (elimination_cause IS NULL
        OR elimination_cause IN ('HEADQUARTERS_CAPTURED', 'MAIN_CHARACTER_KILLED'));
