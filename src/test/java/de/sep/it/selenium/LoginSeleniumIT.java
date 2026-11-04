package de.sep.it.selenium;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class LoginSeleniumIT {

    @org.junit.jupiter.api.BeforeAll
    static void printEnvironment() {
        System.out.println("======================================");
        System.out.println("SELENIUMIT - BROWSER-DATEIPRUEFUNG OHNE BROWSERSTART");
        System.out.println("Es wird nur geprueft, ob Browser-Binaries auffindbar sind.");
        System.out.println("Es wird kein Browser gestartet und kein Selenium-Treiber erzeugt.");
        System.out.println("======================================");
        System.out.println("Betriebssystem: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("Java-Version:   " + System.getProperty("java.version"));
        System.out.println("======================================");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(1)
    void browserInstallation_FirefoxPruefen() {
        checkBrowserFileExistsOnly("firefox");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(2)
    void browserInstallation_ChromiumPruefen() {
        checkBrowserFileExistsOnly("chromium");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(3)
    void browserInstallation_ChromePruefen() {
        checkBrowserFileExistsOnly("chrome");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.Order(4)
    void browserInstallation_EdgePruefen() {
        checkBrowserFileExistsOnly("edge");
    }

    private void checkBrowserFileExistsOnly(String browserName) {
        de.sep.it.util.SqlLogCounter.measure("browserInstallation_" + browserName, () -> {
            BrowserFileCheck browser = detectBrowserFile(browserName);
            System.out.println("======================================");
            System.out.println("BROWSER-DATEIPRUEFUNG OHNE START");
            System.out.println("Browser: " + browserName);
            System.out.println("Gepruefte Namen/Pfade: " + browser.checkedCandidates());
            if (browser.found()) {
                System.out.println("Status: GEFUNDEN");
                System.out.println("Pfad:   " + browser.executable());
                System.out.println("[OK] Browser-Datei ist vorhanden. Browser wurde NICHT gestartet: " + browserName);
            } else {
                System.out.println("Status: NICHT GEFUNDEN");
                System.out.println(
                        "[SKIPPED] Browser-Datei ist nicht installiert oder nicht im PATH auffindbar: " + browserName);
            }
            System.out.println("======================================");
            org.junit.jupiter.api.Assumptions.assumeTrue(browser.found(),
                    "Browser-Datei nicht gefunden: " + browserName + ". Gepruefte Namen/Pfade: "
                            + browser.checkedCandidates());
        });
    }

    private static BrowserFileCheck detectBrowserFile(String browserName) {
        String normalized = browserName == null ? "" : browserName.trim().toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> candidates = browserCandidates(normalized);
        java.util.List<String> checked = new java.util.ArrayList<>();
        for (String candidate : candidates) {
            checked.add(candidate);
            java.util.Optional<String> executable = resolveExecutableWithoutStartingBrowser(candidate);
            if (executable.isPresent()) {
                return new BrowserFileCheck(true, normalized, executable.get(), String.join(", ", checked));
            }
        }
        return new BrowserFileCheck(false, normalized, "", String.join(", ", checked));
    }

    private static java.util.List<String> browserCandidates(String browserName) {
        boolean windows = isWindows();
        boolean linux = isLinux();
        boolean mac = isMac();
        java.util.List<String> candidates = new java.util.ArrayList<>();
        switch (browserName) {
            case "firefox" -> {
                if (windows) {
                    candidates.add("firefox.exe");
                    addWindowsProgramFiles(candidates, "Mozilla Firefox\\firefox.exe");
                } else if (linux) {
                    candidates.add("firefox");
                    candidates.add("firefox-developer-edition");
                    candidates.add("librewolf");
                } else if (mac) {
                    candidates.add("/Applications/Firefox.app/Contents/MacOS/firefox");
                }
            }
            case "chromium" -> {
                if (windows) {
                    candidates.add("chromium.exe");
                } else if (linux) {
                    candidates.add("chromium");
                    candidates.add("chromium-browser");
                } else if (mac) {
                    candidates.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
                }
            }
            case "chrome" -> {
                if (windows) {
                    candidates.add("chrome.exe");
                    addWindowsProgramFiles(candidates, "Google\\Chrome\\Application\\chrome.exe");
                } else if (linux) {
                    candidates.add("google-chrome");
                    candidates.add("google-chrome-stable");
                    candidates.add("chrome");
                } else if (mac) {
                    candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
                }
            }
            case "edge" -> {
                if (windows) {
                    candidates.add("msedge.exe");
                    addWindowsProgramFiles(candidates, "Microsoft\\Edge\\Application\\msedge.exe");
                } else if (linux) {
                    candidates.add("microsoft-edge");
                    candidates.add("microsoft-edge-stable");
                    candidates.add("msedge");
                } else if (mac) {
                    candidates.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
                }
            }
            default -> throw new IllegalArgumentException("Unbekannter Browser: " + browserName);
        }
        return candidates;
    }

    private static void addWindowsProgramFiles(java.util.List<String> candidates, String suffix) {
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String localAppData = System.getenv("LOCALAPPDATA");
        if (programFiles != null && !programFiles.isBlank())
            candidates.add(programFiles + "\\" + suffix);
        if (programFilesX86 != null && !programFilesX86.isBlank())
            candidates.add(programFilesX86 + "\\" + suffix);
        if (localAppData != null && !localAppData.isBlank())
            candidates.add(localAppData + "\\" + suffix);
    }

    private static java.util.Optional<String> resolveExecutableWithoutStartingBrowser(String candidate) {
        if (candidate == null || candidate.isBlank())
            return java.util.Optional.empty();
        java.nio.file.Path directPath = java.nio.file.Path.of(candidate);
        if (directPath.isAbsolute() && java.nio.file.Files.isRegularFile(directPath))
            return java.util.Optional.of(directPath.toString());
        if (candidate.contains("\\") || candidate.contains("/")) {
            return java.nio.file.Files.isRegularFile(directPath) ? java.util.Optional.of(directPath.toString())
                    : java.util.Optional.empty();
        }
        String command = isWindows() ? "where" : "which";
        try {
            Process process = new ProcessBuilder(command, candidate).redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return java.util.Optional.empty();
            }
            String output = new String(process.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            if (process.exitValue() == 0 && !output.isBlank()) {
                return output.lines().map(String::trim).filter(line -> !line.isBlank()).findFirst();
            }
        } catch (java.io.IOException e) {
            return java.util.Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return java.util.Optional.empty();
        }
        return java.util.Optional.empty();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("linux");
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return os.contains("mac") || os.contains("darwin");
    }

    private record BrowserFileCheck(boolean found, String displayName, String executable, String checkedCandidates) {
    }
}