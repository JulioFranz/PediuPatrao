package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "horariosAtendimento")
public class HorarioAtendimento {
    @Id
    private String dia;
    private String abertura;
    private String fechamento;
    private boolean fechado;

    public String getDia() { return dia; }
    public void setDia(String dia) { this.dia = dia; }
    public String getAbertura() { return abertura; }
    public void setAbertura(String abertura) { this.abertura = abertura; }
    public String getFechamento() { return fechamento; }
    public void setFechamento(String fechamento) { this.fechamento = fechamento; }
    public boolean isFechado() { return fechado; }
    public void setFechado(boolean fechado) { this.fechado = fechado; }
}
