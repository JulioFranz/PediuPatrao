package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.nio.charset.StandardCharsets;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public void salvarUsuario(Usuario usuario) {
        if (usuario.getId() != null) {
            throw new IllegalArgumentException("Um novo usuário não pode informar um ID.");
        }
        validarUsername(usuario.getUsername());
        validarSenha(usuario.getPassword());
        validarPerfil(usuario.getRole());
        usuario.setUsername(usuario.getUsername().trim());
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new IllegalArgumentException("Esse username já está em uso.");
        }
        usuario.setRole(PerfilUsuario.fromRole(usuario.getRole()).name());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }

    public void deletarUsuario(String id, String usuarioLogado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuario.getUsername().equals(usuarioLogado)) {
            throw new IllegalArgumentException("Você não pode excluir seu próprio usuário.");
        }
        if (isAdmin(usuario) && usuarioRepository.countByRole(PerfilUsuario.ADMIN.name()) <= 1) {
            throw new IllegalArgumentException("O sistema precisa manter pelo menos um ADMIN.");
        }
        usuarioRepository.deleteById(id);
    }

    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public void atualizarUsuario(String id, Usuario usuarioAtualizado, String usuarioLogado) {
        if (usuarioAtualizado.getId() != null && !id.equals(usuarioAtualizado.getId())) {
            throw new IllegalArgumentException("O ID informado não corresponde ao usuário.");
        }
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        validarPerfil(usuarioAtualizado.getRole());
        validarUsername(usuarioAtualizado.getUsername());

        String novoUsername = usuarioAtualizado.getUsername().trim();
        if (!usuarioExistente.getUsername().equals(novoUsername)
                && usuarioRepository.existsByUsername(novoUsername)) {
            throw new IllegalArgumentException("Esse username já está em uso.");
        }

        String novoRole = PerfilUsuario.fromRole(usuarioAtualizado.getRole()).name();
        if (usuarioExistente.getUsername().equals(usuarioLogado)
                && (!usuarioExistente.getRole().equals(novoRole)
                || !usuarioExistente.getUsername().equals(novoUsername))) {
            throw new IllegalArgumentException("Você não pode alterar seu próprio usuário ou perfil.");
        }
        if (isAdmin(usuarioExistente) && !PerfilUsuario.ADMIN.name().equals(novoRole)
                && usuarioRepository.countByRole(PerfilUsuario.ADMIN.name()) <= 1) {
            throw new IllegalArgumentException("O sistema precisa manter pelo menos um ADMIN.");
        }

        usuarioExistente.setUsername(novoUsername);
        usuarioExistente.setRole(novoRole);
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            validarSenha(usuarioAtualizado.getPassword());
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }
        usuarioRepository.save(usuarioExistente);
    }

    private void validarPerfil(String role) {
        if (PerfilUsuario.fromRole(role) == null) {
            throw new IllegalArgumentException("Selecione um perfil válido.");
        }
    }

    private void validarUsername(String username) {
        if (username == null || !username.trim().matches("[a-zA-Z0-9._-]{3,80}")) {
            throw new IllegalArgumentException("Use de 3 a 80 letras, números, ponto, hífen ou underline no username.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.isBlank() || senha.length() < 8
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres e no máximo 72 bytes.");
        }
    }

    private boolean isAdmin(Usuario usuario) {
        return PerfilUsuario.ADMIN.name().equalsIgnoreCase(usuario.getRole());
    }
}
