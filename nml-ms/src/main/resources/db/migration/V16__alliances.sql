CREATE SEQUENCE public.alliances_id_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE public.alliances (
    id bigint NOT NULL DEFAULT nextval('alliances_id_seq'),
    player_one_id bigint NOT NULL,
    player_two_id bigint NOT NULL,
    created_turn integer NOT NULL,
    ended_turn integer,
    ended_by_player_id bigint,
    betrayal boolean NOT NULL DEFAULT false,
    CONSTRAINT alliances_pkey PRIMARY KEY (id),
    CONSTRAINT alliances_pair_check CHECK (player_one_id < player_two_id),
    CONSTRAINT alliances_player_one_fk FOREIGN KEY (player_one_id) REFERENCES public.players(id) ON DELETE CASCADE,
    CONSTRAINT alliances_player_two_fk FOREIGN KEY (player_two_id) REFERENCES public.players(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX idx_alliances_active_pair ON public.alliances (player_one_id, player_two_id) WHERE ended_turn IS NULL;
CREATE INDEX idx_alliances_player_one ON public.alliances (player_one_id);
CREATE INDEX idx_alliances_player_two ON public.alliances (player_two_id);
CREATE INDEX idx_alliances_ended_turn ON public.alliances (ended_turn);

CREATE SEQUENCE public.alliance_proposals_id_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE public.alliance_proposals (
    id bigint NOT NULL DEFAULT nextval('alliance_proposals_id_seq'),
    kind character varying(20) NOT NULL,
    from_player_id bigint NOT NULL,
    to_player_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    created_turn integer NOT NULL,
    resolved_turn integer,
    CONSTRAINT alliance_proposals_pkey PRIMARY KEY (id),
    CONSTRAINT alliance_proposals_kind_check CHECK (kind IN ('ALLIANCE', 'RUPTURE')),
    CONSTRAINT alliance_proposals_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'WITHDRAWN')),
    CONSTRAINT alliance_proposals_from_fk FOREIGN KEY (from_player_id) REFERENCES public.players(id) ON DELETE CASCADE,
    CONSTRAINT alliance_proposals_to_fk FOREIGN KEY (to_player_id) REFERENCES public.players(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX idx_alliance_proposals_pending ON public.alliance_proposals (kind, from_player_id, to_player_id) WHERE status = 'PENDING';
CREATE INDEX idx_alliance_proposals_to ON public.alliance_proposals (to_player_id, status);
CREATE INDEX idx_alliance_proposals_from ON public.alliance_proposals (from_player_id, status);

CREATE SEQUENCE public.alliance_messages_id_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE public.alliance_messages (
    id bigint NOT NULL DEFAULT nextval('alliance_messages_id_seq'),
    alliance_id bigint NOT NULL,
    sender_player_id bigint NOT NULL,
    body character varying(1000) NOT NULL,
    turn integer NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT alliance_messages_pkey PRIMARY KEY (id),
    CONSTRAINT alliance_messages_alliance_fk FOREIGN KEY (alliance_id) REFERENCES public.alliances(id) ON DELETE CASCADE,
    CONSTRAINT alliance_messages_sender_fk FOREIGN KEY (sender_player_id) REFERENCES public.players(id) ON DELETE CASCADE
);
CREATE INDEX idx_alliance_messages_alliance ON public.alliance_messages (alliance_id, id);

CREATE SEQUENCE public.announcements_id_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE public.announcements (
    id bigint NOT NULL DEFAULT nextval('announcements_id_seq'),
    type character varying(30) NOT NULL,
    actor_player_id bigint NOT NULL,
    target_player_id bigint,
    turn_created integer NOT NULL,
    visible_at_turn integer NOT NULL,
    CONSTRAINT announcements_pkey PRIMARY KEY (id),
    CONSTRAINT announcements_type_check CHECK (type IN ('ALLIANCE_FORMED', 'ALLIANCE_BROKEN', 'BETRAYAL')),
    CONSTRAINT announcements_actor_fk FOREIGN KEY (actor_player_id) REFERENCES public.players(id) ON DELETE CASCADE,
    CONSTRAINT announcements_target_fk FOREIGN KEY (target_player_id) REFERENCES public.players(id) ON DELETE CASCADE
);
CREATE INDEX idx_announcements_visible_at ON public.announcements (visible_at_turn, id);

CREATE SEQUENCE public.pending_captures_id_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE public.pending_captures (
    id bigint NOT NULL DEFAULT nextval('pending_captures_id_seq'),
    board_id bigint NOT NULL,
    sector_number integer NOT NULL,
    turn integer NOT NULL,
    candidate_player_ids character varying(255) NOT NULL,
    resolved boolean NOT NULL DEFAULT false,
    CONSTRAINT pending_captures_pkey PRIMARY KEY (id),
    CONSTRAINT pending_captures_board_fk FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE
);
CREATE INDEX idx_pending_captures_unresolved ON public.pending_captures (resolved, turn);
