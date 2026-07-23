package br.com.oficina48.application.util;

public final class DocumentoConstants {

    private DocumentoConstants() {
        // Utility class
    }

    public static final String DIVIDER_HEADER = "========================================" + System.lineSeparator();
    public static final String DIVIDER_SECTION = "----------------------------------------" + System.lineSeparator();
    public static final String FORMATO_ITEM_RELATORIO = "  - %s: %s x R$ %s (Subtotal: R$ %s)%n";
}
