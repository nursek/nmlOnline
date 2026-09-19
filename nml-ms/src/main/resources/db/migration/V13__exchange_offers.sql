-- Séquence standalone INCREMENT 50 = allocationSize de l'entité (cf. V7).
CREATE SEQUENCE IF NOT EXISTS public.exchange_offers_id_seq START WITH 1;
ALTER SEQUENCE public.exchange_offers_id_seq INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS public.exchange_offer_resources_id_seq START WITH 1;
ALTER SEQUENCE public.exchange_offer_resources_id_seq INCREMENT BY 50;

CREATE TABLE public.exchange_offers (
    id bigint NOT NULL DEFAULT nextval('public.exchange_offers_id_seq'),
    sender_player_id bigint NOT NULL,
    receiver_player_id bigint NOT NULL,
    money double precision NOT NULL DEFAULT 0,
    status character varying(20) NOT NULL,
    created_turn integer NOT NULL,
    expires_turn integer NOT NULL,
    resolved_turn integer,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT exchange_offers_pkey PRIMARY KEY (id)
);

ALTER SEQUENCE public.exchange_offers_id_seq OWNED BY public.exchange_offers.id;

ALTER TABLE public.exchange_offers
    ADD CONSTRAINT fk_exchange_offers_sender FOREIGN KEY (sender_player_id) REFERENCES public.players(id) ON DELETE CASCADE;
ALTER TABLE public.exchange_offers
    ADD CONSTRAINT fk_exchange_offers_receiver FOREIGN KEY (receiver_player_id) REFERENCES public.players(id) ON DELETE CASCADE;

CREATE TABLE public.exchange_offer_resources (
    id bigint NOT NULL DEFAULT nextval('public.exchange_offer_resources_id_seq'),
    offer_id bigint NOT NULL,
    resource_name character varying(255) NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT exchange_offer_resources_pkey PRIMARY KEY (id)
);

ALTER SEQUENCE public.exchange_offer_resources_id_seq OWNED BY public.exchange_offer_resources.id;

ALTER TABLE public.exchange_offer_resources
    ADD CONSTRAINT fk_exchange_offer_resources_offer FOREIGN KEY (offer_id) REFERENCES public.exchange_offers(id) ON DELETE CASCADE;

CREATE INDEX idx_exchange_offers_receiver_status ON public.exchange_offers (receiver_player_id, status);
CREATE INDEX idx_exchange_offers_sender_status ON public.exchange_offers (sender_player_id, status);
CREATE INDEX idx_exchange_offer_resources_offer ON public.exchange_offer_resources (offer_id);
