ALTER TABLE public.boards ADD COLUMN finished_at timestamp(6) with time zone;
ALTER TABLE public.boards ADD COLUMN winner_player_id bigint;
ALTER TABLE public.boards ADD COLUMN winner_adjusted boolean NOT NULL DEFAULT false;

CREATE SEQUENCE IF NOT EXISTS public.elimination_events_id_seq START WITH 1;
ALTER SEQUENCE public.elimination_events_id_seq INCREMENT BY 50;

CREATE TABLE public.elimination_events (
    id bigint NOT NULL DEFAULT nextval('public.elimination_events_id_seq'),
    board_id bigint NOT NULL,
    death_index integer NOT NULL,
    victim_player_id bigint NOT NULL,
    victim_name character varying(255) NOT NULL,
    victim_race character varying(20),
    victim_character_name character varying(100),
    eliminated_turn integer NOT NULL,
    cause character varying(40) NOT NULL,
    killer_player_id bigint,
    killer_name character varying(255),
    assist_player_ids character varying(500),
    battle_sector integer,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT elimination_events_pkey PRIMARY KEY (id),
    CONSTRAINT elimination_events_cause_check
        CHECK (cause IN ('HEADQUARTERS_CAPTURED', 'MAIN_CHARACTER_KILLED')),
    CONSTRAINT elimination_events_board_fk
        FOREIGN KEY (board_id) REFERENCES public.boards (id) ON DELETE CASCADE
);

ALTER SEQUENCE public.elimination_events_id_seq OWNED BY public.elimination_events.id;

CREATE INDEX elimination_events_board_order_idx
    ON public.elimination_events (board_id, death_index);
CREATE UNIQUE INDEX elimination_events_board_victim_uk
    ON public.elimination_events (board_id, victim_player_id);
CREATE UNIQUE INDEX elimination_events_board_death_uk
    ON public.elimination_events (board_id, death_index);

CREATE SEQUENCE IF NOT EXISTS public.gazette_issues_id_seq START WITH 1;
ALTER SEQUENCE public.gazette_issues_id_seq INCREMENT BY 50;

CREATE TABLE public.gazette_issues (
    id bigint NOT NULL DEFAULT nextval('public.gazette_issues_id_seq'),
    board_id bigint NOT NULL,
    turn integer NOT NULL,
    issue_number integer NOT NULL,
    status character varying(20) NOT NULL,
    main_headline character varying(255),
    payload text NOT NULL,
    editor_notes text,
    final_edition boolean NOT NULL DEFAULT false,
    created_at timestamp(6) with time zone NOT NULL,
    published_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    published_by_user_id bigint,
    CONSTRAINT gazette_issues_pkey PRIMARY KEY (id),
    CONSTRAINT gazette_issues_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT gazette_issues_board_fk
        FOREIGN KEY (board_id) REFERENCES public.boards (id) ON DELETE CASCADE
);

ALTER SEQUENCE public.gazette_issues_id_seq OWNED BY public.gazette_issues.id;

CREATE UNIQUE INDEX gazette_issues_board_turn_uk
    ON public.gazette_issues (board_id, turn);
CREATE UNIQUE INDEX gazette_issues_board_issue_uk
    ON public.gazette_issues (board_id, issue_number);

ALTER TABLE public.announcements DROP CONSTRAINT announcements_type_check;
ALTER TABLE public.announcements
    ADD CONSTRAINT announcements_type_check
    CHECK (type IN ('ALLIANCE_FORMED', 'ALLIANCE_BROKEN', 'BETRAYAL', 'FINAL_DUEL'));
