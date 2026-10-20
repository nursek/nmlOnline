CREATE TABLE public.game_statistics (
    stat_key character varying(64) NOT NULL,
    total bigint NOT NULL DEFAULT 0,
    CONSTRAINT game_statistics_pkey PRIMARY KEY (stat_key)
);

-- Reprend les pertes déjà journalisées dans les rapports de combat.
WITH casualties AS (
    SELECT jsonb_array_elements(payload::jsonb -> 'casualties') AS casualty
    FROM public.battle_reports
    WHERE payload IS NOT NULL
),
counts AS (
    SELECT 'UNIT_' || (casualty->>'unitType') AS stat_key, count(*) AS total
    FROM casualties
    WHERE casualty->>'category' = 'INFANTRY'
      AND casualty->>'unitType' IN ('LARBIN', 'VOYOU', 'MALFRAT', 'BRUTE')
    GROUP BY casualty->>'unitType'

    UNION ALL

    SELECT 'CHARACTER', count(*)
    FROM casualties
    WHERE casualty->>'category' = 'CHARACTER'

    UNION ALL

    SELECT 'VEHICLE_' || CASE casualty->>'label'
               WHEN 'Véhicule à tourelle' THEN 'TOURELLE'
               WHEN 'VTT léger' THEN 'VTT_LEGER'
               WHEN 'VTT blindé' THEN 'VTT_BLINDE'
               WHEN 'Tank de combat' THEN 'TANK'
               WHEN 'Hélicoptère de combat' THEN 'HELICOPTERE'
               WHEN 'Avion de transport' THEN 'AVION_TRANSPORT'
           END,
           count(*)
    FROM casualties
    WHERE casualty->>'category' = 'VEHICLE'
      AND casualty->>'label' IN (
          'Véhicule à tourelle', 'VTT léger', 'VTT blindé',
          'Tank de combat', 'Hélicoptère de combat', 'Avion de transport'
      )
    GROUP BY casualty->>'label'
)
INSERT INTO public.game_statistics (stat_key, total)
SELECT stat_key, total FROM counts
WHERE stat_key IS NOT NULL
ON CONFLICT (stat_key) DO UPDATE SET total = public.game_statistics.total + EXCLUDED.total;
