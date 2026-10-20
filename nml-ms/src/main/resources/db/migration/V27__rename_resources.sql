CREATE TEMP TABLE resource_rename (
    old_name character varying(255) PRIMARY KEY,
    new_name character varying(255) NOT NULL
);

INSERT INTO resource_rename (old_name, new_name) VALUES
    ('Cigares', 'Caisse d''armes'),
    ('Alcool', 'Prométhéum'),
    ('Antiquités', 'Plastacier'),
    ('Ivoire', 'Adamentium'),
    ('Uranium', 'Conteneur blindé'),
    ('Or', 'Auramite'),
    ('Joyaux', 'Noctilithe');

-- resource.name est unique : si la cible existe déjà, on fusionne les références avant de supprimer l'ancienne.
UPDATE public.resource r
SET name = m.new_name
FROM resource_rename m
WHERE r.name = m.old_name
  AND NOT EXISTS (SELECT 1 FROM public.resource existing WHERE existing.name = m.new_name);

UPDATE public.player_resources pr
SET resource_name = m.new_name
FROM resource_rename m
WHERE pr.resource_name = m.old_name;

UPDATE public.player_resources pr
SET resource_id = target.id
FROM resource_rename m
JOIN public.resource old_resource ON old_resource.name = m.old_name
JOIN public.resource target ON target.name = m.new_name
WHERE pr.resource_id = old_resource.id;

DELETE FROM public.resource old_resource
USING resource_rename m
WHERE old_resource.name = m.old_name
  AND EXISTS (SELECT 1 FROM public.resource target WHERE target.name = m.new_name);

UPDATE public.sectors s
SET resource_name = m.new_name
FROM resource_rename m
WHERE s.resource_name = m.old_name;

UPDATE public.player_actions pa
SET resource_name = m.new_name
FROM resource_rename m
WHERE pa.resource_name = m.old_name;

UPDATE public.exchange_offer_resources eor
SET resource_name = m.new_name
FROM resource_rename m
WHERE eor.resource_name = m.old_name;

DROP TABLE resource_rename;
