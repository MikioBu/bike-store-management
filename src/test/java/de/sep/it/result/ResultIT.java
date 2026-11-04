package de.sep.it.result;

public class ResultIT {

    public static void main(String[] args) throws Exception {
        java.nio.file.Path failsafeReports = java.nio.file.Path.of("target", "failsafe-reports");
        java.nio.file.Path surefireReports = java.nio.file.Path.of("target", "surefire-reports");
        java.nio.file.Path sqlCountsFile = java.nio.file.Path.of("target", "sql-statement-counts.csv");

        java.util.Map<String, SqlCountResult> sqlCounts = readSqlCounts(sqlCountsFile);
        java.util.List<TestClassResult> results = new java.util.ArrayList<>();
        java.util.List<java.nio.file.Path> reportFiles = new java.util.ArrayList<>();
        reportFiles.addAll(findXmlReports(surefireReports));
        reportFiles.addAll(findXmlReports(failsafeReports));

        for (java.nio.file.Path reportFile : reportFiles) {
            TestClassResult result = readReport(reportFile);
            if (result != null && !result.className.endsWith("ResultIT") && !result.className.endsWith("AusgabeIT")) {
                results.add(result);
            }
        }

        results.sort(java.util.Comparator.comparing(result -> shortClassName(result.className)));

        int totalTests = 0;
        int totalProblems = 0;
        int totalSkipped = 0;
        int totalSqlStatements = 0;
        int sqlMeasuredTests = 0;
        double totalTime = 0.0;

        GroupStats seleniumStats = new GroupStats();
        GroupStats htmlUnitStats = new GroupStats();
        GroupStats otherStats = new GroupStats();

        System.out.println();
        System.out.println("============================================================");
        System.out.println("                  TESTERGEBNISSE - TEAM 15                 ");
        System.out.println("============================================================");

        for (TestClassResult result : results) {
            totalTests += result.tests;
            totalProblems += result.problems;
            totalSkipped += result.skipped;
            totalTime += result.time;

            GroupStats groupStats = statsForClass(result.className, seleniumStats, htmlUnitStats, otherStats);
            groupStats.testClasses++;
            groupStats.tests += result.tests;
            groupStats.problems += result.problems;
            groupStats.skipped += result.skipped;
            groupStats.time += result.time;

            System.out.println();
            System.out.println("Testklasse: " + shortClassName(result.className));
            System.out.println("Status:     " + (result.isSuccessful() ? "OK" : "FEHLER"));
            System.out.println("Tests:      " + result.tests);
            System.out.println("Fehler:     " + result.problems);
            System.out.println("Skipped:    " + result.skipped);
            System.out.printf(java.util.Locale.US, "Dauer:      %.3f s%n", result.time);
            System.out.println("Testfälle:");

            for (TestCaseResult testCase : result.testCases) {
                String normalizedTestName = normalizeTestName(testCase.name);
                String key = sqlKey(result.className, normalizedTestName);
                SqlCountResult sqlCount = sqlCounts.get(key);
                String displayName = displayTestCaseName(result.className, normalizedTestName);

                if (sqlCount != null) {
                    sqlMeasuredTests++;
                    totalSqlStatements += sqlCount.sqlStatements;
                    groupStats.sqlMeasuredTests++;
                    groupStats.sqlStatements += sqlCount.sqlStatements;
                    System.out.println("  - " + testCase.status + " " + displayName + " | SQL-Statements: "
                            + sqlCount.sqlStatements);
                } else {
                    groupStats.testsWithoutSqlCount++;
                    System.out.println(
                            "  - " + testCase.status + " " + displayName + " | SQL-Statements: nicht gemessen");
                }

                String description = describeTestCase(result.className, normalizedTestName);
                if (!description.isBlank()) {
                    System.out.println("    Zweck: " + description);
                }
            }
        }

        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println("SQL-STATEMENT-ÜBERSICHT PRO TEST");
        System.out.println("------------------------------------------------------------");
        if (sqlCounts.isEmpty()) {
            System.out.println("Keine SQL-Zählungen gefunden. Erwartete Datei: " + sqlCountsFile.toAbsolutePath());
        } else {
            sqlCounts.values().stream()
                    .sorted(java.util.Comparator
                            .comparing((SqlCountResult value) -> shortClassName(value.testClass))
                            .thenComparing(value -> value.testMethod))
                    .forEach(value -> System.out.println(shortClassName(value.testClass) + "." + value.testMethod
                            + ": " + value.sqlStatements + " SQL-Statements"));
        }

        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println("SQL-STATEMENT-AUFTEILUNG NACH TESTART");
        System.out.println("------------------------------------------------------------");
        printGroupedSqlCounts("Selenium-/Browser-Tests", sqlCounts, TestType.SELENIUM);
        printGroupedSqlCounts("HtmlUnit-Tests", sqlCounts, TestType.HTMLUNIT);
        printGroupedSqlCounts("Sonstige Tests", sqlCounts, TestType.OTHER);

        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println("GESAMTÜBERSICHT");
        System.out.println("------------------------------------------------------------");
        System.out.println("Gesamte Testklassen:       " + results.size());
        System.out.println("Gesamte Tests:             " + totalTests);
        System.out.println("Gesamte Fehler:            " + totalProblems);
        System.out.println("Gesamte Skipped:           " + totalSkipped);
        System.out.println("Tests mit SQL-Zählung:     " + sqlMeasuredTests);
        System.out.println("SQL-Statements insgesamt:  " + totalSqlStatements);
        System.out.printf(java.util.Locale.US, "Gesamtdauer:               %.3f s%n", totalTime);
        System.out.println("Gesamtstatus:              "
                + (totalProblems == 0 ? "BUILD/TESTS ERFOLGREICH" : "TESTFEHLER VORHANDEN"));

        System.out.println();
        System.out.println("------------------------------------------------------------");
        System.out.println("AUFTEILUNG NACH TESTART");
        System.out.println("------------------------------------------------------------");
        printGroupStats("Selenium-/Browser-Tests", seleniumStats);
        printGroupStats("HtmlUnit-Tests", htmlUnitStats);
        printGroupStats("Sonstige Tests", otherStats);

        System.out.println("============================================================");
        System.out.println();

        if (totalProblems != 0) {
            throw new IllegalStateException(
                    "Mindestens ein Test hatte Fehler. Details stehen oben in der Testübersicht.");
        }
    }

