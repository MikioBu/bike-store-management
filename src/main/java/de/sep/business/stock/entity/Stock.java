package de.sep.business.stock.entity;

import de.sep.business.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "stocks")
public class Stock {

    @EmbeddedId
    private StockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Produkt ist erforderlich")
    private Product product;

    @Column(name = "store_id", nullable = false, insertable = false, updatable = false)
    private Integer storeId;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Bestand ist erforderlich")
    @Min(value = 0, message = "Bestand darf nicht negativ sein")
    private Integer quantity;

    // Konstruktoren

    public Stock() {
    }

    public Stock(StockId id, Product product, Integer storeId, Integer quantity) {
        this.id = id;
        this.product = product;
        this.storeId = storeId;
        this.quantity = quantity;
    }

    // getter setter

    public StockId id() {
        return id;
    }

    public void id(StockId v) {
        this.id = v;
    }

    public Product product() {
        return product;
    }

    public void product(Product v) {
        this.product = v;
    }

    public Integer storeId() {
        return storeId;
    }

    public void storeId(Integer v) {
        this.storeId = v;
    }

    public Integer quantity() {
        return quantity;
    }

    public void quantity(Integer v) {
        this.quantity = v;
    }

    // Getter/Setter

    public StockId getId() {
        return id;
    }

    public void setId(StockId v) {
        this.id = v;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product v) {
        this.product = v;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer v) {
        this.storeId = v;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer v) {
        this.quantity = v;
    }
}
