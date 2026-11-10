package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final PedidoRepository pedidos;

    public DashboardService(PedidoRepository pedidos) {
        this.pedidos = pedidos;
    }

    public Resumo resumo(String inicio, String fim) {
        Periodo periodo = periodo(inicio, fim);
        List<Pedido> lista = consultar(periodo);
        Map<String, Long> totais = new LinkedHashMap<>();
        for (StatusPedido status : StatusPedido.values()) {
            totais.put(status.getDescricao(), 0L);
        }

        BigDecimal faturamento = BigDecimal.ZERO;
        long finalizados = 0;
        long comValor = 0;
        for (Pedido pedido : lista) {
            StatusPedido status = status(pedido);
            totais.merge(status == null ? "Não informado" : status.getDescricao(), 1L, Long::sum);
            if (status == StatusPedido.FINALIZADO) {
                finalizados++;
                if (pedido.getValorTotal() != null) {
                    faturamento = faturamento.add(pedido.getValorTotal());
                    comValor++;
                }
            }
        }

        BigDecimal ticket = comValor == 0 ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(comValor), 2, RoundingMode.HALF_UP);
        List<Situacao> situacoes = new ArrayList<>();
        totais.forEach((nome, quantidade) -> situacoes.add(new Situacao(nome, quantidade)));
        return new Resumo(periodo.inicio(), periodo.fim(), lista.size(), finalizados,
                faturamento, ticket, finalizados - comValor, situacoes);
    }

    public List<VendaDia> vendasPorDia(String inicio, String fim) {
        Periodo periodo = periodo(inicio, fim);
        Map<LocalDate, BigDecimal> dias = new LinkedHashMap<>();
        for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
            dias.put(dia, BigDecimal.ZERO);
        }
        for (Pedido pedido : consultar(periodo)) {
            if (status(pedido) == StatusPedido.FINALIZADO && pedido.getValorTotal() != null) {
                LocalDate dia = pedido.getDataHoraEntrada().toInstant().atZone(FUSO).toLocalDate();
                dias.merge(dia, pedido.getValorTotal(), BigDecimal::add);
            }
        }
        List<VendaDia> vendas = new ArrayList<>();
        dias.forEach((dia, valor) -> vendas.add(new VendaDia(dia, valor)));
        return vendas;
    }

    public List<ProdutoVendido> produtosMaisVendidos(String inicio, String fim) {
        Map<String, ProdutoVendido> ranking = new HashMap<>();
        for (Pedido pedido : consultar(periodo(inicio, fim))) {
            if (status(pedido) != StatusPedido.FINALIZADO || pedido.getItens() == null) {
                continue;
            }
            for (ItemPedido item : pedido.getItens()) {
                if (item == null || item.getProdutoId() == null || item.getProdutoId().isBlank()
                        || item.getNomeProduto() == null || item.getNomeProduto().isBlank()
                        || item.getQuantidade() <= 0) {
                    continue;
                }
                String id = item.getProdutoId();
                ProdutoVendido anterior = ranking.get(id);
                String nome = item.getNomeProduto();
                long quantidade = item.getQuantidade();
                if (anterior != null) {
                    quantidade += anterior.quantidade();
                    nome = anterior.nome();
                }
                ranking.put(id, new ProdutoVendido(id, nome, quantidade));
            }
        }
        return ranking.values().stream()
                .sorted(Comparator.comparingLong(ProdutoVendido::quantidade).reversed()
                        .thenComparing(ProdutoVendido::nome).thenComparing(ProdutoVendido::id))
                .limit(5)
                .toList();
    }

    private List<Pedido> consultar(Periodo periodo) {
        Date inicio = Date.from(periodo.inicio().atStartOfDay(FUSO).toInstant());
        Date fim = Date.from(periodo.fim().plusDays(1).atStartOfDay(FUSO).toInstant());
        return pedidos.buscarNoPeriodo(inicio, fim);
    }

    private Periodo periodo(String inicio, String fim) {
        if (inicio == null && fim == null) {
            LocalDate hoje = LocalDate.now(FUSO);
            return new Periodo(hoje.minusDays(6), hoje);
        }
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe a data inicial e a data final.");
        }

        LocalDate primeiro = lerData(inicio);
        LocalDate ultimo = lerData(fim);
        long dias = ChronoUnit.DAYS.between(primeiro, ultimo);
        if (dias < 0 || dias >= 366) {
            throw new IllegalArgumentException("Selecione um período em ordem com até 366 dias.");
        }
        return new Periodo(primeiro, ultimo);
    }

    private LocalDate lerData(String texto) {
        try {
            if (texto.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") && !texto.startsWith("0000-")) {
                return LocalDate.parse(texto);
            }
        } catch (DateTimeParseException erro) {
            throw new IllegalArgumentException("Use datas válidas no formato AAAA-MM-DD.");
        }
        throw new IllegalArgumentException("Use datas válidas no formato AAAA-MM-DD.");
    }

    private StatusPedido status(Pedido pedido) {
        try {
            return StatusPedido.fromValor(pedido.getStatus());
        } catch (IllegalArgumentException erro) {
            return null;
        }
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {}
    public record Situacao(String status, long quantidade) {}
    public record VendaDia(LocalDate data, BigDecimal faturamento) {}
    public record ProdutoVendido(String id, String nome, long quantidade) {}
    public record Resumo(LocalDate inicio, LocalDate fim, long pedidos, long finalizados,
                         BigDecimal faturamento, BigDecimal ticketMedio, long finalizadosSemValor,
                         List<Situacao> situacoes) {}
}
