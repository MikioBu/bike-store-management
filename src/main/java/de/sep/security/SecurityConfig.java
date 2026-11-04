package de.sep.security;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;
import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

/**
 * Zentrale Security-Konfiguration.
 *
 * callerQuery:
 * Passwort nur für AKTIVE Mitarbeiter (active = 1).
 * Inaktive bekommen kein Passwort zurück, dadurch schlägt der Login fehl.
 *
 * groupsQuery:
 * Rollenvergabe per manager_id:
 *
 * manager_id IS NULL -> ADMIN
 * manager_id = 1     -> USER1
 * sonst              -> USER2
 *
 * Wichtig:
 * Die Rollen müssen auch als Application-Rollen deklariert werden,
 * damit WildFly/Elytron sie als Rollen der Webanwendung kennt.
 */
@DeclareRoles({
        "ADMIN",
        "USER1",
        "USER2"
})
@DatabaseIdentityStoreDefinition(
        dataSourceLookup = "java:jboss/datasources/bikes15",

        callerQuery =
                "SELECT phone " +
                        "FROM staffs " +
                        "WHERE email = ? " +
                        "AND active = 1",

        groupsQuery =
                "SELECT CASE " +
                        "WHEN manager_id IS NULL THEN 'ADMIN' " +
                        "WHEN manager_id = 1 THEN 'USER1' " +
                        "ELSE 'USER2' " +
                        "END AS group_name " +
                        "FROM staffs " +
                        "WHERE email = ? " +
                        "AND active = 1",

        hashAlgorithm = PlaintextPasswordHash.class
)
@CustomFormAuthenticationMechanismDefinition(
        loginToContinue = @LoginToContinue(
                loginPage = "/login.xhtml",
                errorPage = "/login.xhtml?error=true"
        )
)
@ApplicationScoped
public class SecurityConfig {
}