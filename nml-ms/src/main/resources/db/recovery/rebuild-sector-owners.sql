-- ============================================================
-- RÉCUPÉRATION BEST-EFFORT DE L'APPARTENANCE DES SECTEURS
-- ============================================================
-- À exécuter À LA MAIN sur la base de prod si les secteurs ont perdu leur
-- owner_id (ex: un re-import de board destructif a tourné) MAIS que les unités
-- / bâtiments survivent encore en base (table combat_entities avec player_id).
--
-- Cette réparation n'est PAS une migration Flyway (placée hors db/migration/)
-- : elle ne doit pas s'exécuter automatiquement à chaque boot.
--
-- Logique : pour chaque secteur orphelin (owner_id IS NULL), on prend le
-- player_id d'une combat_entities rattachée à ce secteur (unité/bâtiment) —
-- c'est la seule trace d'appartenance qui reste quand owner_id a été effacé.
--
-- Si l'incident a aussi supprimé les combat_entities (cascade orphanRemoval
-- lors d'un getSectorsList().clear()), aucun script SQL ne peut reconstruire
-- l'appartenance : il faut soit restaurer un dump, soit réimporter les joueurs
-- via l'API admin (POST /api/admin/players/import) qui réassigne leurs
-- secteurs à partir des players/*.json exportés.
-- ============================================================

BEGIN;

-- 1. Bilan avant réparation
SELECT b.id            AS board_id,
       b.name          AS board_name,
       COUNT(s.*)      AS total_secteurs,
       COUNT(s.*) FILTER (WHERE s.owner_id IS NULL) AS secteurs_orphelins
FROM public.boards b
JOIN public.sectors s ON s.board_id = b.id
GROUP BY b.id, b.name;

-- 2. Réassignation best-effort : on dérive owner_id depuis une unité/bâtiment survivant
UPDATE public.sectors s
SET owner_id = sub.player_id
FROM (
    SELECT DISTINCT ON (ce.board_id, ce.sector_number)
           ce.board_id, ce.sector_number, ce.player_id
    FROM public.combat_entities ce
    WHERE ce.player_id IS NOT NULL
      AND ce.sector_number IS NOT NULL
      AND ce.entity_type IN ('UNIT', 'BUILDING', 'CHARACTER')
    ORDER BY ce.board_id, ce.sector_number
) sub
WHERE s.board_id = sub.board_id
  AND s.number   = sub.sector_number
  AND s.owner_id IS NULL
  AND sub.player_id IN (SELECT id FROM public.players);  -- sécurité : owner valide

-- 3. Bilan après réparation
SELECT b.id            AS board_id,
       b.name          AS board_name,
       COUNT(s.*)      AS total_secteurs,
       COUNT(s.*) FILTER (WHERE s.owner_id IS NULL) AS secteurs_encore_orphelins
FROM public.boards b
JOIN public.sectors s ON s.board_id = b.id
GROUP BY b.id, b.name;

COMMIT;