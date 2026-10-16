# Pediu Patrão

Projeto da atividade prática M1 de Engenharia de Software. O sistema permite cadastrar clientes e produtos, registrar pedidos e acompanhar o atendimento de uma pizzaria.

A atividade foi desenvolvida sobre o projeto fornecido, usando Java 21, Spring Boot, Spring Security, MongoDB e Thymeleaf.

## Como executar

É necessário ter Java 21 e MongoDB instalados. A variável `JAVA_HOME` deve apontar para a pasta do JDK, sem incluir a pasta `bin`. O Maven Wrapper já está no projeto.

Baixe o repositório e entre na pasta:

```powershell
git clone https://github.com/JulioFranz/PediuPatrao.git
cd PediuPatrao
```

Se o MongoDB ainda não estiver rodando, abra um terminal nessa pasta e execute:

```powershell
New-Item -ItemType Directory -Force .\data\mongo
mongod --dbpath .\data\mongo --bind_ip 127.0.0.1 --port 27017
```

O comando `mongod` precisa estar no PATH. Também é possível usar o caminho completo do arquivo `mongod.exe`. Deixe esse terminal aberto.

Em outro terminal, na pasta do projeto, inicie a aplicação:

```powershell
$env:APP_ADMIN_PASSWORD = Read-Host "Escolha a senha do primeiro admin"
.\mvnw.cmd spring-boot:run
```

Na primeira execução, o Maven baixa as bibliotecas necessárias. Depois, acesse http://localhost:8080.

Se o banco de usuários estiver vazio, será criado o usuário `admin` com a senha informada. Essa configuração não altera a senha de contas existentes. Para os novos usuários, use senhas com pelo menos 8 caracteres.

Entre como administrador e cadastre as contas de gerente e atendente em **Configurações > Usuários**.

A conexão com o banco está em `src/main/resources/application.properties`:

- Endereço: `127.0.0.1`
- Porta: `27017`
- Banco: `pizzaria`

Para encerrar, pressione `Ctrl+C` no terminal da aplicação e no terminal do MongoDB.

## Perfis de acesso

| Perfil | Permissões |
| --- | --- |
| ADMIN | Administrar usuários e perfis, consultar pedidos e auditoria. |
| GERENTE | Registrar e gerenciar pedidos, aplicar descontos, cancelar, consultar clientes e auditoria. |
| ATENDENTE | Cadastrar e editar clientes, registrar pedidos e acompanhar o andamento. |

Administrador e gerente também podem manter produtos, funcionários, entregadores e horários. O atendente pode consultar os produtos.

O cadastro de funcionários é separado das contas de acesso. Entregadores são os funcionários cadastrados com o cargo Entregador. A exclusão de clientes está bloqueada para os três perfis.

## Requisitos implementados

- **RF01:** três perfis no login, permissões nas páginas e APIs, proteção contra alterar o próprio perfil ou remover o último administrador.
- **RF02:** pedidos com vários produtos, quantidades, preços registrados, total calculado no servidor e identificação de quem registrou.
- **RF03:** validação das mudanças de status e registro de horário e responsável pela entrada e saída.
- **RF04:** desconto de até 20% e cancelamento com justificativa, exclusivos do gerente e permitidos somente antes da saída.
- **RF05:** auditoria de pedidos e clientes com usuário, perfil, horário e valores anteriores e novos. Consulta restrita a administrador e gerente.

## Regras dos pedidos

O pedido passa por Recebido, Em preparação e Pronto. Depois, pode seguir para Saiu para entrega ou Retirado e, por último, Finalizado.

O sistema impede pular etapas. Ao sair para entrega ou ser retirado, registra o horário e o responsável pela saída. Depois do cancelamento, o pedido não aceita novas alterações.

O total é calculado com os preços do cadastro de produtos. Cada item guarda o preço usado no pedido, então mudar o produto depois não altera o histórico.

O desconto vai de 0% a 20%. Informar um novo percentual substitui o anterior, e 0% remove o desconto. O administrador não pode aplicar descontos nem cancelar pedidos.

## Principais alterações

Foram ajustados os perfis de acesso, o cálculo e o fluxo dos pedidos. Também foram adicionados desconto, cancelamento com motivo, auditoria e o botão Sair.

A auditoria registra as mudanças de telefone e endereço dos clientes e as operações dos pedidos. Não existem opções comuns para editar ou excluir esse histórico.

Foram corrigidas a edição e a exclusão de produtos e criadas as telas de funcionários, entregadores e horários. Os horários são informativos e não bloqueiam o registro de pedidos.

As operações de alteração usam a proteção CSRF do Spring Security. As senhas dos usuários são protegidas com BCrypt.

## Testes

Os testes estão em `src/test/java`. Para executar:

```powershell
.\mvnw.cmd test
```

São 35 testes básicos, separados em segurança, usuários, pedidos, clientes e auditoria. Eles usam dados simulados e não precisam do MongoDB ligado. A última execução terminou sem falhas ou erros.
