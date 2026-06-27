# Oficina48_Billing_Service
Projeto Pós-Tech Fase 04 - Microserviço Billing

Este microsserviço é responsável pelo processamento de faturamentos e pagamentos utilizando o gateway de pagamento Mercado Pago (Sandbox) e mensageria assíncrona com AWS SQS.

---

## 📋 Referência das Filas SQS

O microsserviço utiliza as seguintes filas SQS para a integração de eventos:

| Nome da Fila | Direção | Evento Correspondente | Descrição |
| :--- | :--- | :--- | :--- |
| `faturamento-solicitado` | **Entrada** (Consumer) | `FaturamentoSolicitadoEvent` | Recebe solicitações para iniciar um faturamento (OS Service). |
| `faturamento-pendente` | **Saída** (Producer) | `FaturamentoPendenteEvent` | Notifica que a cobrança foi gerada no Mercado Pago contendo o link/checkout Pix. |
| `faturamento-concluido` | **Saída** (Producer) | `FaturamentoConcluidoEvent` | Notifica que o pagamento foi concluído com sucesso (via Webhook). |
| `faturamento-falhou` | **Saída** (Producer) | `FaturamentoFalhouEvent` | Notifica que a cobrança ou o pagamento falharam/foram rejeitados. |

---

## 🛠️ Guia de Testes do Fluxo de Mensageria Local

Este guia descreve os passos e comandos necessários para testar localmente o ciclo de faturamento e comunicação do microsserviço utilizando o **LocalStack** para simular as filas SQS.

### Pré-requisitos
* Docker e Docker Compose instalados e rodando.
* AWS CLI configurado ou `awslocal` instalado.

---

### 1. Inicializar os Containers
Suba o banco de dados PostgreSQL e o simulador LocalStack:
```bash
docker compose up -d
```

### 2. Simular Solicitação de Faturamento (Fila de Entrada)
Envie o evento de solicitação de faturamento para a fila `faturamento-solicitado` para que o serviço processe e gere a cobrança no Mercado Pago:

```bash
aws sqs send-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-solicitado \
  --message-body '{
    "ordemServicoId": 12345,
    "valor": 150.00,
    "clienteEmail": "cliente@email.com",
    "clienteNome": "Cliente Teste",
    "clienteCpf": "12345678901",
    "descricao": "Cobrança da Ordem de Serviço #12345"
  }'
```

### 3. Obter o Link de Pagamento Gerado (Fila de Resposta)
Consuma a mensagem da fila `faturamento-pendente` para obter o link do Pix e o ID do pagamento gerado:

```bash
aws sqs receive-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-pendente
```
> 💡 **Nota:** O corpo da mensagem conterá o `pagamentoLink` (checkout do Mercado Pago) e o `pagamentoId` (ID da preferência). Copie o `pagamentoId` para usar nos passos abaixo.

---

### 4. Simular Status de Pagamento (Integração Webhook)

#### Cenário A: Pagamento Aprovado (Sucesso)
1. Efetue o pagamento acessando o link do Mercado Pago em modo Sandbox com cartões de teste de sucesso.
2. Dispare a notificação do webhook do Mercado Pago para o microsserviço:

```bash
curl -X POST http://localhost:8080/mercadopago/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "type": "payment",
    "data": {
      "id": "<PAGAMENTO_ID_OBTIDO_NO_PASSO_3>"
    }
  }'
```

3. Verifique o disparo do evento na fila `faturamento-concluido`:

```bash
aws sqs receive-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-concluido
```

#### Cenário B: Pagamento Recusado (Falha)
1. Efetue o pagamento no link gerado usando cartões de teste de erro do Mercado Pago (ex: sem saldo).
2. Chame o webhook:

```bash
curl -X POST http://localhost:8080/mercadopago/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "type": "payment",
    "data": {
      "id": "<PAGAMENTO_ID_OBTIDO_NO_PASSO_3>"
    }
  }'
```

3. Verifique o disparo do evento na fila `faturamento-falhou`:

```bash
aws sqs receive-message \
  --endpoint-url http://localhost:4566 \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-falhou
```

---

### 🧹 Limpeza de Filas (Purge)
Para limpar as mensagens e reiniciar os cenários de testes locais sem interferência, execute:

```bash
# Limpar solicitações
aws sqs purge-queue --endpoint-url http://localhost:4566 --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-solicitado

# Limpar pendentes
aws sqs purge-queue --endpoint-url http://localhost:4566 --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-pendente

# Limpar concluídos
aws sqs purge-queue --endpoint-url http://localhost:4566 --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-concluido

# Limpar falhas
aws sqs purge-queue --endpoint-url http://localhost:4566 --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/faturamento-falhou
```

---

## 🔄 Fluxos de Negócio e Integração

O sistema se comunica e gerencia o faturamento através de 4 fluxos principais:

