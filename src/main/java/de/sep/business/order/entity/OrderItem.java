package de.sep.business.order.entity;

import java.math.BigDecimal;

import de.sep.business.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Produkt ist erforderlich")
    private Product product;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Menge ist erforderlich")
    @Min(value = 1, message = "Menge muss mindestens 1 sein")
    private Integer quantity;

    @Column(name = "list_price", nullable = false)
    @NotNull(message = "Listenpreis ist erforderlich")
    @DecimalMin(value = "0.0", inclusive = false, message = "Listenpreis muss groesser als 0 sein")
    @Digits(integer = 17, fraction = 2, message = "Ungueltiges Preisformat")
    private BigDecimal listPrice;

    @Column(name = "discount", nullable = false)
    @NotNull(message = "Rabatt ist erforderlich")
    @DecimalMin(value = "0.0", message = "Rabatt darf nicht negativ sein")
    @DecimalMax(value = "1.0", message = "Rabatt darf maximal 100% betragen")
    @Digits(integer = 1, fraction = 2, message = "Ungueltiges Rabattformat")
    private BigDecimal discount;

    public OrderItem() {
    }

    public OrderItemId id() {
        return id;
    }

    public void id(OrderItemId v) {
        this.id = v;
    }

    public Order order() {
        return order;
    }

    public void order(Order v) {
        this.order = v;
    }

    public Product product() {
        return product;
    }

    public void product(Product v) {
        this.product = v;
    }

    public Integer quantity() {
        return quantity;
    }

    public void quantity(Integer v) {
        this.quantity = v;
    }

    public BigDecimal listPrice() {
        return listPrice;
    }

    public void listPrice(BigDecimal v) {
        this.listPrice = v;
    }

    public BigDecimal discount() {
        return discount;
    }

    public void discount(BigDecimal v) {
        this.discount = v;
    }

    public OrderItemId getId() {
        return id;
    }

    public void setId(OrderItemId v) {
        this.id = v;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order v) {
        this.order = v;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product v) {
        this.product = v;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer v) {
        this.quantity = v;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public void setListPrice(BigDecimal v) {
        this.listPrice = v;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal v) {
        this.discount = v;
    }
}
