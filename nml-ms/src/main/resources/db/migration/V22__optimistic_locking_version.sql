ALTER TABLE public.boards ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE public.players ADD COLUMN version bigint NOT NULL DEFAULT 0;
