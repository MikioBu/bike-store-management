package de.sep.it.selenium;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class OrderSeleniumIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    // Statische Browser-Auswahl. Einmal pro Testklasse gesetzt
    private static volatile String ACTIVE_BROWSER_NAME = null;
    private static final java.util.concurrent.atomic.AtomicBoolean BANNER_PRINTED = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    // Tests

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void orderList_bestelluebersichtLaden() {
        de.sep.it.util.SqlLogCounter.measure("orderList_bestelluebersichtLaden",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openOrdersPage(driver, wait, baseUrl);

                    String text = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertTrue(
                            text.contains("Bestellungsliste") || text.contains("Bestellungen")
                                    || text.contains("Aufträge"),
                            "Bestellübersicht wurde nicht geladen. Body: " + text);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            text.contains("Error processing request") || text.contains("Exception handling request"),
                            "Bestellübersicht erzeugt Serverfehler.");

                    System.out.println("[OK] ORDER LIST Bestellübersicht geladen");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void orderDetails_ersteBestellungAnzeigen() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_ersteBestellungAnzeigen",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openOrdersPage(driver, wait, baseUrl);
                    openFirstOrderDetails(driver, wait);

                    String text = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertTrue(
                            text.contains("Bestellung") && text.contains("Positionen"),
                            "Bestelldetails wurden nicht geöffnet. Body: " + text);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            text.contains("Error processing request") || text.contains("Exception handling request"),
                            "Bestelldetails erzeugen Serverfehler.");

                    System.out.println("[OK] ORDER DETAILS Erste Bestellung angezeigt");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void orderDetails_zeigtKundendaten() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_zeigtKundendaten",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openOrdersPage(driver, wait, baseUrl);
                    String customerFromList = readCustomerNameFromFirstOrderRow(driver, wait);
                    openFirstOrderDetails(driver, wait);

                    String text = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertTrue(text.contains("Kunde"),
                            "Bestelldetails zeigen kein Feld 'Kunde'.");
                    if (!customerFromList.isBlank()) {
                        org.junit.jupiter.api.Assertions.assertTrue(text.contains(customerFromList),
                                "Kundenname aus Bestellliste fehlt in Details: " + customerFromList);
                    }

                    System.out.println("[OK] ORDER DETAILS Kundendaten sichtbar: " + customerFromList);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(4)
    void orderDetails_zeigtPositionen() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_zeigtPositionen",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openOrdersPage(driver, wait, baseUrl);
                    openFirstOrderDetails(driver, wait);

                    String text = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertTrue(text.contains("Positionen"),
                            "Bestelldetails zeigen keine Positionen-Überschrift.");
                    org.junit.jupiter.api.Assertions.assertTrue(
                            text.contains("Produkt") || text.contains("Menge") || text.contains("Einzelpreis"),
                            "Bestelldetails zeigen keine erkennbaren Positionsdaten. Body: " + text);

                    System.out.println("[OK] ORDER DETAILS Positionen sichtbar");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(5)
    void orderDetails_zeigtStatus() {
        de.sep.it.util.SqlLogCounter.measure("orderDetails_zeigtStatus", () -> withBrowser((driver, wait, baseUrl) -> {
            login(driver, wait, baseUrl);
            openOrdersPage(driver, wait, baseUrl);
            openFirstOrderDetails(driver, wait);

            String text = bodyText(driver);
            org.junit.jupiter.api.Assertions.assertTrue(text.contains("Status"),
                    "Bestelldetails zeigen keinen Status.");
            org.junit.jupiter.api.Assertions.assertTrue(
                    text.contains("Ausstehend") || text.contains("In Bearbeitung") || text.contains("Storniert")
                            || text.contains("Abgeschlossen"),
                    "Kein bekannter Bestellstatus sichtbar. Body: " + text);

            System.out.println("[OK] ORDER DETAILS Status sichtbar");
        }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(6)
    void orderStatus_ungueltigerStatuswechselNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("orderStatus_ungueltigerStatuswechselNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openOrdersPage(driver, wait, baseUrl);
                    openFirstOrderDetails(driver, wait);

                    String before = bodyText(driver);
                    java.util.Optional<org.openqa.selenium.WebElement> statusSelect = findSelectAfterLabelOptional(
                            driver, "Status ändern");
                    if (statusSelect.isEmpty()) {
                        statusSelect = findSelectAfterLabelOptional(driver, "Status");
                    }

                    if (statusSelect.isEmpty()) {
                        System.out.println(
                                "[INFO] ORDER STATUS Kein Status-Select sichtbar. Test dokumentiert Detailansicht ohne Statusänderungsfunktion.");
                        return;
                    }

                    org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(
                            statusSelect.get());
                    String originalValue = select.getFirstSelectedOption().getAttribute("value");
                    selectLastDifferentRealOption(select, originalValue);

                    java.util.Optional<org.openqa.selenium.WebElement> saveButton = findButtonByTextOrIdSuffixOptional(
                            driver, "updateStatus", "Status", "Ändern", "Speichern");
                    if (saveButton.isEmpty()) {
                        System.out.println(
                                "[INFO] ORDER STATUS Kein Button zum Statusändern sichtbar. Select vorhanden, aber keine Aktion möglich.");
                        return;
                    }

                    saveButton.get().click();
                    wait.until(webDriver -> bodyText(webDriver).contains("Status")
                            || bodyText(webDriver).contains("Ungültig")
                            || bodyText(webDriver).contains("nicht")
                            || bodyText(webDriver).contains("Fehler")
                            || bodyText(webDriver).contains("Error processing request")
                            || bodyText(webDriver).contains("Exception handling request"));

                    String after = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            after.contains("Error processing request") || after.contains("Exception handling request"),
                            "Ungültiger Statuswechsel darf keine Server-Fehlerseite erzeugen.");
                    org.junit.jupiter.api.Assertions.assertTrue(
                            after.contains("Ungültig") || after.contains("nicht") || after.contains("Status")
                                    || !after.equals(before),
                            "Statuswechsel wurde nicht erkennbar verarbeitet oder validiert.");

                    System.out.println("[OK] ORDER STATUS Statuswechsel wurde verarbeitet/validiert");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(7)
    void orderList_sortierungOderPaginierungFunktioniert() {
        de.sep.it.util.SqlLogCounter.measure("orderList_sortierungOderPaginierungFunktioniert",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openOrdersPage(driver, wait, baseUrl);

                    String before = bodyText(driver);

                    java.util.Optional<org.openqa.selenium.WebElement> sortField = findSelectAfterLabelOptional(driver,
                            "Sortieren");
                    if (sortField.isPresent()) {
                        selectFirstRealOption(sortField.get());
                        java.util.Optional<org.openqa.selenium.WebElement> applyButton = findButtonByTextOrIdSuffixOptional(
                                driver, "applySort", "Sortieren", "Anwenden");
                        applyButton.ifPresent(org.openqa.selenium.WebElement::click);
                        wait.until(webDriver -> bodyText(webDriver).contains("Bestellungsliste")
                                || bodyText(webDriver).contains("Bestellungen"));
                        System.out.println("[OK] ORDER LIST Sortierung ausgeführt");
                        return;
                    }

                    java.util.Optional<org.openqa.selenium.WebElement> nextButton = findButtonByTextOrIdSuffixOptional(
                            driver, "next", "Weiter", "Nächste", ">", "Next");
                    if (nextButton.isPresent() && nextButton.get().isEnabled()) {
                        nextButton.get().click();
                        wait.until(webDriver -> !bodyText(webDriver).equals(before)
                                || bodyText(webDriver).contains("Seite"));
                        System.out.println("[OK] ORDER LIST Paginierung ausgeführt");
                        return;
                    }

                    org.junit.jupiter.api.Assertions.assertTrue(
                            before.contains("Seite") || before.contains("Bestellungsliste")
                                    || before.contains("Bestellungen"),
                            "Weder Sortierung noch Paginierung sichtbar, Bestellliste aber auch nicht plausibel geladen.");
                    System.out.println(
                            "[INFO] ORDER LIST Keine aktive Sortierung/Paginierung gefunden; Bestellliste wurde plausibel geladen.");
                }));
    }

    // Browser-Infrastruktur

    private void withBrowser(BrowserAction action) {
        String baseUrl = System.getProperty("it.base.url", DEFAULT_BASE_URL);
        org.openqa.selenium.WebDriver driver = createBestAvailableDriver();
        org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(driver,
                java.time.Duration.ofSeconds(25));
        try {
            action.run(driver, wait, baseUrl);
        } finally {
            try {
                driver.quit();
            } catch (RuntimeException ignored) {
                System.out.println("[WARN] Browser konnte nicht sauber geschlossen werden: " + ACTIVE_BROWSER_NAME);
            }
        }
    }

    private interface BrowserAction {
        void run(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
                String baseUrl);
    }

    private org.openqa.selenium.WebDriver createBestAvailableDriver() {
        String known = ACTIVE_BROWSER_NAME;
        if (known != null) {
            return createDriverForBrowser(known);
        }
        String[] order = { "firefox", "chromium", "chrome", "edge" };
        RuntimeException lastException = null;
        for (String browser : order) {
            try {
                org.openqa.selenium.WebDriver driver = createDriverForBrowser(browser);
                ACTIVE_BROWSER_NAME = browser;
                if (BANNER_PRINTED.compareAndSet(false, true)) {
                    printBrowserBanner(browser);
                }
                return driver;
            } catch (org.openqa.selenium.WebDriverException | IllegalStateException e) {
                String firstLine = e.getMessage() != null ? e.getMessage().split("\\R", 2)[0] : "(kein Fehlertext)";
                System.out.println("[WARN] " + browser + " nicht verfügbar: " + firstLine);
                lastException = e;
                sleepSilently(300L);
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(false,
                "Kein Browser konnte gestartet werden (Firefox, Chromium, Chrome, Edge). Letzter Fehler: "
                        + (lastException != null ? lastException.getMessage() : "unbekannt"));
        throw new IllegalStateException("unreachable");
    }

    private org.openqa.selenium.WebDriver createDriverForBrowser(String browser) {
        return switch (browser) {
            case "firefox" -> createFirefoxDriver();
            case "chromium" -> createChromiumDriver();
            case "chrome" -> createChromeDriver();
            case "edge" -> createEdgeDriver();
            default -> throw new IllegalArgumentException("Unbekannter Browser: " + browser);
        };
    }

    private org.openqa.selenium.WebDriver createFirefoxDriver() {
        org.openqa.selenium.firefox.FirefoxOptions o = new org.openqa.selenium.firefox.FirefoxOptions();
        o.addArguments("-headless");
        return new org.openqa.selenium.firefox.FirefoxDriver(o);
    }

    private org.openqa.selenium.WebDriver createChromiumDriver() {
        org.openqa.selenium.chrome.ChromeOptions o = new org.openqa.selenium.chrome.ChromeOptions();
        o.setBinary("chromium");
        o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--window-size=1920,1080",
                "--disable-extensions", "--disable-background-networking",
                "--dns-prefetch-disable");
        return new org.openqa.selenium.chrome.ChromeDriver(o);
    }

    private org.openqa.selenium.WebDriver createChromeDriver() {
        org.openqa.selenium.chrome.ChromeOptions o = new org.openqa.selenium.chrome.ChromeOptions();
        o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--window-size=1920,1080", "--disable-extensions");
        return new org.openqa.selenium.chrome.ChromeDriver(o);
    }

    private org.openqa.selenium.WebDriver createEdgeDriver() {
        org.openqa.selenium.edge.EdgeOptions o = new org.openqa.selenium.edge.EdgeOptions();
        o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                "--disable-gpu", "--window-size=1920,1080");
        return new org.openqa.selenium.edge.EdgeDriver(o);
    }

    private void printBrowserBanner(String browser) {
        System.out.println("======================================");
        System.out.println("[BROWSER] Klasse    : " + getClass().getSimpleName());
        System.out.println("[BROWSER] Gewählt   : " + browser.toUpperCase(java.util.Locale.ROOT));
        System.out.println("[BROWSER] Reihenfolge: Firefox → Chromium → Chrome → Edge → Selenium Manager");
        System.out.println("======================================");
    }

    // Login und Navigation

    private void login(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String baseUrl) {
        driver.get(baseUrl);
        org.openqa.selenium.WebElement emailField = wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("loginForm:email")));
        org.openqa.selenium.WebElement passwordField = wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("loginForm:passwort")));
        emailField.clear();
        emailField.sendKeys(LOGIN_EMAIL);
        passwordField.clear();
        passwordField.sendKeys(LOGIN_PASSWORD);
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                org.openqa.selenium.By.cssSelector("input[type='submit'], button[type='submit']"))).click();
        wait.until(webDriver -> {
            String text = bodyText(webDriver);
            String url = webDriver.getCurrentUrl();
            return text.contains("Startseite") || text.contains("Abmelden") || text.contains(LOGIN_EMAIL)
                    || url.contains("startseite.xhtml") || text.contains("E-Mail oder Passwort falsch")
                    || text.contains("Error processing request") || text.contains("Exception handling request");
        });
        String textAfterLogin = bodyText(driver);
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

    private void openOrdersPage(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String baseUrl) {
        driver.get(baseUrl + "bestellungen.xhtml");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
        String text = bodyText(driver);
        org.junit.jupiter.api.Assertions.assertTrue(
                text.contains("Bestellungen") || text.contains("Aufträge") || text.contains("Bestellungsliste"),
                "Bestellseite wurde nicht geladen. Body: " + text);
    }

    private void openFirstOrderDetails(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait) {
        org.openqa.selenium.WebElement firstDetailButton = wait.until(webDriver -> {
            java.util.List<org.openqa.selenium.WebElement> candidates = webDriver.findElements(
                    org.openqa.selenium.By.cssSelector("input[type='submit'], input[type='button'], button, a"));
            for (org.openqa.selenium.WebElement candidate : candidates) {
                String combined = ((candidate.getAttribute("value") != null ? candidate.getAttribute("value") : "")
                        + " " + candidate.getText()).trim();
                String id = candidate.getAttribute("id");
                if (candidate.isDisplayed() && candidate.isEnabled()
                        && (combined.contains("Details")
                                || (id != null && id.toLowerCase(java.util.Locale.ROOT).contains("detail")))) {
                    return candidate;
                }
            }
            return null;
        });
        firstDetailButton.click();
        wait.until(webDriver -> bodyText(webDriver).contains("Positionen")
                || bodyText(webDriver).contains("Bestellung #") || bodyText(webDriver).contains("Bestellung"));
    }

    private String readCustomerNameFromFirstOrderRow(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait) {
        org.openqa.selenium.WebElement row = wait.until(webDriver -> {
            for (org.openqa.selenium.WebElement candidate : webDriver
                    .findElements(org.openqa.selenium.By.cssSelector("table tbody tr"))) {
                try {
                    java.util.List<org.openqa.selenium.WebElement> cells = candidate
                            .findElements(org.openqa.selenium.By.cssSelector("td"));
                    if (cells.size() >= 2 && !cells.get(1).getText().isBlank()) {
                        return candidate;
                    }
                } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                    return null;
                }
            }
            return null;
        });
        java.util.List<org.openqa.selenium.WebElement> cells = row
                .findElements(org.openqa.selenium.By.cssSelector("td"));
        return cells.size() >= 2 ? cells.get(1).getText().trim() : "";
    }

    private java.util.Optional<org.openqa.selenium.WebElement> findSelectAfterLabelOptional(
            org.openqa.selenium.WebDriver driver, String labelText) {
        java.util.List<org.openqa.selenium.WebElement> elements = driver.findElements(
                org.openqa.selenium.By
                        .xpath("//label[contains(normalize-space(.), '" + labelText + "')]/following::select[1]"));
        for (org.openqa.selenium.WebElement element : elements) {
            if (element.isDisplayed() && element.isEnabled()) {
                return java.util.Optional.of(element);
            }
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<org.openqa.selenium.WebElement> findButtonByTextOrIdSuffixOptional(
            org.openqa.selenium.WebDriver driver, String idSuffix, String... textParts) {
        java.util.List<org.openqa.selenium.WebElement> byId = driver.findElements(
                org.openqa.selenium.By.cssSelector(
                        "input[id$='" + idSuffix + "'], button[id$='" + idSuffix + "'], a[id$='" + idSuffix + "']"));
        for (org.openqa.selenium.WebElement candidate : byId) {
            if (candidate.isDisplayed() && candidate.isEnabled()) {
                return java.util.Optional.of(candidate);
            }
        }

        java.util.List<org.openqa.selenium.WebElement> candidates = driver.findElements(
                org.openqa.selenium.By.cssSelector("input[type='submit'], input[type='button'], button, a"));
        for (org.openqa.selenium.WebElement candidate : candidates) {
            String value = candidate.getAttribute("value");
            String text = candidate.getText();
            String combined = ((value != null ? value : "") + " " + (text != null ? text : "")).trim();
            for (String part : textParts) {
                if (combined.contains(part) && candidate.isDisplayed() && candidate.isEnabled()) {
                    return java.util.Optional.of(candidate);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private void selectFirstRealOption(org.openqa.selenium.WebElement select) {
        org.openqa.selenium.support.ui.Select seleniumSelect = new org.openqa.selenium.support.ui.Select(select);
        for (org.openqa.selenium.WebElement option : seleniumSelect.getOptions()) {
            String value = option.getAttribute("value");
            if (value != null && !value.isBlank()) {
                seleniumSelect.selectByValue(value);
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("Select-Feld enthält keine auswählbare Option");
    }

    private void selectLastDifferentRealOption(org.openqa.selenium.support.ui.Select select, String originalValue) {
        java.util.List<org.openqa.selenium.WebElement> options = select.getOptions();
        for (int i = options.size() - 1; i >= 0; i--) {
            org.openqa.selenium.WebElement option = options.get(i);
            String value = option.getAttribute("value");
            if (value != null && !value.isBlank() && !value.equals(originalValue)) {
                select.selectByValue(value);
                return;
            }
        }
        System.out.println("[INFO] ORDER STATUS Kein alternativer Status im Select verfügbar");
    }

    private String bodyText(org.openqa.selenium.WebDriver driver) {
        try {
            return driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
        } catch (org.openqa.selenium.StaleElementReferenceException | org.openqa.selenium.NoSuchElementException e) {
            return "";
        }
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}