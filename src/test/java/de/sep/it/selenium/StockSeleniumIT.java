package de.sep.it.selenium;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class StockSeleniumIT {

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
        void stockList_lagerbestaendeLaden() {
                de.sep.it.util.SqlLogCounter.measure("stockList_lagerbestaendeLaden",
                                () -> withBrowser((driver, wait, baseUrl) -> {
                                        login(driver, wait, baseUrl);
                                        openStockPage(driver, wait, baseUrl);

                                        String text = bodyText(driver);
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        text.contains("Lagerbestände") || text.contains("Bestandsliste")
                                                                        || text.contains("Aktuelle Bestände"),
                                                        "Lagerbestandsseite wurde nicht plausibel geladen. Body: "
                                                                        + text);
                                        org.junit.jupiter.api.Assertions.assertFalse(
                                                        text.contains("Error processing request")
                                                                        || text.contains("Exception handling request"),
                                                        "Lagerbestandsseite erzeugt Serverfehler.");

                                        org.openqa.selenium.WebElement firstRow = findFirstStockRow(wait);
                                        org.junit.jupiter.api.Assertions.assertFalse(firstRow.getText().isBlank(),
                                                        "Erste Lagerbestandszeile ist leer");

                                        System.out.println("[OK] STOCK LIST Lagerbestände geladen");
                                }));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Order(2)
        void stockList_zeigtProduktUndNiederlassung() {
                de.sep.it.util.SqlLogCounter.measure("stockList_zeigtProduktUndNiederlassung",
                                () -> withBrowser((driver, wait, baseUrl) -> {
                                        login(driver, wait, baseUrl);
                                        openStockPage(driver, wait, baseUrl);

                                        String text = bodyText(driver);
                                        org.junit.jupiter.api.Assertions.assertTrue(
                                                        text.contains("Produkt") && (text.contains("Filial-ID")
                                                                        || text.contains("Niederlassung")
                                                                        || text.contains("Bestand")),
                                                        "Lagerbestandsliste zeigt Produkt-/Filialinformationen nicht erkennbar. Body: "
                                                                        + text);

                                        org.openqa.selenium.WebElement firstRow = findFirstStockRow(wait);
                                        java.util.List<org.openqa.selenium.WebElement> cells = firstRow
                                                        .findElements(org.openqa.selenium.By.cssSelector("td"));

                                        org.junit.jupiter.api.Assertions.assertTrue(cells.size() >= 5,
                                                        "Lagerbestandszeile enthält zu wenige Spalten. Erwartet: Produkt-ID, Produkt, Kategorie, Filial-ID, Bestand. Zeile: "
                                                                        + firstRow.getText());

                                        String productId = cells.get(0).getText().trim();
                                        String productName = cells.get(1).getText().trim();
                                        String category = cells.get(2).getText().trim();
                                        String storeId = cells.get(3).getText().trim();
                                        String quantity = cells.get(4).getText().trim();

                                        org.junit.jupiter.api.Assertions.assertFalse(productId.isBlank(),
                                                        "Produkt-ID ist leer. Zeile: " + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertFalse(productName.isBlank(),
                                                        "Produktname ist leer. Zeile: " + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertFalse(storeId.isBlank(),
                                                        "Filial-ID/Niederlassung ist leer. Zeile: "
                                                                        + firstRow.getText());
                                        org.junit.jupiter.api.Assertions.assertFalse(quantity.isBlank(),
                                                        "Bestand ist leer. Zeile: " + firstRow.getText());

                                        System.out.println(
                                                        "[OK] STOCK LIST Produkt und Niederlassung sichtbar: Produkt="
                                                                        + productName + ", Kategorie=" + category
                                                                        + ", Filial-ID=" + storeId
                                                                        + ", Bestand=" + quantity);
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
                if (known != null)
                        return createDriverForBrowser(known);
                String[] order = { "firefox", "chromium", "chrome", "edge" };
                RuntimeException lastException = null;
                for (String browser : order) {
                        try {
                                org.openqa.selenium.WebDriver driver = createDriverForBrowser(browser);
                                ACTIVE_BROWSER_NAME = browser;
                                if (BANNER_PRINTED.compareAndSet(false, true))
                                        printBrowserBanner(browser);
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

        private void openStockPage(org.openqa.selenium.WebDriver driver,
                        org.openqa.selenium.support.ui.WebDriverWait wait, String baseUrl) {
                driver.get(baseUrl + "lagerbestaende.xhtml");
                wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
                String text = bodyText(driver);
                org.junit.jupiter.api.Assertions.assertTrue(
                                text.contains("Lagerbestände") || text.contains("Bestandsliste")
                                                || text.contains("Aktuelle Bestände"),
                                "Lagerbestandsseite wurde nicht geladen. Body: " + text);
        }

        private org.openqa.selenium.WebElement findFirstStockRow(
                        org.openqa.selenium.support.ui.WebDriverWait wait) {
                return wait.until(webDriver -> {
                        java.util.List<org.openqa.selenium.WebElement> rows = webDriver
                                        .findElements(org.openqa.selenium.By.cssSelector("table tbody tr"));
                        for (org.openqa.selenium.WebElement row : rows) {
                                try {
                                        java.util.List<org.openqa.selenium.WebElement> cells = row
                                                        .findElements(org.openqa.selenium.By.cssSelector("td"));
                                        if (row.isDisplayed() && cells.size() >= 5 && !row.getText().isBlank())
                                                return row;
                                } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                                        return null;
                                }
                        }
                        return null;
                });
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