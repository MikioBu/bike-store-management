package de.sep.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * View-Bean für die Login-Seite.
 * Nutzt Jakarta Security SecurityContext um die Authentifizierung anzustoßen.
 */
@Named
@RequestScoped
public class LoginView {

    private String email;
    private String passwort;
    private boolean fehler = false;

    @Inject
    private SecurityContext securityContext;

    public void login() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        AuthenticationStatus status = securityContext.authenticate(
                (HttpServletRequest) ec.getRequest(),
                (HttpServletResponse) ec.getResponse(),
                AuthenticationParameters.withParams()
                        .credential(new UsernamePasswordCredential(email, passwort)));

        switch (status) {
            case SUCCESS -> {
                try {
                    ec.redirect(ec.getRequestContextPath() + "/startseite.xhtml");
                } catch (Exception e) {
                    fc.responseComplete();
                }
            }
            case SEND_FAILURE, NOT_DONE -> fehler = true;
            default -> fehler = true;
        }
    }

    public void logout() throws Exception {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        HttpServletRequest request = (HttpServletRequest) ec.getRequest();
        request.logout();
        request.getSession().invalidate();

        ec.redirect(ec.getRequestContextPath() + "/login.xhtml");
    }

    // Getter / Setter

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getPasswort() {
        return passwort;
    }

    public void setPasswort(String v) {
        this.passwort = v;
    }

    public boolean isFehler() {
        return fehler;
    }
}
