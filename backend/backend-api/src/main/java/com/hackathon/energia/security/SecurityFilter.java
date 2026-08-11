package com.hackathon.energia.security;

import com.hackathon.energia.exception.TokenInvalidoException;
import com.hackathon.energia.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        var token = recuperarToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
        }

        try{
            var subject = tokenService.getSubject(token);
            repository.findByLogin(subject).ifPresent(usuario -> {
                if (usuario.isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        } catch (TokenInvalidoException ex) {
            // No setear contexto; Spring devolverá 401 en rutas protegidas
        }
    }

    private String recuperarToken(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
