CREATE SEQUENCE IF NOT EXISTS public.notifications_id_seq START WITH 1;
ALTER SEQUENCE public.notifications_id_seq INCREMENT BY 50;

CREATE TABLE public.notifications (
    id bigint NOT NULL DEFAULT nextval('public.notifications_id_seq'),
    board_id bigint NOT NULL,
    user_id bigint NOT NULL,
    type character varying(40) NOT NULL,
    title character varying(160) NOT NULL,
    message character varying(500) NOT NULL,
    link character varying(255),
    created_at timestamp(6) with time zone NOT NULL,
    read_at timestamp(6) with time zone,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_type_check CHECK (type IN (
        'EXCHANGE_RECEIVED',
        'EXCHANGE_ACCEPTED',
        'EXCHANGE_DECLINED',
        'EXCHANGE_CANCELLED',
        'ALLIANCE_PROPOSAL',
        'ALLIANCE_ACCEPTED',
        'ALLIANCE_DECLINED',
        'ALLIANCE_BROKEN',
        'TURN_ENDED',
        'GAME_STARTED',
        'GAME_FINISHED',
        'PLAYER_ELIMINATED',
        'GAZETTE_PUBLISHED'
    )),
    CONSTRAINT notifications_board_fk
        FOREIGN KEY (board_id) REFERENCES public.boards (id) ON DELETE CASCADE
);

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;

CREATE INDEX notifications_user_board_created_idx
    ON public.notifications (user_id, board_id, created_at DESC, id DESC);
CREATE INDEX notifications_user_board_unread_idx
    ON public.notifications (user_id, board_id) WHERE read_at IS NULL;
