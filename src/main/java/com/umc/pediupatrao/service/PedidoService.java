package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private ClienteService clienteService;

    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getId() != null) {
            throw new IllegalArgumentException("Um novo pedido não pode informar um ID.");
        }
        if (pedido.getClienteId() == null || clienteService.buscarPorId(pedido.getClienteId()).isEmpty()) {
            throw new IllegalArgumentException("Selecione um cliente cadastrado.");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um item ao pedido.");
        }

        BigDecimal total = BigDecimal.ZERO;
        ArrayList<ItemPedido> itensCalculados = new ArrayList<>();
        for (ItemPedido itemRecebido : pedido.getItens()) {
            if (itemRecebido == null || itemRecebido.getQuantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade dos itens deve ser maior que zero.");
            }
            if (itemRecebido.getProdutoId() == null || itemRecebido.getProdutoId().isBlank()) {
                throw new IllegalArgumentException("Selecione um produto para cada item.");
            }
            Produto produto = produtoService.buscarPorId(itemRecebido.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
            if (Boolean.FALSE.equals(produto.getAtivo()) || produto.getPreco() == null
                    || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0) {
                throw new IllegalArgumentException("O produto selecionado não está disponível.");
            }

            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = preco.multiply(BigDecimal.valueOf(itemRecebido.getQuantidade()))
                    .setScale(2, RoundingMode.HALF_UP);
            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(itemRecebido.getQuantidade());
            item.setPrecoUnitario(preco);
            item.setSubtotal(subtotal);
            itensCalculados.add(item);
            total = total.add(subtotal);
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        pedido.setItens(itensCalculados);
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        pedido.setValorTotal(total.setScale(2, RoundingMode.HALF_UP));
        pedido.setResponsavelRegistro(autenticacao == null ? "sistema" : autenticacao.getName());
        pedido.setDataHoraEntrada(new Date());
        pedido.setDataHoraSaida(null);
        pedido.setResponsavelSaida(null);
        pedido.setStatus(StatusPedido.RECEBIDO.name());
        pedido.setJustificativaCancelamento(null);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", salvo.getId(), "CRIADO",
                "Pedido registrado. Total: " + salvo.getValorTotal(), Map.of(), dadosAuditoria(salvo));
        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        if (percentual == null || percentual.signum() < 0
                || percentual.compareTo(new BigDecimal("20")) > 0) {
            throw new IllegalArgumentException("O desconto deve ser de 0% a 20%.");
        }
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        StatusPedido status = StatusPedido.fromValor(pedido.getStatus());
        if (pedido.getDataHoraSaida() != null || (status != StatusPedido.RECEBIDO
                && status != StatusPedido.EM_PREPARACAO && status != StatusPedido.PRONTO)) {
            throw new IllegalArgumentException("O desconto só pode ser alterado antes da saída e do cancelamento.");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("Esse pedido antigo não possui itens com preços registrados.");
        }

        BigDecimal valorItens = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            if (item == null || item.getPrecoUnitario() == null
                    || item.getPrecoUnitario().signum() < 0 || item.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Os itens do pedido possuem valores inválidos.");
            }
            valorItens = valorItens.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }

        BigDecimal descontoAnterior = pedido.getDescontoPercentual() == null
                ? BigDecimal.ZERO : pedido.getDescontoPercentual();
        BigDecimal totalAnterior = pedido.getValorTotal();
        Map<String, String> dadosAnteriores = dadosAuditoria(pedido);
        BigDecimal novoTotal = valorItens.multiply(BigDecimal.ONE.subtract(percentual.movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
        pedido.setDescontoPercentual(percentual);
        pedido.setValorTotal(novoTotal);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("PEDIDO", id, "DESCONTO_ATUALIZADO",
                "Desconto: " + descontoAnterior + "% para " + percentual
                + "%. Total: " + totalAnterior + " para " + novoTotal + ".",
                dadosAnteriores, dadosAuditoria(salvo));
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido atualizarStatus(String id, String status, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
        StatusPedido atual = StatusPedido.fromValor(pedido.getStatus());
        StatusPedido proximo = StatusPedido.fromValor(status);
        if (!atual.podeAvancarPara(proximo)) {
            throw new IllegalArgumentException("Essa mudança de status não é permitida.");
        }
        if (proximo == StatusPedido.CANCELADO && (justificativa == null || justificativa.isBlank())) {
            throw new IllegalArgumentException("Informe a justificativa do cancelamento.");
        }
        if (proximo == StatusPedido.CANCELADO && justificativa.length() > 300) {
            throw new IllegalArgumentException("Use até 300 caracteres na justificativa.");
        }
        Map<String, String> dadosAnteriores = dadosAuditoria(pedido);
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (proximo == StatusPedido.SAIU_PARA_ENTREGA || proximo == StatusPedido.RETIRADO) {
            pedido.setDataHoraSaida(new Date());
            pedido.setResponsavelSaida(autenticacao == null ? "sistema" : autenticacao.getName());
        }
        pedido.setStatus(proximo.name());
        if (proximo == StatusPedido.CANCELADO) {
            pedido.setJustificativaCancelamento(justificativa.trim());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        String acao = proximo == StatusPedido.CANCELADO ? "CANCELADO" : "STATUS_ATUALIZADO";
        String detalhes = "Status alterado de " + atual.name() + " para " + proximo.name() + ".";
        if (proximo == StatusPedido.CANCELADO) {
            detalhes += " Justificativa: " + justificativa.trim();
        }
        Map<String, String> dadosNovos = dadosAuditoria(salvo);
        auditoriaService.registrar("PEDIDO", salvo.getId(), acao, detalhes, dadosAnteriores, dadosNovos);
        return salvo;
    }

    private Map<String, String> dadosAuditoria(Pedido pedido) {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("clienteId", pedido.getClienteId());
        dados.put("status", pedido.getStatus());
        dados.put("justificativaCancelamento", pedido.getJustificativaCancelamento());
        dados.put("descontoPercentual", valorDecimal(pedido.getDescontoPercentual()));
        dados.put("valorTotal", valorDecimal(pedido.getValorTotal()));
        dados.put("responsavelRegistro", pedido.getResponsavelRegistro());
        dados.put("dataHoraEntrada", valorData(pedido.getDataHoraEntrada()));
        dados.put("responsavelSaida", pedido.getResponsavelSaida());
        dados.put("dataHoraSaida", valorData(pedido.getDataHoraSaida()));

        StringBuilder itens = new StringBuilder();
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                if (item == null) {
                    continue;
                }
                if (!itens.isEmpty()) {
                    itens.append("; ");
                }
                itens.append(item.getProdutoId()).append(" - ").append(item.getNomeProduto())
                        .append(" | quantidade: ").append(item.getQuantidade())
                        .append(" | preco: ").append(valorDecimal(item.getPrecoUnitario()))
                        .append(" | subtotal: ").append(valorDecimal(item.getSubtotal()));
            }
        }
        dados.put("itens", itens.toString());
        return dados;
    }

    private String valorDecimal(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros().toPlainString();
    }

    private String valorData(Date valor) {
        return valor == null ? null : valor.toInstant().toString();
    }

}
