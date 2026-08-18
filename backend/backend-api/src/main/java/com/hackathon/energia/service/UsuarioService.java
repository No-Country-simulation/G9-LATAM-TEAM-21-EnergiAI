package com.hackathon.energia.service;

import com.hackathon.energia.dto.DatosRegistroUsuario;
import com.hackathon.energia.exception.ResourceAlreadyExistsException;
import com.hackathon.energia.model.Usuario;
import com.hackathon.energia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public Usuario registro(DatosRegistroUsuario datos) {
        if (repository.existsByLogin(datos.login())) {
            throw new ResourceAlreadyExistsException("Login ya registrado");
        }

        var usuario = new Usuario(datos.login(), encoder.encode(datos.password()));
        repository.save(usuario);
        return usuario;
    }

}
