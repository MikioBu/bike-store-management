package de.sep.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.identitystore.PasswordHash;
import java.util.Map;

/**
 * Passwort-Strategie: Telefonnummer ohne Vorwahl.
 * 
 *  z.B.:
 * "(555) 234-5684" --> Passwort: "234-5684"
 * "+49 89 12345" --> Passwort: "12345"
 * "5551234" --> kein Leerzeichen --> ganzer String ist Passwort
 */
@ApplicationScoped
public class PlaintextPasswordHash implements PasswordHash {

    @Override
    public void initialize(Map<String, String> parameters) {
        // keine Konfiguration nötig
    }

    @Override
    public String generate(char[] password) {
        // Wird beim Speichern gerufen - wir speichern nichts, DB bleibt unverändert
        return new String(password);
    }

    @Override
    public boolean verify(char[] password, String hashedPassword) {
        if (password == null || hashedPassword == null)
            return false;

        String eingabe = new String(password).strip();
        String ohneVorwahl = ohneVorwahl(hashedPassword.strip());

        return eingabe.equals(ohneVorwahl);
    }

    /**
     * Entfernt die Vorwahl: alles bis einschließlich dem letzten Leerzeichen.
     * Gibt den ganzen String zurück wenn kein Leerzeichen vorhanden.
     */
    private String ohneVorwahl(String phone) {
        int lastSpace = phone.lastIndexOf(' ');
        if (lastSpace < 0)
            return phone;
        return phone.substring(lastSpace + 1);
    }
}
