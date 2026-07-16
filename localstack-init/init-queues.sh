#!/bin/bash
echo "Initializing SQS queues..."
awslocal sqs create-queue --queue-name faturamento-solicitado
awslocal sqs create-queue --queue-name faturamento-pendente
awslocal sqs create-queue --queue-name faturamento-concluido
awslocal sqs create-queue --queue-name faturamento-falhou
awslocal sqs create-queue --queue-name falha-pagamento
awslocal sqs create-queue --queue-name orcamento-solicitado
awslocal sqs create-queue --queue-name orcamento-aprovado
awslocal sqs create-queue --queue-name orcamento-reprovado
echo "All SQS queues created successfully!"
