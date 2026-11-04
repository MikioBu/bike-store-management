package de.sep.business.order.view;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.sep.business.customer.boundary.CustomerService;
import de.sep.business.customer.entity.Customer;
import de.sep.business.order.OrderSortDirection;
import de.sep.business.order.OrderSortField;
import de.sep.business.order.boundary.OrderService;
import de.sep.business.order.entity.Order;
import de.sep.business.order.entity.OrderItem;
import de.sep.business.product.boundary.ProductService;
import de.sep.business.product.entity.Product;
import de.sep.business.staff.boundary.StaffService;
import de.sep.business.staff.entity.Staff;
import de.sep.business.store.boundary.StoreService;
import de.sep.business.store.entity.Store;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named
@ViewScoped
public class OrderView implements Serializable {

    private static final long serialVersionUID = 13L;

    @Inject
    private OrderService orderService;
    @Inject
    private CustomerService customerService;
    @Inject
    private StoreService storeService;
    @Inject
    private StaffService staffService;
    @Inject
    private ProductService productService;

    // Listendaten
    private List<Order> orderList;
    private long totalCount;

    // Paginierung
    private int page = 0;
    private int rowsPerPage = 50;

    // Sortierung
    private OrderSortField sortField = OrderSortField.ORDER_DATE;
    private OrderSortDirection sortDirection = OrderSortDirection.DESC;

    // Anlegen-Panel
    private Order formOrder;
    private boolean creationPanelVisible = false;

    // Dropdown-Listen (nur für ADMIN / USER1)
    private List<Customer> customerList;
    private List<Store> storeList;
    private List<Staff> staffList;
    private List<Product> productList;

    // Ausgewählte IDs im Formular
    private Integer selectedCustomerId;
    private Integer selectedStoreId;
    private Integer selectedStaffId;

    // Positions-Eingaben
    private Integer selectedProductId;
    private Integer selectedQuantity = 1;
    private BigDecimal selectedDiscountPercent = BigDecimal.ZERO;
    private List<OrderItem> tempOrderItems = new ArrayList<>();

    // Detailansicht
    private Order focusedOrder;
    private boolean detailPanelVisible = false;

    // Status-Änderung
    private Integer newStatusForUpdate;

    // Initialisierung: Laden der ersten Seite und aller Dropdown-Listen.

    @PostConstruct
    public void init() {
        totalCount = orderService.countAllOrders();
        reload();

        // Dropdown-Daten werden erst beim Öffnen des Anlegen-Dialogs geladen.
        // So werden reine Listen-/Detailaufrufe nicht durch unnötige SELECTs belastet.
        customerList = Collections.emptyList();
        storeList = Collections.emptyList();
        staffList = Collections.emptyList();
        productList = Collections.emptyList();
    }

    private void reload() {
        List<Order> result = orderService.getOrdersLimitedPerPage(
                page * rowsPerPage, rowsPerPage, sortField, sortDirection);
        orderList = result != null ? result : Collections.emptyList();
    }

    // Paginierung

    public void nextPage() {
        if (hasFollowingPage()) {
            page++;
            reload();
        }
    }

    public void previousPage() {
        if (hasPrecedingPage()) {
            page--;
            reload();
        }
    }

    public boolean hasFollowingPage() {
        return (long) (page + 1) * rowsPerPage < totalCount;
    }

