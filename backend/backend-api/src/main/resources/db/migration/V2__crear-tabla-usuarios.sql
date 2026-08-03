CREATE TABLE usuarios(
    id              VARCHAR(36) NOT NULL,
    login           VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE (login)
);

