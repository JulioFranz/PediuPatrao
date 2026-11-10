# Dashboard administrativo

## O que o painel entrega

| Requisito | Na prática |
| --- | --- |
| RF01 | Mostra pedidos, finalizados, faturamento e ticket médio. |
| RF02 | Mostra o faturamento de cada dia em um gráfico de linha. |
| RF03 | Mostra a quantidade de pedidos em cada status. |
| RF04 | Lista até cinco produtos por unidades vendidas. |
| RF05 | Permite escolher as datas ou usar os atalhos de 7 e 30 dias. |
| RF06 | A tela consulta três endpoints que leem os pedidos do MongoDB. |
| RF07 | O botão Atualizar consulta o banco novamente. A tela informa carregamento, erro, período vazio e horário da última atualização. |

## Requisitos não funcionais

- A tela, a API e o Swagger só podem ser acessados por ADMIN e GERENTE.
- A API é de leitura e não devolve dados pessoais dos clientes.
- O MongoDB filtra pela data de entrada. O intervalo máximo é de 366 dias por consulta.
- O painel funciona com o mesmo login e layout do sistema. Os gráficos têm rótulos e a tabela se ajusta a telas menores.

## Regras dos números

O período inclui a data inicial e a final no horário de São Paulo. A data usada é a entrada do pedido. A situação mostrada é a atual, mesmo quando o pedido mudou de status depois do período.

Todos os pedidos do período entram na contagem por status. Só os finalizados com valor registrado entram no faturamento e no ticket médio. O faturamento usa o valor já salvo com desconto. O ticket médio divide esse valor pela quantidade de finalizados que têm valor registrado.

O ranking usa os itens dos pedidos finalizados e soma unidades pelo ID do produto. O nome exibido vem do item mais recente no período. Pedidos antigos sem data não entram na consulta. Sem valor, eles ainda podem contar no status e no ranking se tiverem itens, mas ficam fora dos cálculos em reais.

## API

Entre no sistema como ADMIN ou GERENTE. Abra `/dashboard` para ver o painel e `/swagger-ui.html` para testar a API na mesma sessão.

Todos os endpoints abaixo são GET. Os parâmetros `inicio` e `fim` usam o formato `AAAA-MM-DD`. Sem os dois parâmetros, o sistema consulta hoje e os seis dias anteriores. Para informar um período manual, envie as duas datas.

### Resumo

```http
GET /api/dashboard/resumo?inicio=2026-11-09&fim=2026-11-09
```

```json
{
  "inicio": "2026-11-09",
  "fim": "2026-11-09",
  "pedidos": 2,
  "finalizados": 1,
  "faturamento": 45.00,
  "ticketMedio": 45.00,
  "finalizadosSemValor": 0,
  "situacoes": [
    {"status": "Recebido", "quantidade": 0},
    {"status": "Em preparação", "quantidade": 0},
    {"status": "Pronto", "quantidade": 0},
    {"status": "Saiu para entrega", "quantidade": 0},
    {"status": "Retirado", "quantidade": 0},
    {"status": "Finalizado", "quantidade": 1},
    {"status": "Cancelado", "quantidade": 1}
  ]
}
```

### Vendas por dia

```http
GET /api/dashboard/vendas-por-dia?inicio=2026-11-09&fim=2026-11-10
```

```json
[
  {"data": "2026-11-09", "faturamento": 45.00},
  {"data": "2026-11-10", "faturamento": 0}
]
```

### Produtos mais vendidos

```http
GET /api/dashboard/produtos-mais-vendidos?inicio=2026-11-09&fim=2026-11-09
```

```json
[
  {"id": "pizza-casa", "nome": "Pizza da casa", "quantidade": 2}
]
```

### Respostas possíveis

| Código | Quando acontece |
| --- | --- |
| 200 | Consulta concluída. Um período vazio devolve contagens zeradas, série diária com zeros e ranking vazio. |
| 400 | Data ausente, inválida, fora de ordem ou período maior que 366 dias. O corpo traz uma mensagem em texto. |
| 401 | A chamada à API não tem sessão válida. |
| 403 | O usuário está logado, mas o perfil não tem acesso. |
| 500 | Erro interno, por exemplo se a consulta ao banco falhar. A tela pede para tentar novamente. |

## Como o código foi separado

A API ficou na branch `feature/api-dashboard`, criada a partir de `develop`. A tela ficou em `feature/tela-dashboard`, com a API já integrada. O serviço calcula os números em Java. O JavaScript busca os resultados e desenha os gráficos com o Chart.js do projeto. O Swagger foi incluído para testar os endpoints pelo navegador.

## Evidências de teste

Os testes foram executados em 10/11/2026. O Maven registrou 42 testes aprovados, sendo 7 do dashboard.

```powershell
.\mvnw.cmd test
```

Os testes do dashboard cobrem cálculos, ranking, datas e permissões. ADMIN e GERENTE receberam 200. ATENDENTE recebeu 403 e a consulta sem sessão recebeu 401. [Saída do Maven](evidencias/testes-maven.txt)

Também conferi o painel no navegador com o MongoDB local.

| Teste | Resultado |
| --- | --- |
| Abrir como gerente e escolher 30 dias | O painel mostrou 3 pedidos, 1 finalizado, faturamento e ticket médio de R$ 45,00 e uma pizza no ranking. |
| Consultar um período sem pedidos | Os indicadores ficaram zerados e o painel avisou que não havia dados. |
| Informar a data inicial depois da final | O painel mostrou o aviso e manteve o último resultado válido. |
| Consultar o resumo pelo Swagger | A API retornou 200 com os mesmos totais do painel para o período. |
