ALTER TABLE public.sniper_orders ADD COLUMN board_id bigint;
UPDATE public.sniper_orders o SET board_id = e.board_id FROM public.combat_entities e WHERE e.id = o.unit_id;
UPDATE public.sniper_orders SET board_id = (SELECT min(id) FROM public.boards) WHERE board_id IS NULL;
ALTER TABLE public.sniper_orders ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.sniper_orders
    ADD CONSTRAINT fk_sniper_orders_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;

DROP INDEX public.sniper_orders_turn_unit_uk;
CREATE UNIQUE INDEX sniper_orders_board_turn_unit_uk
    ON public.sniper_orders (board_id, turn, unit_id) WHERE status = 'PENDING';
CREATE INDEX idx_sniper_orders_board_turn_status
    ON public.sniper_orders (board_id, turn, status);
