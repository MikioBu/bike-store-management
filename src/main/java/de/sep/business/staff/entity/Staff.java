package de.sep.business.staff.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "staffs")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "staffs_seq")
    @SequenceGenerator(name = "staffs_seq", sequenceName = "staffs_staff_id_seq", allocationSize = 1)
    @Column(name = "staff_id")
    private Integer staffId;

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

    @Column(name = "phone", length = 255)
    @Size(max = 255, message = "Telefonnummer darf maximal 255 Zeichen haben")
    @Pattern(regexp = "^[+\\d(][\\d\\s()\\-]{2,}$", message = "Ungültiges Telefonnummernformat")
    private String phone;

    @Column(name = "active", nullable = false)
    @NotNull(message = "Aktivstatus ist erforderlich")
    @Min(value = 0, message = "Aktivstatus ungültig")
    @Max(value = 1, message = "Aktivstatus ungültig")
    private Integer active;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Staff manager;

    @Column(name = "store_id", nullable = false)
    @NotNull(message = "Niederlassung ist erforderlich")
    private Integer storeId;

    // Konstruktoren

    public Staff() {
    }

    public Staff(String firstName, String lastName, String email, String phone,
            Integer active, Staff manager, Integer storeId) {
        firstName(firstName);
        lastName(lastName);
        email(email);
        phone(phone);
        active(active);
        manager(manager);
        storeId(storeId);
    }

    public Integer staffId() {
        return staffId;
    }

    public String firstName() {
        return firstName;
    }

    public void firstName(String v) {
        this.firstName = sanitize(v);
    }

    public String lastName() {
        return lastName;
    }

    public void lastName(String v) {
        this.lastName = sanitize(v);
    }

    public String email() {
        return email;
    }

    public void email(String v) {
        this.email = sanitize(v);
    }

    public String phone() {
        return phone;
    }

    public void phone(String v) {
        this.phone = sanitize(v);
    }

    public Integer active() {
        return active;
    }

    public void active(Integer v) {
        this.active = v;
    }

    public Staff manager() {
        return manager;
    }

    public void manager(Staff v) {
        this.manager = v;
    }

    public Integer storeId() {
        return storeId;
    }

    public void storeId(Integer v) {
        this.storeId = v;
    }

    // Getter/Setter

    public Integer getStaffId() {
        return staffId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String v) {
        this.firstName = sanitize(v);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String v) {
        this.lastName = sanitize(v);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = sanitize(v);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        this.phone = sanitize(v);
    }

    public Integer getActive() {
        return active;
    }

    public void setActive(Integer v) {
        this.active = v;
    }

    public Staff getManager() {
        return manager;
    }

    public void setManager(Staff v) {
        this.manager = v;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer v) {
        this.storeId = v;
    }

    private String sanitize(String input) {
        return input != null ? input.strip() : null;
    }
}
