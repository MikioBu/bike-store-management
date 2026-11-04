package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class CustomerCrudHtmlUnitIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    private static final String UNIQUE = String.valueOf(System.currentTimeMillis());
    private static final String FIRST_NAME = "HtmlUnit";
    private static final String LAST_NAME = "AaaCrudkunde";
    private static final String EMAIL = "htmlunit.customer." + UNIQUE + "@example.test";
    private static final String PHONE = "(111) 222-3333";
    private static final String STREET = "Teststrasse 1";
    private static final String CITY = "Teststadt";
    private static final String UPDATED_CITY = "Updatehausen";
    private static final String STATE = "TS";
    private static final String ZIP_CODE = "12345";

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(6)
    void customerCreate_neuenKundenAnlegen() {
        de.sep.it.util.SqlLogCounter.measure("customerCreate_neuenKundenAnlegen", () -> {
            withClient((webClient, baseUrl) -> {
                login(webClient, baseUrl);

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                customersPage = createCustomer(
                        webClient,
                        customersPage,
                        FIRST_NAME,
                        LAST_NAME,
                        EMAIL,
                        PHONE,
                        STREET,
                        CITY,
                        STATE,
                        ZIP_CODE);

                customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                String rowText = findCustomerRowByEmail(customersPage, EMAIL).asNormalizedText();

                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(FIRST_NAME), "Create: Vorname fehlt.");
                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(LAST_NAME), "Create: Nachname fehlt.");
                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(EMAIL), "Create: E-Mail fehlt.");
                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(CITY), "Create: Stadt fehlt.");

                System.out.println("[OK] customerCreate_neuenKundenAnlegen: " + EMAIL);
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(7)
    void customerRead_angelegtenKundenInTabelleFinden() {
        de.sep.it.util.SqlLogCounter.measure("customerRead_angelegtenKundenInTabelleFinden", () -> {
            withClient((webClient, baseUrl) -> {
                login(webClient, baseUrl);

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                String rowText = findCustomerRowByEmail(customersPage, EMAIL).asNormalizedText();

                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(FIRST_NAME), "Read: Vorname fehlt.");
                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(LAST_NAME), "Read: Nachname fehlt.");
                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(EMAIL), "Read: E-Mail fehlt.");
                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(PHONE), "Read: Telefon fehlt.");

                System.out.println("[OK] customerRead_angelegtenKundenInTabelleFinden: " + EMAIL);
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(8)
    void customerUpdate_stadtDesKundenAendern() {
        de.sep.it.util.SqlLogCounter.measure("customerUpdate_stadtDesKundenAendern", () -> {
            withClient((webClient, baseUrl) -> {
                login(webClient, baseUrl);

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                customersPage = editCustomerCity(webClient, customersPage, EMAIL, UPDATED_CITY);

                customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                String rowText = findCustomerRowByEmail(customersPage, EMAIL).asNormalizedText();

                org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(UPDATED_CITY),
                        "Update: Neue Stadt fehlt.");

                System.out.println("[OK] customerUpdate_stadtDesKundenAendern: " + EMAIL);
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(9)
    void customerDelete_angelegtenKundenLoeschen() {
        de.sep.it.util.SqlLogCounter.measure("customerDelete_angelegtenKundenLoeschen", () -> {
            withClient((webClient, baseUrl) -> {
                login(webClient, baseUrl);

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                customersPage = deleteCustomer(webClient, customersPage, EMAIL);

                customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                org.junit.jupiter.api.Assertions.assertFalse(isCustomerVisibleByEmail(customersPage, EMAIL),
                        "Delete: Kunde ist noch sichtbar.");

                System.out.println("[OK] customerDelete_angelegtenKundenLoeschen: " + EMAIL);
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(10)
    void customerDelete_kundeMitBestehenderBestellungNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("customerDelete_kundeMitBestehenderBestellungNichtErlaubt", () -> {
            withClient((webClient, baseUrl) -> {
                login(webClient, baseUrl);

                String customerNameFromExistingOrder = findCustomerNameFromFirstExistingOrder(webClient, baseUrl);

                org.junit.jupiter.api.Assertions.assertFalse(customerNameFromExistingOrder.isBlank(),
                        "Kein Kunde aus bestehender Bestellung gefunden.");

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                org.htmlunit.html.HtmlElement customerRow = findCustomerRowByFullName(
                        customersPage,
                        customerNameFromExistingOrder);

                java.util.Optional<org.htmlunit.html.HtmlElement> deleteButton = findOptionalButtonInRow(customerRow,
                        "deleteCustomerButton");

                if (deleteButton.isEmpty()) {
                    System.out.println("[OK] Kunde mit bestehender Bestellung hat keinen Löschbutton: "
                            + customerNameFromExistingOrder);
                    return;
                }

                org.htmlunit.html.HtmlPage pageAfterDeleteAttempt = click(webClient, deleteButton.get());
                String textAfterDeleteAttempt = bodyText(pageAfterDeleteAttempt);

                assertNoServerError(textAfterDeleteAttempt);

                customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                org.junit.jupiter.api.Assertions.assertTrue(
                        isCustomerVisibleByFullName(customersPage, customerNameFromExistingOrder),
                        "Kunde mit bestehender Bestellung wurde gelöscht, obwohl das nicht erlaubt sein sollte: "
                                + customerNameFromExistingOrder);

                System.out.println("[OK] customerDelete_kundeMitBestehenderBestellungNichtErlaubt: "
                        + customerNameFromExistingOrder);
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(11)
    void customerCreate_ohnePflichtfeldNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("customerCreate_ohnePflichtfeldNichtErlaubt", () -> {
            withClient((webClient, baseUrl) -> {
                String invalidEmail = "htmlunit.customer.required." + UNIQUE + "@example.test";

                login(webClient, baseUrl);

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);
                customersPage = openCreateCustomerForm(webClient, customersPage);

                type(customersPage, "createCustomerForm:firstName", "");
                type(customersPage, "createCustomerForm:lastName", "Pflichtfeldtest");
                type(customersPage, "createCustomerForm:email", invalidEmail);
                type(customersPage, "createCustomerForm:phone", PHONE);
                type(customersPage, "createCustomerForm:street", STREET);
                type(customersPage, "createCustomerForm:city", CITY);
                type(customersPage, "createCustomerForm:state", STATE);
                type(customersPage, "createCustomerForm:zipCode", ZIP_CODE);

                org.htmlunit.html.HtmlPage pageAfterSave = clickById(
                        webClient,
                        customersPage,
                        "createCustomerForm:saveCreateCustomerButton");

                assertNoServerError(bodyText(pageAfterSave));

                customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                org.junit.jupiter.api.Assertions.assertFalse(
                        isCustomerVisibleByEmail(customersPage, invalidEmail),
                        "Kunde ohne Pflichtfeld wurde trotzdem gespeichert.");

                System.out.println("[OK] customerCreate_ohnePflichtfeldNichtErlaubt");
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(12)
    void customerCreate_ungueltigeEmailNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("customerCreate_ungueltigeEmailNichtErlaubt", () -> {
            withClient((webClient, baseUrl) -> {
                String invalidEmail = "ungueltige-email-" + UNIQUE;

                login(webClient, baseUrl);

                org.htmlunit.html.HtmlPage customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);
                customersPage = openCreateCustomerForm(webClient, customersPage);

                type(customersPage, "createCustomerForm:firstName", "Invalid");
                type(customersPage, "createCustomerForm:lastName", "Emailtest");
                type(customersPage, "createCustomerForm:email", invalidEmail);
                type(customersPage, "createCustomerForm:phone", PHONE);
                type(customersPage, "createCustomerForm:street", STREET);
                type(customersPage, "createCustomerForm:city", CITY);
                type(customersPage, "createCustomerForm:state", STATE);
                type(customersPage, "createCustomerForm:zipCode", ZIP_CODE);

                org.htmlunit.html.HtmlPage pageAfterSave = clickById(
                        webClient,
                        customersPage,
                        "createCustomerForm:saveCreateCustomerButton");

                assertNoServerError(bodyText(pageAfterSave));

                customersPage = openCustomersPage(webClient, baseUrl);
                customersPage = sortByLastNameAscending(webClient, customersPage);

                org.junit.jupiter.api.Assertions.assertFalse(
                        isCustomerVisibleByEmail(customersPage, invalidEmail),
                        "Kunde mit ungültiger E-Mail wurde trotzdem gespeichert.");

                System.out.println("[OK] customerCreate_ungueltigeEmailNichtErlaubt");
            });
        });
    }

    private void withClient(HtmlUnitAction action) throws Exception {
        String baseUrl = normalizeBaseUrl(System.getProperty("it.base.url", DEFAULT_BASE_URL));

        try (org.htmlunit.WebClient webClient = new org.htmlunit.WebClient(org.htmlunit.BrowserVersion.FIREFOX)) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setRedirectEnabled(true);
            webClient.setConfirmHandler((page, message) -> true);

            action.run(webClient, baseUrl);
        }
    }

    private org.htmlunit.html.HtmlPage login(org.htmlunit.WebClient webClient, String baseUrl) throws Exception {
        org.htmlunit.html.HtmlPage loginPage = webClient.getPage(baseUrl);

        org.htmlunit.html.HtmlInput emailField = loginPage.getHtmlElementById("loginForm:email");
        org.htmlunit.html.HtmlInput passwordField = loginPage.getHtmlElementById("loginForm:passwort");

        emailField.setValue(LOGIN_EMAIL);
        passwordField.setValue(LOGIN_PASSWORD);

        org.htmlunit.html.HtmlPage pageAfterLogin = click(webClient, findLoginButton(loginPage));
        String text = bodyText(pageAfterLogin);

        assertNoServerError(text);

        org.junit.jupiter.api.Assertions.assertFalse(text.contains("E-Mail oder Passwort falsch"),
                "Login fehlgeschlagen.");
        org.junit.jupiter.api.Assertions.assertTrue(
                text.contains("Startseite")
                        || text.contains(LOGIN_EMAIL)
                        || text.contains("Abmelden")
                        || pageAfterLogin.getUrl().toString().contains("startseite.xhtml"),
                "Login war nicht erfolgreich. URL: " + pageAfterLogin.getUrl());

        return pageAfterLogin;
    }

    private org.htmlunit.html.HtmlPage openCustomersPage(org.htmlunit.WebClient webClient, String baseUrl)
            throws Exception {
        org.htmlunit.html.HtmlPage page = webClient.getPage(baseUrl + "kunden.xhtml");
        String text = bodyText(page);

        assertNoServerError(text);

        org.junit.jupiter.api.Assertions.assertTrue(
                text.contains("Kunden") || text.contains("Kundenliste"),
                "Kundenseite wurde nicht geladen. URL: " + page.getUrl());

        return page;
    }

    private org.htmlunit.html.HtmlPage openOrdersPage(org.htmlunit.WebClient webClient, String baseUrl)
            throws Exception {
        org.htmlunit.html.HtmlPage page = webClient.getPage(baseUrl + "bestellungen.xhtml");
        String text = bodyText(page);

        assertNoServerError(text);

        org.junit.jupiter.api.Assertions.assertTrue(
                text.contains("Bestellungen") || text.contains("Aufträge"),
                "Bestellseite wurde nicht geladen. URL: " + page.getUrl());

        return page;
    }

    private org.htmlunit.html.HtmlPage sortByLastNameAscending(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlPage page) throws Exception {

        org.htmlunit.html.HtmlSelect sortField = page.getHtmlElementById("sortCustomerForm:sortField");
        org.htmlunit.html.HtmlSelect sortDirection = page.getHtmlElementById("sortCustomerForm:sortDirection");

        sortField.getOptionByValue("LAST_NAME").setSelected(true);
        sortDirection.getOptionByValue("ASC").setSelected(true);

        org.htmlunit.html.HtmlPage sortedPage = clickById(webClient, page, "sortCustomerForm:applySortButton");

        org.junit.jupiter.api.Assertions.assertTrue(
                bodyText(sortedPage).contains("Kundenliste"),
                "Kundenliste wurde nach Sortierung nicht angezeigt.");

        return sortedPage;
    }

    private org.htmlunit.html.HtmlPage openCreateCustomerForm(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlPage page) throws Exception {

        org.htmlunit.html.HtmlPage formPage = clickById(
                webClient,
                page,
                "openCreateCustomerForm:openCreateCustomerButton");

        org.junit.jupiter.api.Assertions.assertNotNull(
                formPage.getHtmlElementById("createCustomerForm:firstName"),
                "Create-Formular wurde nicht geöffnet.");

        return formPage;
    }

    private org.htmlunit.html.HtmlPage createCustomer(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlPage page,
            String firstName,
            String lastName,
            String email,
            String phone,
            String street,
            String city,
            String state,
            String zipCode) throws Exception {

        org.htmlunit.html.HtmlPage formPage = openCreateCustomerForm(webClient, page);

        type(formPage, "createCustomerForm:firstName", firstName);
        type(formPage, "createCustomerForm:lastName", lastName);
        type(formPage, "createCustomerForm:email", email);
        type(formPage, "createCustomerForm:phone", phone);
        type(formPage, "createCustomerForm:street", street);
        type(formPage, "createCustomerForm:city", city);
        type(formPage, "createCustomerForm:state", state);
        type(formPage, "createCustomerForm:zipCode", zipCode);

        org.htmlunit.html.HtmlPage pageAfterSave = clickById(
                webClient,
                formPage,
                "createCustomerForm:saveCreateCustomerButton");

        assertNoServerError(bodyText(pageAfterSave));

        return pageAfterSave;
    }

    private org.htmlunit.html.HtmlPage editCustomerCity(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlPage page,
            String email,
            String updatedCity) throws Exception {

        org.htmlunit.html.HtmlElement row = findCustomerRowByEmail(page, email);
        org.htmlunit.html.HtmlElement editButton = findButtonInRow(row, "editCustomerButton");

        org.htmlunit.html.HtmlPage editPage = click(webClient, editButton);

        type(editPage, "editCustomerForm:city", updatedCity);

        org.htmlunit.html.HtmlPage pageAfterSave = clickById(
                webClient,
                editPage,
                "editCustomerForm:saveEditCustomerButton");

        assertNoServerError(bodyText(pageAfterSave));

        return pageAfterSave;
    }

    private org.htmlunit.html.HtmlPage deleteCustomer(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlPage page,
            String email) throws Exception {

        org.htmlunit.html.HtmlElement row = findCustomerRowByEmail(page, email);
        org.htmlunit.html.HtmlElement deleteButton = findButtonInRow(row, "deleteCustomerButton");

        org.htmlunit.html.HtmlPage pageAfterDelete = click(webClient, deleteButton);

        assertNoServerError(bodyText(pageAfterDelete));

        return pageAfterDelete;
    }

    private String findCustomerNameFromFirstExistingOrder(
            org.htmlunit.WebClient webClient,
            String baseUrl) throws Exception {

        org.htmlunit.html.HtmlPage ordersPage = openOrdersPage(webClient, baseUrl);

        java.util.List<org.htmlunit.html.HtmlTableRow> rows = ordersPage.getByXPath("//table//tr[td]");

        for (org.htmlunit.html.HtmlTableRow row : rows) {
            java.util.List<org.htmlunit.html.HtmlTableCell> cells = row.getCells();

            if (cells.size() >= 2) {
                String customerName = cells.get(1).asNormalizedText().trim();

                if (!customerName.isBlank()) {
                    return customerName;
                }
            }
        }

        return "";
    }

    private org.htmlunit.html.HtmlElement findCustomerRowByEmail(
            org.htmlunit.html.HtmlPage page,
            String email) {

        java.util.List<org.htmlunit.html.HtmlElement> rows = page
                .getByXPath("//table[contains(@id, 'customerTable')]//tr");

        for (org.htmlunit.html.HtmlElement row : rows) {
            String text = row.asNormalizedText();

            if (text.contains(email)) {
                return row;
            }
        }

        throw new org.opentest4j.AssertionFailedError("Kunde wurde nicht gefunden: " + email);
    }

    private org.htmlunit.html.HtmlElement findCustomerRowByFullName(
            org.htmlunit.html.HtmlPage page,
            String fullName) {

        String normalizedName = normalize(fullName);

        java.util.List<org.htmlunit.html.HtmlElement> rows = page
                .getByXPath("//table[contains(@id, 'customerTable')]//tr");

        for (org.htmlunit.html.HtmlElement row : rows) {
            String rowText = normalize(row.asNormalizedText());

            if (rowText.contains(normalizedName) || containsAllNameParts(rowText, normalizedName)) {
                return row;
            }
        }

        throw new org.opentest4j.AssertionFailedError("Kunde wurde nicht gefunden: " + fullName);
    }

    private boolean isCustomerVisibleByEmail(org.htmlunit.html.HtmlPage page, String email) {
        java.util.List<org.htmlunit.html.HtmlElement> rows = page
                .getByXPath("//table[contains(@id, 'customerTable')]//tr");

        for (org.htmlunit.html.HtmlElement row : rows) {
            if (row.asNormalizedText().contains(email)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCustomerVisibleByFullName(org.htmlunit.html.HtmlPage page, String fullName) {
        String normalizedName = normalize(fullName);

        java.util.List<org.htmlunit.html.HtmlElement> rows = page
                .getByXPath("//table[contains(@id, 'customerTable')]//tr");

        for (org.htmlunit.html.HtmlElement row : rows) {
            String rowText = normalize(row.asNormalizedText());

            if (rowText.contains(normalizedName) || containsAllNameParts(rowText, normalizedName)) {
                return true;
            }
        }

        return false;
    }

    private org.htmlunit.html.HtmlElement findButtonInRow(
            org.htmlunit.html.HtmlElement row,
            String idPart) {

        return findOptionalButtonInRow(row, idPart)
                .orElseThrow(() -> new org.opentest4j.AssertionFailedError("Button nicht gefunden: " + idPart));
    }

    private java.util.Optional<org.htmlunit.html.HtmlElement> findOptionalButtonInRow(
            org.htmlunit.html.HtmlElement row,
            String idPart) {

        java.util.List<org.htmlunit.html.HtmlElement> buttons = row.getByXPath(
                ".//input[contains(@id, '" + idPart + "')] | .//button[contains(@id, '" + idPart + "')]");

        return buttons.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(buttons.get(0));
    }

    private org.htmlunit.html.HtmlElement findLoginButton(org.htmlunit.html.HtmlPage page) {
        java.util.List<org.htmlunit.html.HtmlElement> buttons = page.getByXPath(
                "//input[@type='submit'] | //button[@type='submit'] | //input[contains(@value, 'Anmelden')] | //button[contains(normalize-space(.), 'Anmelden')]");

        org.junit.jupiter.api.Assertions.assertFalse(buttons.isEmpty(), "Login-Button wurde nicht gefunden.");

        return buttons.get(0);
    }

    private org.htmlunit.html.HtmlPage clickById(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlPage page,
            String id) throws Exception {

        org.htmlunit.html.HtmlElement element = page.getHtmlElementById(id);
        return click(webClient, element);
    }

    private org.htmlunit.html.HtmlPage click(
            org.htmlunit.WebClient webClient,
            org.htmlunit.html.HtmlElement element) throws Exception {

        org.htmlunit.Page page = element.click();
        webClient.waitForBackgroundJavaScript(500);

        org.junit.jupiter.api.Assertions.assertTrue(page instanceof org.htmlunit.html.HtmlPage,
                "Erwartet wurde eine HTML-Seite, bekommen wurde: " + page.getClass().getName());

        return (org.htmlunit.html.HtmlPage) page;
    }

    private void type(org.htmlunit.html.HtmlPage page, String id, String value) {
        org.htmlunit.html.HtmlInput input = page.getHtmlElementById(id);
        input.setValue(value);
    }

    private String bodyText(org.htmlunit.html.HtmlPage page) {
        return page.asNormalizedText();
    }

    private void assertNoServerError(String text) {
        org.junit.jupiter.api.Assertions.assertFalse(text.contains("Error processing request"),
                "Serverfehler: Error processing request");
        org.junit.jupiter.api.Assertions.assertFalse(text.contains("Exception handling request"),
                "Serverfehler: Exception handling request");
    }

    private boolean containsAllNameParts(String rowText, String fullName) {
        String[] parts = fullName.split("\\s+");

        for (String part : parts) {
            if (!part.isBlank() && !rowText.contains(part)) {
                return false;
            }
        }

        return true;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }

        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    @FunctionalInterface
    private interface HtmlUnitAction {
        void run(org.htmlunit.WebClient webClient, String baseUrl) throws Exception;
    }
}