    private enum TestType {
        SELENIUM,
        HTMLUNIT,
        OTHER
    }

    private static GroupStats statsForClass(String className, GroupStats seleniumStats, GroupStats htmlUnitStats,
            GroupStats otherStats) {
        TestType type = testTypeForClass(className);
        return switch (type) {
            case HTMLUNIT -> htmlUnitStats;
            case SELENIUM -> seleniumStats;
            case OTHER -> otherStats;
        };
    }

    private static TestType testTypeForClass(String className) {
        if (isHtmlUnitTestClass(className)) {
            return TestType.HTMLUNIT;
        }
        if (isSeleniumOrBrowserTestClass(className)) {
            return TestType.SELENIUM;
        }
        return TestType.OTHER;
    }

    private static boolean isHtmlUnitTestClass(String className) {
        return shortClassName(className).contains("HtmlUnit");
    }

    private static boolean isSeleniumOrBrowserTestClass(String className) {
        String shortName = shortClassName(className);
        return shortName.contains("Selenium")
                || (shortName.endsWith("CrudIT") && !shortName.contains("HtmlUnit"));
    }

    private static void printGroupedSqlCounts(String title, java.util.Map<String, SqlCountResult> sqlCounts,
            TestType type) {
        java.util.List<SqlCountResult> values = sqlCounts.values().stream()
                .filter(value -> testTypeForClass(value.testClass) == type)
                .sorted(java.util.Comparator
                        .comparing((SqlCountResult value) -> shortClassName(value.testClass))
                        .thenComparing(value -> value.testMethod))
                .toList();

        int total = values.stream().mapToInt(value -> value.sqlStatements).sum();

        System.out.println();
        System.out.println(title + ":");
        if (values.isEmpty()) {
            System.out.println("  Keine SQL-Zählungen in dieser Gruppe.");
            System.out.println("  Summe: 0 SQL-Statements");
            return;
        }

        for (SqlCountResult value : values) {
            System.out.println("  " + shortClassName(value.testClass) + "." + value.testMethod
                    + ": " + value.sqlStatements + " SQL-Statements");
        }
        System.out.println("  Summe: " + total + " SQL-Statements");
    }

