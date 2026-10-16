package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.*;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock private PedidoRepository pedidoRepository;
    @Mock private ProdutoService produtoService;
    @Mock private ClienteService clienteService;
    @Mock private AuditoriaService auditoriaService;
    @InjectMocks private PedidoService pedidoService;

    @BeforeEach
    void entrar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gerente.balcao", null));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculaItensERegistraEntradaSemAceitarTotalInformado() {
        Pedido pedido = new Pedido();
        pedido.setClienteId("cliente-balcao");
        pedido.setItens(List.of(item("pizza", 2, "0"), item("bebida", 1, "0")));
        pedido.setValorTotal(BigDecimal.ONE);
        pedido.setDescontoPercentual(new BigDecimal("99"));
        pedido.setResponsavelRegistro("outro");
        pedido.setDataHoraSaida(new Date());
        when(clienteService.buscarPorId("cliente-balcao")).thenReturn(Optional.of(new Cliente()));
        when(produtoService.buscarPorId("pizza")).thenReturn(Optional.of(produto("pizza", 25.50)));
        when(produtoService.buscarPorId("bebida")).thenReturn(Optional.of(produto("bebida", 7.0)));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        Pedido salvo = pedidoService.criarPedido(pedido);

        assertEquals(new BigDecimal("58.00"), salvo.getValorTotal());
        assertEquals(new BigDecimal("25.50"), salvo.getItens().get(0).getPrecoUnitario());
        assertEquals(BigDecimal.ZERO, salvo.getDescontoPercentual());
        assertEquals("gerente.balcao", salvo.getResponsavelRegistro());
        assertEquals("RECEBIDO", salvo.getStatus());
        assertNotNull(salvo.getDataHoraEntrada());
        assertNull(salvo.getDataHoraSaida());
        verify(auditoriaService).registrar(eq("PEDIDO"), any(), eq("CRIADO"), anyString(), anyMap(), anyMap());
    }

    @Test
    void recusaQuantidadeZero() {
        Pedido pedido = new Pedido();
        pedido.setClienteId("cliente-balcao");
        pedido.setItens(List.of(item("pizza", 0, "20")));
        when(clienteService.buscarPorId("cliente-balcao")).thenReturn(Optional.of(new Cliente()));

        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(pedido));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void descontoSubstituiOAnterior() {
        Pedido pedido = pedidoCadastrado("RECEBIDO");
        pedido.setDescontoPercentual(new BigDecimal("20"));
        pedido.setValorTotal(new BigDecimal("32.00"));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.aplicarDesconto("pedido-balcao", new BigDecimal("10"));

        assertEquals(new BigDecimal("36.00"), salvo.getValorTotal());
        assertEquals(new BigDecimal("10"), salvo.getDescontoPercentual());
        verify(auditoriaService).registrar(eq("PEDIDO"), eq("pedido-balcao"), eq("DESCONTO_ATUALIZADO"), anyString(), anyMap(), anyMap());
    }

    @Test
    void recusaDescontoAcimaDoLimite() {
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.aplicarDesconto("pedido-balcao", new BigDecimal("21")));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void recusaDescontoDepoisDaSaida() {
        pedidoCadastrado("SAIU_PARA_ENTREGA");
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.aplicarDesconto("pedido-balcao", BigDecimal.TEN));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void registraHorarioEResponsavelNaSaida() {
        Pedido pedido = pedidoCadastrado("PRONTO");
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.atualizarStatus("pedido-balcao", "SAIU_PARA_ENTREGA", null);

        assertNotNull(salvo.getDataHoraSaida());
        assertEquals("gerente.balcao", salvo.getResponsavelSaida());
        assertEquals("SAIU_PARA_ENTREGA", salvo.getStatus());
    }

    @Test
    void naoPermitePularEtapas() {
        pedidoCadastrado("RECEBIDO");
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus("pedido-balcao", "FINALIZADO", null));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelamentoExigeJustificativa() {
        pedidoCadastrado("RECEBIDO");
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus("pedido-balcao", "CANCELADO", " "));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelaMantendoItensETotal() {
        Pedido pedido = pedidoCadastrado("EM_PREPARACAO");
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido salvo = pedidoService.atualizarStatus("pedido-balcao", "CANCELADO", " Compra repetida ");

        assertEquals("CANCELADO", salvo.getStatus());
        assertEquals("Compra repetida", salvo.getJustificativaCancelamento());
        assertEquals(new BigDecimal("40.00"), salvo.getValorTotal());
        assertEquals(1, salvo.getItens().size());
        verify(auditoriaService).registrar(eq("PEDIDO"), eq("pedido-balcao"), eq("CANCELADO"), anyString(), anyMap(), anyMap());
    }

    @Test
    void pedidoCanceladoNaoMudaDeStatus() {
        pedidoCadastrado("CANCELADO");
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus("pedido-balcao", "EM_PREPARACAO", null));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void fluxoAceitaEntregaOuRetiradaAntesDaFinalizacao() {
        assertTrue(StatusPedido.RECEBIDO.podeAvancarPara(StatusPedido.EM_PREPARACAO));
        assertTrue(StatusPedido.EM_PREPARACAO.podeAvancarPara(StatusPedido.PRONTO));
        assertTrue(StatusPedido.PRONTO.podeAvancarPara(StatusPedido.SAIU_PARA_ENTREGA));
        assertTrue(StatusPedido.PRONTO.podeAvancarPara(StatusPedido.RETIRADO));
        assertTrue(StatusPedido.SAIU_PARA_ENTREGA.podeAvancarPara(StatusPedido.FINALIZADO));
        assertTrue(StatusPedido.RETIRADO.podeAvancarPara(StatusPedido.FINALIZADO));
        assertFalse(StatusPedido.RETIRADO.podeAvancarPara(StatusPedido.CANCELADO));
        assertFalse(StatusPedido.FINALIZADO.podeAvancarPara(StatusPedido.CANCELADO));
    }

    private Pedido pedidoCadastrado(String status) {
        Pedido pedido = new Pedido();
        pedido.setId("pedido-balcao");
        pedido.setStatus(status);
        pedido.setItens(List.of(item("pizza", 2, "20.00")));
        pedido.setValorTotal(new BigDecimal("40.00"));
        when(pedidoRepository.findById("pedido-balcao")).thenReturn(Optional.of(pedido));
        return pedido;
    }

    private ItemPedido item(String produtoId, int quantidade, String preco) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(produtoId);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(new BigDecimal(preco));
        return item;
    }

    private Produto produto(String id, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(id);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
