ALTER TABLE public.combat_entities ADD COLUMN injured_at_turn integer DEFAULT 0;
ALTER TABLE public.combat_entities ADD COLUMN healing_delayed boolean DEFAULT false;
