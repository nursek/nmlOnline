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
    money INT NOT NULL,
    army BLOB,
    equipment BLOB
);
INSERT INTO PLAYERS (username, money, army, equipment) VALUES ('nursek', 3000, NULL, NULL);
INSERT INTO PLAYERS (username, money, army, equipment) VALUES ('lurio', 2900, NULL, NULL);