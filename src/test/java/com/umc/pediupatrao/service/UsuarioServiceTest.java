package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    @Test
    void salvaUsuarioComSenhaProtegidaEPerfilValido() {
        Usuario usuario = usuario("balcao.teste", "atendente");
        usuario.setPassword("senha-de-teste");
        when(passwordEncoder.encode("senha-de-teste")).thenReturn("senha-protegida");

        usuarioService.salvarUsuario(usuario);

        assertEquals("ATENDENTE", usuario.getRole());
        assertEquals("senha-protegida", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cadastroNaoPodeSobrescreverIdExistente() {
        Usuario usuario = usuario("balcao.teste", "ADMIN");
        usuario.setId("usuario-existente");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.salvarUsuario(usuario));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void recusaPerfilDesconhecido() {
        Usuario usuario = usuario("balcao.teste", "DONO");
        usuario.setPassword("senha-de-teste");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.salvarUsuario(usuario));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioNaoPodeMudarProprioPerfil() {
        Usuario atual = usuario("admin.teste", "ADMIN");
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(atual));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizarUsuario(
                "usuario-1", usuario("admin.teste", "GERENTE"), "admin.teste"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioNaoPodeExcluirPropriaConta() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario("admin.teste", "ADMIN")));
        assertThrows(IllegalArgumentException.class, () -> usuarioService.deletarUsuario("usuario-1", "admin.teste"));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void preservaUltimoAdministrador() {
        when(usuarioRepository.findById("usuario-1")).thenReturn(Optional.of(usuario("admin.teste", "ADMIN")));
        when(usuarioRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.deletarUsuario("usuario-1", "outro.admin"));
        assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizarUsuario(
                "usuario-1", usuario("admin.teste", "GERENTE"), "outro.admin"));
        verify(usuarioRepository, never()).deleteById(any());
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuario(String nome, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setUsername(nome);
        usuario.setRole(perfil);
        return usuario;
    }
}
