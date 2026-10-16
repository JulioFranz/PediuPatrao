package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.AlteracaoAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(String tipo, String registroId, String acao, String detalhes) {
        gravar(tipo, registroId, acao, detalhes, List.of());
    }

    public void registrar(String tipo, String registroId, String acao, String detalhes,
            Map<String, String> anteriores, Map<String, String> novos) {
        List<AlteracaoAuditoria> alteracoes = new ArrayList<>();
        var campos = new LinkedHashSet<>(anteriores.keySet());
        campos.addAll(novos.keySet());
        for (String campo : campos) {
            String anterior = normalizar(anteriores.get(campo));
            String novo = normalizar(novos.get(campo));
            if (!Objects.equals(anterior, novo)) {
                AlteracaoAuditoria alteracao = new AlteracaoAuditoria();
                alteracao.setCampo(campo);
                alteracao.setValorAnterior(anterior);
                alteracao.setValorNovo(novo);
                alteracoes.add(alteracao);
            }
        }
        if (alteracoes.isEmpty() && !"CRIADO".equals(acao) && !"EXCLUIDO".equals(acao)) {
            return;
        }
        gravar(tipo, registroId, acao, detalhes, alteracoes);
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private void gravar(String tipo, String registroId, String acao, String detalhes,
            List<AlteracaoAuditoria> alteracoes) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setUsuario(autenticacao == null ? "sistema" : autenticacao.getName());
        registro.setPerfil(autenticacao == null ? "SISTEMA" : autenticacao.getAuthorities().stream()
                .map(autoridade -> autoridade.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("SISTEMA"));
        registro.setTipo(tipo);
        registro.setRegistroId(registroId);
        registro.setAcao(acao);
        registro.setDetalhes(detalhes);
        registro.setDataHora(new Date());
        registro.setAlteracoes(alteracoes);
        auditoriaRepository.save(registro);
    }

    public List<RegistroAuditoria> listarTodos() {
        return auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
    }
}
