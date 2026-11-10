ALTER TABLE public.turn_journals ADD COLUMN board_id bigint;
UPDATE public.turn_journals SET board_id = (SELECT min(id) FROM public.boards) WHERE board_id IS NULL;
ALTER TABLE public.turn_journals ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.turn_journals
    ADD CONSTRAINT fk_turn_journals_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;

DROP INDEX public.idx_turn_journals_turn;
CREATE INDEX idx_turn_journals_board_turn ON public.turn_journals (board_id, turn);
