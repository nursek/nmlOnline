CREATE TABLE public.sector_notes (
    player_id bigint NOT NULL,
    board_id bigint NOT NULL,
    sector_number integer NOT NULL,
    note character varying(500) NOT NULL,
    PRIMARY KEY (player_id, board_id, sector_number)
);

ALTER TABLE public.sector_notes
    ADD CONSTRAINT sector_notes_player_fk
    FOREIGN KEY (player_id) REFERENCES public.players(id) ON DELETE CASCADE;

ALTER TABLE public.sector_notes
    ADD CONSTRAINT sector_notes_sector_fk
    FOREIGN KEY (board_id, sector_number) REFERENCES public.sectors(board_id, number) ON DELETE CASCADE;
