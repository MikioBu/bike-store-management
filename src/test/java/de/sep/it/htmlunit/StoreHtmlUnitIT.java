package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class StoreHtmlUnitIT {

        private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
        private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
        private static final String LOGIN_PASSWORD = "555-5554";

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(1)
        void storeList_niederlassungenLaden() {
                de.sep.it.util.SqlLogCounter.measure("storeList_niederlassungenLaden",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openStoresPage(webClient, baseUrl);

                                        String text = page.asNormalizedText();
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        text.contains("Niederlassungen") || text.contains("Filialliste")
                                                                        || text.contains("Standorte"),
                                                        "Niederlassungsseite wurde nicht plausibel geladen. Body: "
                                                                        + text);
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        text.contains("Error processing request")
                                                                        || text.contains("Exception handling request"),
                                                        "Niederlassungsseite erzeugt Serverfehler.");

                                        org.htmlunit.html.HtmlTableRow firstRow = findFirstStoreRow(page);
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        firstRow.asNormalizedText().isBlank(),
                                                        "Erste Niederlassungszeile ist leer");

                                        System.out.println("[OK] STORE LIST Niederlassungen geladen");
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(2)
        void storeList_zeigtKontaktdaten() {
                de.sep.it.util.SqlLogCounter.measure("storeList_zeigtKontaktdaten",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openStoresPage(webClient, baseUrl);

                                        String text = page.asNormalizedText();
                                        String normalizedText = text.toLowerCase(java.util.Locale.ROOT);
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        normalizedText.contains("e-mail")
                                                                        || normalizedText.contains("telefon"),
                                                        "Niederlassungsliste zeigt keine Kontaktspalten. Body: "
                                                                        + text);

                                        org.htmlunit.html.HtmlTableRow firstRow = findFirstStoreRow(page);
                                        java.util.List<org.htmlunit.html.HtmlTableCell> cells = firstRow.getCells();
                                        org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 4,
                                                        "Niederlassungszeile enthält zu wenige Spalten für Kontaktdaten. Zeile: "
                                                                        + firstRow.asNormalizedText());

                                        String email = cells.get(2).asNormalizedText().trim();
                                        String phone = cells.get(3).asNormalizedText().trim();
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        email.contains("@") || !phone.isBlank(),
                                                        "Kontaktinformationen sind in der ersten Niederlassungszeile nicht sichtbar. Zeile: "
                                                                        + firstRow.asNormalizedText());

                                        System.out.println("[OK] STORE LIST Kontaktdaten sichtbar: E-Mail=" + email
                                                        + ", Telefon=" + phone);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(3)
        void storeList_sortierungOderTabelleFunktioniert() {
                de.sep.it.util.SqlLogCounter.measure("storeList_sortierungOderTabelleFunktioniert",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openStoresPage(webClient, baseUrl);

                                        String before = page.asNormalizedText();
                                        org.htmlunit.html.HtmlTableRow firstRowBefore = findFirstStoreRow(page);
                                        String firstRowTextBefore = firstRowBefore.asNormalizedText();

                                        java.util.Optional<org.htmlunit.html.HtmlSelect> sortField = findSelectAfterLabelOptional(
                                                        page, "Sortieren");
                                        if (sortField.isPresent()) {
                                                selectLastRealOption(sortField.get());

                                                java.util.Optional<org.htmlunit.html.HtmlSelect> directionSelect = findSecondSelectNearSortOptional(
                                                                page);
                                                directionSelect.ifPresent(this::selectLastRealOption);

                                                java.util.Optional<org.htmlunit.html.HtmlElement> applyButton = findButtonByValueOptional(
                                                                page,
                                                                "Anwenden");
                                                if (applyButton.isPresent()) {
                                                        page = applyButton.get().click();
                                                        webClient.waitForBackgroundJavaScript(500);
                                                }

                                                org.htmlunit.html.HtmlTableRow firstRowAfter = findFirstStoreRow(page);
                                                org.junit.jupiter.api.Assertions.assertFalse(
                                                                firstRowAfter.asNormalizedText().isBlank(),
                                                                "Tabelle ist nach Sortierung leer");

                                                System.out.println(
                                                                "[OK] STORE LIST Sortierung/Tabelle funktioniert. Vorher="
                                                                                + firstRowTextBefore + " | Nachher="
                                                                                + firstRowAfter.asNormalizedText());
                                                return;
                                        }

                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        before.contains("Filialliste")
                                                                        || before.contains("Niederlassungen"),
                                                        "Keine Sortierung gefunden und Tabelle nicht plausibel geladen.");
                                        System.out.println(
                                                        "[INFO] STORE LIST Kein Sortierfeld gefunden; Tabelle wurde plausibel geladen");
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(4)
        void storeDetailsOderRead_zeigtZugehoerigeInformationen() {
                de.sep.it.util.SqlLogCounter.measure("storeDetailsOderRead_zeigtZugehoerigeInformationen",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openStoresPage(webClient, baseUrl);

                                        org.htmlunit.html.HtmlTableRow firstRow = findFirstStoreRow(page);
                                        java.util.List<org.htmlunit.html.HtmlTableCell> cells = firstRow.getCells();

                                        org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 8,
                                                        "Niederlassungstabelle enthält nicht alle erwarteten Spalten. Erwartet: ID, Filialname, E-Mail, Telefon, Straße, Stadt, Bundesland, PLZ. Zeile: "
                                                                        + firstRow.asNormalizedText());

                                        String id = cells.get(0).asNormalizedText().trim();
                                        String name = cells.get(1).asNormalizedText().trim();
                                        String email = cells.get(2).asNormalizedText().trim();
                                        String phone = cells.get(3).asNormalizedText().trim();
                                        String street = cells.get(4).asNormalizedText().trim();
                                        String city = cells.get(5).asNormalizedText().trim();
                                        String state = cells.get(6).asNormalizedText().trim();
                                        String zip = cells.get(7).asNormalizedText().trim();

                                        org.junit.jupiter.api.Assertions.assertFalse(id.isBlank(),
                                                        "Store-ID ist leer. Zeile: " + firstRow.asNormalizedText());
                                        org.junit.jupiter.api.Assertions.assertFalse(name.isBlank(),
                                                        "Filialname ist leer. Zeile: " + firstRow.asNormalizedText());
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        email.contains("@") || !phone.isBlank(),
                                                        "E-Mail/Telefon sind nicht plausibel sichtbar. Zeile: "
                                                                        + firstRow.asNormalizedText());
                                        org.junit.jupiter.api.Assertions.assertFalse(city.isBlank(),
                                                        "Stadt ist leer. Zeile: " + firstRow.asNormalizedText());
                                        org.junit.jupiter.api.Assertions.assertFalse(zip.isBlank(),
                                                        "PLZ ist leer. Zeile: " + firstRow.asNormalizedText());

                                        System.out.println("[OK] STORE READ Zugehörige Informationen sichtbar: ID=" + id
                                                        + ", Name=" + name + ", E-Mail=" + email + ", Telefon=" + phone
                                                        + ", Straße=" + street + ", Stadt=" + city + ", Bundesland="
                                                        + state + ", PLZ=" + zip);
                                }));
        }

        private void withClient(HtmlUnitAction action) throws Exception {
                String baseUrl = System.getProperty("it.base.url", DEFAULT_BASE_URL);

                try (org.htmlunit.WebClient webClient = new org.htmlunit.WebClient(
                                org.htmlunit.BrowserVersion.FIREFOX)) {
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

                org.htmlunit.html.HtmlPage pageAfterLogin = findSubmitElement(page).click();
                webClient.waitForBackgroundJavaScript(500);

                String textAfterLogin = pageAfterLogin.asNormalizedText();
                org.junit.jupiter.api.Assertions.assertFalse(textAfterLogin.contains("Error processing request"),
                                "Login: Serverfehler");
                org.junit.jupiter.api.Assertions.assertFalse(textAfterLogin.contains("Exception handling request"),
                                "Login: Exception beim Request");
                org.junit.jupiter.api.Assertions.assertFalse(textAfterLogin.contains("E-Mail oder Passwort falsch"),
                                "Login fehlgeschlagen");
                org.junit.jupiter.api.Assertions.assertTrue(
                                textAfterLogin.contains("Startseite") || textAfterLogin.contains(LOGIN_EMAIL)
                                                || textAfterLogin.contains("Abmelden"),
                                "Login war nicht erfolgreich");
        }

        private org.htmlunit.html.HtmlPage openStoresPage(org.htmlunit.WebClient webClient, String baseUrl)
                        throws Exception {
                org.htmlunit.html.HtmlPage page = webClient.getPage(baseUrl + "niederlassungen.xhtml");
                String text = page.asNormalizedText();
                org.junit.jupiter.api.Assertions.assertTrue(
                                text.contains("Niederlassungen") || text.contains("Filialliste")
                                                || text.contains("Standorte"),
                                "Niederlassungsseite wurde nicht geladen. Body: " + text);
                org.junit.jupiter.api.Assertions.assertFalse(
                                text.contains("Error processing request")
                                                || text.contains("Exception handling request"),
                                "Niederlassungsseite erzeugt Serverfehler.");
                return page;
        }

        private org.htmlunit.html.HtmlTableRow findFirstStoreRow(org.htmlunit.html.HtmlPage page) {
                java.util.List<org.htmlunit.html.HtmlTableRow> rows = page.getByXPath("//table//tr[td]");
                for (org.htmlunit.html.HtmlTableRow row : rows) {
                        java.util.List<org.htmlunit.html.HtmlTableCell> cells = row.getCells();
                        if (cells.size() >= 8 && !row.asNormalizedText().isBlank()) {
                                return row;
                        }
                }
                org.junit.jupiter.api.Assertions
                                .fail("Keine Niederlassungszeile mit mindestens 8 Spalten gefunden. Body: "
                                                + page.asNormalizedText());
                return null;
        }

        private java.util.Optional<org.htmlunit.html.HtmlSelect> findSelectAfterLabelOptional(
                        org.htmlunit.html.HtmlPage page, String labelText) {
                java.util.List<org.htmlunit.html.HtmlSelect> elements = page.getByXPath(
                                "//label[contains(normalize-space(.), '" + labelText + "')]/following::select[1]");
                if (elements.isEmpty()) {
                        return java.util.Optional.empty();
                }
                return java.util.Optional.of(elements.get(0));
        }

        private java.util.Optional<org.htmlunit.html.HtmlSelect> findSecondSelectNearSortOptional(
                        org.htmlunit.html.HtmlPage page) {
                java.util.List<org.htmlunit.html.HtmlSelect> elements = page.getByXPath(
                                "//label[contains(normalize-space(.), 'Sortieren')]/following::select[2]");
                if (elements.isEmpty()) {
                        return java.util.Optional.empty();
                }
                return java.util.Optional.of(elements.get(0));
        }

        private java.util.Optional<org.htmlunit.html.HtmlElement> findButtonByValueOptional(
                        org.htmlunit.html.HtmlPage page, String valuePart) {
                java.util.List<org.htmlunit.html.HtmlElement> elements = page.getByXPath(
                                "//input[contains(@value, '" + valuePart + "')]"
                                                + " | //button[contains(normalize-space(.), '" + valuePart + "')]"
                                                + " | //a[contains(normalize-space(.), '" + valuePart + "')]");
                if (elements.isEmpty()) {
                        return java.util.Optional.empty();
                }
                return java.util.Optional.of(elements.get(0));
        }

        private void selectLastRealOption(org.htmlunit.html.HtmlSelect select) {
                java.util.List<org.htmlunit.html.HtmlOption> options = select.getOptions();
                for (int i = options.size() - 1; i >= 0; i--) {
                        String value = options.get(i).getValueAttribute();
                        if (value != null && !value.isBlank()) {
                                select.setSelectedAttribute(options.get(i), true);
                                return;
                        }
                }
                org.junit.jupiter.api.Assertions.fail("Select-Feld enthält keine auswählbare Option");
        }

        private org.htmlunit.html.HtmlElement findSubmitElement(org.htmlunit.html.HtmlPage page) {
                java.util.List<org.htmlunit.html.HtmlElement> candidates = page.getByXPath(
                                "//input[@type='submit'] | //button[@type='submit'] | //input[contains(@value, 'Anmelden')] | //button[contains(normalize-space(.), 'Anmelden')]");
                org.junit.jupiter.api.Assertions.assertFalse(candidates.isEmpty(), "Login-Button wurde nicht gefunden");
                return candidates.get(0);
        }
}
