package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock private ClienteRepository clienteRepository;
    @Mock private AuditoriaService auditoriaService;
    @InjectMocks private ClienteService clienteService;
    @Captor private ArgumentCaptor<Map<String, String>> anteriores;
    @Captor private ArgumentCaptor<Map<String, String>> novos;

    @Test
    void edicaoEnviaTelefoneEEnderecoAnterioresParaAuditoria() {
        Cliente antes = cliente("11911112222", "Rua do Balcão");
        Cliente depois = cliente("11933334444", "Rua da Cozinha");
        when(clienteRepository.findById("cliente-1")).thenReturn(Optional.of(antes));
        when(clienteRepository.save(depois)).thenReturn(depois);

        clienteService.salvar(depois);

        verify(auditoriaService).registrar(eq("CLIENTE"), eq("cliente-1"), eq("ATUALIZADO"),
                anyString(), anteriores.capture(), novos.capture());
        assertEquals("11911112222", anteriores.getValue().get("telefone"));
        assertEquals("11933334444", novos.getValue().get("telefone"));
        assertEquals("Rua do Balcão", anteriores.getValue().get("logradouro"));
        assertEquals("Rua da Cozinha", novos.getValue().get("logradouro"));
    }

    @Test
    void recusaTelefoneIncompleto() {
        Cliente cliente = cliente("123", "Rua do Balcão");
        assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(cliente));
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(auditoriaService);
    }

    private Cliente cliente(String telefone, String rua) {
        Cliente cliente = new Cliente();
        cliente.setId("cliente-1");
        cliente.setNome("Cliente do balcão");
        cliente.setTelefone(telefone);
        cliente.setLogradouro(rua);
        return cliente;
    }
}
