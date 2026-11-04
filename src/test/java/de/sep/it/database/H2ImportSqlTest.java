package de.sep.it.database;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2ImportSqlTest {

    private static final Path IMPORT_SQL_PATH = Path.of("src", "test", "resources-war", "import.sql");

    @Test
    void importSqlKannDirektInH2GeladenWerden() throws Exception {
        assertTrue(
                Files.exists(IMPORT_SQL_PATH),
                "import.sql muss unter src/test/resources-war/import.sql liegen");

        String importSql = Files.readString(IMPORT_SQL_PATH, StandardCharsets.UTF_8);

        assertFalse(importSql.isBlank(), "import.sql darf nicht leer sein");
        assertFalse(importSql.contains("COPY public."), "import.sql darf keine PostgreSQL-COPY-Blöcke enthalten");
        assertFalse(importSql.contains("\\."), "import.sql darf keine PostgreSQL-COPY-Endmarker enthalten");

        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:importsqltest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "sa",
                "sa")) {

            createSchema(connection);
            executeSqlScript(connection, importSql);

            assertTableCount(connection, "brands", 9);
            assertTableCount(connection, "categories", 7);
            assertTableCount(connection, "stores", 3);
            assertTableCount(connection, "staffs", 10);
            assertTableCount(connection, "customers", 30);
            assertTableCount(connection, "products", 30);
            assertTableCount(connection, "stocks", 90);
            assertTableCount(connection, "orders", 30);
            assertTableCount(connection, "order_items", 93);

            assertFabiolaLoginDaten(connection);
            assertBestellungenSindMitPositionenVorhanden(connection);
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        executeUpdate(connection, """
                CREATE TABLE brands (
                    brand_id INTEGER NOT NULL,
                    brand_name VARCHAR(255) NOT NULL,
                    PRIMARY KEY (brand_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE categories (
                    category_id INTEGER NOT NULL,
                    category_name VARCHAR(255) NOT NULL,
                    PRIMARY KEY (category_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE stores (
                    store_id INTEGER NOT NULL,
                    city VARCHAR(255),
                    email VARCHAR(255),
                    phone VARCHAR(255),
                    state VARCHAR(255),
                    store_name VARCHAR(255) NOT NULL,
                    street VARCHAR(255),
                    zip_code VARCHAR(255),
                    PRIMARY KEY (store_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE staffs (
                    staff_id INTEGER NOT NULL,
                    active INTEGER NOT NULL CHECK (active >= 0 AND active <= 1),
                    email VARCHAR(255) NOT NULL,
                    first_name VARCHAR(255) NOT NULL,
                    last_name VARCHAR(255) NOT NULL,
                    phone VARCHAR(255),
                    manager_id INTEGER,
                    store_id INTEGER NOT NULL,
                    PRIMARY KEY (staff_id),
                    FOREIGN KEY (manager_id) REFERENCES staffs(staff_id),
                    FOREIGN KEY (store_id) REFERENCES stores(store_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE customers (
                    customer_id INTEGER NOT NULL,
                    city VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    first_name VARCHAR(255) NOT NULL,
                    last_name VARCHAR(255) NOT NULL,
                    phone VARCHAR(255) NOT NULL,
                    state VARCHAR(255) NOT NULL,
                    street VARCHAR(255) NOT NULL,
                    zip_code VARCHAR(255) NOT NULL,
                    PRIMARY KEY (customer_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE products (
                    product_id INTEGER NOT NULL,
                    list_price NUMERIC(19,2) NOT NULL,
                    model_year INTEGER NOT NULL CHECK (model_year >= 1900 AND model_year <= 2100),
                    product_name VARCHAR(255) NOT NULL,
                    brand_id INTEGER NOT NULL,
                    category_id INTEGER NOT NULL,
                    PRIMARY KEY (product_id),
                    FOREIGN KEY (brand_id) REFERENCES brands(brand_id),
                    FOREIGN KEY (category_id) REFERENCES categories(category_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE stocks (
                    product_id INTEGER NOT NULL,
                    store_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity >= 0),
                    PRIMARY KEY (product_id, store_id),
                    FOREIGN KEY (product_id) REFERENCES products(product_id),
                    FOREIGN KEY (store_id) REFERENCES stores(store_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE orders (
                    order_id INTEGER NOT NULL,
                    order_date DATE NOT NULL,
                    order_status INTEGER NOT NULL CHECK (order_status >= 1 AND order_status <= 4),
                    required_date DATE NOT NULL,
                    shipped_date DATE,
                    customer_id INTEGER NOT NULL,
                    staff_id INTEGER NOT NULL,
                    store_id INTEGER NOT NULL,
                    PRIMARY KEY (order_id),
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                    FOREIGN KEY (staff_id) REFERENCES staffs(staff_id),
                    FOREIGN KEY (store_id) REFERENCES stores(store_id)
                )
                """);

        executeUpdate(connection, """
                CREATE TABLE order_items (
                    item_id INTEGER NOT NULL,
                    order_id INTEGER NOT NULL,
                    discount NUMERIC(3,2) NOT NULL,
                    list_price NUMERIC(19,2) NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity >= 1),
                    product_id INTEGER NOT NULL,
                    PRIMARY KEY (item_id, order_id),
                    FOREIGN KEY (order_id) REFERENCES orders(order_id),
                    FOREIGN KEY (product_id) REFERENCES products(product_id)
                )
                """);
    }

    private void executeSqlScript(Connection connection, String sqlScript) throws SQLException {
        String[] statements = sqlScript.split(";");

        int executedStatements = 0;

        for (String rawStatement : statements) {
            String statement = rawStatement.trim();

            if (statement.isBlank() || statement.startsWith("--")) {
                continue;
            }

            try (Statement jdbcStatement = connection.createStatement()) {
                jdbcStatement.executeUpdate(statement);
                executedStatements++;
            }
        }

        assertTrue(executedStatements > 0, "Es wurden keine INSERT-Statements aus import.sql ausgeführt");
    }

    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void assertTableCount(Connection connection, String tableName, int expectedCount) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {

            assertTrue(resultSet.next(), "COUNT(*) für Tabelle " + tableName + " lieferte kein Ergebnis");
            assertEquals(expectedCount, resultSet.getInt(1), "Unerwartete Anzahl Datensätze in Tabelle " + tableName);
        }
    }

    private void assertFabiolaLoginDaten(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT staff_id, email, phone, active, manager_id, store_id
                        FROM staffs
                        WHERE email = 'fabiola.jackson@bikes.shop'
                        """)) {

            assertTrue(resultSet.next(), "Fabiola Jackson muss in staffs vorhanden sein");
            assertEquals(1, resultSet.getInt("staff_id"));
            assertEquals("fabiola.jackson@bikes.shop", resultSet.getString("email"));
            assertEquals("(831) 555-5554", resultSet.getString("phone"));
            assertEquals(1, resultSet.getInt("active"));
            assertEquals(1, resultSet.getInt("store_id"));
            assertEquals(null, resultSet.getObject("manager_id"));
        }

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT phone
                        FROM staffs
                        WHERE email = 'fabiola.jackson@bikes.shop'
                        AND active = 1
                        """)) {

            assertTrue(resultSet.next(), "Security callerQuery muss Fabiola finden");
            assertEquals("(831) 555-5554", resultSet.getString("phone"));
        }

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT CASE
                            WHEN manager_id IS NULL THEN 'ADMIN'
                            WHEN manager_id = 1 THEN 'USER1'
                            ELSE 'USER2'
                        END AS group_name
                        FROM staffs
                        WHERE email = 'fabiola.jackson@bikes.shop'
                        AND active = 1
                        """)) {

            assertTrue(resultSet.next(), "Security groupsQuery muss Fabiola finden");
            assertEquals("ADMIN", resultSet.getString("group_name"));
        }
    }

    private void assertBestellungenSindMitPositionenVorhanden(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM orders o
                        JOIN order_items oi ON oi.order_id = o.order_id
                        JOIN products p ON p.product_id = oi.product_id
                        JOIN customers c ON c.customer_id = o.customer_id
                        JOIN staffs s ON s.staff_id = o.staff_id
                        JOIN stores st ON st.store_id = o.store_id
                        """)) {

            assertTrue(resultSet.next(), "Join-Prüfung lieferte kein Ergebnis");
            assertEquals(93, resultSet.getInt(1), "Alle order_items müssen vollständig über FKs verknüpfbar sein");
        }
    }
}
