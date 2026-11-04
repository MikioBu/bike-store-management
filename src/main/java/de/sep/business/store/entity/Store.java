package de.sep.business.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stores_seq")
    @SequenceGenerator(name = "stores_seq", sequenceName = "stores_store_id_seq", allocationSize = 1)
    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "store_name", length = 255, nullable = false)
    @NotBlank(message = "Filialname ist erforderlich")
    @Size(max = 255, message = "Filialname darf maximal 255 Zeichen haben")
    private String storeName;

    @Column(name = "phone", length = 255)
    @Size(max = 255, message = "Telefonnummer darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[+\\d][\\d\\s()\\-]{3,}$", message = "Ungültiges Telefonnummernformat")
    private String phone;

    @Column(name = "email", length = 255)
    @Size(max = 255, message = "E-Mail darf maximal 255 Zeichen haben")
    @Email(message = "Ungültiges E-Mail-Format")
    private String email;

    @Column(name = "street", length = 255)
    @Size(max = 255, message = "Straße darf maximal 255 Zeichen haben")
    private String street;

    @Column(name = "city", length = 255)
    @Size(max = 255, message = "Stadt darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[\\p{L}\\s'\\-]+$", message = "Stadt enthält ungültige Zeichen")
    private String city;

    @Column(name = "state", length = 255)
    @Size(max = 255, message = "Bundesland/Land darf maximal 255 Zeichen haben")
    private String state;

    @Column(name = "zip_code", length = 255)
    @Size(max = 255, message = "Postleitzahl darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[A-Z0-9\\s\\-]{3,10}$", message = "Ungültiges Postleitzahlenformat")
    private String zipCode;

    // Konstruktoren

    public Store() {
    }

    public Store(String storeName, String phone, String email,
            String street, String city, String state, String zipCode) {
        storeName(storeName);
        phone(phone);
        email(email);
        street(street);
        city(city);
        state(state);
        zipCode(zipCode);
    }

    public Integer storeId() {
        return storeId;
    }

    public String storeName() {
        return storeName;
    }

    public void storeName(String v) {
        this.storeName = sanitize(v);
    }

    public String phone() {
        return phone;
    }

    public void phone(String v) {
        this.phone = sanitize(v);
    }

    public String email() {
        return email;
    }

    public void email(String v) {
        this.email = sanitize(v);
    }

    public String street() {
        return street;
    }

    public void street(String v) {
        this.street = sanitize(v);
    }

    public String city() {
        return city;
    }

    public void city(String v) {
        this.city = sanitize(v);
    }

    public String state() {
        return state;
    }

    public void state(String v) {
        this.state = sanitize(v);
    }

    public String zipCode() {
        return zipCode;
    }

    public void zipCode(String v) {
        this.zipCode = sanitize(v);
    }

    // Getter/Setter

    public Integer getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String v) {
        this.storeName = sanitize(v);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        this.phone = sanitize(v);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = sanitize(v);
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String v) {
        this.street = sanitize(v);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String v) {
        this.city = sanitize(v);
    }

    public String getState() {
        return state;
    }

    public void setState(String v) {
        this.state = sanitize(v);
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String v) {
        this.zipCode = sanitize(v);
    }

    private String sanitize(String input) {
        return input != null ? input.strip() : null;
    }
}
