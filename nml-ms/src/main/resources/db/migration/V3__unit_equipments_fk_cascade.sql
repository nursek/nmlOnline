-- Phase 2 (fix HTTP 500 sur POST /api/admin/turn/resolve/next-hop) :
-- La FK unit_equipments.unit_id → combat_entities.id était créée sans ON DELETE
-- CASCADE dans V1__baseline.sql. À l'époque, Unit.unitEquipments portait
-- cascade=ALL + orphanRemoval=true, donc Hibernate DELETEait les rows unit_equipments
-- avant de DELETE la combat_entities parente — la FK stricte suffisait comme
-- filet de sécurité. Mais ce cascading JPA a par effet de bord émis un
--   UPDATE unit_equipments SET unit_id=NULL WHERE id=?
-- (release-FK en cascade depuis Sector.army.orphanRemoval sur une Unit déplacée)
-- qui violait unit_id NOT NULL → DataIntegrityViolationException au commit de
-- TurnResolutionOrchestrator.advanceHop → HTTP 500 quand l'unité déplacée portait
-- des équipements (cas démo lurio attaquant équipé en secteur 41).
-- Le mapping JPA ne porte plus orphanRemoval sur Unit.unitEquipments (voir Unit.java) ;
-- cascade=ALL reste (REMOVE) pour que la cascade de suppression d'une Unit émette
-- proprement des DELETE FROM unit_equipments WHERE id=? via l'ORM. La FK devient
-- ON DELETE CASCADE en complément, ceinture de sécurité DB-side si l'ORM laisse
-- un row orphelin (par ex. à la suite d'un état transitoire inattendu).
-- ponytail: ceiling = pas d'audit trail des équipements détruits (les rows partent
--   en cascade silencieuse à la suppression d'une unité) ; upgrade path = soft-delete
--   sur UnitEquipment (colonne deleted_at) + explicit delete dans UnitService, hors
--   scope de ce fix.

ALTER TABLE public.unit_equipments
    DROP CONSTRAINT IF EXISTS fk8nq4cv16iq9am2hcwp76dpq9n;

ALTER TABLE public.unit_equipments
    ADD CONSTRAINT fk8nq4cv16iq9am2hcwp76dpq9n
    FOREIGN KEY (unit_id) REFERENCES public.combat_entities(id) ON DELETE CASCADE;