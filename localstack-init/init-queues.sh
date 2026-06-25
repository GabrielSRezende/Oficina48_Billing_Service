#!/bin/bash
echo "Initializing SQS queues..."
awslocal sqs create-queue --queue-name faturamento-solicitado
awslocal sqs create-queue --queue-name faturamento-pendente
awslocal sqs create-queue --queue-name faturamento-concluido
awslocal sqs create-queue --queue-name faturamento-falhou
echo "All SQS queues created successfully!"
