package de.sep.it.selenium;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class CustomerSeleniumIT {

    private static final String DEFAULT_BASE_URL = "http://localhost:8180/team-15-it/";
    private static final String LOGIN_EMAIL = "fabiola.jackson@bikes.shop";
    private static final String LOGIN_PASSWORD = "555-5554";

    private static final String UNIQUE = String.valueOf(System.currentTimeMillis());
    private static final String FIRST_NAME = "Selenium";
    private static final String LAST_NAME = "AaaCrudkunde";
    private static final String EMAIL = "selenium.customer." + UNIQUE + "@example.test";
    private static final String PHONE = "(111) 222-3333";
    private static final String STREET = "Teststrasse 1";
    private static final String CITY = "Teststadt";
    private static final String UPDATED_CITY = "Updatehausen";
    private static final String STATE = "TS";
    private static final String ZIP_CODE = "12345";

    // Statische Browser-Auswahl. Einmal pro Testklasse gesetzt
    private static volatile String ACTIVE_BROWSER_NAME = null;
    private static final java.util.concurrent.atomic.AtomicBoolean BANNER_PRINTED = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    // Tests

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void customerCreate_neuenKundenAnlegen() {
        de.sep.it.util.SqlLogCounter.measure("customerCreate_neuenKundenAnlegen",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    createCustomer(driver, wait, FIRST_NAME, LAST_NAME, EMAIL, PHONE, STREET, CITY, STATE, ZIP_CODE);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    String rowText = findCustomerRowByEmail(wait, EMAIL).getText();
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(FIRST_NAME), "Create: Vorname fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(LAST_NAME), "Create: Nachname fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(EMAIL), "Create: E-Mail fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(CITY), "Create: Stadt fehlt");
                    System.out.println("[OK] CREATE Kunde angelegt: " + EMAIL);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void customerRead_angelegtenKundenInTabelleFinden() {
        de.sep.it.util.SqlLogCounter.measure("customerRead_angelegtenKundenInTabelleFinden",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    String rowText = findCustomerRowByEmail(wait, EMAIL).getText();
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(FIRST_NAME), "Read: Vorname fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(LAST_NAME), "Read: Nachname fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(EMAIL), "Read: E-Mail fehlt");
                    org.junit.jupiter.api.Assertions.assertTrue(rowText.contains(PHONE), "Read: Telefon fehlt");
                    System.out.println("[OK] READ Kunde gefunden: " + EMAIL);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void customerUpdate_stadtDesKundenAendern() {
        de.sep.it.util.SqlLogCounter.measure("customerUpdate_stadtDesKundenAendern",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    editCustomerCity(driver, wait, EMAIL, UPDATED_CITY);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    org.junit.jupiter.api.Assertions.assertTrue(
                            findCustomerRowByEmail(wait, EMAIL).getText().contains(UPDATED_CITY),
                            "Update: Stadt fehlt");
                    System.out.println("[OK] UPDATE Kunde aktualisiert: " + EMAIL + " -> Stadt=" + UPDATED_CITY);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(4)
    void customerDelete_angelegtenKundenLoeschen() {
        de.sep.it.util.SqlLogCounter.measure("customerDelete_angelegtenKundenLoeschen",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    deleteCustomer(driver, wait, EMAIL);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    org.junit.jupiter.api.Assertions.assertFalse(isCustomerVisibleByEmail(driver, EMAIL),
                            "Delete: Kunde ist noch sichtbar");
                    System.out.println("[OK] DELETE Kunde gelöscht: " + EMAIL);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(5)
    void customerDelete_kundeMitBestehenderBestellungNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("customerDelete_kundeMitBestehenderBestellungNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    login(driver, wait, baseUrl);

                    String customerNameFromExistingOrder = findCustomerNameFromFirstExistingOrder(driver, wait,
                            baseUrl);
                    org.junit.jupiter.api.Assertions.assertFalse(customerNameFromExistingOrder.isBlank(),
                            "Kein Kunde aus bestehender Bestellung gefunden");
                    System.out.println(
                            "[INFO] DELETE BLOCKED nutzt bestehenden Bestellkunden: " + customerNameFromExistingOrder);

                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    org.openqa.selenium.WebElement customerRow = findCustomerRowByFullName(wait,
                            customerNameFromExistingOrder);
                    java.util.Optional<org.openqa.selenium.WebElement> deleteButton = findOptionalButtonInRow(
                            customerRow, "deleteCustomerButton");

                    if (deleteButton.isEmpty()) {
                        System.out
                                .println("[OK] DELETE BLOCKED Kunde mit bestehender Bestellung hat keinen Löschbutton: "
                                        + customerNameFromExistingOrder);
                        return;
                    }

                    deleteButton.get().click();
                    if (isAlertPresent(driver)) {
                        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent()).accept();
                    }

                    wait.until(webDriver -> bodyText(webDriver).contains(customerNameFromExistingOrder)
                            || bodyText(webDriver).contains("Bestellung")
                            || bodyText(webDriver).contains("nicht")
                            || bodyText(webDriver).contains("Constraint")
                            || bodyText(webDriver).contains("referenziert")
                            || bodyText(webDriver).contains("Error processing request")
                            || bodyText(webDriver).contains("Exception handling request"));

                    String textAfterDeleteAttempt = bodyText(driver);
                    org.junit.jupiter.api.Assertions.assertFalse(
                            textAfterDeleteAttempt.contains("Error processing request")
                                    || textAfterDeleteAttempt.contains("Exception handling request"),
                            "Kunde mit Bestellung darf nicht gelöscht werden; es soll aber keine Server-Fehlerseite entstehen.");

                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    org.junit.jupiter.api.Assertions.assertTrue(
                            isCustomerVisibleByFullName(driver, customerNameFromExistingOrder),
                            "Kunde mit bestehender Bestellung wurde gelöscht, obwohl das fachlich nicht erlaubt sein sollte: "
                                    + customerNameFromExistingOrder);
                    System.out.println("[OK] DELETE BLOCKED Kunde mit bestehender Bestellung wurde nicht gelöscht: "
                            + customerNameFromExistingOrder);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(6)
    void customerCreate_ohnePflichtfeldNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("customerCreate_ohnePflichtfeldNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    String invalidEmail = "selenium.customer.required." + UNIQUE + "@example.test";
                    login(driver, wait, baseUrl);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    openCreateCustomerForm(wait);
                    type(driver, "createCustomerForm:firstName", "");
                    type(driver, "createCustomerForm:lastName", "Pflichtfeldtest");
                    type(driver, "createCustomerForm:email", invalidEmail);
                    type(driver, "createCustomerForm:phone", PHONE);
                    type(driver, "createCustomerForm:street", STREET);
                    type(driver, "createCustomerForm:city", CITY);
                    type(driver, "createCustomerForm:state", STATE);
                    type(driver, "createCustomerForm:zipCode", ZIP_CODE);
                    clickById(wait, "createCustomerForm:saveCreateCustomerButton");
                    wait.until(webDriver -> isElementPresent(webDriver,
                            org.openqa.selenium.By.id("createCustomerForm:firstName"))
                            || !bodyText(webDriver).contains(invalidEmail));
                    org.junit.jupiter.api.Assertions.assertFalse(
                            bodyText(driver).contains("Error processing request")
                                    || bodyText(driver).contains("Exception handling request"),
                            "Pflichtfeld-Validierung darf keine Server-Fehlerseite erzeugen.");
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    org.junit.jupiter.api.Assertions.assertFalse(isCustomerVisibleByEmail(driver, invalidEmail),
                            "Kunde ohne Pflichtfeld wurde trotzdem gespeichert.");
                    System.out
                            .println("[OK] VALIDATION Kunde ohne Pflichtfeld wurde nicht gespeichert: " + invalidEmail);
                }));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(7)
    void customerCreate_ungueltigeEmailNichtErlaubt() {
        de.sep.it.util.SqlLogCounter.measure("customerCreate_ungueltigeEmailNichtErlaubt",
                () -> withBrowser((driver, wait, baseUrl) -> {
                    String invalidEmail = "ungueltige-email-" + UNIQUE;
                    login(driver, wait, baseUrl);
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    openCreateCustomerForm(wait);
                    type(driver, "createCustomerForm:firstName", "Invalid");
                    type(driver, "createCustomerForm:lastName", "Emailtest");
                    type(driver, "createCustomerForm:email", invalidEmail);
                    type(driver, "createCustomerForm:phone", PHONE);
                    type(driver, "createCustomerForm:street", STREET);
                    type(driver, "createCustomerForm:city", CITY);
                    type(driver, "createCustomerForm:state", STATE);
                    type(driver, "createCustomerForm:zipCode", ZIP_CODE);
                    clickById(wait, "createCustomerForm:saveCreateCustomerButton");
                    wait.until(webDriver -> isElementPresent(webDriver,
                            org.openqa.selenium.By.id("createCustomerForm:email"))
                            || !bodyText(webDriver).contains(invalidEmail));
                    org.junit.jupiter.api.Assertions.assertFalse(
                            bodyText(driver).contains("Error processing request")
                                    || bodyText(driver).contains("Exception handling request"),
                            "E-Mail-Validierung darf keine Server-Fehlerseite erzeugen.");
                    openCustomersPage(driver, wait, baseUrl);
                    sortByLastNameAscending(driver, wait);
                    org.junit.jupiter.api.Assertions.assertFalse(isCustomerVisibleByEmail(driver, invalidEmail),
                            "Kunde mit ungültiger E-Mail wurde trotzdem gespeichert.");
                    System.out.println(
                            "[OK] VALIDATION Kunde mit ungültiger E-Mail wurde nicht gespeichert: " + invalidEmail);
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

    /**
     * Gibt einen fertig gestarteten WebDriver zurück. Beim ersten Aufruf wird die
     * Fallback-Kette Firefox --> Chromium --> Chrome --> Edge durchprobiert und das
     * Ergebnis einmalig in ACTIVE_BROWSER_NAME gespeichert. Alle Folgeaufrufe
     * innerhalb derselben Testklasse verwenden direkt den gefundenen Browser.
     */
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
        System.out.println("[BROWSER] Reihenfolge: Firefox --> Chromium --> Chrome --> Edge --> Selenium Manager");
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
        wait.until(webDriver -> bodyText(webDriver).contains("Startseite") || bodyText(webDriver).contains("Abmelden")
                || bodyText(webDriver).contains(LOGIN_EMAIL)
                || bodyText(webDriver).contains("Error processing request")
                || bodyText(webDriver).contains("E-Mail oder Passwort falsch"));
        String text = bodyText(driver);
        org.junit.jupiter.api.Assertions.assertFalse(text.contains("Error processing request"), "Login: Serverfehler");
        org.junit.jupiter.api.Assertions.assertFalse(text.contains("E-Mail oder Passwort falsch"),
                "Login fehlgeschlagen");
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("Startseite") || text.contains(LOGIN_EMAIL),
                "Login war nicht erfolgreich");
    }

    private String findCustomerNameFromFirstExistingOrder(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait, String baseUrl) {
        openOrdersPage(driver, wait, baseUrl);
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
        String customerName = cells.get(1).getText().trim();
        org.junit.jupiter.api.Assertions.assertFalse(customerName.isBlank(), "Kundenname aus Bestellung ist leer");
        return customerName;
    }

    private void openCustomersPage(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait, String baseUrl) {
        driver.get(baseUrl + "kunden.xhtml");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
        org.junit.jupiter.api.Assertions.assertTrue(
                bodyText(driver).contains("Kunden") || bodyText(driver).contains("Kundenliste"),
                "Kundenseite wurde nicht geladen.");
    }

    private void openOrdersPage(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String baseUrl) {
        driver.get(baseUrl + "bestellungen.xhtml");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.tagName("body")));
        org.junit.jupiter.api.Assertions.assertTrue(
                bodyText(driver).contains("Bestellungen") || bodyText(driver).contains("Aufträge"),
                "Bestellseite wurde nicht geladen.");
    }

    private void sortByLastNameAscending(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait) {
        org.openqa.selenium.WebElement sortField = wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("sortCustomerForm:sortField")));
        org.openqa.selenium.WebElement sortDirection = wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("sortCustomerForm:sortDirection")));
        new org.openqa.selenium.support.ui.Select(sortField).selectByValue("LAST_NAME");
        new org.openqa.selenium.support.ui.Select(sortDirection).selectByValue("ASC");
        clickById(wait, "sortCustomerForm:applySortButton");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("customerTableForm:customerTable")));
    }

    private void openCreateCustomerForm(org.openqa.selenium.support.ui.WebDriverWait wait) {
        clickById(wait, "openCreateCustomerForm:openCreateCustomerButton");
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("createCustomerForm:firstName")));
    }

    private void createCustomer(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String firstName, String lastName,
            String email, String phone, String street, String city, String state, String zipCode) {
        openCreateCustomerForm(wait);
        type(driver, "createCustomerForm:firstName", firstName);
        type(driver, "createCustomerForm:lastName", lastName);
        type(driver, "createCustomerForm:email", email);
        type(driver, "createCustomerForm:phone", phone);
        type(driver, "createCustomerForm:street", street);
        type(driver, "createCustomerForm:city", city);
        type(driver, "createCustomerForm:state", state);
        type(driver, "createCustomerForm:zipCode", zipCode);
        clickById(wait, "createCustomerForm:saveCreateCustomerButton");
        wait.until(webDriver -> bodyText(webDriver).contains(email)
                || !isElementPresent(webDriver, org.openqa.selenium.By.id("createCustomerForm:firstName")));
    }

    private void editCustomerCity(org.openqa.selenium.WebDriver driver,
            org.openqa.selenium.support.ui.WebDriverWait wait, String email, String updatedCity) {
        org.openqa.selenium.WebElement row = findCustomerRowByEmail(wait, email);
        findButtonInRow(row, "editCustomerButton").click();
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                .visibilityOfElementLocated(org.openqa.selenium.By.id("editCustomerForm:city")));
        type(driver, "editCustomerForm:city", updatedCity);
        clickById(wait, "editCustomerForm:saveEditCustomerButton");
        wait.until(webDriver -> bodyText(webDriver).contains(updatedCity)
                || !isElementPresent(webDriver, org.openqa.selenium.By.id("editCustomerForm:city")));
    }

    private void deleteCustomer(org.openqa.selenium.WebDriver driver, org.openqa.selenium.support.ui.WebDriverWait wait,
            String email) {
        org.openqa.selenium.WebElement row = findCustomerRowByEmail(wait, email);
        findButtonInRow(row, "deleteCustomerButton").click();
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent()).accept();
        wait.until(webDriver -> !isCustomerVisibleByEmail(webDriver, email));
    }

    private org.openqa.selenium.WebElement findCustomerRowByEmail(org.openqa.selenium.support.ui.WebDriverWait wait,
            String email) {
        return wait.until(webDriver -> {
            for (org.openqa.selenium.WebElement row : webDriver
                    .findElements(org.openqa.selenium.By.cssSelector("table[id$='customerTable'] tbody tr"))) {
                try {
                    if (row.getText().contains(email))
                        return row;
                } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                    return null;
                }
            }
            return null;
        });
    }

    private org.openqa.selenium.WebElement findCustomerRowByFullName(org.openqa.selenium.support.ui.WebDriverWait wait,
            String fullName) {
        return wait.until(webDriver -> {
            for (org.openqa.selenium.WebElement row : webDriver
                    .findElements(org.openqa.selenium.By.cssSelector("table[id$='customerTable'] tbody tr"))) {
                try {
                    if (row.getText().contains(fullName))
                        return row;
                } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                    return null;
                }
            }
            return null;
        });
    }

    private org.openqa.selenium.WebElement findButtonInRow(org.openqa.selenium.WebElement row, String idSuffix) {
        return findOptionalButtonInRow(row, idSuffix).orElseThrow(
                () -> new org.openqa.selenium.NoSuchElementException("Button nicht gefunden: " + idSuffix));
    }

    private java.util.Optional<org.openqa.selenium.WebElement> findOptionalButtonInRow(
            org.openqa.selenium.WebElement row, String idSuffix) {
        java.util.List<org.openqa.selenium.WebElement> buttons = row.findElements(
                org.openqa.selenium.By.cssSelector("input[id$='" + idSuffix + "'], button[id$='" + idSuffix + "']"));
        return buttons.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(buttons.get(0));
    }

    private void clickById(org.openqa.selenium.support.ui.WebDriverWait wait, String id) {
        wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(org.openqa.selenium.By.id(id)))
                .click();
    }

    private void type(org.openqa.selenium.WebDriver driver, String id, String value) {
        org.openqa.selenium.WebElement element = driver.findElement(org.openqa.selenium.By.id(id));
        element.clear();
        element.sendKeys(value);
    }

    private boolean isCustomerVisibleByEmail(org.openqa.selenium.WebDriver driver, String email) {
        for (org.openqa.selenium.WebElement row : driver
                .findElements(org.openqa.selenium.By.cssSelector("table[id$='customerTable'] tbody tr"))) {
            try {
                if (row.getText().contains(email))
                    return row.isDisplayed();
            } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                return true;
            }
        }
        return false;
    }

    private boolean isCustomerVisibleByFullName(org.openqa.selenium.WebDriver driver, String fullName) {
        for (org.openqa.selenium.WebElement row : driver
                .findElements(org.openqa.selenium.By.cssSelector("table[id$='customerTable'] tbody tr"))) {
            try {
                if (row.getText().contains(fullName))
                    return row.isDisplayed();
            } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                return true;
            }
        }
        return false;
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