    public boolean hasPrecedingPage() {
        return page > 0;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalCount / rowsPerPage);
    }

    public void applySort() {
        page = 0;
        reload();
    }

    // Anlegen-Panel

    public void openCreationPanel() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        if (req.isUserInRole("ADMIN") || req.isUserInRole("USER1")) {
            customerList = customerService.getAllCustomers();
            storeList = storeService.getAllStores();
            staffList = staffService.getAllStaff();
            productList = productService.getAllProducts();
        }

        if (customerList == null)
            customerList = Collections.emptyList();
        if (storeList == null)
            storeList = Collections.emptyList();
        if (staffList == null)
            staffList = Collections.emptyList();
        if (productList == null)
            productList = Collections.emptyList();

        formOrder = new Order();
        formOrder.setOrderDate(LocalDate.now());
        formOrder.setRequiredDate(LocalDate.now().plusDays(7));
        formOrder.setOrderStatus(1);
        selectedCustomerId = null;
        selectedStoreId = null;
        selectedStaffId = null;
        selectedProductId = null;
        selectedQuantity = 1;
        selectedDiscountPercent = BigDecimal.ZERO;
        tempOrderItems.clear();
        creationPanelVisible = true;
    }

    public void closeCreationPanel() {
        creationPanelVisible = false;
        formOrder = null;
        tempOrderItems.clear();
    }

    // Delegiert Positions-Erstellung an den Service.
    public void addOrderItem() {
        if (selectedProductId != null) {
            for (OrderItem existing : tempOrderItems) {
                if (existing.getProduct().getProductId().equals(selectedProductId)) {
                    addError("Produkt bereits in der Liste");
                    return;
                }
            }
        }
        try {
            OrderItem item = orderService.buildOrderItem(
                    selectedProductId, selectedQuantity, selectedDiscountPercent);
            tempOrderItems.add(item);
            selectedProductId = null;
            selectedQuantity = 1;
            selectedDiscountPercent = BigDecimal.ZERO;
            addInfo("Produkt hinzugefügt");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        }
    }

    public void removeOrderItem(OrderItem item) {
        tempOrderItems.remove(item);
    }

    /**
     * Speichert die Bestellung. Der Service prüft intern den Lagerbestand
     * für jede Position und wirft eine Exception wenn nicht genug vorhanden.
     */
    public void saveOrder() {
        try {
            orderService.createOrder(
                    selectedCustomerId, selectedStoreId, selectedStaffId,
                    formOrder.getOrderDate(), formOrder.getRequiredDate(),
                    formOrder.getOrderStatus(), new ArrayList<>(tempOrderItems));
            addInfo("Bestellung erfolgreich angelegt");
            closeCreationPanel();
            totalCount = orderService.countAllOrders();
            reload();
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Bestellung konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    // Detailansicht

    public void openDetail(Order entry) {
        focusedOrder = orderService.findOrderWithItems(entry.getOrderId());
        newStatusForUpdate = focusedOrder != null ? focusedOrder.getOrderStatus() : null;
        detailPanelVisible = true;
    }

    public void closeDetail() {
        detailPanelVisible = false;
        focusedOrder = null;
        newStatusForUpdate = null;
    }

    // Status-Update

    /**
     * Aktualisiert den Status der aktuell geöffneten Bestellung.
     * Bei Processing --> Completed werden Lagerbestände automatisch reduziert.
     */
    public void updateStatus() {
        if (focusedOrder == null || newStatusForUpdate == null)
            return;
        try {
            orderService.updateOrderStatus(focusedOrder.getOrderId(), newStatusForUpdate);
            // Detailansicht neu laden damit Status + shippedDate aktuell sind
            focusedOrder = orderService.findOrderWithItems(focusedOrder.getOrderId());
            reload();
            addInfo("Status erfolgreich aktualisiert");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Status konnte nicht geändert werden: " + e.getMessage());
        }
    }

    // Hilfsmethoden

    public BigDecimal getTotalAmount() {
        return orderService.calculateTotal(tempOrderItems);
    }

    public String getStatusLabel(Integer status) {
        if (status == null)
            return "Unbekannt";
        return switch (status) {
            case 1 -> "Ausstehend";
            case 2 -> "In Bearbeitung";
            case 3 -> "Storniert";
            case 4 -> "Abgeschlossen";
            default -> "Unbekannt";
        };
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", msg));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", msg));
    }

    // Getter & Setter

    public List<Order> getOrderList() {
        return orderList;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int r) {
        this.page = 0;
        this.rowsPerPage = r;
        reload();
    }

    public OrderSortField getSortField() {
        return sortField;
    }

    public void setSortField(OrderSortField f) {
        this.sortField = f != null ? f : OrderSortField.ORDER_DATE;
    }

    public OrderSortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(OrderSortDirection d) {
        this.sortDirection = d != null ? d : OrderSortDirection.DESC;
    }

    public Order getFormOrder() {
        return formOrder;
    }

    public void setFormOrder(Order o) {
        this.formOrder = o;
    }

    public boolean isCreationPanelVisible() {
        return creationPanelVisible;
    }

    public void setCreationPanelVisible(boolean v) {
        this.creationPanelVisible = v;
    }

    public Order getFocusedOrder() {
        return focusedOrder;
    }

    public void setFocusedOrder(Order o) {
        this.focusedOrder = o;
    }

    public boolean isDetailPanelVisible() {
        return detailPanelVisible;
    }

    public void setDetailPanelVisible(boolean v) {
        this.detailPanelVisible = v;
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public List<Store> getStoreList() {
        return storeList;
    }

    public List<Staff> getStaffList() {
        return staffList;
    }

    public List<Product> getProductList() {
        return productList;
    }

    public Integer getSelectedCustomerId() {
        return selectedCustomerId;
    }

    public void setSelectedCustomerId(Integer id) {
        this.selectedCustomerId = id;
    }

    public Integer getSelectedStoreId() {
        return selectedStoreId;
    }

    public void setSelectedStoreId(Integer id) {
        this.selectedStoreId = id;
    }

    public Integer getSelectedStaffId() {
        return selectedStaffId;
    }

    public void setSelectedStaffId(Integer id) {
        this.selectedStaffId = id;
    }

    public Integer getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Integer id) {
        this.selectedProductId = id;
    }

    public Integer getSelectedQuantity() {
        return selectedQuantity;
    }

    public void setSelectedQuantity(Integer q) {
        this.selectedQuantity = q;
    }

    public BigDecimal getSelectedDiscountPercent() {
        return selectedDiscountPercent;
    }

    public void setSelectedDiscountPercent(BigDecimal p) {
        this.selectedDiscountPercent = p;
    }

    public List<OrderItem> getTempOrderItems() {
        return tempOrderItems;
    }

    public Integer getNewStatusForUpdate() {
        return newStatusForUpdate;
    }

    public void setNewStatusForUpdate(Integer s) {
        this.newStatusForUpdate = s;
    }
}
