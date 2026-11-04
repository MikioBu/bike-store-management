package de.sep.business.staff.view;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import de.sep.business.staff.StaffSortDirection;
import de.sep.business.staff.StaffSortField;
import de.sep.business.staff.boundary.StaffService;
import de.sep.business.staff.entity.Staff;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class StaffView implements Serializable {

    private static final long serialVersionUID = 17L;

    @Inject
    private StaffService staffService;

    private List<Staff> staffList;
    private long totalCount;
    private int page = 0;
    private int rowsPerPage = 50;
    private StaffSortField sortField = StaffSortField.LAST_NAME;
    private StaffSortDirection sortDirection = StaffSortDirection.ASC;

    // Detail
    private Staff focusedStaff;
    private boolean detailPanelVisible = false;

    // Anlegen
    private Staff formStaff;
    private boolean creationPanelVisible = false;
    private Integer selectedManagerId;
    private List<Staff> managerList;

    // Bearbeiten
    private Staff editStaff;
    private Integer editStaffId; // ID separat, da Staff kein setStaffId hat
    private boolean editPanelVisible = false;
    private Integer editManagerId;

    @PostConstruct
    public void init() {
        totalCount = staffService.countAllStaff();
        managerList = staffService.getAllStaffAsManagers();
        if (managerList == null)
            managerList = Collections.emptyList();
        reload();
    }

    private void reload() {
        List<Staff> result = staffService.getStaffLimitedPerPage(
                page * rowsPerPage, rowsPerPage, sortField, sortDirection);
        staffList = result != null ? result : Collections.emptyList();
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

    // Detail
    public void openDetail(Staff entry) {
        focusedStaff = entry;
        detailPanelVisible = true;
    }

    public void closeDetail() {
        detailPanelVisible = false;
        focusedStaff = null;
    }

    // Anlegen
    public void openCreationPanel() {
        formStaff = new Staff();
        formStaff.setActive(1);
        selectedManagerId = null;
        creationPanelVisible = true;
    }

    public void closeCreationPanel() {
        creationPanelVisible = false;
        formStaff = null;
        selectedManagerId = null;
    }

    public void saveStaff() {
        try {
            staffService.saveStaff(formStaff, selectedManagerId);
            closeCreationPanel();
            totalCount = staffService.countAllStaff();
            managerList = staffService.getAllStaffAsManagers();
            reload();
            addInfo("Mitarbeiter wurde angelegt");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Mitarbeiter konnte nicht gespeichert werden");
        }
    }

    // Bearbeiten
    public void openEditPanel(Staff entry) {
        // ID separat merken (Staff hat kein setStaffId)
        editStaffId = entry.getStaffId();
        // Kopie anlegen damit Abbrechen die Tabelle nicht verändert
        editStaff = new Staff();
        editStaff.setFirstName(entry.getFirstName());
        editStaff.setLastName(entry.getLastName());
        editStaff.setEmail(entry.getEmail());
        editStaff.setPhone(entry.getPhone());
        editStaff.setStoreId(entry.getStoreId());
        editStaff.setActive(entry.getActive());
        editManagerId = (entry.getManager() != null) ? entry.getManager().getStaffId() : null;
        editPanelVisible = true;
    }

    public void closeEditPanel() {
        editPanelVisible = false;
        editStaff = null;
        editStaffId = null;
        editManagerId = null;
    }

    public void updateStaff() {
        try {
            staffService.updateStaff(editStaffId, editStaff, editManagerId);
            closeEditPanel();
            managerList = staffService.getAllStaffAsManagers();
            reload();
            addInfo("Mitarbeiter wurde aktualisiert");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Mitarbeiter konnte nicht gespeichert werden");
        }
    }

    // Deaktivieren / Aktivieren - selectedIdToDelete-Muster für JSF
    private Integer selectedIdToToggle;

    public void deactivateStaff() {
        if (selectedIdToToggle == null)
            return;
        try {
            staffService.deactivateStaff(selectedIdToToggle);
            reload();
            addInfo("Mitarbeiter wurde deaktiviert");
        } catch (Exception e) {
            addError("Fehler beim Deaktivieren");
        } finally {
            selectedIdToToggle = null;
        }
    }

    public void activateStaff() {
        if (selectedIdToToggle == null)
            return;
        try {
            staffService.activateStaff(selectedIdToToggle);
            reload();
            addInfo("Mitarbeiter wurde aktiviert");
        } catch (Exception e) {
            addError("Fehler beim Aktivieren");
        } finally {
            selectedIdToToggle = null;
        }
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
    public List<Staff> getStaffList() {
        return staffList;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int p) {
        this.page = p;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int r) {
        this.page = 0;
        this.rowsPerPage = r;
        reload();
    }

    public StaffSortField getSortField() {
        return sortField;
    }

    public void setSortField(StaffSortField f) {
        this.sortField = f != null ? f : StaffSortField.LAST_NAME;
    }

    public StaffSortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(StaffSortDirection d) {
        this.sortDirection = d != null ? d : StaffSortDirection.ASC;
    }

    public Staff getFocusedStaff() {
        return focusedStaff;
    }

    public void setFocusedStaff(Staff s) {
        this.focusedStaff = s;
    }

    public boolean isDetailPanelVisible() {
        return detailPanelVisible;
    }

    public void setDetailPanelVisible(boolean v) {
        this.detailPanelVisible = v;
    }

    public Staff getFormStaff() {
        return formStaff;
    }

    public void setFormStaff(Staff s) {
        this.formStaff = s;
    }

    public boolean isCreationPanelVisible() {
        return creationPanelVisible;
    }

    public void setCreationPanelVisible(boolean v) {
        this.creationPanelVisible = v;
    }

    public Integer getSelectedManagerId() {
        return selectedManagerId;
    }

    public void setSelectedManagerId(Integer id) {
        this.selectedManagerId = id;
    }

    public List<Staff> getManagerList() {
        return managerList;
    }

    public Staff getEditStaff() {
        return editStaff;
    }

    public void setEditStaff(Staff s) {
        this.editStaff = s;
    }

    public Integer getEditStaffId() {
        return editStaffId;
    }

    public void setEditStaffId(Integer id) {
        this.editStaffId = id;
    }

    public boolean isEditPanelVisible() {
        return editPanelVisible;
    }

    public void setEditPanelVisible(boolean v) {
        this.editPanelVisible = v;
    }

    public Integer getEditManagerId() {
        return editManagerId;
    }

    public void setEditManagerId(Integer id) {
        this.editManagerId = id;
    }

    public Integer getSelectedIdToToggle() {
        return selectedIdToToggle;
    }

    public void setSelectedIdToToggle(Integer id) {
        this.selectedIdToToggle = id;
    }
}
