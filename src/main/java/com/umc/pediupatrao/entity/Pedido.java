package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private String status;
    private String justificativaCancelamento;
    private int quantidade;
    private String sabor;
    private List<ItemPedido> itens;
    private BigDecimal descontoPercentual;
    private BigDecimal valorTotal;
    private String responsavelRegistro;
    private Date dataHoraEntrada;
    private String responsavelSaida;
    private Date dataHoraSaida;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }

    @JsonIgnore
    public List<StatusPedido> getProximosStatus() {
        try {
            return StatusPedido.fromValor(status).getProximos();
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @JsonIgnore
    public boolean isPodeAplicarDesconto() {
        return dataHoraSaida == null && itens != null && !itens.isEmpty()
                && getProximosStatus().contains(StatusPedido.CANCELADO);
    }

    @JsonIgnore
    public String getStatusDescricao() {
        try {
            return StatusPedido.fromValor(status).getDescricao();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }
    public BigDecimal getDescontoPercentual() { return descontoPercentual; }
    public void setDescontoPercentual(BigDecimal descontoPercentual) { this.descontoPercentual = descontoPercentual; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public String getResponsavelRegistro() { return responsavelRegistro; }
    public void setResponsavelRegistro(String responsavelRegistro) { this.responsavelRegistro = responsavelRegistro; }
    public Date getDataHoraEntrada() { return dataHoraEntrada; }
    public void setDataHoraEntrada(Date dataHoraEntrada) { this.dataHoraEntrada = dataHoraEntrada; }
    public String getResponsavelSaida() { return responsavelSaida; }
    public void setResponsavelSaida(String responsavelSaida) { this.responsavelSaida = responsavelSaida; }
    public Date getDataHoraSaida() { return dataHoraSaida; }
    public void setDataHoraSaida(Date dataHoraSaida) { this.dataHoraSaida = dataHoraSaida; }
    
    
}
