ALTER TABLE public.movement_orders
    ADD COLUMN submitted_at timestamp(6) with time zone NOT NULL DEFAULT now();
