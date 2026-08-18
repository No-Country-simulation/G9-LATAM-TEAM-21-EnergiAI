CREATE TABLE refresh_tokens(
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    token_hash          VARCHAR(64) NOT NULL,
    usuario_id          VARCHAR(36) NOT NULL,
    expira_en           TIMESTAMP NOT NULL,
    revocado            BOOLEAN NOT NULL DEFAULT FALSE,

    UNIQUE (token_hash),
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);