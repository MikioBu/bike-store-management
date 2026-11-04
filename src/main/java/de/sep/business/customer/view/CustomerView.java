package de.sep.business.customer.view;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import de.sep.business.customer.CustomerSortDirection;
import de.sep.business.customer.CustomerSortField;
import de.sep.business.customer.boundary.CustomerService;
import de.sep.business.customer.entity.Customer;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class CustomerView implements Serializable {

    private static final long serialVersionUID = 42L;

    @Inject
    private CustomerService customerService;

    private List<Customer> customerList;
    private long totalCount;
    private int page = 0;
    private int rowsPerPage = 50;

    private CustomerSortField sortField = CustomerSortField.LAST_NAME;
    private CustomerSortDirection sortDirection = CustomerSortDirection.ASC;

    // Anlegen
    private Customer formCustomer;
    private boolean creationPanelVisible = false;

    // Bearbeiten
    private Customer editCustomer;
    private Integer editCustomerId;
    private boolean editPanelVisible = false;

    // Initialisierung

    @PostConstruct
    public void init() {
        totalCount = customerService.countAllCustomers();
        reload();
    }

    private void reload() {
        List<Customer> result = customerService.getCustomersLimitedPerPage(
                page * rowsPerPage, rowsPerPage, sortField, sortDirection);
        customerList = result != null ? result : Collections.emptyList();
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

    // Sortierung

    public void applySort() {
        page = 0;
        reload();
    }

    // Anlegen

    public void openCreationPanel() {
        formCustomer = new Customer();
        creationPanelVisible = true;
    }

    public void closeCreationPanel() {
        creationPanelVisible = false;
        formCustomer = null;
    }

    public void saveCustomer() {
        try {
            customerService.saveCustomer(formCustomer);
            creationPanelVisible = false;
            formCustomer = null;
            totalCount = customerService.countAllCustomers();
            reload();
            addInfo("Kunde wurde angelegt");
        } catch (IllegalArgumentException | IllegalStateException e) {
            addError(e.getMessage());
        } catch (Exception ex) {
            addError("Kunde konnte nicht gespeichert werden");
        }
    }

    // Bearbeiten

    public void openEditPanel(Customer customer) {
        editCustomerId = customer.getCustomerId();
        // Kopie erstellen, damit Abbrechen keine Änderungen hinterlässt
        editCustomer = new Customer();
        editCustomer.setFirstName(customer.getFirstName());
        editCustomer.setLastName(customer.getLastName());
        editCustomer.setEmail(customer.getEmail());
        editCustomer.setPhone(customer.getPhone());
        editCustomer.setStreet(customer.getStreet());
        editCustomer.setCity(customer.getCity());
        editCustomer.setState(customer.getState());
        editCustomer.setZipCode(customer.getZipCode());
        editPanelVisible = true;
    }

    public void closeEditPanel() {
        editPanelVisible = false;
        editCustomer = null;
        editCustomerId = null;
    }

    public void updateCustomer() {
        try {
            customerService.updateCustomer(editCustomerId, editCustomer);
            editPanelVisible = false;
            editCustomer = null;
            editCustomerId = null;
            reload();
            addInfo("Kunde wurde aktualisiert");
        } catch (IllegalArgumentException | IllegalStateException e) {
            addError(e.getMessage());
        } catch (Exception ex) {
            addError("Kunde konnte nicht aktualisiert werden");
        }
    }

    // Löschen

    public void deleteCustomer(Integer id) {
        try {
            customerService.deleteCustomer(id);
            totalCount = customerService.countAllCustomers();
            if (page > 0 && page >= getTotalPages())
                page--;
            reload();
            addInfo("Kunde wurde gelöscht");
        } catch (Exception ex) {
            addError("Kunde kann nicht gelöscht werden - noch Bestellungen vorhanden");
        }
    }

    // Hilfsmethoden

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", msg));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", msg));
    }

    // Getter & Setter

    public List<Customer> getCustomerList() {
        return customerList;
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

    public CustomerSortField getSortField() {
        return sortField;
    }

    public void setSortField(CustomerSortField f) {
        this.sortField = f != null ? f : CustomerSortField.LAST_NAME;
    }

    public CustomerSortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(CustomerSortDirection d) {
        this.sortDirection = d != null ? d : CustomerSortDirection.ASC;
    }

    public Customer getFormCustomer() {
        return formCustomer;
    }

    public void setFormCustomer(Customer c) {
        this.formCustomer = c;
    }

    public boolean isCreationPanelVisible() {
        return creationPanelVisible;
    }

    public void setCreationPanelVisible(boolean v) {
        this.creationPanelVisible = v;
    }

    public Customer getEditCustomer() {
        return editCustomer;
    }

    public void setEditCustomer(Customer c) {
        this.editCustomer = c;
    }

    public Integer getEditCustomerId() {
        return editCustomerId;
    }

    public boolean isEditPanelVisible() {
        return editPanelVisible;
    }

    public void setEditPanelVisible(boolean v) {
        this.editPanelVisible = v;
    }
}
