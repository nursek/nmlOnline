CREATE TEMP TABLE equipment_rename (
    old_name character varying(255) PRIMARY KEY,
    new_name character varying(255) NOT NULL
);

INSERT INTO equipment_rename (old_name, new_name) VALUES
    ('Enmitic Disintegrator Pistol', 'Pistolet 9mm'),
    ('Gauss Blaster', 'Pistolet-mitrailleur'),
    ('Tesla Carbine', 'HK-MP7'),
    ('Gauss Flayer', 'Mitrailleuse'),
    ('Gauss Reaper', 'Fusil d''assaut'),
    ('Gauss Destructor', 'Mini machine gun'),
    ('Particle Caster', 'Fusil à pompe'),
    ('Transdimensional Beamer', 'Winchester'),
    ('Particle Beamer', 'Tromblon'),
    ('Gauss Cannon', 'Bombes collantes'),
    ('Heavy Gauss Cannon', 'Lance-roquettes'),
    ('Enmitic Annihilator', 'Lance-grenades'),
    ('Synaptic Disintegrator', 'Fusil de sniper léger'),
    ('Particle Shredder', 'Fusil de sniper lourd'),
    ('Enmitic Exterminator', 'Fusil de sniper de combat'),
    ('Flensing Claw', 'Poing américain'),
    ('Voidblade', 'Matraque télescopique'),
    ('Hyperphase Reap-Blade', 'Batte de métal'),
    ('Hyperphase Sword', 'Machette'),
    ('Hyperphase Glaive', 'Hache de bûcheron'),
    ('Hyperphase Harvester', 'Tronçonneuse'),
    ('Monomolecular Proboscis', 'Clé à molette'),
    ('Rod of Covenant', 'Pied-de-biche'),
    ('Staff of Light', 'Démonte-pneu'),
    ('Tachyon Arrow', 'Couteau de cuisine'),
    ('Hyperphase Thresher', 'Couteau de combat'),
    ('Phylactery', 'Tenue ultra légère'),
    ('Solar Pulse', 'Grenade lacrymogène'),
    ('Phase Shifter', 'Gilet pare-balles léger'),
    ('Sempiternal Weave', 'Gilet pare-balles moyen'),
    ('Quantum Shielding', 'Équipement militaire complet'),
    ('Lightning Field', 'Protection dorsale'),
    ('Dispersion Shield', 'Bouclier anti-émeutes'),
    ('Veil of Darkness', 'Grenade assourdissante'),
    ('Nightmare Shroud', 'Tenue légère en fibre chauffante');

-- Noms génériques restaurés : CsvDataLoader saute le catalogue si la table n'est pas vide.
UPDATE public.equipment e
SET name = r.new_name
FROM equipment_rename r
WHERE e.name = r.old_name;

-- Les actions en cours gardent le nom en clair : sans cette reprise, leur annulation casse.
UPDATE public.player_actions pa
SET equipment_name = r.new_name
FROM equipment_rename r
WHERE pa.equipment_name = r.old_name;

DROP TABLE equipment_rename;
