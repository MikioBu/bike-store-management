package de.sep.business.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "brands")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brands_seq")
    @SequenceGenerator(name = "brands_seq", sequenceName = "brands_brand_id_seq", allocationSize = 1)
    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "brand_name", length = 255, nullable = false)
    @NotBlank(message = "Markenname ist erforderlich")
    @Size(max = 255, message = "Markenname darf maximal 255 Zeichen haben")
    private String brandName;

    public Brand() {
    }

    public Brand(String brandName) {
        brandName(brandName);
    }

    
    public Integer brandId() {
        return brandId;
    }

    public String brandName() {
        return brandName;
    }

    public void brandName(String v) {
        this.brandName = v != null ? v.strip() : null;
    }

    // JavaBean
    public Integer getBrandId() {
        return brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String v) {
        this.brandName = v != null ? v.strip() : null;
    }
}
