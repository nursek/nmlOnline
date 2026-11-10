-- Une partie = un plateau : joueur, ordres, alliances, rapports et statistiques portent leur board_id.
ALTER TABLE public.players ADD COLUMN board_id bigint;
UPDATE public.players SET board_id = (SELECT min(id) FROM public.boards);
ALTER TABLE public.players ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.players
    ADD CONSTRAINT fk_players_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
ALTER TABLE public.players DROP CONSTRAINT ukpblmuavgrnr991e41662asko;
ALTER TABLE public.players DROP CONSTRAINT ukr2bdiqerficgwok3omt8rwlxb;
CREATE UNIQUE INDEX uk_players_board_name ON public.players (board_id, name);
CREATE UNIQUE INDEX uk_players_board_user ON public.players (board_id, user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_players_board ON public.players (board_id);

ALTER TABLE public.movement_orders ADD COLUMN board_id bigint;
UPDATE public.movement_orders SET board_id = (SELECT min(id) FROM public.boards);
ALTER TABLE public.movement_orders ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.movement_orders
    ADD CONSTRAINT fk_movement_orders_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
CREATE INDEX idx_movement_orders_board_turn ON public.movement_orders (board_id, turn, status);

UPDATE public.player_actions SET board_id = (SELECT min(id) FROM public.boards) WHERE board_id IS NULL;
ALTER TABLE public.player_actions ALTER COLUMN board_id SET NOT NULL;
CREATE INDEX idx_player_actions_board_turn ON public.player_actions (board_id, turn, status);

ALTER TABLE public.alliances ADD COLUMN board_id bigint;
UPDATE public.alliances SET board_id = (SELECT min(id) FROM public.boards);
ALTER TABLE public.alliances ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.alliances
    ADD CONSTRAINT fk_alliances_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
CREATE INDEX idx_alliances_board ON public.alliances (board_id);

ALTER TABLE public.battle_reports ADD COLUMN board_id bigint;
UPDATE public.battle_reports SET board_id = (SELECT min(id) FROM public.boards);
-- Base sans aucune partie (bootstrap raté) : ces lignes ne sont rattachables à aucun jeu.
DELETE FROM public.battle_reports WHERE board_id IS NULL;
ALTER TABLE public.battle_reports ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.battle_reports
    ADD CONSTRAINT fk_battle_reports_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
CREATE INDEX idx_battle_reports_board ON public.battle_reports (board_id);

ALTER TABLE public.game_statistics ADD COLUMN board_id bigint;
UPDATE public.game_statistics SET board_id = (SELECT min(id) FROM public.boards);
DELETE FROM public.game_statistics WHERE board_id IS NULL;
ALTER TABLE public.game_statistics ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.game_statistics DROP CONSTRAINT game_statistics_pkey;
ALTER TABLE public.game_statistics ADD CONSTRAINT game_statistics_pkey PRIMARY KEY (board_id, stat_key);
ALTER TABLE public.game_statistics
    ADD CONSTRAINT fk_game_statistics_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;

-- Actions plateforme (création de plateau, catalogue) sans partie : board_id reste nullable.
ALTER TABLE public.admin_audit_log ADD COLUMN board_id bigint;
CREATE INDEX idx_admin_audit_board ON public.admin_audit_log (board_id);

CREATE TABLE public.board_admins (
    board_id bigint NOT NULL,
    user_id bigint NOT NULL,
    PRIMARY KEY (board_id, user_id),
    CONSTRAINT fk_board_admins_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE
);

ALTER TABLE public.exchange_offers ADD COLUMN board_id bigint;
UPDATE public.exchange_offers SET board_id = (SELECT min(id) FROM public.boards);
ALTER TABLE public.exchange_offers ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.exchange_offers
    ADD CONSTRAINT fk_exchange_offers_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
CREATE INDEX idx_exchange_offers_board ON public.exchange_offers (board_id, status);

ALTER TABLE public.announcements ADD COLUMN board_id bigint;
UPDATE public.announcements SET board_id = (SELECT min(id) FROM public.boards);
ALTER TABLE public.announcements ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.announcements
    ADD CONSTRAINT fk_announcements_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
CREATE INDEX idx_announcements_board_visible ON public.announcements (board_id, visible_at_turn, id);

ALTER TABLE public.alliance_proposals ADD COLUMN board_id bigint;
UPDATE public.alliance_proposals SET board_id = (SELECT min(id) FROM public.boards);
ALTER TABLE public.alliance_proposals ALTER COLUMN board_id SET NOT NULL;
ALTER TABLE public.alliance_proposals
    ADD CONSTRAINT fk_alliance_proposals_board FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;
CREATE INDEX idx_alliance_proposals_board ON public.alliance_proposals (board_id, created_turn);

-- Un nom de personnage n'est unique que dans sa partie : les fixtures se réimportent dans une 2e partie.
ALTER TABLE public.combat_entities DROP CONSTRAINT IF EXISTS ukti530xb422xy1qvtwp3gufvf8;
CREATE UNIQUE INDEX uk_combat_entities_board_character_name
    ON public.combat_entities (board_id, character_name)
    WHERE character_name IS NOT NULL AND board_id IS NOT NULL;
