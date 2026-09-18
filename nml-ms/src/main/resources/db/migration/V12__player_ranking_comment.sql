-- Indice du MJ affiché dans le classement militaire (un seul par joueur, écrasé chaque tour).
ALTER TABLE public.players
    ADD COLUMN ranking_comment character varying(500);
