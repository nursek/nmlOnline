-- Séquence standalone INCREMENT 50 = allocationSize de l'entité (cf. V8).
CREATE SEQUENCE IF NOT EXISTS public.turn_journals_id_seq START WITH 1;
ALTER SEQUENCE public.turn_journals_id_seq INCREMENT BY 50;

CREATE TABLE public.turn_journals (
    id bigint NOT NULL DEFAULT nextval('public.turn_journals_id_seq'),
    turn integer NOT NULL,
    payload text NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT turn_journals_pkey PRIMARY KEY (id)
);

ALTER SEQUENCE public.turn_journals_id_seq OWNED BY public.turn_journals.id;

CREATE INDEX idx_turn_journals_turn ON public.turn_journals (turn);
