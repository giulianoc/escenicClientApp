package com.client.service.util;

import com.client.service.LoginManager;
import org.apache.log4j.Logger;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.io.Serializable;

public class AuthorizationListener implements PhaseListener, Serializable {

    private final Logger mLogger = Logger.getLogger(AuthorizationListener.class);

    private LoginManager loginManager = new LoginManager();

    @Override
    public void afterPhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();
        NavigationHandler navigationHandler = context.getApplication().getNavigationHandler();

        
        String currentPage = context.getViewRoot().getViewId();
 
        boolean isProtectedPage = currentPage.contains("/protected/");
        boolean isAuthenticated = loginManager.isAuthenticated();

        mLogger.info("isProtectedPage: " + isProtectedPage + ", isAuthenticated: " + isAuthenticated);

        if (isProtectedPage && !isAuthenticated)
        {
            navigationHandler.handleNavigation(context, null, "/login?faces-redirect=true");
        }
    }

    @Override
    public void beforePhase(PhaseEvent event) {
        //Nothing ...
    }

    @Override
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}
