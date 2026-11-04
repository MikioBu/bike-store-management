package de.sep.business.customer.boundary;

import java.util.List;

import de.sep.business.customer.CustomerSortDirection;
import de.sep.business.customer.CustomerSortField;
import de.sep.business.customer.entity.Customer;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class CustomerService {

    @PersistenceContext
    EntityManager em;

    @RolesAllowed({ "ADMIN", "USER1" })
    public void saveCustomer(Customer customer) {
        em.persist(customer);
    }

    @RolesAllowed({ "ADMIN", "USER1" })
    public void updateCustomer(Integer id, Customer updated) {
        Customer existing = em.find(Customer.class, id);
        if (existing == null)
            throw new IllegalStateException("Kunde nicht gefunden");

        if (updated.getFirstName() == null || updated.getFirstName().isBlank())
            throw new IllegalArgumentException("Vorname ist erforderlich");
        if (updated.getLastName() == null || updated.getLastName().isBlank())
            throw new IllegalArgumentException("Nachname ist erforderlich");
        if (updated.getEmail() == null || updated.getEmail().isBlank())
            throw new IllegalArgumentException("E-Mail ist erforderlich");
        if (updated.getPhone() == null || updated.getPhone().isBlank())
            throw new IllegalArgumentException("Telefon ist erforderlich");
        if (updated.getStreet() == null || updated.getStreet().isBlank())
            throw new IllegalArgumentException("Straße ist erforderlich");
        if (updated.getCity() == null || updated.getCity().isBlank())
            throw new IllegalArgumentException("Stadt ist erforderlich");
        if (updated.getState() == null || updated.getState().isBlank())
            throw new IllegalArgumentException("Bundesland ist erforderlich");
        if (updated.getZipCode() == null || updated.getZipCode().isBlank())
            throw new IllegalArgumentException("Postleitzahl ist erforderlich");

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setStreet(updated.getStreet());
        existing.setCity(updated.getCity());
        existing.setState(updated.getState());
        existing.setZipCode(updated.getZipCode());

        em.merge(existing);
    }

    @RolesAllowed({ "ADMIN", "USER1" })
    public void deleteCustomer(Integer id) {
        Customer c = em.find(Customer.class, id);
        if (c != null)
            em.remove(c);
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public Customer findCustomerById(Integer id) {
        return em.find(Customer.class, id);
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Customer> getAllCustomers() {
        return em.createQuery("SELECT c FROM Customer c ORDER BY c.lastName ASC, c.firstName ASC",
                Customer.class).getResultList();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public long countAllCustomers() {
        return em.createQuery("SELECT COUNT(c) FROM Customer c", Long.class)
                .getSingleResult();
    }

    @RolesAllowed({ "ADMIN", "USER1", "USER2" })
    public List<Customer> getCustomersLimitedPerPage(int offset, int limit,
            CustomerSortField field, CustomerSortDirection direction) {

        String sortColumn = switch (field) {
            case FIRST_NAME -> "c.firstName";
            case LAST_NAME -> "c.lastName";
            case CITY -> "c.city";
            case STATE -> "c.state";
            case ZIP_CODE -> "c.zipCode";
            case EMAIL -> "c.email";
        };
        String sortOrder = direction == CustomerSortDirection.DESC ? "DESC" : "ASC";
        return em.createQuery(
                "SELECT c FROM Customer c ORDER BY " + sortColumn + " " + sortOrder,
                Customer.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
