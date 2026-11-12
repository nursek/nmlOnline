ALTER TABLE public.boards ADD COLUMN started_at timestamp(6) with time zone;
ALTER TABLE public.boards ADD COLUMN preset_key character varying(64);

-- Les parties existantes sont déjà lancées : seules les nouvelles passent par le tour 0.
UPDATE public.boards SET started_at = now();

-- Le tour 0 est un état valide (préparation).
ALTER TABLE public.boards DROP CONSTRAINT boards_current_turn_positive;
ALTER TABLE public.boards ADD CONSTRAINT boards_current_turn_non_negative CHECK (current_turn >= 0);
