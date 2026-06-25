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
