ALTER TABLE public.equipment
    ADD COLUMN IF NOT EXISTS vehicle_bonus double precision NOT NULL DEFAULT 0;

ALTER TABLE public.equipment
    ADD COLUMN IF NOT EXISTS vehicle_bonus_target character varying(255);

ALTER TABLE public.equipment
    ADD CONSTRAINT equipment_vehicle_bonus_target_check
        CHECK (vehicle_bonus_target IS NULL OR vehicle_bonus_target IN ('GROUND', 'AERIAL'));

-- Les catalogues existants (prod : import admin) doivent recevoir les bonus anti-véhicules.
UPDATE public.equipment SET vehicle_bonus = 100, vehicle_bonus_target = 'GROUND' WHERE name = 'Gauss Cannon';
UPDATE public.equipment SET vehicle_bonus = 100, vehicle_bonus_target = 'AERIAL' WHERE name = 'Heavy Gauss Cannon';
UPDATE public.equipment SET vehicle_bonus = 100, vehicle_bonus_target = 'GROUND' WHERE name = 'Enmitic Annihilator';
