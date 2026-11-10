# Evidências de teste

Data: 10/11/2026

## Maven

Comando: `./mvnw.cmd test`.

Resultado: 42 testes passaram. São 35 testes que já existiam e 7 testes novos de cálculos, datas e acesso à API.

[Saída do Maven](evidencias/testes-maven.txt)

## Navegador e MongoDB local

| Verificação | Resultado | Captura |
| --- | --- | --- |
| Abrir como gerente e escolher 30 dias | 3 pedidos, 1 finalizado, faturamento e ticket médio de R$ 45,00. A pizza de teste aparece no ranking com 1 unidade. | [Painel](evidencias/painel.png) |
| Escolher os últimos 7 dias | Nenhum pedido no intervalo. Os indicadores ficam zerados e a tela avisa que não há dados. | [Período vazio](evidencias/periodo-vazio.png) |
| Enviar a data inicial depois da final | A tela mostra a mensagem de validação e mantém o último resultado, identificado pelo período exibido. | [Datas invertidas](evidencias/periodo-invalido.png) |
| Executar a consulta de resumo no Swagger | A API retorna 200, com os mesmos totais da tela para o período informado. | [Swagger](evidencias/swagger.png) |

Nos testes automatizados, ADMIN e GERENTE receberam 200. ATENDENTE recebeu 403 e a consulta sem sessão recebeu 401. As capturas acima usam somente os números do painel e não mostram dados pessoais dos clientes.
