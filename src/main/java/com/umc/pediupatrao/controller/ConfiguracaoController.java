package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Funcionario;
import com.umc.pediupatrao.entity.HorarioAtendimento;
import com.umc.pediupatrao.service.FuncionarioService;
import com.umc.pediupatrao.service.HorarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConfiguracaoController {
    private final FuncionarioService funcionarioService;
    private final HorarioService horarioService;

    public ConfiguracaoController(FuncionarioService funcionarioService, HorarioService horarioService) {
        this.funcionarioService = funcionarioService;
        this.horarioService = horarioService;
    }

    @GetMapping("/funcionarios")
    public String funcionarios(Model model) {
        return listarFuncionarios(model, false);
    }

    @GetMapping("/entregadores")
    public String entregadores(Model model) {
        return listarFuncionarios(model, true);
    }

    private String listarFuncionarios(Model model, boolean entregadores) {
        model.addAttribute("funcionarios", funcionarioService.listar(entregadores));
        model.addAttribute("entregadores", entregadores);
        model.addAttribute("content", "funcionarios/lista :: content");
        return "layout";
    }

    @GetMapping("/funcionarios/novo")
    public String novoFuncionario(@RequestParam(defaultValue = "false") boolean entregadores, Model model) {
        Funcionario funcionario = new Funcionario();
        if (entregadores) funcionario.setCargo("Entregador");
        model.addAttribute("funcionario", funcionario);
        model.addAttribute("entregadores", entregadores);
        model.addAttribute("content", "funcionarios/form :: content");
        return "layout";
    }

    @GetMapping("/funcionarios/editar/{id}")
    public String editarFuncionario(@PathVariable String id, @RequestParam(defaultValue = "false") boolean entregadores,
            Model model, RedirectAttributes aviso) {
        try {
            model.addAttribute("funcionario", funcionarioService.buscar(id));
            model.addAttribute("entregadores", entregadores);
            model.addAttribute("content", "funcionarios/form :: content");
            return "layout";
        } catch (IllegalArgumentException e) {
            aviso.addFlashAttribute("erro", e.getMessage());
            return "redirect:/funcionarios";
        }
    }

    @PostMapping("/funcionarios/salvar")
    public String criarFuncionario(@ModelAttribute Funcionario funcionario,
            @RequestParam(defaultValue = "false") boolean entregadores, RedirectAttributes aviso) {
        try {
            if (funcionario.getId() != null) throw new IllegalArgumentException("Um novo funcionário não pode informar um ID.");
            funcionarioService.salvar(funcionario);
            aviso.addFlashAttribute("sucesso", "Funcionário cadastrado!");
        } catch (IllegalArgumentException e) {
            aviso.addFlashAttribute("erro", e.getMessage());
        }
        return entregadores ? "redirect:/entregadores" : "redirect:/funcionarios";
    }

    @PostMapping("/funcionarios/editar/{id}")
    public String salvarFuncionario(@PathVariable String id, @ModelAttribute Funcionario funcionario,
            @RequestParam(defaultValue = "false") boolean entregadores, RedirectAttributes aviso) {
        try {
            funcionario.setId(id);
            funcionarioService.salvar(funcionario);
            aviso.addFlashAttribute("sucesso", "Funcionário atualizado!");
        } catch (IllegalArgumentException e) {
            aviso.addFlashAttribute("erro", e.getMessage());
        }
        return entregadores ? "redirect:/entregadores" : "redirect:/funcionarios";
    }

    @PostMapping("/funcionarios/excluir/{id}")
    public String excluirFuncionario(@PathVariable String id, @RequestParam(defaultValue = "false") boolean entregadores,
            RedirectAttributes aviso) {
        try {
            funcionarioService.excluir(id);
            aviso.addFlashAttribute("sucesso", "Funcionário excluído!");
        } catch (IllegalArgumentException e) {
            aviso.addFlashAttribute("erro", e.getMessage());
        }
        return entregadores ? "redirect:/entregadores" : "redirect:/funcionarios";
    }

    @GetMapping("/horarios")
    public String horarios(Model model) {
        model.addAttribute("horarios", horarioService.listar());
        model.addAttribute("content", "horarios :: content");
        return "layout";
    }

    @PostMapping("/horarios/salvar")
    public String salvarHorario(@ModelAttribute HorarioAtendimento horario, RedirectAttributes aviso) {
        try {
            horarioService.salvar(horario);
            aviso.addFlashAttribute("sucesso", "Horário de " + horario.getDia() + " salvo!");
        } catch (IllegalArgumentException e) {
            aviso.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/horarios";
    }
}
