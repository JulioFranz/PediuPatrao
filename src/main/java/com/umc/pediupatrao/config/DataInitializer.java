package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (repo.count() == 0 && !adminPassword.isBlank()) {
                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(passwordEncoder.encode(adminPassword));
                user.setRole(PerfilUsuario.ADMIN.name());

                repo.save(user);
                System.out.println("Usuário admin criado!");
            }

            repo.findAll().forEach(usuario -> {
                PerfilUsuario perfil = PerfilUsuario.fromRole(usuario.getRole());
                if (perfil == null || "USER".equalsIgnoreCase(usuario.getRole())) {
                    perfil = PerfilUsuario.ATENDENTE;
                }
                if (!perfil.name().equals(usuario.getRole())) {
                    usuario.setRole(perfil.name());
                    repo.save(usuario);
                }
            });
        };
    }
}
