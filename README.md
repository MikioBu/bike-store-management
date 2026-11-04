# Bike Store Management

A web app for managing a chain of bike stores. Staff can log in and manage customers, products, orders, stock and stores. Access depends on the user's role.

Built by a team of 5 students for the Software Engineering Project course at Ostfalia University of Applied Sciences (summer 2026).

## Features

- Customers, products, orders, stock, staff and stores with create, edit, delete, sorting and paging
- Orders with multiple order items
- Login with three roles (admin and two user levels)
- Mobile friendly layout
- Automated UI tests with Selenium and HtmlUnit

## Tech stack

- Java 25, Jakarta EE (JSF, JPA, CDI, Security)
- WildFly
- PostgreSQL, H2 for tests
- Maven

## Performance

We measured the SQL statements each UI test triggers and optimized the queries (lazy loading, JOIN FETCH, lighter dropdown data). The total dropped from 637 to 212 statements, and the order list went from up to 70 statements per page to 3. Details are in `docs/testergebnisse-4.txt` (German).

## Running the tests

```
mvn clean verify
```

This builds the app, starts a WildFly test server with an H2 database, runs all tests and prints a summary at the end. No local database is needed.

## Running locally

1. Create a PostgreSQL database called `bikes` and load the sample data from `src/test/resources-war/import.sql`
2. Set your database user and password in `src/main/webapp/WEB-INF/second-iteration-ds.xml`
3. Build with `mvn package` and deploy the WAR to WildFly

Demo login from the sample data: `fabiola.jackson@bikes.shop` with password `555-5554`
