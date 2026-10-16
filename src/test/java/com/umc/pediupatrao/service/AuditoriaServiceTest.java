package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {
    @Mock private AuditoriaRepository auditoriaRepository;
    @InjectMocks private AuditoriaService auditoriaService;

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraAutorPerfilEValoresAlterados() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "balcao.teste", null, AuthorityUtils.createAuthorityList("ROLE_ATENDENTE")));
        Map<String, String> antes = new LinkedHashMap<>();
        antes.put("telefone", "11911112222");
        antes.put("logradouro", "Rua do Balcão");
        Map<String, String> depois = Map.of("telefone", "11933334444", "logradouro", "Rua da Cozinha");

        auditoriaService.registrar("CLIENTE", "cliente-1", "ATUALIZADO", "Cadastro alterado.", antes, depois);

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        RegistroAuditoria registro = captor.getValue();
        assertEquals("balcao.teste", registro.getUsuario());
        assertEquals("ATENDENTE", registro.getPerfil());
        assertEquals("cliente-1", registro.getRegistroId());
        assertEquals("ATUALIZADO", registro.getAcao());
        assertNotNull(registro.getDataHora());
        assertEquals(2, registro.getAlteracoes().size());
        assertEquals("11911112222", registro.getAlteracoes().get(0).getValorAnterior());
        assertEquals("11933334444", registro.getAlteracoes().get(0).getValorNovo());
        assertEquals("Rua da Cozinha", registro.getAlteracoes().get(1).getValorNovo());
    }

    @Test
    void naoCriaEventoQuandoNadaMudou() {
        Map<String, String> dados = Map.of("nome", "Cliente do balcão");
        auditoriaService.registrar("CLIENTE", "cliente-1", "ATUALIZADO", "Cadastro alterado.", dados, dados);
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void listaHistoricoDoMaisRecenteParaOAntigo() {
        auditoriaService.listarTodos();
        verify(auditoriaRepository).findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
    }
}
