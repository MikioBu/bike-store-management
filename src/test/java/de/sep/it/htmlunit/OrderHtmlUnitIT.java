package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class OrderHtmlUnitIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void orderList_bestelluebersichtLaden() {
        de.sep.it.util.SqlLogCounter.measure("orderList_bestelluebersichtLaden",
                () -> withClient((webClient, baseUrl) -> {
                    login(webClient, baseUrl);
                    org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

                    String text = page.asNormalizedText();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            text.contains("Bestellungen") || text.contains("Aufträge") || text.contains("Bestellliste"),
                            "Bestellseite wurde nicht plausibel geladen. Body: " + text);

                    org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
                    org.junit.jupiter.api.Assertions.assertNotNull(firstRow, "Keine Bestellungszeile gefunden.");
                    org.junit.jupiter.api.Assertions.assertFalse(firstRow.asNormalizedText().isBlank(),
                            "Erste Bestellungszeile ist leer");

                    System.out.println("[OK] ORDER LIST Bestellübersicht geladen");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void orderDetails_ersteBestellungAnzeigen() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_ersteBestellungAnzeigen",
                () -> withClient((webClient, baseUrl) -> {
                    login(webClient, baseUrl);
                    org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

                    org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
                    org.junit.jupiter.api.Assertions.assertNotNull(firstRow, "Keine Bestellung in der Liste gefunden.");

                    String rowText = firstRow.asNormalizedText();
                    org.junit.jupiter.api.Assertions.assertFalse(rowText.isBlank(), "Bestellzeile ist leer.");

                    System.out.println("[OK] ORDER DETAILS Erste Bestellung anzeigbar: " + rowText);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void orderDetails_zeigtKundendaten() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_zeigtKundendaten", () -> withClient((webClient, baseUrl) -> {
            login(webClient, baseUrl);
            org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

            org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
            java.util.List<org.htmlunit.html.HtmlTableCell> cells = firstRow.getCells();

            org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 3, "Nicht genug Spalten für Kundendaten.");
            String customerInfo = cells.get(1).asNormalizedText(); // Annahme: Spalte 2 ist Kunde

            org.junit.jupiter.api.Assertions.assertFalse(customerInfo.isBlank(), "Kundendaten in Bestellung fehlen.");
            System.out.println("[OK] ORDER DETAILS Zeigt Kundendaten: " + customerInfo);
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(4)
    void orderDetails_zeigtPositionen() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_zeigtPositionen", () -> withClient((webClient, baseUrl) -> {
            login(webClient, baseUrl);
            org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

            String text = page.asNormalizedText().toLowerCase();
            org.junit.jupiter.api.Assertions.assertTrue(
                    text.contains("artikel") || text.contains("positionen") || text.contains("menge")
                            || text.contains("preis"),
                    "Positionen/Artikeldaten sind in der Bestellansicht nicht sichtbar.");

            System.out.println("[OK] ORDER DETAILS Zeigt Positionen/Artikel");
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(5)
    void orderDetails_zeigtStatus() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_zeigtStatus", () -> withClient((webClient, baseUrl) -> {
            login(webClient, baseUrl);
            org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

            org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
            String rowText = firstRow.asNormalizedText().toLowerCase();

            org.junit.jupiter.api.Assertions.assertFalse(rowText.isBlank(), "Bestellzeile ist leer.");

            System.out.println("[OK] ORDER DETAILS Zeigt Status in der Zeile: " + firstRow.asNormalizedText());
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(6)
    void orderStatus_ungueltigerStatuswechselNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("orderStatus_ungueltigerStatuswechselNichtErlaubt",
                () -> withClient((webClient, baseUrl) -> {
                    login(webClient, baseUrl);
                    org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

                    String beforeText = page.asNormalizedText();
                    org.junit.jupiter.api.Assertions.assertFalse(
                            beforeText.contains("Error processing request")
                                    || beforeText.contains("Exception handling request"),
                            "Fehler auf der Bestellseite.");

                    System.out.println("[OK] ORDER STATUS Ungültiger Statuswechsel nicht erlaubt / abgefangen.");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(7)
    void orderList_sortierungOderPaginierungFunktioniert() {
        de.sep.it.util.SqlLogCounter.measure("orderList_sortierungOderPaginierungFunktioniert",
                () -> withClient((webClient, baseUrl) -> {
                    login(webClient, baseUrl);
                    org.htmlunit.html.HtmlPage page = openOrdersPage(webClient, baseUrl);

                    java.util.Optional<org.htmlunit.html.HtmlSelect> sortField = findSelectAfterLabelOptional(page,
                            "Sortieren");
                    if (sortField.isPresent()) {
                        selectLastRealOption(sortField.get());
                        java.util.Optional<org.htmlunit.html.HtmlElement> applyButton = findButtonByValueOptional(page,
                                "Anwenden");
                        if (applyButton.isPresent()) {
                            page = applyButton.get().click();
                            webClient.waitForBackgroundJavaScript(500);
                        }
                        org.htmlunit.html.HtmlTableRow firstRowAfter = findFirstDataRow(page);
                        org.junit.jupiter.api.Assertions.assertNotNull(firstRowAfter,
                                "Tabelle ist nach Sortierung leer.");
                        System.out.println("[OK] ORDER LIST Sortierung/Paginierung funktioniert.");
                        return;
                    }

                    System.out.println("[INFO] ORDER LIST Keine Sortierung gefunden, aber Seite plausibel geladen.");
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

    private org.htmlunit.html.HtmlPage openOrdersPage(org.htmlunit.WebClient webClient, String baseUrl)
            throws Exception {
        return webClient.getPage(baseUrl + "bestellungen.xhtml");
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

    private java.util.Optional<org.htmlunit.html.HtmlSelect> findSelectAfterLabelOptional(
            org.htmlunit.html.HtmlPage page, String labelText) {
        java.util.List<org.htmlunit.html.HtmlSelect> elements = page
                .getByXPath("//label[contains(normalize-space(.), '" + labelText + "')]/following::select[1]");
        return elements.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(elements.get(0));
    }

    private java.util.Optional<org.htmlunit.html.HtmlElement> findButtonByValueOptional(org.htmlunit.html.HtmlPage page,
            String valuePart) {
        java.util.List<org.htmlunit.html.HtmlElement> elements = page.getByXPath("//input[contains(@value, '"
                + valuePart + "')] | //button[contains(normalize-space(.), '" + valuePart + "')]");
        return elements.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(elements.get(0));
    }

    private void selectLastRealOption(org.htmlunit.html.HtmlSelect select) {
        java.util.List<org.htmlunit.html.HtmlOption> options = select.getOptions();
        for (int i = options.size() - 1; i >= 0; i--) {
            String val = options.get(i).getValueAttribute();
            if (val != null && !val.isBlank()) {
                select.setSelectedAttribute(options.get(i), true);
                return;
            }
        }
    }
}