CREATE TABLE IF NOT EXISTS CREDENTIALS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    refresh_token_expiry BIGINT
);
INSERT INTO CREDENTIALS (username, password) VALUES ('test', '$2a$10$WMsQsnTZ/7pFn.klPSeJ0.m0B1bnsAt9wFgkIduzvmkMF2PzvAOUq');
INSERT INTO CREDENTIALS (username, password) VALUES ('a', '$2a$12$ca/.P6xWRGFiH5Ra0UXMk.NhNBxYgCX5aEYDDG3nv9CsaZ1FExMnm');
INSERT INTO CREDENTIALS (username, password) VALUES ('lurio', '$2y$10$PoKeBxBu4AhIM9yMbEUIzOf8SHbdHC8/A5BHqq9jkUT.YiZbsXZNe');
INSERT INTO CREDENTIALS (username, password) VALUES ('nursek', '$2y$10$X41e/q5zcdbR8T5AMatbFuaXhj.E2fvEJ7DivsuqSlNeY97mrI0mW');

CREATE TABLE IF NOT EXISTS PLAYERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    stats BLOB,
    equipments BLOB,
    sectors BLOB
);
INSERT INTO PLAYERS (username, stats, equipments, sectors) VALUES ('nursek', NULL, NULL, NULL);
INSERT INTO PLAYERS (username, stats, equipments, sectors) VALUES ('lurio', NULL, NULL, NULL);
INSERT INTO PLAYERS (username, stats, equipments, sectors) VALUES ('test', NULL, NULL, NULL);

