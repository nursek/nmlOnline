-- Journal des actions joueur (achats, équipements, ventes, déplacements) avec annulation en cascade.
-- Même schéma de séquence standalone que V7 (INCREMENT 50 = allocationSize de l'entité).
CREATE SEQUENCE IF NOT EXISTS public.player_actions_id_seq START WITH 1;
ALTER SEQUENCE public.player_actions_id_seq INCREMENT BY 50;

CREATE TABLE public.player_actions (
    id bigint NOT NULL DEFAULT nextval('public.player_actions_id_seq'),
    player_id bigint NOT NULL,
    turn integer NOT NULL,
    type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    equipment_name character varying(255),
    resource_name character varying(255),
    quantity integer,
    money double precision,
    unit_id bigint,
    vehicle_id bigint,
    building_id bigint,
    board_id bigint,
    from_sector_number integer,
    to_sector_number integer,
    prev_last_moved_turn integer,
    prev_has_moved boolean,
    CONSTRAINT player_actions_pkey PRIMARY KEY (id)
);

ALTER SEQUENCE public.player_actions_id_seq OWNED BY public.player_actions.id;

ALTER TABLE public.player_actions
    ADD CONSTRAINT fk_player_actions_player FOREIGN KEY (player_id) REFERENCES public.players(id) ON DELETE CASCADE;

CREATE INDEX idx_player_actions_player_turn ON public.player_actions (player_id, turn);
