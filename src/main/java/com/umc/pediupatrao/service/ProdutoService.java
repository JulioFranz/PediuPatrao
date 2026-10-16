package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public Produto novoProduto(Produto produto) {
        if (produto.getId() != null) {
            throw new IllegalArgumentException("Um novo produto não pode informar um ID.");
        }
        return salvar(produto);
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    public void excluir(String id) {
        if (!produtoRepository.existsById(id)) {
            throw new IllegalArgumentException("Produto não encontrado.");
        }
        produtoRepository.deleteById(id);
    }


    public Produto salvar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()
                || produto.getNome().length() > 100) {
            throw new IllegalArgumentException("Informe um nome de produto com até 100 caracteres.");
        }
        if (produto.getTipo() == null || produto.getTipo().isBlank() || produto.getTipo().length() > 50) {
            throw new IllegalArgumentException("Informe o tipo do produto com até 50 caracteres.");
        }
        if (produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() < 0) {
            throw new IllegalArgumentException("Informe um preço válido, igual ou maior que zero.");
        }
        if (produto.getId() != null && !produtoRepository.existsById(produto.getId())) {
            throw new IllegalArgumentException("Produto não encontrado para atualização.");
        }
        produto.setNome(produto.getNome().trim());
        produto.setTipo(produto.getTipo().trim());
        produto.setAtivo(Boolean.TRUE.equals(produto.getAtivo()));
        return produtoRepository.save(produto);
    }
}
