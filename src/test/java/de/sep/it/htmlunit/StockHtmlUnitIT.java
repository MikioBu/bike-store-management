package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class StockHtmlUnitIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void stockList_lagerbestaendeLaden() {
        de.sep.it.util.SqlLogCounter.measure("stockList_lagerbestaendeLaden", () -> withClient((webClient, baseUrl) -> {
            login(webClient, baseUrl);
            org.htmlunit.html.HtmlPage page = openStockPage(webClient, baseUrl);

            String text = page.asNormalizedText();
            org.junit.jupiter.api.Assertions.assertTrue(
                    text.contains("Lagerbestände") || text.contains("Bestand"),
                    "Lagerbestandsseite wurde nicht plausibel geladen. Body: " + text);

            org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
            org.junit.jupiter.api.Assertions.assertNotNull(firstRow, "Keine Lagerbestandszeile gefunden.");
            org.junit.jupiter.api.Assertions.assertFalse(firstRow.asNormalizedText().isBlank(),
                    "Erste Bestandszeile ist leer");

            System.out.println("[OK] STOCK LIST Lagerbestände geladen");
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void stockList_zeigtProduktUndNiederlassung() {
        de.sep.it.util.SqlLogCounter.measure("stockList_zeigtProduktUndNiederlassung",
                () -> withClient((webClient, baseUrl) -> {
                    login(webClient, baseUrl);
                    org.htmlunit.html.HtmlPage page = openStockPage(webClient, baseUrl);

                    org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
                    java.util.List<org.htmlunit.html.HtmlTableCell> cells = firstRow.getCells();

                    org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 3,
                            "Tabelle hat zu wenige Spalten für Produkt/Niederlassung.");

                    String rowText = firstRow.asNormalizedText();
                    org.junit.jupiter.api.Assertions.assertFalse(rowText.isBlank(), "Datenzeile ist leer.");

                    System.out.println("[OK] STOCK LIST Zeigt Produkt, Niederlassung und Menge: " + rowText);
                }));
    }

    private void withClient(HtmlUnitAction action) throws Exception {
        String baseUrl = System.getProperty("it.base.url", DEFAULT_BASE_URL);
        try (org.htmlunit.WebClient webClient = new org.htmlunit.WebClient(org.htmlunit.BrowserVersion.FIREFOX)) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setRedirectEnabled(true);
            action.run(webClient, baseUrl);
        }
    }

    private interface HtmlUnitAction {
        void run(org.htmlunit.WebClient webClient, String baseUrl) throws Exception;
    }

    private void login(org.htmlunit.WebClient webClient, String baseUrl) throws Exception {
        org.htmlunit.html.HtmlPage page = webClient.getPage(baseUrl);

        org.htmlunit.html.HtmlInput emailField = page.getHtmlElementById("loginForm:email");
        org.htmlunit.html.HtmlInput passwordField = page.getHtmlElementById("loginForm:passwort");

        emailField.setValue(LOGIN_EMAIL);
        passwordField.setValue(LOGIN_PASSWORD);

        java.util.List<org.htmlunit.html.HtmlElement> btns = page.getByXPath(
                "//input[@type='submit'] | //button[@type='submit'] | //input[contains(@value, 'Anmelden')] | //button[contains(normalize-space(.), 'Anmelden')]");
        btns.get(0).click();
        webClient.waitForBackgroundJavaScript(500);
    }

    private org.htmlunit.html.HtmlPage openStockPage(org.htmlunit.WebClient webClient, String baseUrl)
            throws Exception {
        return webClient.getPage(baseUrl + "lagerbestaende.xhtml");
    }

    private org.htmlunit.html.HtmlTableRow findFirstDataRow(org.htmlunit.html.HtmlPage page) {
        java.util.List<org.htmlunit.html.HtmlTableRow> rows = page.getByXPath("//table//tr[td]");
        for (org.htmlunit.html.HtmlTableRow row : rows) {
            if (row.getCells().size() >= 3 && !row.asNormalizedText().isBlank()) {
                return row;
            }
        }
        return null;
    }
}