package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Funcionario;
import com.umc.pediupatrao.repository.FuncionarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public List<Funcionario> listar(boolean apenasEntregadores) {
        List<Funcionario> funcionarios = funcionarioRepository.findAll();
        if (apenasEntregadores) {
            return funcionarios.stream().filter(f -> "Entregador".equalsIgnoreCase(f.getCargo())).toList();
        }
        return funcionarios;
    }

    public Funcionario buscar(String id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));
    }

    public void salvar(Funcionario funcionario) {
        if (funcionario.getNome() == null || funcionario.getNome().isBlank() || funcionario.getNome().length() > 100) {
            throw new IllegalArgumentException("Informe o nome com até 100 caracteres.");
        }
        if (funcionario.getTelefone() == null || !funcionario.getTelefone().replaceAll("\\D", "").matches("\\d{10,11}")) {
            throw new IllegalArgumentException("Informe o telefone com DDD e 10 ou 11 números.");
        }
        if (funcionario.getCargo() == null || funcionario.getCargo().isBlank() || funcionario.getCargo().length() > 50) {
            throw new IllegalArgumentException("Informe o cargo com até 50 caracteres.");
        }
        if (funcionario.getId() != null) buscar(funcionario.getId());
        funcionario.setNome(funcionario.getNome().trim());
        funcionario.setTelefone(funcionario.getTelefone().trim());
        funcionario.setCargo(funcionario.getCargo().trim());
        funcionarioRepository.save(funcionario);
    }

    public void excluir(String id) {
        buscar(id);
        funcionarioRepository.deleteById(id);
    }
}
