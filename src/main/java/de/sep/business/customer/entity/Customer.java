package de.sep.business.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customers_seq")
    @SequenceGenerator(name = "customers_seq", sequenceName = "customers_customer_id_seq", allocationSize = 1)
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "first_name", length = 255, nullable = false)
    @NotBlank(message = "Vorname ist erforderlich")
    @Size(max = 255, message = "Vorname darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[\\p{L}\\s'\\-]+$", message = "Vorname enthält ungültige Zeichen")
    private String firstName;

    @Column(name = "last_name", length = 255, nullable = false)
    @NotBlank(message = "Nachname ist erforderlich")
    @Size(max = 255, message = "Nachname darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[\\p{L}\\s'\\-]+$", message = "Nachname enthält ungültige Zeichen")
    private String lastName;

    @Column(name = "email", length = 255, nullable = false)
    @NotBlank(message = "E-Mail ist erforderlich")
    @Size(max = 255, message = "E-Mail darf maximal 255 Zeichen haben")
    @Email(message = "Ungültiges E-Mail-Format")
    private String email;

    @Column(name = "phone", length = 255, nullable = false)
    @NotBlank(message = "Telefonnummer ist erforderlich")
    @Size(max = 255, message = "Telefonnummer darf maximal 255 Zeichen haben")
    // Erlaubt Formate wie: (717) 746-6659 | +49 89 12345 | 5551234
    @Pattern(regexp = "^[+\\d(][\\d\\s()\\-]{3,}$", message = "Ungültiges Telefonnummernformat (Beispiel: (717) 746-6659)")
    private String phone;

    @Column(name = "street", length = 255, nullable = false)
    @NotBlank(message = "Straße ist erforderlich")
    @Size(max = 255, message = "Straße darf maximal 255 Zeichen haben")
    private String street;

    @Column(name = "city", length = 255, nullable = false)
    @NotBlank(message = "Stadt ist erforderlich")
    @Size(max = 255, message = "Stadt darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[\\p{L}\\s'\\-]+$", message = "Stadt enthält ungültige Zeichen")
    private String city;

    @Column(name = "state", length = 255, nullable = false)
    @NotBlank(message = "Bundesland/Land ist erforderlich")
    @Size(max = 255, message = "Bundesland/Land darf maximal 255 Zeichen haben")
    private String state;

    @Column(name = "zip_code", length = 255, nullable = false)
    @NotBlank(message = "Postleitzahl ist erforderlich")
    @Size(max = 255, message = "Postleitzahl darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[A-Z0-9\\s\\-]{3,10}$", message = "Ungültiges Postleitzahlenformat")
    private String zipCode;

    // Konstruktoren

    public Customer() {
    }

    public Customer(String firstName, String lastName, String email, String phone,
            String street, String city, String state, String zipCode) {
        firstName(firstName);
        lastName(lastName);
        email(email);
        phone(phone);
        street(street);
        city(city);
        state(state);
        zipCode(zipCode);
    }

    public Integer customerId() {
        return customerId;
    }

    public String firstName() {
        return firstName;
    }

    public void firstName(String value) {
        this.firstName = sanitize(value);
    }

    public String lastName() {
        return lastName;
    }

    public void lastName(String value) {
        this.lastName = sanitize(value);
    }

    public String email() {
        return email;
    }

    public void email(String value) {
        this.email = sanitize(value);
    }

    public String phone() {
        return phone;
    }

    public void phone(String value) {
        this.phone = sanitize(value);
    }

    public String street() {
        return street;
    }

    public void street(String value) {
        this.street = sanitize(value);
    }

    public String city() {
        return city;
    }

    public void city(String value) {
        this.city = sanitize(value);
    }

    public String state() {
        return state;
    }

    public void state(String value) {
        this.state = sanitize(value);
    }

    public String zipCode() {
        return zipCode;
    }

    public void zipCode(String value) {
        this.zipCode = sanitize(value);
    }

    // Getter/Setter

    public Integer getCustomerId() {
        return customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String value) {
        this.firstName = sanitize(value);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String value) {
        this.lastName = sanitize(value);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = sanitize(value);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String value) {
        this.phone = sanitize(value);
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String value) {
        this.street = sanitize(value);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String value) {
        this.city = sanitize(value);
    }

    public String getState() {
        return state;
    }

    public void setState(String value) {
        this.state = sanitize(value);
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String value) {
        this.zipCode = sanitize(value);
    }

    // Hilfsmethode

    private String sanitize(String input) {
        return input != null ? input.strip() : null;
    }
}
