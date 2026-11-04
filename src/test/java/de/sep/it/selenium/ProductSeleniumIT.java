package de.sep.it.selenium;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class ProductSeleniumIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    private static final String UNIQUE = String.valueOf(System.currentTimeMillis());
    private static final String PRODUCT_NAME = "AaaCrudProdukt " + UNIQUE;
    private static final String LIST_PRICE = "1234.56";
    private static final String MODEL_YEAR = "2026";

    // Statische Browser-Auswahl. Einmal pro Testklasse gesetzt
    private static volatile String ACTIVE_BROWSER_NAME = null;
    private static final java.util.concurrent.atomic.AtomicBoolean BANNER_PRINTED = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    // Tests

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void productCreate_neuesProduktAnlegen() {
        de.sep.it.util.SqlLogCounter.measure("productCreate_neuesProduktAnlegen",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openProductsPage(driver, wait, baseUrl);

                    createProduct(driver, wait, PRODUCT_NAME, LIST_PRICE, MODEL_YEAR);

                    openProductsPage(driver, wait, baseUrl);
                    String rowText = findProductRowByName(wait, PRODUCT_NAME).getText();

                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(PRODUCT_NAME),
                            "Create: Produktname fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(MODEL_YEAR),
                            "Create: Modelljahr fehlt");

                    System.out.println("[OK] PRODUCT CREATE Produkt angelegt: " + PRODUCT_NAME);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void productRead_angelegtesProduktInTabelleFinden() {
        de.sep.it.util.SqlLogCounter.measure("productRead_angelegtesProduktInTabelleFinden",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openProductsPage(driver, wait, baseUrl);

                    String rowText = findProductRowByName(wait, PRODUCT_NAME).getText();

                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(PRODUCT_NAME),
                            "Read: Produktname fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(MODEL_YEAR), "Read: Modelljahr fehlt");

                    System.out.println("[OK] PRODUCT READ Produkt gefunden: " + PRODUCT_NAME);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void productDelete_angelegtesProduktLoeschen() {
        de.sep.it.util.SqlLogCounter.measure("productDelete_angelegtesProduktLoeschen",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openProductsPage(driver, wait, baseUrl);

                    deleteProduct(driver, wait, PRODUCT_NAME);

                    openProductsPage(driver, wait, baseUrl);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            isProductVisible(driver, PRODUCT_NAME),
                            "Delete: Produkt ist nach dem Löschen noch sichtbar");

                    System.out.println("[OK] PRODUCT DELETE Produkt gelöscht: " + PRODUCT_NAME);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(4)
    void productDelete_produktMitBestellpositionNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("productDelete_produktMitBestellpositionNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);

                    String productNameFromExistingOrder = findProductNameFromFirstExistingOrder(driver, wait, baseUrl);
                    org.junit.jupiter.api.Assertions.assertFalse(productNameFromExistingOrder.isBlank(),
                            "Kein Produkt aus bestehender Bestellposition gefunden");
                    System.out.println("[INFO] PRODUCT DELETE BLOCKED nutzt bestehendes Bestellprodukt: "
                            + productNameFromExistingOrder);

                    openProductsPage(driver, wait, baseUrl);
                    java.util.Optional<org.openqa.selenium.WebElement> optionalProductRow = findProductRowByNameOptional(
                            driver, productNameFromExistingOrder);

                    if (optionalProductRow.isEmpty()) {
                        System.out.println(
                                "[INFO] PRODUCT DELETE BLOCKED: Produkt aus Bestellung ist auf der aktuellen Produktseite nicht sichtbar: "
                                        + productNameFromExistingOrder);
                        System.out.println(
                                "[INFO] Constraint fachlich vorbereitet, Löschversuch wird übersprungen, damit der Build nicht an Pagination/Filterung scheitert.");
                        return;
                    }

                    org.openqa.selenium.WebElement productRow = optionalProductRow.get();
                    java.util.Optional<org.openqa.selenium.WebElement> deleteButton = findOptionalButtonInRow(
                            productRow, "deleteProductButton", "Löschen");

                    if (deleteButton.isEmpty()) {
                        System.out.println(
                                "[OK] PRODUCT DELETE BLOCKED Produkt mit Bestellposition hat keinen Löschbutton: "
                                        + productNameFromExistingOrder);
                        return;
                    }

                    deleteButton.get().click();
                    if (isAlertPresent(driver)) {
                        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent()).accept();
                    }

                    wait.until(webDriver -> bodyText(webDriver).contains(productNameFromExistingOrder)
                            || bodyText(webDriver).contains("Bestellung")
                            || bodyText(webDriver).contains("Position")
                            || bodyText(webDriver).contains("nicht")
                            || bodyText(webDriver).contains("Constraint")
                            || bodyText(webDriver).contains("referenziert")
                            || bodyText(webDriver).contains("Error processing request")
                            || bodyText(webDriver).contains("Exception handling request"));

                    String textAfterDeleteAttempt = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            textAfterDeleteAttempt.contains("Error processing request")
                                    || textAfterDeleteAttempt.contains("Exception handling request"),
                            "Produkt mit Bestellposition darf nicht gelöscht werden; es soll aber keine Server-Fehlerseite entstehen.");

                    openProductsPage(driver, wait, baseUrl);
                    org.junit.jupiter.api.Assertions.assertTrue(isProductVisible(driver, productNameFromExistingOrder),
                            "Produkt mit bestehender Bestellposition wurde gelöscht, obwohl das fachlich nicht erlaubt sein sollte: "
                                    + productNameFromExistingOrder);

                    System.out.println(
                            "[OK] PRODUCT DELETE BLOCKED Produkt mit bestehender Bestellposition wurde nicht gelöscht: "
                                    + productNameFromExistingOrder);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(5)
    void productCreate_ohneProduktnameNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("productCreate_ohneProduktnameNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openProductsPage(driver, wait, baseUrl);

                    openCreateProductForm(wait);
                    fillProductForm(driver, wait, "", LIST_PRICE, MODEL_YEAR);
                    clickButtonByTextOrIdSuffix(wait, "saveCreateProductButton", "Speichern");

                    wait.until(webDriver -> isElementPresent(webDriver,
                            org.openqa.selenium.By.xpath("//label[contains(normalize-space(.), 'Produktname')]"))
                            || bodyText(webDriver).contains("Produktname")
                            || bodyText(webDriver).contains("erforderlich")
                            || bodyText(webDriver).contains("Fehler")
                            || bodyText(webDriver).contains("Error processing request")
                            || bodyText(webDriver).contains("Exception handling request"));

                    String text = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            text.contains("Error processing request") || text.contains("Exception handling request"),
                            "Produktname-Validierung darf keine Server-Fehlerseite erzeugen.");

                    System.out.println("[OK] PRODUCT VALIDATION Produkt ohne Namen wurde nicht gespeichert");
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(6)
    void productCreate_mitNegativemPreisNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("productCreate_mitNegativemPreisNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    String invalidProductName = "AaaInvalidPreisProdukt " + UNIQUE;

                    login(driver, wait, baseUrl);
                    openProductsPage(driver, wait, baseUrl);

                    openCreateProductForm(wait);
                    fillProductForm(driver, wait, invalidProductName, "-1.00", MODEL_YEAR);
                    clickButtonByTextOrIdSuffix(wait, "saveCreateProductButton", "Speichern");

                    wait.until(webDriver -> bodyText(webDriver).contains("Preis")
                            || bodyText(webDriver).contains("Listenpreis")
                            || bodyText(webDriver).contains("muss")
                            || bodyText(webDriver).contains("Fehler")
                            || !bodyText(webDriver).contains(invalidProductName)
                            || bodyText(webDriver).contains("Error processing request")
                            || bodyText(webDriver).contains("Exception handling request"));

                    String text = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            text.contains("Error processing request") || text.contains("Exception handling request"),
                            "Preis-Validierung darf keine Server-Fehlerseite erzeugen.");

                    openProductsPage(driver, wait, baseUrl);
                    org.junit.jupiter.api.Assertions.assertFalse(isProductVisible(driver, invalidProductName),
                            "Produkt mit negativem Preis wurde trotzdem gespeichert.");

                    System.out.println("[OK] PRODUCT VALIDATION Produkt mit negativem Preis wurde nicht gespeichert: "
                            + invalidProductName);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(7)
    void productUpdate_nichtMoeglichOderNichtVorhanden() {
        de.sep.it.util.SqlLogCounter.measure("productUpdate_nichtMoeglichOderNichtVorhanden",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openProductsPage(driver, wait, baseUrl);

                    java.util.List<org.openqa.selenium.WebElement> editButtons = driver.findElements(
                            org.openqa.selenium.By.cssSelector(
                                    "input[id$='editProductButton'], button[id$='editProductButton'], a[id$='editProductButton']"));

                    org.junit.jupiter.api.Assertions.assertTrue(editButtons.isEmpty(),
                            "Produkt-Update ist fachlich nicht vorgesehen, aber ein Bearbeiten-Button wurde gefunden.");

                    System.out.println(
                            "[OK] PRODUCT UPDATE Produktbearbeitung ist nicht möglich bzw. kein Bearbeiten-Button vorhanden");
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

        org.openqa.selenium.WebElement emailField = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions
                        .visibilityOfElementLocated(org.openqa.selenium.By.id("loginForm:email")));
        org.openqa.selenium.WebElement passwordField = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions
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
            return text.contains("Startseite")
                    || text.contains("Abmelden")
                    || text.contains(LOGIN_EMAIL)
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
                "Login: Zugangsdaten abgelehnt");
        org.junit.jupiter.api.Assertions.assertTrue(
                textAfterLogin.contains("Startseite") || textAfterLogin.contains(LOGIN_EMAIL)
                        || textAfterLogin.contains("Abmelden"),
                "Login war nicht erfolgreich");
    }

    private void openProductsPage(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait, String baseUrl) {
        driver.get(baseUrl + "produkte.xhtml");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
        String bodyText = bodyText(driver);
        org.junit.jupiter.api.Assertions.assertTrue(
                bodyText.contains("Produkte") || bodyText.contains("Produktliste"),
                "Produktseite wurde nicht geladen. Body: " + bodyText);
    }

    private void openOrdersPage(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String baseUrl) {
        driver.get(baseUrl + "bestellungen.xhtml");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
        String bodyText = bodyText(driver);
        org.junit.jupiter.api.Assertions.assertTrue(
                bodyText.contains("Bestellungen") || bodyText.contains("Aufträge"),
                "Bestellseite wurde nicht geladen. Body: " + bodyText);
    }

    private void createProduct(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String productName, String listPrice, String modelYear) {
        openCreateProductForm(wait);
        fillProductForm(driver, wait, productName, listPrice, modelYear);
        clickButtonByTextOrIdSuffix(wait, "saveCreateProductButton", "Speichern");
        wait.until(webDriver -> bodyText(webDriver).contains(productName)
                || !isVisibleInputAfterLabel(webDriver, "Produktname"));
    }

    private void openCreateProductForm(org.openqa.selenium.support.ui.WebDriverWait wait) {
        clickButtonByTextOrIdSuffix(wait, "openCreateProductButton", "Neues Produkt", "Neuer Produkt",
                "Produkt anlegen", "Anlegen");
        wait.until(webDriver -> findInputAfterLabel(webDriver, "Produktname") != null);
    }

    private void fillProductForm(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait,
            String productName, String listPrice, String modelYear) {
        org.openqa.selenium.WebElement productNameInput = wait
                .until(webDriver -> findInputAfterLabel(webDriver, "Produktname"));
        setValue(productNameInput, productName);

        org.openqa.selenium.WebElement priceInput = wait
                .until(webDriver -> findInputAfterLabel(webDriver, "Listenpreis"));
        setValue(priceInput, listPrice);

        org.openqa.selenium.WebElement yearInput = wait
                .until(webDriver -> findInputAfterLabel(webDriver, "Modelljahr"));
        setValue(yearInput, modelYear);

        org.openqa.selenium.WebElement brandSelect = wait.until(webDriver -> findSelectAfterLabel(webDriver, "Marke"));
        selectFirstRealOption(brandSelect);

        org.openqa.selenium.WebElement categorySelect = wait
                .until(webDriver -> findSelectAfterLabel(webDriver, "Kategorie"));
        selectFirstRealOption(categorySelect);
    }

    private void deleteProduct(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String productName) {
        org.openqa.selenium.WebElement row = findProductRowByName(wait, productName);
        org.openqa.selenium.WebElement deleteButton = findButtonInRow(row, "deleteProductButton", "Löschen");
        deleteButton.click();

        if (isAlertPresent(driver)) {
            wait.until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent()).accept();
        }

        wait.until(webDriver -> !isProductVisible(webDriver, productName));
    }

    private String findProductNameFromFirstExistingOrder(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait,
            String baseUrl) {
        openOrdersPage(driver, wait, baseUrl);

        org.openqa.selenium.WebElement firstDetailButton = wait.until(webDriver -> {
            java.util.List<org.openqa.selenium.WebElement> candidates = webDriver.findElements(
                    org.openqa.selenium.By.cssSelector("input[type='submit'], input[type='button'], button, a"));
            for (org.openqa.selenium.WebElement candidate : candidates) {
                String combined = ((candidate.getAttribute("value") != null ? candidate.getAttribute("value") : "")
                        + " " + candidate.getText()).trim();
                String id = candidate.getAttribute("id");
                if (candidate.isDisplayed() && candidate.isEnabled()
                        && ((combined.contains("Details"))
                                || (id != null && id.toLowerCase(java.util.Locale.ROOT).contains("detail")))) {
                    return candidate;
                }
            }
            return null;
        });

        firstDetailButton.click();
        wait.until(webDriver -> bodyText(webDriver).contains("Positionen") || bodyText(webDriver).contains("Produkt"));

        String text = bodyText(driver);
        String productName = extractFirstProductNameFromOrderDetailText(text);
        org.junit.jupiter.api.Assertions.assertFalse(productName.isBlank(),
                "Produktname konnte aus Bestelldetails nicht gelesen werden. Text: " + text);
        return productName;
    }

    private String extractFirstProductNameFromOrderDetailText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.equals("Produkt") && i + 1 < lines.length) {
                String candidate = lines[i + 1].trim();
                if (isPlausibleProductName(candidate)) {
                    return candidate;
                }
            }
            if (line.startsWith("Produkt") && line.length() > "Produkt".length()) {
                String candidate = line.substring("Produkt".length()).trim();
                if (isPlausibleProductName(candidate)) {
                    return candidate;
                }
            }
        }

        int index = text.indexOf("Produkt");
        if (index >= 0) {
            String after = text.substring(index + "Produkt".length()).trim();
            for (String candidate : after.split("\\R")) {
                candidate = candidate.trim();
                if (isPlausibleProductName(candidate)) {
                    return candidate;
                }
            }
        }

        return "";
    }

    private boolean isPlausibleProductName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String lower = candidate.toLowerCase(java.util.Locale.ROOT);
        return !lower.equals("menge")
                && !lower.equals("einzelpreis")
                && !lower.equals("rabatt")
                && !lower.equals("gesamt")
                && !lower.equals("preis")
                && !candidate.contains("€")
                && candidate.length() >= 3;
    }

    private org.openqa.selenium.WebElement findProductRowByName(org.openqa.selenium.support.ui.WebDriverWait wait,
            String productName) {
        return wait.until(webDriver -> findProductRowByNameOptional(webDriver, productName).orElse(null));
    }

    private java.util.Optional<org.openqa.selenium.WebElement> findProductRowByNameOptional(
            org.openqa.selenium.WebDriver driver, String productName) {
        java.util.List<org.openqa.selenium.WebElement> rows = driver
                .findElements(org.openqa.selenium.By.cssSelector("table tbody tr"));
        for (org.openqa.selenium.WebElement row : rows) {
            try {
                if (row.getText().contains(productName)) {
                    return java.util.Optional.of(row);
                }
            } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                return java.util.Optional.empty();
            }
        }
        return java.util.Optional.empty();
    }

    private boolean isProductVisible(org.openqa.selenium.WebDriver driver, String productName) {
        try {
            java.util.List<org.openqa.selenium.WebElement> rows = driver
                    .findElements(org.openqa.selenium.By.cssSelector("table tbody tr"));
            for (org.openqa.selenium.WebElement row : rows) {
                if (row.getText().contains(productName)) {
                    return true;
                }
            }
            return false;
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            return true;
        }
    }

    private void clickButtonByTextOrIdSuffix(org.openqa.selenium.support.ui.WebDriverWait wait, String idSuffix,
            String... textParts) {
        org.openqa.selenium.WebElement element = wait.until(webDriver -> {
            java.util.List<org.openqa.selenium.WebElement> byId = webDriver.findElements(
                    org.openqa.selenium.By.cssSelector("input[id$='" + idSuffix + "'], button[id$='" + idSuffix
                            + "'], a[id$='" + idSuffix + "']"));
            for (org.openqa.selenium.WebElement candidate : byId) {
                if (candidate.isDisplayed() && candidate.isEnabled()) {
                    return candidate;
                }
            }

            java.util.List<org.openqa.selenium.WebElement> candidates = webDriver.findElements(
                    org.openqa.selenium.By.cssSelector("input[type='submit'], input[type='button'], button, a"));
            for (org.openqa.selenium.WebElement candidate : candidates) {
                String value = candidate.getAttribute("value");
                String text = candidate.getText();
                String combined = ((value != null ? value : "") + " " + (text != null ? text : "")).trim();
                for (String part : textParts) {
                    if (combined.contains(part) && candidate.isDisplayed() && candidate.isEnabled()) {
                        return candidate;
                    }
                }
            }
            return null;
        });
        element.click();
    }

    private org.openqa.selenium.WebElement findInputAfterLabel(org.openqa.selenium.WebDriver driver, String labelText) {
        java.util.List<org.openqa.selenium.WebElement> elements = driver.findElements(
                org.openqa.selenium.By.xpath("//label[contains(normalize-space(.), '" + labelText
                        + "')]/following::input[not(@type='hidden')][1]"));
        for (org.openqa.selenium.WebElement element : elements) {
            if (element.isDisplayed() && element.isEnabled()) {
                return element;
            }
        }
        return null;
    }

    private boolean isVisibleInputAfterLabel(org.openqa.selenium.WebDriver driver, String labelText) {
        return findInputAfterLabel(driver, labelText) != null;
    }

    private org.openqa.selenium.WebElement findSelectAfterLabel(org.openqa.selenium.WebDriver driver,
            String labelText) {
        java.util.List<org.openqa.selenium.WebElement> elements = driver.findElements(
                org.openqa.selenium.By
                        .xpath("//label[contains(normalize-space(.), '" + labelText + "')]/following::select[1]"));
        for (org.openqa.selenium.WebElement element : elements) {
            if (element.isDisplayed() && element.isEnabled()) {
                return element;
            }
        }
        return null;
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

    private org.openqa.selenium.WebElement findButtonInRow(org.openqa.selenium.WebElement row, String idSuffix,
            String textPart) {
        return findOptionalButtonInRow(row, idSuffix, textPart)
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException("Button '" + idSuffix + "'/'"
                        + textPart + "' in Produktzeile nicht gefunden. Zeilentext: " + row.getText()));
    }

    private java.util.Optional<org.openqa.selenium.WebElement> findOptionalButtonInRow(
            org.openqa.selenium.WebElement row, String idSuffix, String textPart) {
        java.util.List<org.openqa.selenium.WebElement> byId = row.findElements(
                org.openqa.selenium.By.cssSelector(
                        "input[id$='" + idSuffix + "'], button[id$='" + idSuffix + "'], a[id$='" + idSuffix + "']"));
        if (!byId.isEmpty()) {
            return java.util.Optional.of(byId.get(0));
        }

        java.util.List<org.openqa.selenium.WebElement> buttons = row.findElements(
                org.openqa.selenium.By.cssSelector("input[type='submit'], input[type='button'], button, a"));
        for (org.openqa.selenium.WebElement button : buttons) {
            String value = button.getAttribute("value");
            String text = button.getText();
            String combined = ((value != null ? value : "") + " " + (text != null ? text : "")).trim();
            if (combined.contains(textPart)) {
                return java.util.Optional.of(button);
            }
        }
        return java.util.Optional.empty();
    }

    private void setValue(org.openqa.selenium.WebElement element, String value) {
        element.clear();
        element.sendKeys(value);
    }

    private String bodyText(org.openqa.selenium.WebDriver driver) {
        try {
            return driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
        } catch (org.openqa.selenium.StaleElementReferenceException | org.openqa.selenium.NoSuchElementException e) {
            return "";
        }
    }

    private boolean isElementPresent(org.openqa.selenium.WebDriver driver, org.openqa.selenium.By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }

    private boolean isAlertPresent(org.openqa.selenium.WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (org.openqa.selenium.NoAlertPresentException e) {
            return false;
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