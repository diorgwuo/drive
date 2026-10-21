--liquibase formatted sql

--changeset dior:1
CREATE TABLE users(
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled boolean DEFAULT TRUE
);
--rollback drop table users;