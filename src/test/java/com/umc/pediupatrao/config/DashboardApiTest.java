package com.umc.pediupatrao.config;

import com.umc.pediupatrao.controller.DashboardApiController;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardApiController.class)
@Import({SecurityConfig.class, DashboardService.class})
class DashboardApiTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UsuarioDetailsService usuarios;
    @MockitoBean private PedidoRepository pedidos;

    @Test
    void administradorEGerentePodemConsultar() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedidos").value(0));
        mvc.perform(get("/api/dashboard/vendas-por-dia").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(7));
        mvc.perform(get("/api/dashboard/produtos-mais-vendidos").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void bloqueiaAtendenteEUsuarioSemSessao() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/resumo").with(user("balcao").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void devolveErroQuandoFaltaUmaData() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").param("inicio", "2026-11-01")
                        .with(user("gerente").roles("GERENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Informe a data inicial e a data final."));
    }
}
