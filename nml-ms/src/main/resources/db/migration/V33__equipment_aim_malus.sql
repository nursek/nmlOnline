ALTER TABLE public.equipment
    ADD COLUMN aim_malus_reduction double precision NOT NULL DEFAULT 0,
    ADD COLUMN enemy_aim_malus double precision NOT NULL DEFAULT 0;

ALTER TABLE public.equipment
    ADD CONSTRAINT equipment_aim_malus_check
    CHECK (aim_malus_reduction >= 0 AND aim_malus_reduction <= 100
        AND enemy_aim_malus >= 0 AND enemy_aim_malus <= 100);

-- Les catalogues existants (prod : import admin) doivent recevoir les modificateurs de visée sniper.
UPDATE public.equipment SET aim_malus_reduction = 10 WHERE name = 'Particle Shredder';
UPDATE public.equipment SET aim_malus_reduction = 15 WHERE name = 'Enmitic Exterminator';
UPDATE public.equipment SET enemy_aim_malus = 15 WHERE name = 'Gilet de camouflage optique';
