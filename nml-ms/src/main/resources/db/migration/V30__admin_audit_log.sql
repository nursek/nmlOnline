CREATE TABLE public.admin_audit_log (
    id bigserial PRIMARY KEY,
    admin_username character varying(100),
    player_id bigint,
    action character varying(50) NOT NULL,
    details character varying(2000),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    turn integer
);

CREATE INDEX admin_audit_log_player_created_idx
    ON public.admin_audit_log (player_id, created_at DESC);

CREATE INDEX admin_audit_log_created_idx
    ON public.admin_audit_log (created_at DESC);
