package de.sep.it.selenium;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class StoreSeleniumIT {

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
        void storeList_niederlassungenLaden() {
                de.sep.it.util.SqlLogCounter.measure("storeList_niederlassungenLaden",
                                () -> withBrowser((driver, wait, baseUrl) -> {
                                        login(driver, wait, baseUrl);
                                        openStoresPage(driver, wait, baseUrl);

                                        String text = bodyText(driver);
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        text.contains("Niederlassungen") || text.contains("Filialliste")
                                                                        || text.contains("Standorte"),
                                                        "Niederlassungsseite wurde nicht plausibel geladen. Body: "
                                                                        + text);
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        text.contains("Error processing request")
                                                                        || text.contains("Exception handling request"),
                                                        "Niederlassungsseite erzeugt Serverfehler.");

                                        org.openqa.selenium.WebElement firstRow = findFirstStoreRow(wait);
                                        org.junit.jupiter.api.Assertions.assertFalse(firstRow.getText().isBlank(),
                                                        "Erste Niederlassungszeile ist leer");

                                        System.out.println("[OK] STORE LIST Niederlassungen geladen");
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(2)
        void storeList_zeigtKontaktdaten() {
                de.sep.it.util.SqlLogCounter.measure("storeList_zeigtKontaktdaten",
                                () -> withBrowser((driver, wait, baseUrl) -> {
                                        login(driver, wait, baseUrl);
                                        openStoresPage(driver, wait, baseUrl);

                                        String text = bodyText(driver);
                                        String normalizedText = text.toLowerCase(java.util.Locale.ROOT);
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        normalizedText.contains("e-mail")
                                                                        || normalizedText.contains("telefon"),
                                                        "Niederlassungsliste zeigt keine Kontaktspalten. Body: "
                                                                        + text);

                                        org.openqa.selenium.WebElement firstRow = findFirstStoreRow(wait);
                                        java.util.List<org.openqa.selenium.WebElement> cells = firstRow
                                                        .findElements(org.openqa.selenium.By.cssSelector("td"));
                                        org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 4,
                                                        "Niederlassungszeile enthält zu wenige Spalten für Kontaktdaten. Zeile: "
                                                                        + firstRow.getText());

                                        String email = cells.get(2).getText().trim();
                                        String phone = cells.get(3).getText().trim();
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        email.contains("@") || !phone.isBlank(),
                                                        "Kontaktinformationen sind in der ersten Niederlassungszeile nicht sichtbar. Zeile: "
                                                                        + firstRow.getText());

                                        System.out.println("[OK] STORE LIST Kontaktdaten sichtbar: E-Mail=" + email
                                                        + ", Telefon=" + phone);
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(3)
        void storeList_sortierungOderTabelleFunktioniert() {
                de.sep.it.util.SqlLogCounter.measure("storeList_sortierungOderTabelleFunktioniert",
                                () -> withBrowser((driver, wait, baseUrl) -> {
                                        login(driver, wait, baseUrl);
                                        openStoresPage(driver, wait, baseUrl);

                                        String before = bodyText(driver);
                                        org.openqa.selenium.WebElement firstRowBefore = findFirstStoreRow(wait);
                                        String firstRowTextBefore = firstRowBefore.getText();

                                        java.util.Optional<org.openqa.selenium.WebElement> sortField = findSelectAfterLabelOptional(
                                                        driver, "Sortieren");
                                        if (sortField.isPresent()) {
                                                selectLastRealOption(sortField.get());

                                                java.util.Optional<org.openqa.selenium.WebElement> directionSelect = findSecondSelectNearSortOptional(
                                                                driver);
                                                directionSelect.ifPresent(this::selectLastRealOption);

                                                java.util.Optional<org.openqa.selenium.WebElement> applyButton = findButtonByTextOrIdSuffixOptional(
                                                                driver, "applySort", "Anwenden", "Sortieren");
                                                applyButton.ifPresent(org.openqa.selenium.WebElement::click);

                                                wait.until(webDriver -> bodyText(webDriver).contains("Filialliste")
                                                                || bodyText(webDriver).contains("Niederlassungen"));
                                                org.openqa.selenium.WebElement firstRowAfter = findFirstStoreRow(wait);
                                                org.junit.jupiter.api.Assertions.assertFalse(
                                                                firstRowAfter.getText().isBlank(),
                                                                "Tabelle ist nach Sortierung leer");

                                                System.out.println(
                                                                "[OK] STORE LIST Sortierung/Tabelle funktioniert. Vorher="
                                                                                + firstRowTextBefore + " | Nachher="
                                                                                + firstRowAfter.getText());
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
                                () -> withBrowser((driver, wait, baseUrl) -> {
                                        login(driver, wait, baseUrl);
                                        openStoresPage(driver, wait, baseUrl);

                                        org.openqa.selenium.WebElement firstRow = findFirstStoreRow(wait);
                                        java.util.List<org.openqa.selenium.WebElement> cells = firstRow
                                                        .findElements(org.openqa.selenium.By.cssSelector("td"));

                                        org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 8,
                                                        "Niederlassungstabelle enthält nicht alle erwarteten Spalten. Erwartet: ID, Filialname, E-Mail, Telefon, Straße, Stadt, Bundesland, PLZ. Zeile: "
                                                                        + firstRow.getText());

                                        String id = cells.get(0).getText().trim();
                                        String name = cells.get(1).getText().trim();
                                        String email = cells.get(2).getText().trim();
                                        String phone = cells.get(3).getText().trim();
                                        String street = cells.get(4).getText().trim();
                                        String city = cells.get(5).getText().trim();
                                        String state = cells.get(6).getText().trim();
                                        String zip = cells.get(7).getText().trim();

                                        org.junit.jupiter.api.Assertions.assertFalse(id.isBlank(),
                                                        "Store-ID ist leer. Zeile: " + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertFalse(name.isBlank(),
                                                        "Filialname ist leer. Zeile: " + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        email.contains("@") || !phone.isBlank(),
                                                        "E-Mail/Telefon sind nicht plausibel sichtbar. Zeile: "
                                                                        + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertFalse(city.isBlank(),
                                                        "Stadt ist leer. Zeile: " + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertFalse(zip.isBlank(),
                                                        "PLZ ist leer. Zeile: " + firstRow.getText());

                                        System.out.println("[OK] STORE READ Zugehörige Informationen sichtbar: ID=" + id
                                                        + ", Name=" + name + ", E-Mail=" + email + ", Telefon=" + phone
                                                        + ", Straße=" + street + ", Stadt=" + city + ", Bundesland="
                                                        + state
                                                        + ", PLZ=" + zip);
                                }));
        }

        // Browser-Infrastruktur

        private void withBrowser(BrowserAction action) {
                String baseUrl = System.getProperty("it.base.url", DEFAULT_BASE_URL);
                org.openqa.selenium.WebDriver driver = createBestAvailableDriver();
                org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(
                                driver, java.time.Duration.ofSeconds(25));
                try {
                        action.run(driver, wait, baseUrl);
                } finally {
                        try {
                                driver.quit();
                        } catch (RuntimeException ignored) {
                                System.out.println("[WARN] Browser konnte nicht sauber geschlossen werden: "
                                                + ACTIVE_BROWSER_NAME);
                        }
                }
        }

        private interface BrowserAction {
                void run(org.openqa.selenium.WebDriver driver,
                                org.openqa.selenium.support.ui.WebDriverWait wait,
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
                                String firstLine = e.getMessage() != null ? e.getMessage().split("\\R", 2)[0]
                                                : "(kein Fehlertext)";
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

        private void sleepSilently(long millis) {
                try {
                        Thread.sleep(millis);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }
        }

        // Login und Navigation

        private void login(org.openqa.selenium.WebDriver driver,
                        org.openqa.selenium.support.ui.WebDriverWait wait, String baseUrl) {
                driver.get(baseUrl);
                org.openqa.selenium.WebElement emailField = wait.until(
                                org.openqa.selenium.support.ui.ExpectedConditions
                                                .visibilityOfElementLocated(
                                                                org.openqa.selenium.By.id("loginForm:email")));
                org.openqa.selenium.WebElement passwordField = wait.until(
                                org.openqa.selenium.support.ui.ExpectedConditions
                                                .visibilityOfElementLocated(
                                                                org.openqa.selenium.By.id("loginForm:passwort")));
                emailField.clear();
                emailField.sendKeys(LOGIN_EMAIL);
                passwordField.clear();
                passwordField.sendKeys(LOGIN_PASSWORD);
                wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                                org.openqa.selenium.By.cssSelector("input[type='submit'], button[type='submit']")))
                                .click();
                wait.until(webDriver -> {
                        String text = bodyText(webDriver);
                        String url = webDriver.getCurrentUrl();
                        return text.contains("Startseite") || text.contains("Abmelden") || text.contains(LOGIN_EMAIL)
                                        || url.contains("startseite.xhtml")
                                        || text.contains("E-Mail oder Passwort falsch")
                                        || text.contains("Error processing request")
                                        || text.contains("Exception handling request");
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

        private void openStoresPage(org.openqa.selenium.WebDriver driver,
                        org.openqa.selenium.support.ui.WebDriverWait wait, String baseUrl) {
                driver.get(baseUrl + "niederlassungen.xhtml");
                wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
                String text = bodyText(driver);
                org.junit.jupiter.api.Assertions.assertTrue(
                                text.contains("Niederlassungen") || text.contains("Filialliste")
                                                || text.contains("Standorte"),
                                "Niederlassungsseite wurde nicht geladen. Body: " + text);
        }

        private org.openqa.selenium.WebElement findFirstStoreRow(
                        org.openqa.selenium.support.ui.WebDriverWait wait) {
                return wait.until(webDriver -> {
                        java.util.List<org.openqa.selenium.WebElement> rows = webDriver
                                        .findElements(org.openqa.selenium.By.cssSelector("table tbody tr"));
                        for (org.openqa.selenium.WebElement row : rows) {
                                try {
                                        java.util.List<org.openqa.selenium.WebElement> cells = row
                                                        .findElements(org.openqa.selenium.By.cssSelector("td"));
                                        if (row.isDisplayed() && cells.size() >= 8 && !row.getText().isBlank()) {
                                                return row;
                                        }
                                } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                                        return null;
                                }
                        }
                        return null;
                });
        }

        private java.util.Optional<org.openqa.selenium.WebElement> findSelectAfterLabelOptional(
                        org.openqa.selenium.WebDriver driver, String labelText) {
                java.util.List<org.openqa.selenium.WebElement> elements = driver.findElements(
                                org.openqa.selenium.By.xpath("//label[contains(normalize-space(.), '" + labelText
                                                + "')]/following::select[1]"));
                for (org.openqa.selenium.WebElement element : elements) {
                        if (element.isDisplayed() && element.isEnabled())
                                return java.util.Optional.of(element);
                }
                return java.util.Optional.empty();
        }

        private java.util.Optional<org.openqa.selenium.WebElement> findSecondSelectNearSortOptional(
                        org.openqa.selenium.WebDriver driver) {
                java.util.List<org.openqa.selenium.WebElement> elements = driver.findElements(
                                org.openqa.selenium.By.xpath(
                                                "//label[contains(normalize-space(.), 'Sortieren')]/following::select[2]"));
                for (org.openqa.selenium.WebElement element : elements) {
                        if (element.isDisplayed() && element.isEnabled())
                                return java.util.Optional.of(element);
                }
                return java.util.Optional.empty();
        }

        private java.util.Optional<org.openqa.selenium.WebElement> findButtonByTextOrIdSuffixOptional(
                        org.openqa.selenium.WebDriver driver, String idSuffix, String... textParts) {
                java.util.List<org.openqa.selenium.WebElement> byId = driver.findElements(
                                org.openqa.selenium.By.cssSelector("input[id$='" + idSuffix + "'], button[id$='"
                                                + idSuffix + "'], a[id$='" + idSuffix + "']"));
                for (org.openqa.selenium.WebElement c : byId) {
                        if (c.isDisplayed() && c.isEnabled())
                                return java.util.Optional.of(c);
                }
                java.util.List<org.openqa.selenium.WebElement> candidates = driver.findElements(
                                org.openqa.selenium.By.cssSelector(
                                                "input[type='submit'], input[type='button'], button, a"));
                for (org.openqa.selenium.WebElement c : candidates) {
                        String value = c.getAttribute("value");
                        String text = c.getText();
                        String combined = ((value != null ? value : "") + " " + (text != null ? text : "")).trim();
                        for (String part : textParts) {
                                if (combined.contains(part) && c.isDisplayed() && c.isEnabled())
                                        return java.util.Optional.of(c);
                        }
                }
                return java.util.Optional.empty();
        }

        private void selectLastRealOption(org.openqa.selenium.WebElement select) {
                org.openqa.selenium.support.ui.Select s = new org.openqa.selenium.support.ui.Select(select);
                java.util.List<org.openqa.selenium.WebElement> options = s.getOptions();
                for (int i = options.size() - 1; i >= 0; i--) {
                        String value = options.get(i).getAttribute("value");
                        if (value != null && !value.isBlank()) {
                                s.selectByValue(value);
                                return;
                        }
                }
                org.junit.jupiter.api.Assertions.fail("Select-Feld enthält keine auswählbare Option");
        }

        private String bodyText(org.openqa.selenium.WebDriver driver) {
                try {
                        return driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
                } catch (org.openqa.selenium.StaleElementReferenceException
                                | org.openqa.selenium.NoSuchElementException e) {
                        return "";
                }
        }
}