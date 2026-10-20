UPDATE public.players p
SET total_vehicles_value = GREATEST(COALESCE(p.total_vehicles_value, 0) - COALESCE(w.value, 0), 0)
FROM (
    SELECT player_id,
           SUM(CASE vehicle_type
                   WHEN 'TOURELLE' THEN 1300
                   WHEN 'VTT_LEGER' THEN 4000
                   WHEN 'VTT_BLINDE' THEN 6500
                   WHEN 'TANK' THEN 7500
                   WHEN 'HELICOPTERE' THEN 9000
                   WHEN 'AVION_TRANSPORT' THEN 15000
                   ELSE 0
               END) AS value
    FROM public.combat_entities
    WHERE entity_type = 'VEHICLE' AND is_destroyed
    GROUP BY player_id
) w
WHERE p.id = w.player_id;

-- Détache les FK pilot_id/vehicle_id avant le DELETE des épaves.
UPDATE public.combat_entities
SET pilot_id = NULL
WHERE pilot_id IN (
    SELECT id FROM public.combat_entities WHERE entity_type = 'VEHICLE' AND is_destroyed
);

UPDATE public.combat_entities
SET vehicle_id = NULL
WHERE vehicle_id IN (
    SELECT id FROM public.combat_entities WHERE entity_type = 'VEHICLE' AND is_destroyed
);

DELETE FROM public.combat_entities
WHERE entity_type = 'VEHICLE' AND is_destroyed;
