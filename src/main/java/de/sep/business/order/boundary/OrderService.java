package de.sep.business.order.boundary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.sep.business.customer.boundary.CustomerService;
import de.sep.business.order.OrderSortDirection;
import de.sep.business.order.OrderSortField;
import de.sep.business.order.entity.Order;
import de.sep.business.order.entity.OrderItem;
import de.sep.business.order.entity.OrderItemId;
import de.sep.business.product.boundary.ProductService;
import de.sep.business.product.entity.Product;
import de.sep.business.stock.entity.Stock;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class OrderService {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private CustomerService customerService;

    @Inject
    private ProductService productService;

    // Anlegen

    /**
     * Legt eine neue Bestellung an.
     * Prüft für jede Position ob das Produkt in der gewählten Niederlassung
     * auf Lager ist (Bestand >= bestellte Menge).
     *
     * @throws IllegalArgumentException bei fehlenden Pflichtfeldern oder
     *                                  unzureichendem Lagerbestand
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void createOrder(Integer customerId, Integer storeId, Integer staffId,
            LocalDate orderDate, LocalDate requiredDate,
            Integer orderStatus, List<OrderItem> items) {

        if (customerId == null)
            throw new IllegalArgumentException("Bitte wählen Sie einen Kunden aus");
        if (storeId == null)
            throw new IllegalArgumentException("Bitte wählen Sie eine Niederlassung aus");
        if (staffId == null)
            throw new IllegalArgumentException("Bitte wählen Sie einen Mitarbeiter aus");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Bitte fügen Sie mindestens ein Produkt hinzu");
        if (orderDate == null)
            throw new IllegalArgumentException("Bestelldatum ist erforderlich");
        if (requiredDate == null)
            throw new IllegalArgumentException("Lieferdatum ist erforderlich");

        // Lagerprüfung für alle Positionen
        for (OrderItem item : items) {
            Integer productId = item.getProduct().getProductId();
            Stock stock = findStock(productId, storeId);

            if (stock == null || stock.getQuantity() < item.getQuantity()) {
                int verfuegbar = (stock == null) ? 0 : stock.getQuantity();
                throw new IllegalArgumentException(
                        "Produkt \"" + item.getProduct().getProductName()
                                + "\" nicht ausreichend auf Lager (verfügbar: "
                                + verfuegbar + ", benötigt: " + item.getQuantity() + ")");
            }
        }

        // Bestellung persistieren
        Order order = new Order();
        order.setCustomer(customerService.findCustomerById(customerId));
        order.setStoreId(storeId);
        order.setStaffId(staffId);
        order.setOrderDate(orderDate);
        order.setRequiredDate(requiredDate);
        order.setOrderStatus(orderStatus != null ? orderStatus : 1);

        int itemId = 1;
        for (OrderItem item : items) {
            item.setOrder(order);
            item.setId(new OrderItemId(null, itemId++));
        }
        order.setItems(items);
        em.persist(order);
    }

    // Status-Update

    /**
     * Aktualisiert den Status einer Bestellung.
     * Wechsel von "In Bearbeitung" auf "Abgeschlossen": Lagerbestand in der
     * Niederlassung wird für jede Position reduziert.
     *
     * @throws IllegalArgumentException wenn Lagerbestand nicht ausreicht
     * @throws IllegalStateException    wenn Bestellung nicht gefunden
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void updateOrderStatus(Integer orderId, Integer newStatus) {

        Order order = em.find(Order.class, orderId);
        if (order == null)
            throw new IllegalStateException("Bestellung nicht gefunden");

        Integer oldStatus = order.getOrderStatus();

        // Zentrale Status-Validierung einmal abgeschlossen = immer abgeschlossen
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalStateException("Ungültiger Statuswechsel");
        }

        // Wenn Status auf "Abgeschlossen" gesetzt wird --> Lager reduzieren
        if (Integer.valueOf(4).equals(newStatus)) {

            List<OrderItem> items = em.createQuery(
                    "SELECT i FROM OrderItem i WHERE i.id.orderId = :oid",
                    OrderItem.class)
                    .setParameter("oid", orderId)
                    .getResultList();

            for (OrderItem item : items) {
                Integer productId = item.getProduct().getProductId();
                Integer storeId = order.getStoreId();

                Stock stock = findStock(productId, storeId);

                if (stock == null || stock.getQuantity() < item.getQuantity()) {
                    int verfuegbar = (stock == null) ? 0 : stock.getQuantity();
                    throw new IllegalArgumentException(
                            "Lagerbestand für \"" + item.getProduct().getProductName()
                                    + "\" nicht ausreichend (verfügbar: "
                                    + verfuegbar + ", benötigt: " + item.getQuantity() + ")");
                }

                stock.setQuantity(stock.getQuantity() - item.getQuantity());
                em.merge(stock);
            }

            order.setShippedDate(LocalDate.now());
        }

        order.setOrderStatus(newStatus);
        em.merge(order);
        em.flush();
    }

    // Hilfsmethode für Status-Übergänge (nur bestimmte Wechsel erlaubt)
    private boolean isValidTransition(Integer oldStatus, Integer newStatus) {
        return switch (oldStatus) {
            case 1 -> newStatus == 2; // Neu → In Bearbeitung
            case 2 -> newStatus == 3 || newStatus == 4; // In Bearbeitung → Versendet/Abgeschlossen
            case 3 -> newStatus == 4; // Versendet → Abgeschlossen
            case 4 -> false; // 🔒 final
            default -> false;
        };
    }

    // Hilfsmethode Lagerzugriff

    private Stock findStock(Integer productId, Integer storeId) {
        try {
            List<Stock> result = em.createQuery(
                    "SELECT s FROM Stock s WHERE s.id.productId = :pid AND s.id.storeId = :sid",
                    Stock.class)
                    .setParameter("pid", productId)
                    .setParameter("sid", storeId)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    // Positions-Erstellung

    /**
     * Erstellt eine neue Bestellposition aus den UI-Eingaben.
     * Validiert Menge und Rabatt, setzt Listenpreis aus dem Produkt.
     *
     * @throws IllegalArgumentException bei ungültigen Eingaben
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public OrderItem buildOrderItem(Integer productId, Integer quantity,
            BigDecimal discountPercent) {

        if (productId == null)
            throw new IllegalArgumentException("Bitte wählen Sie ein Produkt aus");
        if (quantity == null || quantity < 1)
            throw new IllegalArgumentException("Menge muss mindestens 1 sein");

        BigDecimal discount = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            if (discountPercent.compareTo(new BigDecimal("100")) > 0)
                throw new IllegalArgumentException("Rabatt muss zwischen 0 und 100% liegen");
            discount = discountPercent.divide(BigDecimal.valueOf(100));
        }

        Product product = productService.findProductById(productId);
        if (product == null)
            throw new IllegalArgumentException("Produkt nicht gefunden");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setListPrice(product.getListPrice());
        item.setDiscount(discount);
        return item;
    }

    // Berechnung: Gesamtpreis einer Bestellung basierend auf den Positionen

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public BigDecimal calculateTotal(List<OrderItem> items) {
        if (items == null)
            return BigDecimal.ZERO;
        return items.stream()
                .map(i -> i.getListPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity()))
                        .multiply(BigDecimal.ONE.subtract(i.getDiscount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Lesen: Bestellung mit zugehörigen Positionen und Kundeninformationen

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public Order findOrderWithItems(Integer id) {
        Order order = em.find(Order.class, id);
        if (order == null) {
            return null;
        }

        // Strict-JPQL-sicher: keine FETCH-JOIN-Aliase verwenden.
        // Benötigte Lazy-Daten werden innerhalb der EJB-Transaktion initialisiert,
        // damit die JSF-Detailansicht sie danach ohne LazyInitialization-Probleme
        // anzeigen kann.
        if (order.getCustomer() != null) {
            order.getCustomer().getFirstName();
            order.getCustomer().getLastName();
        }

        if (order.getItems() != null) {
            order.getItems().size();
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().getProductName();
                    item.getProduct().getListPrice();
                }
            }
        }

        return order;
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public long countAllOrders() {
        return em.createQuery("SELECT COUNT(o) FROM Order o", Long.class)
                .getSingleResult();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Order> getOrdersLimitedPerPage(int offset, int limit,
            OrderSortField field, OrderSortDirection direction) {

        String sortColumn = switch (field) {
            case ORDER_DATE -> "o.orderDate";
            case REQUIRED_DATE -> "o.requiredDate";
            case SHIPPED_DATE -> "o.shippedDate";
            case ORDER_STATUS -> "o.orderStatus";
            case CUSTOMER -> "o.customer.lastName";
            case STORE -> "o.storeId";
        };
        String sortOrder = direction == OrderSortDirection.DESC ? "DESC" : "ASC";

        return em.createQuery(
                "SELECT o FROM Order o " +
                        "LEFT JOIN FETCH o.customer " +
                        "ORDER BY " + sortColumn + " " + sortOrder,
                Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
