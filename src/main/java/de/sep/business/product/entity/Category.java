package de.sep.business.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq")
    @SequenceGenerator(name = "categories_seq", sequenceName = "categories_category_id_seq", allocationSize = 1)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", length = 255, nullable = false)
    @NotBlank(message = "Kategoriename ist erforderlich")
    @Size(max = 255, message = "Kategoriename darf maximal 255 Zeichen haben")
    private String categoryName;

    public Category() {
    }

    public Category(String categoryName) {
        categoryName(categoryName);
    }


    public Integer categoryId() {
        return categoryId;
    }

    public String categoryName() {
        return categoryName;
    }

    public void categoryName(String v) {
        this.categoryName = v != null ? v.strip() : null;
    }

    // JavaBean
    public Integer getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String v) {
        this.categoryName = v != null ? v.strip() : null;
    }
}
