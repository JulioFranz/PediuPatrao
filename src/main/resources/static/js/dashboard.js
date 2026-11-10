const dashboard = document.getElementById("dashboard-vendas");
const porId = id => document.getElementById(id);
const filtro = porId("filtro-dashboard");
const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const numeros = new Intl.NumberFormat("pt-BR");
let graficoVendas;
let graficoStatus;
let periodoExibido = "";

function periodoRecente(dias) {
    const partes = new Intl.DateTimeFormat("pt-BR", {
        timeZone: "America/Sao_Paulo", year: "numeric", month: "2-digit", day: "2-digit"
    }).formatToParts(new Date());
    const parte = tipo => partes.find(item => item.type === tipo).value;
    const hoje = parte("year") + "-" + parte("month") + "-" + parte("day");
    const inicio = new Date(hoje + "T00:00:00Z");
    inicio.setUTCDate(inicio.getUTCDate() - dias + 1);
    filtro.elements.inicio.value = inicio.toISOString().slice(0, 10);
    filtro.elements.fim.value = hoje;
}

function dataCurta(data) {
    return data.slice(8, 10) + "/" + data.slice(5, 7);
}

async function consultar(rota, parametros) {
    const resposta = await fetch(dashboard.dataset.api + "/" + rota + "?" + parametros);
    if (resposta.status === 401 || resposta.redirected) {
        window.location.assign(dashboard.dataset.login);
        throw new Error("A sessão expirou.");
    }
    if (resposta.status === 400) {
        throw new Error(await resposta.text());
    }
    if (resposta.status === 403) {
        throw new Error("Seu perfil não pode consultar o dashboard.");
    }
    if (!resposta.ok) {
        throw new Error("Não foi possível buscar os dados. Tente atualizar novamente.");
    }
    return resposta.json();
}

function desenharGraficos(vendas, situacoes) {
    if (typeof Chart !== "function") {
        throw new Error("Não foi possível carregar os gráficos. Recarregue a página.");
    }
    if (graficoVendas) graficoVendas.destroy();
    if (graficoStatus) graficoStatus.destroy();

    graficoVendas = new Chart(porId("grafico-vendas"), {
        type: "line",
        data: {
            labels: vendas.map(venda => dataCurta(venda.data)),
            datasets: [{
                data: vendas.map(venda => venda.faturamento),
                borderColor: "#1766ae",
                backgroundColor: "rgba(23, 102, 174, 0.12)",
                pointRadius: vendas.length > 31 ? 0 : 3,
                lineTension: 0
            }]
        },
        options: {
            maintainAspectRatio: false,
            legend: { display: false },
            scales: { yAxes: [{ ticks: { beginAtZero: true, callback: valor => moeda.format(valor) } }] },
            tooltips: { callbacks: { label: ponto => moeda.format(ponto.yLabel) } }
        }
    });

    graficoStatus = new Chart(porId("grafico-status"), {
        type: "horizontalBar",
        data: {
            labels: situacoes.map(item => item.status),
            datasets: [{
                data: situacoes.map(item => item.quantidade),
                backgroundColor: situacoes.map(item => item.status === "Cancelado" ? "#c36b69" : "#397a84")
            }]
        },
        options: {
            maintainAspectRatio: false,
            legend: { display: false },
            scales: { xAxes: [{ ticks: { beginAtZero: true, precision: 0 } }] }
        }
    });
}

function mostrarRanking(produtos) {
    const corpo = porId("ranking-dashboard");
    corpo.replaceChildren();
    if (!produtos.length) {
        const celula = corpo.insertRow().insertCell();
        celula.colSpan = 3;
        celula.textContent = "Nenhum produto vendido no período.";
        return;
    }
    produtos.forEach((produto, indice) => {
        const linha = corpo.insertRow();
        linha.insertCell().textContent = indice + 1;
        linha.insertCell().textContent = produto.nome;
        const unidades = linha.insertCell();
        unidades.className = "text-end";
        unidades.textContent = numeros.format(produto.quantidade);
    });
}

async function atualizar() {
    const erro = porId("erro-dashboard");
    const periodo = porId("periodo-dashboard");
    erro.hidden = true;
    periodo.textContent = "Carregando dados...";
    filtro.querySelectorAll("button").forEach(botao => botao.disabled = true);

    try {
        const parametros = new URLSearchParams(new FormData(filtro));
        const [resumo, vendas, produtos] = await Promise.all([
            consultar("resumo", parametros),
            consultar("vendas-por-dia", parametros),
            consultar("produtos-mais-vendidos", parametros)
        ]);
        desenharGraficos(vendas, resumo.situacoes);
        porId("pedidos").textContent = numeros.format(resumo.pedidos);
        porId("finalizados").textContent = numeros.format(resumo.finalizados);
        porId("faturamento").textContent = moeda.format(resumo.faturamento);
        porId("ticketMedio").textContent = moeda.format(resumo.ticketMedio);
        mostrarRanking(produtos);
        porId("vazio-dashboard").hidden = resumo.pedidos !== 0;
        const aviso = porId("sem-valor-dashboard");
        aviso.hidden = resumo.finalizadosSemValor === 0;
        aviso.textContent = resumo.finalizadosSemValor
                + " pedido(s) finalizado(s) sem valor cadastrado ficaram fora do faturamento e do ticket médio.";
        periodoExibido = "Período exibido: " + resumo.inicio.split("-").reverse().join("/") + " a "
                + resumo.fim.split("-").reverse().join("/") + ". Atualizado às " + new Date().toLocaleTimeString("pt-BR") + ".";
        periodo.textContent = periodoExibido;
    } catch (falha) {
        erro.hidden = false;
        erro.textContent = falha instanceof TypeError ? "Falha de conexão. Tente atualizar novamente." : falha.message;
        periodo.textContent = periodoExibido || "Nenhum resultado carregado.";
    } finally {
        filtro.querySelectorAll("button").forEach(botao => botao.disabled = false);
    }
}

filtro.addEventListener("submit", evento => {
    evento.preventDefault();
    atualizar();
});
filtro.querySelectorAll("[data-dias]").forEach(botao => {
    botao.addEventListener("click", () => {
        periodoRecente(Number(botao.dataset.dias));
        atualizar();
    });
});
periodoRecente(7);
atualizar();
