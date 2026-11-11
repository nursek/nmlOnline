CREATE SEQUENCE public.sniper_orders_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE public.sniper_orders (
    id bigint NOT NULL DEFAULT nextval('public.sniper_orders_id_seq'),
    player_id bigint NOT NULL,
    turn integer NOT NULL,
    unit_id bigint NOT NULL,
    from_sector integer NOT NULL,
    target_sector integer NOT NULL,
    status character varying(20) NOT NULL,
    status_message character varying(500),
    submitted_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT sniper_orders_pkey PRIMARY KEY (id),
    CONSTRAINT sniper_orders_status_check
        CHECK (status IN ('PENDING', 'RESOLVED', 'CANCELLED', 'BLOCKED'))
);

ALTER SEQUENCE public.sniper_orders_id_seq OWNED BY public.sniper_orders.id;

ALTER TABLE public.sniper_orders
    ADD CONSTRAINT sniper_orders_player_fk
    FOREIGN KEY (player_id) REFERENCES public.players(id) ON DELETE CASCADE;

ALTER TABLE public.sniper_orders
    ADD CONSTRAINT sniper_orders_unit_fk
    FOREIGN KEY (unit_id) REFERENCES public.combat_entities(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX sniper_orders_turn_unit_uk
    ON public.sniper_orders (turn, unit_id) WHERE status = 'PENDING';