    private static void printGroupStats(String title, GroupStats stats) {
        System.out.println();
        System.out.println(title + ":");
        System.out.println("Testklassen:               " + stats.testClasses);
        System.out.println("Tests:                     " + stats.tests);
        System.out.println("Fehler:                    " + stats.problems);
        System.out.println("Skipped:                   " + stats.skipped);
        System.out.println("Tests mit SQL-Zählung:     " + stats.sqlMeasuredTests);
        System.out.println("Tests ohne SQL-Zählung:    " + stats.testsWithoutSqlCount);
        System.out.println("SQL-Statements insgesamt:  " + stats.sqlStatements);
        System.out.printf(java.util.Locale.US, "Dauer:                     %.3f s%n", stats.time);
    }

    private static java.util.Map<String, SqlCountResult> readSqlCounts(java.nio.file.Path file)
            throws java.io.IOException {
        java.util.Map<String, SqlCountResult> counts = new java.util.LinkedHashMap<>();
        if (!java.nio.file.Files.exists(file)) {
            return counts;
        }

        java.util.List<String> lines = java.nio.file.Files.readAllLines(file, java.nio.charset.StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (line.startsWith("testClass,")) {
                continue;
            }
            if (line.startsWith("test,")) {
                continue;
            }

            java.util.List<String> columns = parseCsvLine(line);
            try {
                if (columns.size() >= 4) {
                    String testClass = columns.get(0);
                    String testMethod = normalizeTestName(columns.get(1));
                    String measurementName = columns.get(2);
                    int sqlStatements = Integer.parseInt(columns.get(3));
                    counts.put(sqlKey(testClass, testMethod),
                            new SqlCountResult(testClass, testMethod, measurementName, sqlStatements));
                } else if (columns.size() >= 2) {
                    String testMethod = normalizeTestName(columns.get(0));
                    int sqlStatements = Integer.parseInt(columns.get(1));
                    counts.put(sqlKey("", testMethod), new SqlCountResult("", testMethod, testMethod, sqlStatements));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return counts;
    }

    private static java.util.List<String> parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }

    private static java.util.List<java.nio.file.Path> findXmlReports(java.nio.file.Path directory)
            throws java.io.IOException {
        java.util.List<java.nio.file.Path> files = new java.util.ArrayList<>();
        if (!java.nio.file.Files.isDirectory(directory)) {
            return files;
        }
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(directory)) {
            stream.filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("TEST-"))
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .forEach(files::add);
        }
        return files;
    }

    private static TestClassResult readReport(java.nio.file.Path file) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document document = builder.parse(file.toFile());
        org.w3c.dom.Element root = document.getDocumentElement();
        if (root == null || !"testsuite".equals(root.getTagName())) {
            return null;
        }

        String className = root.getAttribute("name");
        int tests = parseInt(root.getAttribute("tests"));
        int problems = parseInt(root.getAttribute("failures")) + parseInt(root.getAttribute("errors"));
        int skipped = parseInt(root.getAttribute("skipped"));
        double time = parseDouble(root.getAttribute("time"));

