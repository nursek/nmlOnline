-- Phase 3 (fix variante Phase 2 non couverte : équipé chargé LAZY + MOVED 2 hops
-- + pertes de combat déclenchait update unit_equipments SET unit_id=NULL, voir
-- docs/jpa-pitfalls.md §1 et le commentaire ponytail dans Sector.java).
--
-- Comme pour Unit.unitEquipments en Phase 2 (V5), on retire orphanRemoval=true
-- de Sector.army (voir Sector.java). La DELETE des pertes de combat se fait
-- désormais via em.remove explicite dans CombatService.simulateSectorBattle ; la
-- DELETE des armées d'un joueur supprimé se fait via em.remove dans
-- SectorService.removePlayerFromSectors. La FK ON DELETE CASCADE (@OnDelete +
-- cette migration) est une ceinture de sécurité DB-side si l'ORM laisse un row
-- orphelin à la suite d'un état transitoire inattendu (par ex. suppression
-- directe d'un secteur — aucun path actuel ne le fait, mais la cascade protège).
--
-- Aucun path actuel ne DELETE de secteur en prod (BoardService.saveBoard est
-- non-destructive, PlayerService.delete ne supprime pas les secteurs), donc
-- cette migration est défensive. Sans elle, le schéma prod serait incohérent
-- avec le mapping (@OnDelete(CASCADE) est honoré par Hibernate ddl-auto en test,
-- mais pas par le schéma Flyway existant).
-- ponytail: ceiling = pas d'audit trail des unités détruites en cascade ; upgrade
--   path = soft-delete sur combat_entities (colonne deleted_at) + explicit delete,
--   hors scope de ce fix.

ALTER TABLE public.combat_entities
    DROP CONSTRAINT IF EXISTS fk2570jvuu09weacray8up21v0g;

ALTER TABLE public.combat_entities
    ADD CONSTRAINT fk2570jvuu09weacray8up21v0g
    FOREIGN KEY (board_id, sector_number) REFERENCES public.sectors(board_id, number) ON DELETE CASCADE;