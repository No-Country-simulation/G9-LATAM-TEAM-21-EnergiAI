package com.hackathon.energia.security;

import com.hackathon.energia.exception.TokenInvalidoException;
import com.hackathon.energia.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResfreshTokenService {

    private final RefreshTokenRepository repository;

    @Value("${api.security.token.refresh-expiration-days}")
    private long refreshDays;

    @Transactional
    public String crear(Usuario usuario) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        String hash = sha256(raw);

        var refreshToken = new RefreshToken(
                hash,
                usuario,
                Instant.now().plus(refreshDays, ChronoUnit.DAYS)
        );
        repository.save(refreshToken);
        return raw;
    }

    @Transactional(readOnly = true)
    public Usuario validarYObtenerUsuario(String rawToken) {
        String hash = sha256(rawToken);

        var token = repository.findByTokenHashAndRevocadoFalse(hash)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token invalido"));

        if (token.getExpiraEn().isBefore(Instant.now())) {
            throw new TokenInvalidoException("Refresh token expirado");
        }

        return token.getUsuario();
    }

    @Transactional
    public void revocar(String rawToken) {
        String hash = sha256(rawToken);

        var token = repository.findByTokenHashAndRevocadoFalse(hash)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token invalido"));

        token.setRevocado();
        repository.save(token);
    }

    private String sha256(String raw) {
        try {
            MessageDigest mg = MessageDigest.getInstance("SHA-256");
            byte[] hash = mg.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear el token",e);
        }

    }

}
