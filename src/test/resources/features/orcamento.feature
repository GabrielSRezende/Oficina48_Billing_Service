# language: pt
Funcionalidade: Processamento de Decisão de Orçamento no Billing Service
  Como um cliente da oficina
  Eu quero enviar a decisão sobre o orçamento de manutenção do meu veículo
  Para aprovar ou reprovar a execução dos serviços e notificar os demais serviços da oficina

  Cenário: Aprovação de orçamento pendente pelo cliente
    Dado que existe um orçamento cadastrado no status "PENDENTE"
    Quando o cliente envia a decisão de aprovar o orçamento registrado
    Então a resposta da requisição deve retornar status HTTP 204
    E o orçamento registrado no banco de dados deve estar com o status "APROVADO"

  Cenário: Reprovação de orçamento pendente pelo cliente
    Dado que existe um orçamento cadastrado no status "PENDENTE"
    Quando o cliente envia a decisão de reprovar o orçamento registrado
    Então a resposta da requisição deve retornar status HTTP 204
    E o orçamento registrado no banco de dados deve estar com o status "REPROVADO"
