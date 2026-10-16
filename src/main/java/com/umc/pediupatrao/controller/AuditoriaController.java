package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String listar(Model model) {
        model.addAttribute("registros", auditoriaService.listarTodos());
        model.addAttribute("podeVerAuditoria", true);
        model.addAttribute("content", "auditoria :: content");
        return "layout";
    }

    @GetMapping("/api/auditoria")
    @ResponseBody
    public ResponseEntity<List<RegistroAuditoria>> listarApi() {
        return ResponseEntity.ok(auditoriaService.listarTodos());
    }
}
