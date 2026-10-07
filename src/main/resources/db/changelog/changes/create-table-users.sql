--liquibase formatted sql

--changeset dior:1
create table users(
    id SERIAL primary key,
    username varchar(255) UNIQUE NOT NULL,
    password varchar(255) NOT NULL,
    enabled boolean default true,
    account_non_locked boolean default true,
    credentials_non_expired boolean default true,
    account_non_expired boolean default true
);
--rollback drop table users;