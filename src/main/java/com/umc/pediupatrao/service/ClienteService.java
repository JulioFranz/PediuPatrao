package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    public Cliente novoCliente(Cliente cliente) {
        if (cliente.getId() != null) {
            throw new IllegalArgumentException("Um novo cliente não pode informar um ID.");
        }
        return salvar(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        clienteRepository.deleteById(id);
        auditoriaService.registrar("CLIENTE", id, "EXCLUIDO", "Cliente removido.",
                dadosAuditoria(cliente), Map.of());
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    public Cliente salvar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank() || cliente.getNome().length() > 100) {
            throw new IllegalArgumentException("Informe o nome do cliente com até 100 caracteres.");
        }
        if (cliente.getTelefone() == null || !cliente.getTelefone().replaceAll("\\D", "").matches("\\d{10,11}")) {
            throw new IllegalArgumentException("Informe um telefone com DDD e 10 ou 11 números.");
        }
        cliente.setNome(cliente.getNome().trim());
        cliente.setTelefone(cliente.getTelefone().trim());
        boolean novo = cliente.getId() == null;
        Cliente anterior = novo ? null : clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));
        Map<String, String> dadosAnteriores = dadosAuditoria(anterior);

        Cliente salvo = clienteRepository.save(cliente);
        String acao = novo ? "CRIADO" : "ATUALIZADO";
        auditoriaService.registrar("CLIENTE", salvo.getId(), acao,
                "Cadastro de cliente " + acao.toLowerCase() + ".", dadosAnteriores, dadosAuditoria(salvo));
        return salvo;
    }

    private Map<String, String> dadosAuditoria(Cliente cliente) {
        Map<String, String> dados = new LinkedHashMap<>();
        if (cliente == null) {
            return dados;
        }
        dados.put("nome", cliente.getNome());
        dados.put("telefone", cliente.getTelefone());
        dados.put("cep", cliente.getCep());
        dados.put("logradouro", cliente.getLogradouro());
        dados.put("numero", cliente.getNumero());
        dados.put("complemento", cliente.getComplemento());
        dados.put("bairro", cliente.getBairro());
        dados.put("cidade", cliente.getCidade());
        dados.put("estado", cliente.getEstado());
        dados.put("endereco", cliente.getEndereco());
        return dados;
    }
}
