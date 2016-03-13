package com.client.service;

import org.apache.log4j.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 21/10/15
 * Time: 17:28
 * To change this template use File | Settings | File Templates.
 */
public class LoginManager implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    public String login(String userName, String userPassword)
    {
        String loginErrorMessage = EscenicService.isEscenicUserValid(userName, userPassword);

        if (loginErrorMessage != null)
        {
            mLogger.error("login failed. UserName: " + userName + ", Password: " + userPassword);

            return loginErrorMessage;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        session.setAttribute("isAuthenticated", new Boolean(true));

        EscenicService escenicService = new EscenicService(userName, userPassword);
        session.setAttribute("escenicService", escenicService);

        return null;
    }

    public void logout()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        session.setAttribute("isAuthenticated", new Boolean(false));
        session.setAttribute("escenicService", null);
    }

    public boolean isAuthenticated()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        Object oIsAuthenticated = session.getAttribute("isAuthenticated");
        if (oIsAuthenticated == null)
            return false;

        Boolean isAuthenticated = (Boolean) oIsAuthenticated;
        if (isAuthenticated)
            return true;
        else
            return false;
    }

}
