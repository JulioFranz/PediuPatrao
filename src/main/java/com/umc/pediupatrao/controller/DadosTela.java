package com.umc.pediupatrao.controller;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class DadosTela {

    @ModelAttribute
    public void permissoes(Model model, Authentication authentication) {
        boolean admin = temPerfil(authentication, "ADMIN");
        boolean gerente = temPerfil(authentication, "GERENTE");
        model.addAttribute("podeGerenciarUsuarios", admin);
        model.addAttribute("podeGerenciarConfig", admin || gerente);
        model.addAttribute("podeVerAuditoria", admin || gerente);
        model.addAttribute("podeEditarClientes", temPerfil(authentication, "ATENDENTE"));
        model.addAttribute("podeConsultarClientes", gerente || temPerfil(authentication, "ATENDENTE"));
    }

    private boolean temPerfil(Authentication authentication, String perfil) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(autoridade -> autoridade.getAuthority().equals("ROLE_" + perfil));
    }
}
