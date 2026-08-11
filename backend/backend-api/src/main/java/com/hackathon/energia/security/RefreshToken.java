package com.hackathon.energia.security;

import com.hackathon.energia.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private Instant expiraEn;
    private boolean revocado;

    public RefreshToken(String tokenHash, Usuario usuario, Instant expiraEn) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEn = expiraEn;
        this.revocado = false;
    }

    public void setRevocado() {
        this.revocado = true;
    }
}
