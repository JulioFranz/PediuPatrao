package com.umc.pediupatrao.entity;

import java.util.Locale;
import java.util.Arrays;
import java.util.List;

public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }

    public List<StatusPedido> getProximos() {
        return Arrays.stream(values()).filter(this::podeAvancarPara).toList();
    }

    public static StatusPedido fromValor(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Selecione um status válido.");
        }
        try {
            String normalizado = valor.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
            return switch (normalizado) {
                case "EM_PREPARO" -> EM_PREPARACAO;
                case "A_CAMINHO" -> SAIU_PARA_ENTREGA;
                case "ENTREGUE" -> FINALIZADO;
                default -> valueOf(normalizado);
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status de pedido inválido.");
        }
    }

    public boolean podeAvancarPara(StatusPedido proximo) {
        if (proximo == CANCELADO) {
            return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
        }
        return switch (this) {
            case RECEBIDO -> proximo == EM_PREPARACAO;
            case EM_PREPARACAO -> proximo == PRONTO;
            case PRONTO -> proximo == SAIU_PARA_ENTREGA || proximo == RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> proximo == FINALIZADO;
            case FINALIZADO, CANCELADO -> false;
        };
    }
}
