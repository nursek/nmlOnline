-- ⚠️ ATTENTION : Données de seed pour le développement UNIQUEMENT.
-- Ces comptes NE DOIVENT PAS être utilisés en production.
-- En production, supprimer ce fichier ou utiliser un profil dédié.
-- Les ressources, équipements et compatibilités sont chargés depuis les fichiers CSV

CREATE TABLE IF NOT EXISTS CREDENTIALS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    refresh_token_expiry BIGINT,
    role VARCHAR(255)
);

INSERT INTO CREDENTIALS (username, password) VALUES ('test', '$2a$10$WMsQsnTZ/7pFn.klPSeJ0.m0B1bnsAt9wFgkIduzvmkMF2PzvAOUq');
INSERT INTO CREDENTIALS (username, password) VALUES ('a', '$2a$12$ca/.P6xWRGFiH5Ra0UXMk.NhNBxYgCX5aEYDDG3nv9CsaZ1FExMnm');
INSERT INTO CREDENTIALS (username, password) VALUES ('lurio', '$2a$10$PoKeBxBu4AhIM9yMbEUIzOf8SHbdHC8/A5BHqq9jkUT.YiZbsXZNe');
INSERT INTO CREDENTIALS (username, password) VALUES ('nursek', '$2a$10$X41e/q5zcdbR8T5AMatbFuaXhj.E2fvEJ7DivsuqSlNeY97mrI0mW');
INSERT INTO CREDENTIALS (username, password, role) VALUES ('admin', '$2a$10$tczM1DeXDnedUozenHXkWO4Mt32jZ5g8ZBUfYWGzhtISSiQOrfFWq', 'ADMIN');