CREATE TABLE IF NOT EXISTS EQUIPMENT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    cost INT NOT NULL,
    pdf_bonus INT NOT NULL,
    pdc_bonus INT NOT NULL,
    arm_bonus INT NOT NULL,
    evasion_bonus INT NOT NULL,
    compatible_class VARCHAR(255),
    category VARCHAR(100)
);
INSERT INTO EQUIPMENT (name, cost, pdf_bonus, pdc_bonus, arm_bonus, evasion_bonus, compatible_class, category) VALUES
('Pistolet 9mm',400,80,0,0,0,'LEGER','FIREARM'),
('Pistolet-mitrailleur',850,150,0,25,0,'LEGER','FIREARM'),
('HK-MP7',1600,300,0,25,0,'LEGER','FIREARM'),
('Mitrailleuse',1500,200,0,50,0,'TIREUR','FIREARM'),
('Fusil d''assaut',2300,350,0,60,0,'TIREUR','FIREARM'),
('Mini machine gun',2600,400,0,80,0,'TIREUR','FIREARM'),
('Mini machine gun [CM]',3400,500,0,80,0,'TIREUR','FIREARM'),
('Mini machine gun [MP]',4680,400,0,80,0,'TIREUR','FIREARM'),
('Mini machine gun [CM] [MP]',5480,500,0,80,0,'TIREUR','FIREARM'),
('Fusil à pompe',1000,0,200,0,0,'MASTODONTE','FIREARM'),
('Winchester',1500,0,300,0,0,'MASTODONTE','FIREARM'),
('Tromblon',2000,0,400,0,0,'MASTODONTE','FIREARM'),
('Tromblon [DM]',3000,0,400,0,0,'MASTODONTE','FIREARM'),
('Tromblon [MGC]',3500,0,400,0,0,'MASTODONTE','FIREARM'),
('Tromblon [DM] [MGC]',4500,0,400,0,0,'MASTODONTE','FIREARM'),
('Pistolet chauffant',900,100,0,0,0,'ELEMENTAIRE','FIREARM'),
('Fusil à impulsion électromagnétique',2250,250,0,0,0,'ELEMENTAIRE','FIREARM'),
('Canon à glace',2500,300,0,0,0,'ELEMENTAIRE','FIREARM'),
('Lance-flammes',3000,400,0,0,0,'ELEMENTAIRE','FIREARM'),
('Bombes collantes',3400,80,0,0,0,'PILOTE_DESTRUCTEUR','FIREARM'),
('Lance-roquettes',5500,400,0,200,0,'PILOTE_DESTRUCTEUR','FIREARM'),
('Lance-grenades',5800,500,0,100,0,'PILOTE_DESTRUCTEUR','FIREARM'),
('Fusil de sniper léger',1000,150,0,30,0,'SNIPER','FIREARM'),
('Fusil de sniper lourd',1800,300,0,50,0,'SNIPER','FIREARM'),
('Fusil de sniper de combat',2300,400,0,60,0,'SNIPER','FIREARM'),
('Poing américain',100,0,20,0,0,'LEGER','MELEE'),
('Matraque télescopique',200,0,40,0,0,'LEGER','MELEE'),
('Batte de métal',250,0,50,0,0,'TIREUR','MELEE'),
('Machette',375,0,75,0,0,'TIREUR','MELEE'),
('Hache de bûcheron',450,0,90,0,0,'MASTODONTE','MELEE'),
('Tronçonneuse',500,0,100,0,0,'MASTODONTE','MELEE'),
('Matraque électrique',450,0,50,0,0,'ELEMENTAIRE','MELEE'),
('Gantelet électrique',1000,0,85,0,0,'ELEMENTAIRE','MELEE'),
('Panachurros',125,0,25,0,0,'PILOTE_DESTRUCTEUR','MELEE'),
('Panachouquette',250,0,50,0,0,'PILOTE_DESTRUCTEUR','MELEE'),
('Panachoucroute',500,0,100,0,0,'PILOTE_DESTRUCTEUR','MELEE'),
('Couteau de cuisine',200,0,40,0,0,'SNIPER','MELEE'),
('Couteau de combat',300,0,60,0,0,'SNIPER','MELEE'),
('Tenue ultra légère',750,0,0,50,10,'LEGER','DEFENSIVE'),
('Grenade lacrymogène',2500,0,0,0,25,'LEGER','DEFENSIVE'),
('Gilet pare-balles léger',500,0,0,50,0,'TIREUR','DEFENSIVE'),
('Gilet pare-balles moyen',1000,0,0,100,0,'TIREUR','DEFENSIVE'),
('Équipement militaire complet',2000,0,0,200,0,'TIREUR','DEFENSIVE'),
('Protection dorsale',250,0,0,30,0,'MASTODONTE','DEFENSIVE'),
('Bouclier anti-émeutes',1500,0,0,150,0,'MASTODONTE','DEFENSIVE'),
('Bouclier balistique',3000,0,0,300,0,'MASTODONTE','DEFENSIVE'),
('Grenade assourdissante',1250,0,0,0,15,'LEGER','DEFENSIVE'),
('Tenue légère en fibre chauffante',3000,0,0,100,20,'LEGER','DEFENSIVE'),
('Armure conductrice',1750,0,0,100,0,'ELEMENTAIRE','DEFENSIVE'),
('Armure thermorésistante',2250,0,0,200,0,'ELEMENTAIRE','DEFENSIVE'),
('Armure isolante',2250,0,0,200,0,'ELEMENTAIRE','DEFENSIVE'),
('Armure thermique',2750,0,0,200,0,'ELEMENTAIRE','DEFENSIVE'),
('Protège-dents',150,0,0,20,0,'PILOTE_DESTRUCTEUR','DEFENSIVE'),
('Casque militaire',750,0,0,75,0,'PILOTE_DESTRUCTEUR','DEFENSIVE'),
('Gilet Kevlar',1500,0,0,150,0,'PILOTE_DESTRUCTEUR','DEFENSIVE'),
('Treillis de camouflage urbain',1500,0,0,100,10,'SNIPER','DEFENSIVE'),
('Gilet de camouflage optique',2500,0,0,125,10,'SNIPER','DEFENSIVE');