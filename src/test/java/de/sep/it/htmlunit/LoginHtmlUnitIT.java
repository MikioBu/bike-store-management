package de.sep.it.htmlunit;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class LoginHtmlUnitIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void login_erfolgreichAlsAdmin() {
        de.sep.it.util.SqlLogCounter.measure("login_erfolgreichAlsAdmin", () -> {
            withClient((webClient, baseUrl) -> {
                org.htmlunit.html.HtmlPage pageAfterLogin = login(webClient, baseUrl);

                String text = bodyText(pageAfterLogin);
                assertNoServerError(text);
                org.junit.jupiter.api.Assertions.assertTrue(
                        text.contains("Startseite")
                                || text.contains(LOGIN_EMAIL)
                                || text.contains("Abmelden")
                                || pageAfterLogin.getUrl().toString().contains("startseite.xhtml"),
                        "Login als Admin war nicht erfolgreich. URL: " + pageAfterLogin.getUrl());

                System.out.println("[OK] login_erfolgreichAlsAdmin");
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void login_mitFalschemPasswortSchlaegtFehl() {
        de.sep.it.util.SqlLogCounter.measure("login_mitFalschemPasswortSchlaegtFehl", () -> {
            withClient((webClient, baseUrl) -> {
                org.htmlunit.html.HtmlPage loginPage = webClient.getPage(baseUrl);

                org.htmlunit.html.HtmlInput emailField = loginPage.getHtmlElementById("loginForm:email");
                org.htmlunit.html.HtmlInput passwordField = loginPage.getHtmlElementById("loginForm:passwort");

                emailField.setValue(LOGIN_EMAIL);
                passwordField.setValue("falsches-passwort");

                org.htmlunit.html.HtmlPage pageAfterLogin = click(webClient, findLoginButton(loginPage));
                String text = bodyText(pageAfterLogin);

                assertNoServerError(text);
                org.junit.jupiter.api.Assertions.assertFalse(
                        text.contains("Abmelden") && text.contains(LOGIN_EMAIL),
                        "Login mit falschem Passwort war trotzdem erfolgreich.");
                org.junit.jupiter.api.Assertions.assertTrue(
                        text.contains("E-Mail oder Passwort falsch")
                                || text.contains("Bitte melden")
                                || text.contains("Passwort")
                                || pageAfterLogin.getUrl().toString().contains("login"),
                        "Fehlermeldung oder Loginseite wurde nicht angezeigt. URL: " + pageAfterLogin.getUrl());

                System.out.println("[OK] login_mitFalschemPasswortSchlaegtFehl");
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void geschuetzteKundenseite_ohneLoginLeitetZurLoginseite() {
        de.sep.it.util.SqlLogCounter.measure("geschuetzteKundenseite_ohneLoginLeitetZurLoginseite", () -> {
            withClient((webClient, baseUrl) -> {
                org.htmlunit.html.HtmlPage page = webClient.getPage(baseUrl + "kunden.xhtml");
                String text = bodyText(page);

                assertNoServerError(text);
                org.junit.jupiter.api.Assertions.assertTrue(
                        text.contains("Bitte melden")
                                || text.contains("Login")
                                || text.contains("E-Mail-Adresse")
                                || text.contains("Passwort")
                                || page.getUrl().toString().contains("login"),
                        "Kundenseite war ohne Login erreichbar oder keine Loginseite wurde angezeigt. URL: "
                                + page.getUrl());

                System.out.println("[OK] geschuetzteKundenseite_ohneLoginLeitetZurLoginseite");
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(4)
    void logout_fuehrtZurLoginseite() {
        de.sep.it.util.SqlLogCounter.measure("logout_fuehrtZurLoginseite", () -> {
            withClient((webClient, baseUrl) -> {
                org.htmlunit.html.HtmlPage startPage = login(webClient, baseUrl);

                org.htmlunit.html.HtmlElement logoutButton = findButtonByText(startPage, "Abmelden");
                org.htmlunit.html.HtmlPage pageAfterLogout = click(webClient, logoutButton);

                String text = bodyText(pageAfterLogout);
                assertNoServerError(text);

                org.junit.jupiter.api.Assertions.assertTrue(
                        text.contains("Bitte melden")
                                || text.contains("Login")
                                || text.contains("E-Mail-Adresse")
                                || text.contains("Passwort")
                                || pageAfterLogout.getUrl().toString().contains("login"),
                        "Logout hat nicht zur Loginseite geführt. URL: " + pageAfterLogout.getUrl());

                System.out.println("[OK] logout_fuehrtZurLoginseite");
            });
        });
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(5)
    void navigation_startseiteZeigtAlleHauptbereiche() {
        de.sep.it.util.SqlLogCounter.measure("navigation_startseiteZeigtAlleHauptbereiche", () -> {
            withClient((webClient, baseUrl) -> {
                org.htmlunit.html.HtmlPage startPage = login(webClient, baseUrl);
                String text = bodyText(startPage);

                assertNoServerError(text);
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("Kunden"), "Startseite zeigt Kunden nicht.");
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("Produkte"),
                        "Startseite zeigt Produkte nicht.");
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("Bestellungen"),
                        "Startseite zeigt Bestellungen nicht.");
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("Niederlassungen"),
                        "Startseite zeigt Niederlassungen nicht.");
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("Lagerbestände"),
                        "Startseite zeigt Lagerbestände nicht.");
                org.junit.jupiter.api.Assertions.assertTrue(text.contains("Mitarbeiter"),
                        "Startseite zeigt Mitarbeiter nicht. Admin-Login sollte Mitarbeiter sehen.");

                System.out.println("[OK] navigation_startseiteZeigtAlleHauptbereiche");
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

    private org.htmlunit.html.HtmlElement findLoginButton(org.htmlunit.html.HtmlPage page) {
        java.util.List<org.htmlunit.html.HtmlElement> buttons = page.getByXPath(
                "//input[@type='submit'] | //button[@type='submit'] | //input[contains(@value, 'Anmelden')] | //button[contains(normalize-space(.), 'Anmelden')]");
        org.junit.jupiter.api.Assertions.assertFalse(buttons.isEmpty(), "Login-Button wurde nicht gefunden.");
        return buttons.get(0);
    }

    private org.htmlunit.html.HtmlElement findButtonByText(org.htmlunit.html.HtmlPage page, String text) {
        java.util.List<org.htmlunit.html.HtmlElement> buttons = page.getByXPath(
                "//input[contains(@value, '" + text + "')] | //button[contains(normalize-space(.), '" + text + "')]");
        org.junit.jupiter.api.Assertions.assertFalse(buttons.isEmpty(), "Button wurde nicht gefunden: " + text);
        return buttons.get(0);
    }

    private org.htmlunit.html.HtmlPage click(org.htmlunit.WebClient webClient, org.htmlunit.html.HtmlElement element)
            throws Exception {
        org.htmlunit.Page page = element.click();
        webClient.waitForBackgroundJavaScript(500);

        org.junit.jupiter.api.Assertions.assertTrue(page instanceof org.htmlunit.html.HtmlPage,
                "Erwartet wurde eine HTML-Seite, bekommen wurde: " + page.getClass().getName());

        return (org.htmlunit.html.HtmlPage) page;
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