package de.sep.business.order.entity;

import java.time.LocalDate;
import java.util.List;

import de.sep.business.customer.entity.Customer;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_order_id_seq", allocationSize = 1)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_date", nullable = false)
    @NotNull(message = "Bestelldatum ist erforderlich")
    private LocalDate orderDate;

    @Column(name = "required_date", nullable = false)
    @NotNull(message = "Gewünschtes Lieferdatum ist erforderlich")
    private LocalDate requiredDate;

    @Column(name = "shipped_date")
    private LocalDate shippedDate;

    @Column(name = "order_status", nullable = false)
    @NotNull(message = "Bestellstatus ist erforderlich")
    @Min(value = 1, message = "Bestellstatus ungültig")
    @Max(value = 4, message = "Bestellstatus ungültig")
    private Integer orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Kunde ist erforderlich")
    private Customer customer;

    @Column(name = "staff_id", nullable = false)
    @NotNull(message = "Mitarbeiter ist erforderlich")
    private Integer staffId;

    @Column(name = "store_id", nullable = false)
    @NotNull(message = "Niederlassung ist erforderlich")
    private Integer storeId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    // Konstruktoren

    public Order() {
    }

    public Order(LocalDate orderDate, LocalDate requiredDate, LocalDate shippedDate,
            Integer orderStatus, Customer customer, Integer staffId, Integer storeId) {
        this.orderDate = orderDate;
        this.requiredDate = requiredDate;
        this.shippedDate = shippedDate;
        this.orderStatus = orderStatus;
        this.customer = customer;
        this.staffId = staffId;
        this.storeId = storeId;
    }

    public Integer orderId() {
        return orderId;
    }

    public LocalDate orderDate() {
        return orderDate;
    }

    public void orderDate(LocalDate v) {
        this.orderDate = v;
    }

    public LocalDate requiredDate() {
        return requiredDate;
    }

    public void requiredDate(LocalDate v) {
        this.requiredDate = v;
    }

    public LocalDate shippedDate() {
        return shippedDate;
    }

    public void shippedDate(LocalDate v) {
        this.shippedDate = v;
    }

    public Integer orderStatus() {
        return orderStatus;
    }

    public void orderStatus(Integer v) {
        this.orderStatus = v;
    }

    public Customer customer() {
        return customer;
    }

    public void customer(Customer v) {
        this.customer = v;
    }

    public Integer staffId() {
        return staffId;
    }

    public void staffId(Integer v) {
        this.staffId = v;
    }

    public Integer storeId() {
        return storeId;
    }

    public void storeId(Integer v) {
        this.storeId = v;
    }

    public List<OrderItem> items() {
        return items;
    }

    public void items(List<OrderItem> v) {
        this.items = v;
    }

    // Getter/Setter

    public Integer getOrderId() {
        return orderId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate v) {
        this.orderDate = v;
    }

    public LocalDate getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(LocalDate v) {
        this.requiredDate = v;
    }

    public LocalDate getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(LocalDate v) {
        this.shippedDate = v;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer v) {
        this.orderStatus = v;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer v) {
        this.customer = v;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer v) {
        this.staffId = v;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer v) {
        this.storeId = v;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> v) {
        this.items = v;
    }
}
