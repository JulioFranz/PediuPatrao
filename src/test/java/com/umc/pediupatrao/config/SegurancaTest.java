package com.umc.pediupatrao.config;

import com.umc.pediupatrao.controller.AuditoriaController;
import com.umc.pediupatrao.controller.ClienteController;
import com.umc.pediupatrao.controller.PedidoController;
import com.umc.pediupatrao.controller.UsuarioController;
import com.umc.pediupatrao.service.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PedidoController.class, UsuarioController.class, ClienteController.class, AuditoriaController.class})
@Import(SecurityConfig.class)
class SegurancaTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UsuarioDetailsService usuarioDetailsService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private ClienteService clienteService;
    @MockitoBean private AuditoriaService auditoriaService;

    @Test
    void somenteAdminConsultaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").with(user("admin.teste").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").with(user("gerente.teste").roles("GERENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(user("balcao.teste").roles("ATENDENTE"))).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoConsegueCriarAdministradorPelaApi() throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"novo.admin\",\"password\":\"senha-de-teste\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void tresPerfisPodemConsultarPedidos() throws Exception {
        mvc.perform(get("/api/pedidos").with(user("admin.teste").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/pedidos").with(user("gerente.teste").roles("GERENTE"))).andExpect(status().isOk());
        mvc.perform(get("/api/pedidos").with(user("balcao.teste").roles("ATENDENTE"))).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendentePodeRegistrarPedido() throws Exception {
        mvc.perform(post("/api/pedidos").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\"cliente-1\",\"itens\":[{\"produtoId\":\"pizza\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated());
        verify(pedidoService).criarPedido(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeAplicarDescontoECancelar() throws Exception {
        mvc.perform(put("/api/pedidos/pedido-1/desconto").with(csrf()).param("percentual", "10"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/pedidos/pedido-1/status").with(csrf())
                .param("status", "CANCELADO").param("justificativa", "Compra repetida"))
                .andExpect(status().isOk());
        verify(pedidoService).aplicarDesconto("pedido-1", BigDecimal.TEN);
        verify(pedidoService).atualizarStatus("pedido-1", "CANCELADO", "Compra repetida");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeAplicarDescontoNemCancelar() throws Exception {
        mvc.perform(put("/api/pedidos/pedido-1/desconto").with(csrf()).param("percentual", "10"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/pedido-1/status").with(csrf())
                .param("status", "CANCELADO").param("justificativa", "Teste"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeAplicarDescontoNemCancelar() throws Exception {
        mvc.perform(put("/api/pedidos/pedido-1/desconto").with(csrf()).param("percentual", "10"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/pedido-1/status").with(csrf())
                .param("status", "CANCELADO").param("justificativa", "Teste"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void somenteAdminEGerenteConsultamAuditoria() throws Exception {
        mvc.perform(get("/api/auditoria").with(user("admin.teste").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").with(user("gerente.teste").roles("GERENTE"))).andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").with(user("balcao.teste").roles("ATENDENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void cadastroDeClientesFicaComAtendente() throws Exception {
        String cliente = "{\"nome\":\"Cliente do balcão\",\"telefone\":\"11911112222\"}";
        mvc.perform(post("/api/clientes").with(user("balcao.teste").roles("ATENDENTE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cliente)).andExpect(status().isCreated());
        mvc.perform(put("/api/clientes/cliente-1").with(user("gerente.teste").roles("GERENTE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cliente)).andExpect(status().isForbidden());
        mvc.perform(post("/api/clientes").with(user("admin.teste").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cliente)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditoriaNaoTemRotasParaEditarOuExcluir() throws Exception {
        mvc.perform(put("/api/auditoria/registro-1").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/auditoria/registro-1").with(csrf())).andExpect(status().isNotFound());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void precisaEntrarParaConsultarPedidos() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(pedidoService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void semTokenNaoPodeCriarUsuario() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    @WithMockUser
    void sairExigeTokenEVoltaParaLogin() throws Exception {
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
