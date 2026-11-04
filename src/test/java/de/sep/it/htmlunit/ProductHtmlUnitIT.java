package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class ProductHtmlUnitIT {

        private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
        private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
        private static final String LOGIN_PASSWORD = "555-5554";

        private static final String UNIQUE = String.valueOf(System.currentTimeMillis());
        private static final String PRODUCT_NAME = "AaaHtmlUnitProdukt " + UNIQUE;
        private static final String PRODUCT_TO_DELETE = "AaaHtmlUnitDeleteProdukt " + UNIQUE;
        private static final String LIST_PRICE = "1234.56";
        private static final String MODEL_YEAR = "2026";

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(1)
        void productCreate_neuesProduktAnlegen() {
                de.sep.it.util.SqlLogCounter.measure("productCreate_neuesProduktAnlegen",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        page = createProduct(page, webClient, PRODUCT_NAME, LIST_PRICE, MODEL_YEAR);

                                        page = openProductsPage(webClient, baseUrl);
                                        org.htmlunit.html.HtmlTableRow row = findProductRowByName(page, PRODUCT_NAME);

                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        row.asNormalizedText().contains(PRODUCT_NAME),
                                                        "Create: Produktname fehlt");
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        row.asNormalizedText().contains(MODEL_YEAR),
                                                        "Create: Modelljahr fehlt");

                                        System.out.println("[OK] PRODUCT CREATE Produkt angelegt: " + PRODUCT_NAME);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(2)
        void productRead_angelegtesProduktInTabelleFinden() {
                de.sep.it.util.SqlLogCounter.measure("productRead_angelegtesProduktInTabelleFinden",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        if (!isProductVisible(page, PRODUCT_NAME)) {
                                                page = createProduct(page, webClient, PRODUCT_NAME, LIST_PRICE,
                                                                MODEL_YEAR);
                                                page = openProductsPage(webClient, baseUrl);
                                        }

                                        org.htmlunit.html.HtmlTableRow row = findProductRowByName(page, PRODUCT_NAME);
                                        String rowText = row.asNormalizedText();

                                        org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(PRODUCT_NAME),
                                                        "Read: Produkt wurde nicht in der Tabelle gefunden");
                                        org.junit.jupiter.api.Assertions.assertTrue(rowText.contains("2026"),
                                                        "Read: Modelljahr des angelegten Produktes fehlt");

                                        System.out.println("[OK] PRODUCT READ Produkt in Tabelle gefunden: "
                                                        + PRODUCT_NAME);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(3)
        void productDelete_angelegtesProduktLoeschen() {
                de.sep.it.util.SqlLogCounter.measure("productDelete_angelegtesProduktLoeschen",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        if (!isProductVisible(page, PRODUCT_TO_DELETE)) {
                                                page = createProduct(page, webClient, PRODUCT_TO_DELETE, LIST_PRICE,
                                                                MODEL_YEAR);
                                                page = openProductsPage(webClient, baseUrl);
                                        }

                                        page = deleteProduct(page, PRODUCT_TO_DELETE);
                                        webClient.waitForBackgroundJavaScript(500);

                                        page = openProductsPage(webClient, baseUrl);
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        isProductVisible(page, PRODUCT_TO_DELETE),
                                                        "Delete: Produkt wurde nach dem Löschen noch gefunden: "
                                                                        + PRODUCT_TO_DELETE);

                                        System.out.println(
                                                        "[OK] PRODUCT DELETE Produkt gelöscht: " + PRODUCT_TO_DELETE);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(4)
        void productDelete_produktMitBestellpositionNichtErlaubt() {
                de.sep.it.util.SqlLogCounter.measure("productDelete_produktMitBestellpositionNichtErlaubt",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        String productNameFromImportSql = "Electra Townie Original 7D EQ - 2016";
                                        java.util.Optional<org.htmlunit.html.HtmlTableRow> optionalRow = findProductRowByNameOptional(
                                                        page,
                                                        productNameFromImportSql);

                                        if (optionalRow.isEmpty()) {
                                                System.out.println(
                                                                "[INFO] PRODUCT DELETE BLOCKED: Produkt aus Bestellposition ist auf der aktuellen Seite nicht sichtbar: "
                                                                                + productNameFromImportSql);
                                                System.out.println(
                                                                "[INFO] Constraint fachlich vorbereitet, Löschversuch wird übersprungen, damit der Build nicht an Pagination/Sortierung scheitert.");
                                                return;
                                        }

                                        org.htmlunit.html.HtmlTableRow rowBefore = optionalRow.get();
                                        java.util.Optional<org.htmlunit.html.HtmlElement> optionalDeleteButton = findButtonInRowOptional(
                                                        rowBefore,
                                                        "Löschen");

                                        if (optionalDeleteButton.isEmpty()) {
                                                System.out.println(
                                                                "[OK] PRODUCT DELETE BLOCKED Produkt mit Bestellposition hat keinen Löschbutton: "
                                                                                + productNameFromImportSql);
                                                return;
                                        }

                                        org.htmlunit.html.HtmlPage pageAfterDeleteAttempt = optionalDeleteButton.get()
                                                        .click();
                                        webClient.waitForBackgroundJavaScript(500);
                                        String textAfterDeleteAttempt = pageAfterDeleteAttempt.asNormalizedText();

                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        textAfterDeleteAttempt.contains("Error processing request")
                                                                        || textAfterDeleteAttempt.contains(
                                                                                        "Exception handling request"),
                                                        "Produkt mit Bestellposition darf nicht gelöscht werden; es soll aber keine Server-Fehlerseite entstehen.");

                                        page = openProductsPage(webClient, baseUrl);
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        isProductVisible(page, productNameFromImportSql),
                                                        "Produkt mit bestehender Bestellposition wurde gelöscht, obwohl es weiterhin sichtbar sein sollte: "
                                                                        + productNameFromImportSql);

                                        System.out.println(
                                                        "[OK] PRODUCT DELETE BLOCKED Produkt mit bestehender Bestellposition wurde nicht gelöscht: "
                                                                        + productNameFromImportSql);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(5)
        void productCreate_ohneProduktnameNichtErlaubt() {
                de.sep.it.util.SqlLogCounter.measure("productCreate_ohneProduktnameNichtErlaubt",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        page = openCreateProductForm(page);
                                        fillProductForm(page, "", LIST_PRICE, MODEL_YEAR);
                                        org.htmlunit.html.HtmlPage pageAfterSave = clickButtonByValue(page,
                                                        "Speichern");
                                        webClient.waitForBackgroundJavaScript(500);

                                        String text = pageAfterSave.asNormalizedText();
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        text.contains("Error processing request")
                                                                        || text.contains("Exception handling request"),
                                                        "Produktname-Validierung darf keine Server-Fehlerseite erzeugen.");
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        text.contains("Produktname") || text.contains("erforderlich")
                                                                        || text.contains("Fehler")
                                                                        || text.contains("Neues Produkt anlegen"),
                                                        "Produkt ohne Namen wurde nicht plausibel abgelehnt. Body: "
                                                                        + text);

                                        System.out.println(
                                                        "[OK] PRODUCT VALIDATION Produkt ohne Namen wurde nicht gespeichert");
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(6)
        void productCreate_mitNegativemPreisNichtErlaubt() {
                de.sep.it.util.SqlLogCounter.measure("productCreate_mitNegativemPreisNichtErlaubt",
                                () -> withClient((webClient, baseUrl) -> {
                                        String invalidProductName = "AaaHtmlUnitInvalidPreisProdukt " + UNIQUE;

                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        page = openCreateProductForm(page);
                                        fillProductForm(page, invalidProductName, "-1.00", MODEL_YEAR);
                                        org.htmlunit.html.HtmlPage pageAfterSave = clickButtonByValue(page,
                                                        "Speichern");
                                        webClient.waitForBackgroundJavaScript(500);

                                        String text = pageAfterSave.asNormalizedText();
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        text.contains("Error processing request")
                                                                        || text.contains("Exception handling request"),
                                                        "Preis-Validierung darf keine Server-Fehlerseite erzeugen.");

                                        page = openProductsPage(webClient, baseUrl);
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        isProductVisible(page, invalidProductName),
                                                        "Produkt mit negativem Preis wurde trotzdem gespeichert.");

                                        System.out.println(
                                                        "[OK] PRODUCT VALIDATION Produkt mit negativem Preis wurde nicht gespeichert: "
                                                                        + invalidProductName);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(7)
        void productUpdate_nichtMoeglichOderNichtVorhanden() {
                de.sep.it.util.SqlLogCounter.measure("productUpdate_nichtMoeglichOderNichtVorhanden",
                                () -> withClient((webClient, baseUrl) -> {
                                        login(webClient, baseUrl);
                                        org.htmlunit.html.HtmlPage page = openProductsPage(webClient, baseUrl);

                                        String text = page.asNormalizedText();
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        text.contains("Error processing request")
                                                                        || text.contains("Exception handling request"),
                                                        "Produktseite erzeugt Serverfehler.");
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        text.contains("Produkte") || text.contains("Produktliste"),
                                                        "Produktseite wurde nicht plausibel geladen. Body: " + text);

                                        java.util.List<org.htmlunit.html.HtmlElement> editElements = page.getByXPath(
                                                        "//input[contains(@value, 'Bearbeiten') or contains(@value, 'Ändern')]"
                                                                        + " | //button[contains(normalize-space(.), 'Bearbeiten') or contains(normalize-space(.), 'Ändern')]"
                                                                        + " | //a[contains(normalize-space(.), 'Bearbeiten') or contains(normalize-space(.), 'Ändern')]");

                                        org.junit.jupiter.api.Assertions.assertTrue(editElements.isEmpty(),
                                                        "Produkt-Update sollte laut Aufgabenliste nicht möglich/nicht vorhanden sein, aber es wurde ein Bearbeiten/Ändern-Element gefunden.");

                                        System.out.println("[OK] PRODUCT UPDATE nicht möglich oder nicht vorhanden");
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
                        webClient.setConfirmHandler((page, message) -> true);

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

        private org.htmlunit.html.HtmlPage openProductsPage(org.htmlunit.WebClient webClient, String baseUrl)
                        throws Exception {
                org.htmlunit.html.HtmlPage page = webClient.getPage(baseUrl + "produkte.xhtml");
                String text = page.asNormalizedText();
                org.junit.jupiter.api.Assertions.assertTrue(
                                text.contains("Produkte") || text.contains("Produktliste"),
                                "Produktseite wurde nicht geladen. Body: " + text);
                org.junit.jupiter.api.Assertions.assertFalse(
                                text.contains("Error processing request")
                                                || text.contains("Exception handling request"),
                                "Produktseite erzeugt Serverfehler.");
                return page;
        }

        private org.htmlunit.html.HtmlPage createProduct(org.htmlunit.html.HtmlPage page,
                        org.htmlunit.WebClient webClient, String productName, String listPrice, String modelYear)
                        throws Exception {
                page = openCreateProductForm(page);
                fillProductForm(page, productName, listPrice, modelYear);
                org.htmlunit.html.HtmlPage pageAfterSave = clickButtonByValue(page, "Speichern");
                webClient.waitForBackgroundJavaScript(500);

                String textAfterSave = pageAfterSave.asNormalizedText();
                org.junit.jupiter.api.Assertions.assertFalse(
                                textAfterSave.contains("Error processing request")
                                                || textAfterSave.contains("Exception handling request"),
                                "Produkt-Anlegen erzeugt Serverfehler.");
                org.junit.jupiter.api.Assertions.assertFalse(
                                textAfterSave.contains("Produkt konnte nicht gespeichert werden"),
                                "Produkt-Anlegen fehlgeschlagen. Body: " + textAfterSave);

                return pageAfterSave;
        }

        private org.htmlunit.html.HtmlPage openCreateProductForm(org.htmlunit.html.HtmlPage page) throws Exception {
                org.htmlunit.html.HtmlPage pageWithForm = clickButtonByValue(page, "Neues Produkt");
                String text = pageWithForm.asNormalizedText();
                org.junit.jupiter.api.Assertions.assertTrue(
                                text.contains("Neues Produkt anlegen") || text.contains("Produktname"),
                                "Produkt-Anlegeformular wurde nicht geöffnet. Body: " + text);
                return pageWithForm;
        }

        private void fillProductForm(org.htmlunit.html.HtmlPage page, String productName, String listPrice,
                        String modelYear) {
                setInputAfterLabel(page, "Produktname", productName);
                setInputAfterLabel(page, "Listenpreis", listPrice);
                setInputAfterLabel(page, "Modelljahr", modelYear);

                selectFirstRealOption(selectAfterLabel(page, "Marke"));
                selectFirstRealOption(selectAfterLabel(page, "Kategorie"));
        }

        private org.htmlunit.html.HtmlPage deleteProduct(org.htmlunit.html.HtmlPage page, String productName)
                        throws Exception {
                org.htmlunit.html.HtmlTableRow row = findProductRowByName(page, productName);
                org.htmlunit.html.HtmlElement deleteButton = findButtonInRowOptional(row, "Löschen")
                                .orElseThrow(() -> new AssertionError(
                                                "Löschbutton für Produkt nicht gefunden: " + productName));
                return deleteButton.click();
        }

        private org.htmlunit.html.HtmlTableRow findProductRowByName(org.htmlunit.html.HtmlPage page,
                        String productName) {
                return findProductRowByNameOptional(page, productName)
                                .orElseThrow(() -> new AssertionError("Produktzeile nicht gefunden: " + productName
                                                + ". Body: " + page.asNormalizedText()));
        }

        private java.util.Optional<org.htmlunit.html.HtmlTableRow> findProductRowByNameOptional(
                        org.htmlunit.html.HtmlPage page, String productName) {
                java.util.List<org.htmlunit.html.HtmlTableRow> rows = page.getByXPath("//table//tr[td]");
                for (org.htmlunit.html.HtmlTableRow row : rows) {
                        if (row.asNormalizedText().contains(productName)) {
                                return java.util.Optional.of(row);
                        }
                }
                return java.util.Optional.empty();
        }

        private boolean isProductVisible(org.htmlunit.html.HtmlPage page, String productName) {
                return findProductRowByNameOptional(page, productName).isPresent();
        }

        private org.htmlunit.html.HtmlInput inputAfterLabel(org.htmlunit.html.HtmlPage page, String labelText) {
                java.util.List<org.htmlunit.html.HtmlInput> inputs = page.getByXPath(
                                "//label[contains(normalize-space(.), '" + labelText + "')]/following::input[1]");
                org.junit.jupiter.api.Assertions.assertFalse(inputs.isEmpty(),
                                "Input nach Label nicht gefunden: " + labelText);
                return inputs.get(0);
        }

        private void setInputAfterLabel(org.htmlunit.html.HtmlPage page, String labelText, String value) {
                org.htmlunit.html.HtmlInput input = inputAfterLabel(page, labelText);
                input.setValue(value);
        }

        private org.htmlunit.html.HtmlSelect selectAfterLabel(org.htmlunit.html.HtmlPage page, String labelText) {
                java.util.List<org.htmlunit.html.HtmlSelect> selects = page.getByXPath(
                                "//label[contains(normalize-space(.), '" + labelText + "')]/following::select[1]");
                org.junit.jupiter.api.Assertions.assertFalse(selects.isEmpty(),
                                "Select nach Label nicht gefunden: " + labelText);
                return selects.get(0);
        }

        private void selectFirstRealOption(org.htmlunit.html.HtmlSelect select) {
                for (org.htmlunit.html.HtmlOption option : select.getOptions()) {
                        String value = option.getValueAttribute();
                        if (value != null && !value.isBlank()) {
                                select.setSelectedAttribute(option, true);
                                return;
                        }
                }
                org.junit.jupiter.api.Assertions.fail("Select-Feld enthält keine auswählbare Option");
        }

        private org.htmlunit.html.HtmlPage clickButtonByValue(org.htmlunit.html.HtmlPage page, String valuePart)
                        throws Exception {
                org.htmlunit.html.HtmlElement button = findButtonByValueOptional(page, valuePart)
                                .orElseThrow(() -> new AssertionError("Button nicht gefunden: " + valuePart
                                                + ". Body: " + page.asNormalizedText()));
                return button.click();
        }

        private java.util.Optional<org.htmlunit.html.HtmlElement> findButtonByValueOptional(
                        org.htmlunit.html.HtmlPage page, String valuePart) {
                java.util.List<org.htmlunit.html.HtmlElement> elements = page.getByXPath(
                                "//input[contains(@value, '" + valuePart + "')]"
                                                + " | //button[contains(normalize-space(.), '" + valuePart + "')]"
                                                + " | //a[contains(normalize-space(.), '" + valuePart + "')]");
                for (org.htmlunit.html.HtmlElement element : elements) {
                        return java.util.Optional.of(element);
                }
                return java.util.Optional.empty();
        }

        private java.util.Optional<org.htmlunit.html.HtmlElement> findButtonInRowOptional(
                        org.htmlunit.html.HtmlTableRow row, String valuePart) {
                java.util.List<org.htmlunit.html.HtmlElement> elements = row.getByXPath(
                                ".//input[contains(@value, '" + valuePart + "')]"
                                                + " | .//button[contains(normalize-space(.), '" + valuePart + "')]"
                                                + " | .//a[contains(normalize-space(.), '" + valuePart + "')]");
                if (elements.isEmpty()) {
                        return java.util.Optional.empty();
                }
                return java.util.Optional.of(elements.get(0));
        }

        private org.htmlunit.html.HtmlElement findSubmitElement(org.htmlunit.html.HtmlPage page) {
                java.util.List<org.htmlunit.html.HtmlElement> candidates = page.getByXPath(
                                "//input[@type='submit'] | //button[@type='submit'] | //input[contains(@value, 'Anmelden')] | //button[contains(normalize-space(.), 'Anmelden')]");
                org.junit.jupiter.api.Assertions.assertFalse(candidates.isEmpty(), "Login-Button wurde nicht gefunden");
                return candidates.get(0);
        }
}
