ALTER TABLE public.players
    ADD COLUMN eliminated boolean NOT NULL DEFAULT false,
    ADD COLUMN elimination_cause character varying(40),
    ADD COLUMN eliminated_turn integer,
    ADD COLUMN eliminated_by_player_id bigint;

ALTER TABLE public.players
    ADD CONSTRAINT players_elimination_cause_check
    CHECK (elimination_cause IS NULL
        OR elimination_cause IN ('HEADQUARTERS_CAPTURED', 'MAIN_CHARACTER_KILLED'));

ALTER TABLE public.players
    ADD CONSTRAINT players_eliminated_by_fk
    FOREIGN KEY (eliminated_by_player_id) REFERENCES public.players(id) ON DELETE SET NULL;
