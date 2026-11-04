package de.sep.business.product.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_seq")
    @SequenceGenerator(name = "products_seq", sequenceName = "products_product_id_seq", allocationSize = 1, initialValue = 1000)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_name", length = 255, nullable = false)
    @NotBlank(message = "Produktname ist erforderlich")
    @Size(max = 255, message = "Produktname darf maximal 255 Zeichen haben")
    private String productName;

    @Column(name = "list_price", nullable = false)
    @NotNull(message = "Listenpreis ist erforderlich")
    @DecimalMin(value = "0.0", inclusive = false, message = "Listenpreis muss größer als 0 sein")
    @Digits(integer = 17, fraction = 2, message = "Ungültiges Preisformat")
    private BigDecimal listPrice;

    @Column(name = "model_year", nullable = false)
    @NotNull(message = "Modelljahr ist erforderlich")
    @Min(value = 1900, message = "Modelljahr ist ungültig")
    @Max(value = 2100, message = "Modelljahr ist ungültig")
    private Integer modelYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    @NotNull(message = "Marke ist erforderlich")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Kategorie ist erforderlich")
    private Category category;

    // Konstruktoren

    public Product() {
    }

    public Product(String productName, BigDecimal listPrice, Integer modelYear,
            Brand brand, Category category) {
        productName(productName);
        listPrice(listPrice);
        modelYear(modelYear);
        brand(brand);
        category(category);
    }

    public Integer productId() {
        return productId;
    }

    public String productName() {
        return productName;
    }

    public void productName(String v) {
        this.productName = v != null ? v.strip() : null;
    }

    public BigDecimal listPrice() {
        return listPrice;
    }

    public void listPrice(BigDecimal v) {
        this.listPrice = v;
    }

    public Integer modelYear() {
        return modelYear;
    }

    public void modelYear(Integer v) {
        this.modelYear = v;
    }

    public Brand brand() {
        return brand;
    }

    public void brand(Brand v) {
        this.brand = v;
    }

    public Category category() {
        return category;
    }

    public void category(Category v) {
        this.category = v;
    }

    // Getter/Setter

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String v) {
        this.productName = v != null ? v.strip() : null;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public void setListPrice(BigDecimal v) {
        this.listPrice = v;
    }

    public Integer getModelYear() {
        return modelYear;
    }

    public void setModelYear(Integer v) {
        this.modelYear = v;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand v) {
        this.brand = v;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category v) {
        this.category = v;
    }
}