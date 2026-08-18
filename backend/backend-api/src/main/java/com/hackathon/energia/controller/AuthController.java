package com.hackathon.energia.controller;

import com.hackathon.energia.dto.DatosLoginUsuario;
import com.hackathon.energia.dto.DatosRefreshToken;
import com.hackathon.energia.dto.DatosRegistroUsuario;
import com.hackathon.energia.dto.DatosTokenJWT;
import com.hackathon.energia.model.Usuario;
import com.hackathon.energia.security.RefreshTokenService;
import com.hackathon.energia.security.TokenService;
import com.hackathon.energia.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UsuarioService usuarioService;

    @PostMapping("/register")
    public ResponseEntity<DatosTokenJWT> register(@RequestBody @Valid DatosRegistroUsuario datos) {
        var usuario = usuarioService.registro(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(emitirTokens(usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<DatosTokenJWT> login(@RequestBody @Valid DatosLoginUsuario datos) {
        var auth = manager.authenticate(
                new UsernamePasswordAuthenticationToken(datos.login(), datos.password()));
        var usuario = (Usuario) auth.getPrincipal();
        return ResponseEntity.ok(emitirTokens(usuario));
    }

    @PostMapping("/refresh")
    public ResponseEntity<DatosTokenJWT> refresh(@RequestBody @Valid DatosRefreshToken datos) {
        var usuario = refreshTokenService.validarYObtenerUsuario(datos.refreshToken());
        refreshTokenService.revocar(datos.refreshToken()); // rotación
        return ResponseEntity.ok(emitirTokens(usuario));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid DatosRefreshToken datos) {
        refreshTokenService.revocar(datos.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private DatosTokenJWT emitirTokens(Usuario u) {
        return new DatosTokenJWT(
                tokenService.generarAccessToken(u),
                refreshTokenService.crear(u)
        );
    }
}
