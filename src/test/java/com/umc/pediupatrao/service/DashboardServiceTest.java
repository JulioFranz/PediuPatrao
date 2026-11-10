package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    private final PedidoRepository repositorio = mock(PedidoRepository.class);
    private final DashboardService dashboard = new DashboardService(repositorio);

    @Test
    void somaSomenteFinalizadosComValorEContaTodosOsStatus() {
        dados(pedido("FINALIZADO", "36.50", 2), pedido("CANCELADO", "100", 10),
                pedido("ENTREGUE", "20.00", 1), pedido("FINALIZADO", null, 1));

        var resumo = dashboard.resumo("2026-11-01", "2026-11-02");

        assertEquals(4, resumo.pedidos());
        assertEquals(3, resumo.finalizados());
        assertEquals(1, resumo.finalizadosSemValor());
        assertEquals(new BigDecimal("56.50"), resumo.faturamento());
        assertEquals(new BigDecimal("28.25"), resumo.ticketMedio());
        assertTrue(resumo.situacoes().contains(new DashboardService.Situacao("Cancelado", 1)));
    }

    @Test
    void usaDatasDeSaoPauloEPreencheDiasSemVendas() {
        dados(pedido("FINALIZADO", "12.00", 1));

        var dias = dashboard.vendasPorDia("2026-11-01", "2026-11-02");

        assertEquals(new BigDecimal("12.00"), dias.get(0).faturamento());
        assertEquals(BigDecimal.ZERO, dias.get(1).faturamento());
        verify(repositorio).buscarNoPeriodo(data("2026-11-01T03:00:00Z"), data("2026-11-03T03:00:00Z"));
    }

    @Test
    void agrupaProdutosPeloIdESoConsideraPedidosFinalizados() {
        dados(pedido("FINALIZADO", "10.00", 2), pedido("FINALIZADO", "20.00", 4),
                pedido("RECEBIDO", "50.00", 50));

        var ranking = dashboard.produtosMaisVendidos("2026-11-01", "2026-11-02");

        assertEquals(List.of(new DashboardService.ProdutoVendido("pizza", "Pizza da casa", 6)), ranking);
    }

    @Test
    void recusaPeriodoInvalidoAntesDeConsultarOBanco() {
        assertThrows(IllegalArgumentException.class, () -> dashboard.resumo("2026-02-30", "2026-03-01"));
        assertThrows(IllegalArgumentException.class, () -> dashboard.resumo("2026-03-02", "2026-03-01"));
        assertThrows(IllegalArgumentException.class, () -> dashboard.resumo("2026-01-01", "2027-02-01"));
        assertThrows(IllegalArgumentException.class, () -> dashboard.resumo("2026-11-01", null));
        verifyNoInteractions(repositorio);
    }

    private void dados(Pedido... pedidos) {
        when(repositorio.buscarNoPeriodo(data("2026-11-01T03:00:00Z"), data("2026-11-03T03:00:00Z")))
                .thenReturn(List.of(pedidos));
    }

    private Pedido pedido(String status, String total, int quantidade) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setDataHoraEntrada(data("2026-11-02T01:00:00Z"));
        pedido.setValorTotal(total == null ? null : new BigDecimal(total));
        ItemPedido item = new ItemPedido();
        item.setProdutoId("pizza");
        item.setNomeProduto("Pizza da casa");
        item.setQuantidade(quantidade);
        pedido.setItens(List.of(item));
        return pedido;
    }

    private Date data(String instante) {
        return Date.from(Instant.parse(instante));
    }
}
