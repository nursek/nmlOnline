-- Classe retirée du jeu : purger avant de resserrer les CHECK, sinon UnitClass.valueOf casse au chargement JPA.

CREATE TEMP TABLE removed_class_equipment (name character varying(255) PRIMARY KEY);

INSERT INTO removed_class_equipment (name) VALUES
    ('Voltaic Staff'),
    ('Thermal Cutting Beam'),
    ('Entropic Lance'),
    ('Eldritch Lance'),
    ('Gauntlet of Fire'),
    ('Warscythe'),
    ('Gloom Prism'),
    ('Chronometron'),
    ('Mindshackle Scarabs'),
    ('Armure thermique');

WITH removed_bonus AS (
    SELECT ue.unit_id,
           SUM(ce.attack * e.pdf_bonus / 100) AS pdf,
           SUM(ce.attack * e.pdc_bonus / 100) AS pdc,
           SUM(ce.defense * e.arm_bonus / 100) AS armor,
           SUM(e.evasion_bonus) AS evasion
    FROM public.unit_equipments ue
    JOIN public.equipment e ON e.id = ue.equipment_id
    JOIN removed_class_equipment r ON r.name = e.name
    JOIN public.combat_entities ce ON ce.id = ue.unit_id
    GROUP BY ue.unit_id
)
UPDATE public.combat_entities ce
SET pdf = GREATEST(0, ce.pdf - rb.pdf),
    pdc = GREATEST(0, ce.pdc - rb.pdc),
    armor = GREATEST(0, ce.armor - rb.armor),
    evasion = GREATEST(0, ce.evasion - rb.evasion)
FROM removed_bonus rb
WHERE ce.id = rb.unit_id;

DELETE FROM public.unit_equipments ue
USING public.equipment e, removed_class_equipment r
WHERE ue.equipment_id = e.id
  AND e.name = r.name;

DELETE FROM public.equipment_stacks es
USING public.equipment e, removed_class_equipment r
WHERE es.equipment_id = e.id
  AND e.name = r.name;

DELETE FROM public.equipment_compatible_classes
WHERE unit_class = 'ELEMENTAIRE';

DELETE FROM public.equipment_compatible_classes ecc
USING public.equipment e, removed_class_equipment r
WHERE ecc.equipment_id = e.id
  AND e.name = r.name;

DELETE FROM public.unit_classes
WHERE unit_class = 'ELEMENTAIRE';

DELETE FROM public.equipment e
USING removed_class_equipment r
WHERE e.name = r.name;

DROP TABLE removed_class_equipment;

UPDATE public.players p
SET total_equipment_value = COALESCE((
    SELECT SUM(e.cost * es.quantity)
    FROM public.equipment_stacks es
    JOIN public.equipment e ON e.id = es.equipment_id
    WHERE es.player_id = p.id
), 0);

ALTER TABLE public.unit_classes
    DROP CONSTRAINT unit_classes_unit_class_check;

ALTER TABLE public.unit_classes
    ADD CONSTRAINT unit_classes_unit_class_check
    CHECK (unit_class IN ('LEGER', 'TIREUR', 'MASTODONTE', 'PILOTE_DESTRUCTEUR', 'SNIPER'));

ALTER TABLE public.equipment_compatible_classes
    DROP CONSTRAINT equipment_compatible_classes_unit_class_check;

ALTER TABLE public.equipment_compatible_classes
    ADD CONSTRAINT equipment_compatible_classes_unit_class_check
    CHECK (unit_class IN ('LEGER', 'TIREUR', 'MASTODONTE', 'PILOTE_DESTRUCTEUR', 'SNIPER'));