### 1. Geração e Registro do Orçamento Solicitado
* **Nome de Referência:** *Fluxo de Recebimento e Registro de Proposta de Orçamento*
* **Descrição:** Inicialização e o registro de uma proposta de orçamento emitida pelo serviço de ordens de serviço (`OS SERVICE`) para processamento no microsserviço de cobrança (`BILLING SERVICE`).
* **Sequência de Passos:**
  1. O `OS SERVICE` gera um orçamento e publica uma mensagem de evento (`enviar-orcamento-solicitado`) na **FILA SQS**.
  2. O `BILLING SERVICE` consome essa mensagem da fila de forma assíncrona.
  3. O `BILLING SERVICE` processa as informações e realiza duas ações concorrentes/sequenciais:
     * Persiste as informações do orçamento no banco de dados (**BD**) para controle interno.
     * Gera um documento em formato de texto (**TXT (ORÇAMENTO)**) contendo os detalhes legíveis do orçamento.

---

### 2. Processamento de Decisão do Orçamento (Aprovação/Reprovação)
* **Nome de Referência:** *Fluxo de Atualização de Status e Notificação de Parecer de Orçamento*
* **Descrição:** Ocorre quando uma decisão de aprovação ou reprovação do orçamento é submetida diretamente ao serviço de cobrança, que então notifica o serviço de ordens de serviço.
* **Sequência de Passos:**
  1. Uma requisição externa contendo a decisão (**Requisição Aprovado/Reprovado**) é enviada diretamente ao endpoint do **BILLING SERVICE**.
  2. O **BILLING SERVICE** atualiza o status daquele orçamento no banco de dados.
  3. O **BILLING SERVICE** dispara um evento/notificação de retorno para o **OS SERVICE** informando a decisão:
     * `aprovado-orcamento`: se o parecer for favorável.
     * `reprovada-orcamento`: se o parecer for contrário.
  4. O **OS SERVICE** recebe a atualização e altera o estado da Ordem de Serviço correspondente.

---

### 3. Solicitação de Faturamento e Geração de Meio de Pagamento
* **Nome de Referência:** *Fluxo de Solicitação de Faturamento e Emissão de QR Code (Pix)*
* **Descrição:** Trata da solicitação para início do faturamento de uma ordem de serviço aprovada, resultando na criação de uma cobrança externa e geração do código de pagamento.
* **Sequência de Passos:**
  1. O **OS SERVICE** envia um evento de solicitação de faturamento (`faturamento-solicitado`) indicando que a OS está pronta para cobrança.
  2. O **BILLING SERVICE** consome o evento da fila SQS `faturamento-solicitado`.
  3. O **BILLING SERVICE** integra-se com a API do gateway de pagamento (Mercado Pago Sandbox) para registrar a intenção de cobrança.
  4. O **BILLING SERVICE** gera as credenciais de pagamento (como o Pix copy-and-paste ou dados do QR Code) e salva/disponibiliza essa representação em arquivo de texto (**TXT (QR CODE)**), mudando o status da transação para pendente.

---

### 4. Confirmação de Pagamento via Webhook (Conclusão)
* **Nome de Referência:** *Fluxo de Notificação de Pagamento e Liquidação de Faturamento*
* **Descrição:** Confirmação do pagamento pelo gateway externo (via webhook) e a notificação de sucesso ao serviço de ordens de serviço para finalização.
* **Sequência de Passos:**
  1. O gateway de pagamento externo envia uma notificação assíncrona (**Notificação via webhook**) informando que o pagamento foi processado.
  2. O **BILLING SERVICE** recebe a notificação no endpoint de webhook.
  3. O **BILLING SERVICE** busca os detalhes da transação junto à API do gateway para validar se o pagamento foi de fato aprovado.
  4. Uma vez aprovado, o **BILLING SERVICE** altera o status da transação no banco de dados para concluído.
  5. O **BILLING SERVICE** publica uma mensagem de faturamento concluído (`faturamento-concluido`) na fila correspondente.
  6. O **OS SERVICE** consome essa mensagem e atualiza o estado final da Ordem de Serviço correspondente.

### Fluxo de Orquestração da Ordem de Serviço (Saga Pattern)

O diagrama abaixo ilustra o ciclo de vida de uma ordem de serviço e a interação entre os microsserviços, utilizando o `OS SERVICE` como o orquestrador central do fluxo.

```mermaid
sequenceDiagram
    autonumber
    participant OS as OS SERVICE
    participant EX as EXECUTION SERVICE
    participant BL as BILLING SERVICE

    OS->>EX: Criar Ordem de serviço
    OS->>EX: Solicitar Diagnóstico
    EX-->>OS: Diagnóstico Concluído
    OS->>BL: Solicitar Orçamento
    
    alt Orçamento Aprovado
        BL-->>OS: Orçamento Aprovado
        OS->>EX: Enviar para Em execução
        EX-->>OS: Execução Concluída
        OS->>BL: Solicitar Faturamento
        BL-->>OS: Faturamento Concluído
        OS->>EX: Finalizar Ordem de serviço
    else Orçamento Reprovado
        BL-->>OS: Orçamento Reprovado
        Note over OS,BL: O Orquestrador interrompe o fluxo ou dispara a compensação.
    end