package com.umc.pediupatrao.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditoria")
public class RegistroAuditoria {
    @Id
    private String id;
    private String usuario;
    private String perfil;
    private String acao;
    private String tipo;
    private String registroId;
    private String detalhes;
    private Date dataHora;
    private List<AlteracaoAuditoria> alteracoes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getRegistroId() { return registroId; }
    public void setRegistroId(String registroId) { this.registroId = registroId; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public Date getDataHora() { return dataHora; }
    public void setDataHora(Date dataHora) { this.dataHora = dataHora; }
    public List<AlteracaoAuditoria> getAlteracoes() { return alteracoes; }
    public void setAlteracoes(List<AlteracaoAuditoria> alteracoes) { this.alteracoes = alteracoes; }
}
