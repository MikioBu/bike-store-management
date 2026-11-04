package de.sep.it.util;

public final class SqlLogCounter {

    private static final java.nio.charset.Charset LOG_CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private static final Object LOCK = new Object();

    private SqlLogCounter() {
    }

    public static int measure(String testName, SqlAction action) {
        synchronized (LOCK) {
            java.nio.file.Path logFile = logFile();
            java.nio.file.Path countsFile = countsFile();
            CallerInfo callerInfo = detectCallerInfo(testName);

            long startPosition = currentSize(logFile);

            try {
                action.run();
            } catch (org.opentest4j.TestAbortedException e) {
                throw e;
            } catch (org.junit.platform.commons.PreconditionViolationException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Fehler während SQL-Messung für Test '" + testName + "'", e);
            } finally {
                sleepSilently(300);
            }

            String newLogContent = readNewLogContent(logFile, startPosition);
            int sqlStatements = countSqlStatements(newLogContent);

            System.out.println("[SQL] " + callerInfo.testClass + "." + callerInfo.testMethod + ": " + sqlStatements
                    + " SQL-Statements");
            appendCsv(countsFile, callerInfo, sqlStatements);

            return sqlStatements;
        }
    }

    public static int measureAndAssertMax(String testName, int maxSqlStatements, SqlAction action) {
        int sqlStatements = measure(testName, action);
        org.junit.jupiter.api.Assertions.assertTrue(
                sqlStatements <= maxSqlStatements,
                "Test '" + testName + "' hat " + sqlStatements
                        + " SQL-Statements erzeugt. Erlaubt sind maximal " + maxSqlStatements + ".");
        return sqlStatements;
    }

    private static CallerInfo detectCallerInfo(String fallbackTestName) {
        String fallbackClass = "";
        String fallbackMethod = fallbackTestName == null || fallbackTestName.isBlank() ? "unbekannterTest"
                : fallbackTestName;

        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            String methodName = element.getMethodName();

            if (className.equals(SqlLogCounter.class.getName())) {
                continue;
            }
            if (className.startsWith("java.") || className.startsWith("jdk.")) {
                continue;
            }
            if (className.startsWith("org.junit.") || className.startsWith("org.apache.maven.")) {
                continue;
            }
            if (className.contains("$$Lambda") || methodName.startsWith("lambda$")) {
                continue;
            }

            if (className.endsWith("IT")) {
                return new CallerInfo(className, methodName, fallbackMethod);
            }

            if (fallbackClass.isBlank()) {
                fallbackClass = className;
            }
        }

        return new CallerInfo(fallbackClass, fallbackMethod, fallbackMethod);
    }

    private static java.nio.file.Path logFile() {
        String configured = System.getProperty("sql.log.file", "target/server/standalone/log/server.log");
        return java.nio.file.Path.of(configured);
    }

    private static java.nio.file.Path countsFile() {
        String configured = System.getProperty("sql.counts.file", "target/sql-statement-counts.csv");
        return java.nio.file.Path.of(configured);
    }

    private static long currentSize(java.nio.file.Path file) {
        try {
            if (!java.nio.file.Files.exists(file)) {
                return 0L;
            }
            return java.nio.file.Files.size(file);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Konnte Größe der Logdatei nicht lesen: " + file, e);
        }
    }

    private static String readNewLogContent(java.nio.file.Path file, long startPosition) {
        try {
            if (!java.nio.file.Files.exists(file)) {
                return "";
            }

            long fileSize = java.nio.file.Files.size(file);
            long safeStartPosition = Math.min(startPosition, fileSize);
            long bytesLong = fileSize - safeStartPosition;

            if (bytesLong <= 0) {
                return "";
            }
            if (bytesLong > Integer.MAX_VALUE) {
                throw new IllegalStateException("Logabschnitt ist zu groß zum Einlesen: " + bytesLong + " Bytes");
            }

            try (java.nio.channels.SeekableByteChannel channel = java.nio.file.Files.newByteChannel(file,
                    java.nio.file.StandardOpenOption.READ)) {
                channel.position(safeStartPosition);
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate((int) bytesLong);
                while (buffer.hasRemaining() && channel.read(buffer) != -1) {
                    // read until buffer is full or EOF
                }
                buffer.flip();
                return LOG_CHARSET.decode(buffer).toString();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Konnte neuen Logabschnitt nicht lesen: " + file, e);
        }
    }

    private static int countSqlStatements(String logContent) {
        if (logContent == null || logContent.isBlank()) {
            return 0;
        }

        int count = 0;
        String[] lines = logContent.split("\\R");
        for (String line : lines) {
            if (isSqlLogLine(line)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSqlLogLine(String line) {
        if (line == null) {
            return false;
        }
        return line.contains("org.hibernate.SQL") || line.trim().startsWith("Hibernate:");
    }

    private static void appendCsv(java.nio.file.Path file, CallerInfo callerInfo, int sqlStatements) {
        try {
            java.nio.file.Path parent = file.getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }

            boolean writeHeader = !java.nio.file.Files.exists(file) || java.nio.file.Files.size(file) == 0;
            java.util.List<String> lines = new java.util.ArrayList<>();
            if (writeHeader) {
                lines.add("testClass,testMethod,measurementName,sqlStatements");
            }
            lines.add(csv(callerInfo.testClass) + "," + csv(callerInfo.testMethod) + ","
                    + csv(callerInfo.measurementName) + "," + sqlStatements);

            java.nio.file.Files.write(
                    file,
                    lines,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Konnte SQL-Zählergebnis nicht schreiben: " + file, e);
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record CallerInfo(String testClass, String testMethod, String measurementName) {
    }

    @FunctionalInterface
    public interface SqlAction {
        void run() throws Exception;
    }
}