        java.util.List<TestCaseResult> testCases = new java.util.ArrayList<>();
        org.w3c.dom.NodeList testcaseNodes = root.getElementsByTagName("testcase");
        for (int i = 0; i < testcaseNodes.getLength(); i++) {
            if (testcaseNodes.item(i) instanceof org.w3c.dom.Element testcase) {
                testCases.add(new TestCaseResult(testcase.getAttribute("name"), determineTestCaseStatus(testcase)));
            }
        }
        return new TestClassResult(className, tests, problems, skipped, time, testCases);
    }

    private static String determineTestCaseStatus(org.w3c.dom.Element testcase) {
        if (testcase.getElementsByTagName("failure").getLength() > 0
                || testcase.getElementsByTagName("error").getLength() > 0) {
            return "FEHLER";
        }
        if (testcase.getElementsByTagName("skipped").getLength() > 0) {
            return "SKIPPED";
        }
        return "OK";
    }

    private static String displayTestCaseName(String className, String testName) {
        if (className.endsWith("SeleniumIT")) {
            return switch (testName) {
                case "loginFunktioniertMitChrome" -> "Login mit Chrome";
                case "loginFunktioniertMitFirefox" -> "Login mit Firefox";
                case "loginFunktioniertMitEdge" -> "Login mit Microsoft Edge";
                case "loginFunktioniertMitChromiumFedora" -> "Login mit Chromium (Fedora/Linux)";
                default -> testName;
            };
        }
        return testName;
    }

    private static String describeTestCase(String className, String testName) {
        if (className.endsWith("CustomerCrudIT") || className.endsWith("CustomerCrudHtmlUnitIT")) {
            return switch (testName) {
                case "customerCreate_neuenKundenAnlegen" ->
                    "Kunden-CREATE inklusive Anzeigeprüfung und eigener SQL-Statement-Zählung.";
                case "customerRead_angelegtenKundenInTabelleFinden" ->
                    "Kunden-READ inklusive Tabellenprüfung und eigener SQL-Statement-Zählung.";
                case "customerUpdate_stadtDesKundenAendern" ->
                    "Kunden-UPDATE inklusive Persistenzprüfung und eigener SQL-Statement-Zählung.";
                case "customerDelete_angelegtenKundenLoeschen" ->
                    "Kunden-DELETE für einen neu angelegten Kunden ohne Bestellung inklusive eigener SQL-Statement-Zählung.";
                case "customerDelete_kundeMitBestehenderBestellungNichtErlaubt" ->
                    "Constraint-Test: Kunde aus bestehender Bestellung darf nicht gelöscht werden; eigene SQL-Statement-Zählung.";
                case "customerCreate_ohnePflichtfeldNichtErlaubt" ->
                    "Constraint-Test: Kunde ohne Pflichtfeld darf nicht gespeichert werden; eigene SQL-Statement-Zählung.";
                case "customerCreate_ungueltigeEmailNichtErlaubt" ->
                    "Constraint-Test: Kunde mit ungültiger E-Mail darf nicht gespeichert werden; eigene SQL-Statement-Zählung.";
                default -> "Kunden-Testfall inklusive SQL-Statement-Zählung.";
            };
        }
        if (className.endsWith("ProductCrudIT") || className.endsWith("ProductHtmlUnitIT")) {
            return switch (testName) {
                case "productCreate_neuesProduktAnlegen" -> "Produkt-CREATE inklusive SQL-Statement-Zählung.";
                case "productRead_angelegtesProduktInTabelleFinden" -> "Produkt-READ inklusive SQL-Statement-Zählung.";
                case "productDelete_angelegtesProduktLoeschen" ->
                    "Produkt-DELETE inklusive SQL-Statement-Zählung. Produkt-UPDATE wird fachlich bewusst nicht getestet.";
                default -> "Produkt-Testfall.";
            };
        }
        if (className.endsWith("H2ImportSqlTest")) {
            return "Prüft Testdatenimport und H2-Kompatibilität.";
        }
        if (className.endsWith("HtmlUnitLoginIT")) {
            return "Prüft Login mit HtmlUnit.";
        }
        return "";
    }

    private static String sqlKey(String testClass, String testMethod) {
        String normalizedClass = testClass == null ? "" : testClass;
        String normalizedMethod = normalizeTestName(testMethod);
        return normalizedClass + "#" + normalizedMethod;
    }

    private static String normalizeTestName(String testName) {
        if (testName == null) {
            return "";
        }
        int parenthesis = testName.indexOf('(');
        if (parenthesis > 0) {
            return testName.substring(0, parenthesis);
        }
        return testName;
    }

    private static String shortClassName(String className) {
        int lastDot = className == null ? -1 : className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : String.valueOf(className);
    }

    private static int parseInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private static double parseDouble(String value) {
        return value == null || value.isBlank() ? 0.0 : Double.parseDouble(value);
    }

    private record SqlCountResult(String testClass, String testMethod, String measurementName, int sqlStatements) {
    }

    private static class GroupStats {
        int testClasses;
        int tests;
        int problems;
        int skipped;
        int sqlMeasuredTests;
        int testsWithoutSqlCount;
        int sqlStatements;
        double time;
    }

    private static class TestClassResult {
        final String className;
        final int tests;
        final int problems;
        final int skipped;
        final double time;
        final java.util.List<TestCaseResult> testCases;

        TestClassResult(String className, int tests, int problems, int skipped, double time,
                java.util.List<TestCaseResult> testCases) {
            this.className = className;
            this.tests = tests;
            this.problems = problems;
            this.skipped = skipped;
            this.time = time;
            this.testCases = testCases;
        }

        boolean isSuccessful() {
            return problems == 0;
        }
    }

    private static class TestCaseResult {
        final String name;
        final String status;

        TestCaseResult(String name, String status) {
            this.name = name;
            this.status = status;
        }
    }
}
