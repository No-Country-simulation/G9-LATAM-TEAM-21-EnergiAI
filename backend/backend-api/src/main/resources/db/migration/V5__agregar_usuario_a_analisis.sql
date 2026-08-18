ALTER TABLE analisis_energetico
    ADD COLUMN usuario_id VARCHAR(36) NULL,
    ADD CONSTRAINT fk_analisis_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

CREATE INDEX idx_analisis_usuario_id ON analisis_energetico(usuario_id);
