-- Séquence standalone INCREMENT 50 = allocationSize de l'entité (cf. V8).
CREATE SEQUENCE IF NOT EXISTS public.battle_reports_id_seq START WITH 1;
ALTER SEQUENCE public.battle_reports_id_seq INCREMENT BY 50;

CREATE TABLE public.battle_reports (
    id bigint NOT NULL DEFAULT nextval('public.battle_reports_id_seq'),
    turn integer NOT NULL,
    sector_number integer NOT NULL,
    standoff boolean NOT NULL,
    winner_player_id bigint,
    captured_buildings integer NOT NULL,
    payload character varying(100000) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT battle_reports_pkey PRIMARY KEY (id)
);

ALTER SEQUENCE public.battle_reports_id_seq OWNED BY public.battle_reports.id;

CREATE TABLE public.battle_report_players (
    report_id bigint NOT NULL,
    player_id bigint NOT NULL,
    CONSTRAINT battle_report_players_pkey PRIMARY KEY (report_id, player_id)
);

ALTER TABLE public.battle_report_players
    ADD CONSTRAINT fk_battle_report_players_report FOREIGN KEY (report_id) REFERENCES public.battle_reports(id) ON DELETE CASCADE;

ALTER TABLE public.battle_report_players
    ADD CONSTRAINT fk_battle_report_players_player FOREIGN KEY (player_id) REFERENCES public.players(id) ON DELETE CASCADE;

CREATE INDEX idx_battle_reports_turn ON public.battle_reports (turn);
