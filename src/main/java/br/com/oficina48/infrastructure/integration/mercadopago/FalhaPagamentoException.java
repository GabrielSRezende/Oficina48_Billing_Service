package br.com.oficina48.infrastructure.integration.mercadopago;

public class FalhaPagamentoException extends RuntimeException {

    private final String provider;
    private final String tipoErro;

    public FalhaPagamentoException(String provider, String tipoErro, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.tipoErro = tipoErro;
    }

    public String getProvider() {
        return provider;
    }

    public String getTipoErro() {
        return tipoErro;
    }
}
