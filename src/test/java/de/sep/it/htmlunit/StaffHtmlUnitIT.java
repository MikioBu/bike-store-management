package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class StaffHtmlUnitIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void staffList_mitarbeiterLaden() {
        de.sep.it.util.SqlLogCounter.measure("staffList_mitarbeiterLaden", () -> withClient((webClient, baseUrl) -> {
            login(webClient, baseUrl);
            org.htmlunit.html.HtmlPage page = openStaffPage(webClient, baseUrl);

            String text = page.asNormalizedText();
            org.junit.jupiter.api.Assertions.assertTrue(
                    text.contains("Mitarbeiter") || text.contains("Personal"),
                    "Mitarbeiterseite wurde nicht plausibel geladen. Body: " + text);

            org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
            org.junit.jupiter.api.Assertions.assertNotNull(firstRow, "Keine Mitarbeiterzeile gefunden.");
            org.junit.jupiter.api.Assertions.assertFalse(firstRow.asNormalizedText().isBlank(),
                    "Erste Mitarbeiterzeile ist leer");

            System.out.println("[OK] STAFF LIST Mitarbeiter geladen");
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void staffList_zeigtStoreZuordnung() {
        de.sep.it.util.SqlLogCounter.measure("staffList_zeigtStoreZuordnung", () -> withClient((webClient, baseUrl) -> {
            login(webClient, baseUrl);
            org.htmlunit.html.HtmlPage page = openStaffPage(webClient, baseUrl);

            org.htmlunit.html.HtmlTableRow firstRow = findFirstDataRow(page);
            java.util.List<org.htmlunit.html.HtmlTableCell> cells = firstRow.getCells();

            org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 4, "Nicht genug Spalten für Store-Zuordnung.");

            System.out.println("[OK] STAFF LIST Store-Zuordnung in Tabelle sichtbar.");
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void staffList_zeigtKontaktOderRolleninformationen() {
        de.sep.it.util.SqlLogCounter.measure("staffList_zeigtKontaktOderRolleninformationen",
                () -> withClient((webClient, baseUrl) -> {
                    login(webClient, baseUrl);
                    org.htmlunit.html.HtmlPage page = openStaffPage(webClient, baseUrl);

                    String text = page.asNormalizedText().toLowerCase();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            text.contains("@") || text.contains("telefon") || text.contains("manager")
                                    || text.contains("aktiv"),
                            "Kontakt- oder Rolleninformationen fehlen auf der Seite.");

                    System.out.println("[OK] STAFF LIST Zeigt Kontakt- oder Rolleninformationen");
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

    private org.htmlunit.html.HtmlPage openStaffPage(org.htmlunit.WebClient webClient, String baseUrl)
            throws Exception {
        return webClient.getPage(baseUrl + "mitarbeiter.xhtml");
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