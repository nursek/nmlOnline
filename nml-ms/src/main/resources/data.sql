CREATE TABLE IF NOT EXISTS CREDENTIALS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    money INT NOT NULL,
    refresh_token_hash VARCHAR(255)
);
INSERT INTO CREDENTIALS (username, password, money) VALUES ('test', '$2a$10$WMsQsnTZ/7pFn.klPSeJ0.m0B1bnsAt9wFgkIduzvmkMF2PzvAOUq', 100);
INSERT INTO CREDENTIALS (username, password, money) VALUES ('a', '$2a$12$ca/.P6xWRGFiH5Ra0UXMk.NhNBxYgCX5aEYDDG3nv9CsaZ1FExMnm', 100);
INSERT INTO CREDENTIALS (username, password, money) VALUES ('lurio', '$2y$10$PoKeBxBu4AhIM9yMbEUIzOf8SHbdHC8/A5BHqq9jkUT.YiZbsXZNe', 2900);
INSERT INTO CREDENTIALS (username, password, money) VALUES ('nursek', '$2y$10$X41e/q5zcdbR8T5AMatbFuaXhj.E2fvEJ7DivsuqSlNeY97mrI0mW', 3000);