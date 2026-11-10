package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.DashboardService.ProdutoVendido;
import com.umc.pediupatrao.service.DashboardService.Resumo;
import com.umc.pediupatrao.service.DashboardService.VendaDia;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
@OpenAPIDefinition(info = @Info(title = "API de vendas do Pediu Patrão", version = "1.0"))
@Tag(name = "Dashboard")
@ApiResponse(responseCode = "400", description = "Período inválido",
        content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
@ApiResponse(responseCode = "401", description = "Sessão necessária", content = @Content)
@ApiResponse(responseCode = "403", description = "Perfil sem acesso", content = @Content)
@ApiResponse(responseCode = "500", description = "Erro ao consultar os dados", content = @Content)
public class DashboardApiController {
    private final DashboardService dashboard;

    public DashboardApiController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Indicadores e situação dos pedidos")
    @ApiResponse(responseCode = "200", description = "Resumo do período")
    public Resumo resumo(
            @Parameter(description = "Primeiro dia do período, AAAA-MM-DD") @RequestParam(required = false) String inicio,
            @Parameter(description = "Último dia do período, AAAA-MM-DD") @RequestParam(required = false) String fim) {
        return dashboard.resumo(inicio, fim);
    }

    @GetMapping("/vendas-por-dia")
    @Operation(summary = "Faturamento por dia de entrada do pedido")
    @ApiResponse(responseCode = "200", description = "Série diária")
    public List<VendaDia> vendasPorDia(
            @Parameter(description = "Primeiro dia do período, AAAA-MM-DD") @RequestParam(required = false) String inicio,
            @Parameter(description = "Último dia do período, AAAA-MM-DD") @RequestParam(required = false) String fim) {
        return dashboard.vendasPorDia(inicio, fim);
    }

    @GetMapping("/produtos-mais-vendidos")
    @Operation(summary = "Cinco produtos com mais unidades vendidas")
    @ApiResponse(responseCode = "200", description = "Ranking por quantidade")
    public List<ProdutoVendido> produtosMaisVendidos(
            @Parameter(description = "Primeiro dia do período, AAAA-MM-DD") @RequestParam(required = false) String inicio,
            @Parameter(description = "Último dia do período, AAAA-MM-DD") @RequestParam(required = false) String fim) {
        return dashboard.produtosMaisVendidos(inicio, fim);
    }
}
