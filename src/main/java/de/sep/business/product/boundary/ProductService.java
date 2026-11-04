package de.sep.business.product.boundary;

import java.util.List;

import de.sep.business.product.ProductSortDirection;
import de.sep.business.product.ProductSortField;
import de.sep.business.product.entity.Brand;
import de.sep.business.product.entity.Category;
import de.sep.business.product.entity.Product;
import de.sep.business.product.view.StockEntry;
import de.sep.business.stock.entity.Stock;
import de.sep.business.stock.entity.StockId;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ProductService {

    @PersistenceContext
    private EntityManager em;

    // Anlegen mit initialem Lagerbestand

    /**
     * Speichert ein neues Produkt und legt optional einen initialen
     * Lagerbestand für eine Niederlassung an.
     *
     * @param initialStoreId  Niederlassung für Anfangsbestand (kann null sein)
     * @param initialQuantity Anfangsbestand (0 wenn nicht gesetzt)
     * @throws IllegalArgumentException bei fehlenden Pflichtfeldern
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void saveProduct(Product product, Integer brandId, Integer categoryId,
            Integer initialStoreId, Integer initialQuantity) {
        if (brandId == null)
            throw new IllegalArgumentException("Marke muss ausgewählt werden");
        if (categoryId == null)
            throw new IllegalArgumentException("Kategorie muss ausgewählt werden");

        product.setBrand(em.getReference(Brand.class, brandId));
        product.setCategory(em.getReference(Category.class, categoryId));
        em.persist(product);
        // Bei SEQUENCE-IDs steht die Produkt-ID nach persist() zur Verfügung;
        // ein explizites flush() ist hier nicht nötig.

        // Initialen Lagerbestand anlegen wenn Niederlassung gewählt
        if (initialStoreId != null) {
            int qty = (initialQuantity != null && initialQuantity >= 0) ? initialQuantity : 0;
            Stock stock = new Stock(
                    new StockId(product.getProductId(), initialStoreId),
                    product, initialStoreId, qty);
            em.persist(stock);
        }
    }

    /**
     * Speichert ein neues Produkt und legt initialen Lagerbestand
     * für mehrere Niederlassungen an.
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void saveProductWithStocks(Product product, Integer brandId, Integer categoryId,
            List<StockEntry> entries) {
        if (brandId == null)
            throw new IllegalArgumentException("Marke muss ausgewählt werden");
        if (categoryId == null)
            throw new IllegalArgumentException("Kategorie muss ausgewählt werden");

        product.setBrand(em.getReference(Brand.class, brandId));
        product.setCategory(em.getReference(Category.class, categoryId));
        em.persist(product);
        // Bei SEQUENCE-IDs steht die Produkt-ID nach persist() zur Verfügung;
        // ein explizites flush() ist hier nicht nötig.

        if (entries != null) {
            for (StockEntry e : entries) {
                if (e.getStoreId() == null)
                    continue;
                int qty = (e.getQuantity() != null && e.getQuantity() >= 0) ? e.getQuantity() : 0;
                Stock stock = new Stock(
                        new StockId(product.getProductId(), e.getStoreId()),
                        product, e.getStoreId(), qty);
                em.persist(stock);
            }
        }
    }

    /** Alte Signatur ohne Lagerbestand - für Kompatibilität */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void saveProduct(Product product, Integer brandId, Integer categoryId) {
        saveProduct(product, brandId, categoryId, null, null);
    }

    // Löschen nur wenn nicht in Bestellungen

    /**
     * Löscht ein Produkt, sofern es nicht in Bestellpositionen referenziert wird.
     * 
     * @throws IllegalStateException wenn Produkt in Bestellungen vorhanden
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void deleteProduct(Integer id) {
        // Nur aktive Bestellungen (Ausstehend=1, In Bearbeitung=2) blockieren das
        // Löschen.
        // Storniert=3 und Abgeschlossen=4 sind unbedenklich.
        Long count = em.createQuery(
                "SELECT COUNT(i) FROM OrderItem i "
                        + "WHERE i.product.productId = :pid "
                        + "AND i.order.orderStatus IN (1, 2)",
                Long.class)
                .setParameter("pid", id)
                .getSingleResult();

        if (count > 0)
            throw new IllegalStateException(
                    "Produkt ist in " + count
                            + " aktiver Bestellung(en) (Ausstehend/In Bearbeitung) vorhanden und kann nicht gelöscht werden");

        Product p = em.find(Product.class, id);
        if (p != null)
            em.remove(p);
    }

    // Lagerbestand verwalten

    /**
     * Setzt den Lagerbestand eines Produkts in einer Niederlassung.
     * Legt einen neuen Stock-Eintrag an wenn noch keiner existiert.
     */
    @RolesAllowed({ "ADMIN", "USER1" })
    public void updateStock(Integer productId, Integer storeId, Integer quantity) {
        if (productId == null || storeId == null)
            throw new IllegalArgumentException("Produkt und Niederlassung sind erforderlich");
        if (quantity == null || quantity < 0)
            throw new IllegalArgumentException("Bestand darf nicht negativ sein");

        StockId stockId = new StockId(productId, storeId);
        Stock existing = em.find(Stock.class, stockId);

        if (existing != null) {
            existing.setQuantity(quantity);
            em.merge(existing);
        } else {
            Product p = em.find(Product.class, productId);
            if (p == null)
                throw new IllegalArgumentException("Produkt nicht gefunden");
            em.persist(new Stock(stockId, p, storeId, quantity));
        }
    }

    // Suchen

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public Product findProductById(Integer id) {
        return em.find(Product.class, id);
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public Brand findBrandById(Integer id) {
        return em.find(Brand.class, id);
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public Category findCategoryById(Integer id) {
        return em.find(Category.class, id);
    }

    // Zählen & Listen

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public long countAllProducts() {
        return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                .getSingleResult();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Product> getAllProducts() {
        return em.createQuery("SELECT p FROM Product p ORDER BY p.productName ASC",
                Product.class).getResultList();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Brand> getAllBrands() {
        return em.createQuery("SELECT b FROM Brand b ORDER BY b.brandName ASC",
                Brand.class).getResultList();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Category> getAllCategories() {
        return em.createQuery("SELECT c FROM Category c ORDER BY c.categoryName ASC",
                Category.class).getResultList();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Product> getProductsLimitedPerPage(int offset, int limit,
            ProductSortField field, ProductSortDirection direction) {

        String sortColumn = switch (field) {
            case NAME -> "p.productName";
            case LIST_PRICE -> "p.listPrice";
            case MODEL_YEAR -> "p.modelYear";
            case BRAND -> "p.brand.brandName";
            case CATEGORY -> "p.category.categoryName";
        };
        String sortOrder = direction == ProductSortDirection.DESC ? "DESC" : "ASC";

        return em.createQuery(
                "SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.brand " +
                        "LEFT JOIN FETCH p.category " +
                        "ORDER BY " + sortColumn + " " + sortOrder,
                Product.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}