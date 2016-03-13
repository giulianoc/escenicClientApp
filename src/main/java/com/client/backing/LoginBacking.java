package com.client.backing;

import com.client.service.EscenicService;
import com.client.service.LoginManager;
import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 21/10/15
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */
@SessionScoped
@ManagedBean
public class LoginBacking implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private String name;
    private String password;


    private LoginManager loginManager = new LoginManager();


    public String login()
    {
        mLogger.info("loginManager.login(" + name + ", " + password + ")");

        String loginErrorMessage = loginManager.login(name, password);
        if (loginErrorMessage == null)
        {

            // return "/protected/pages/articlesBrowser?faces-redirect=true";
            return "/protected/pages/escenicBrowser?faces-redirect=true";
        }
        else
        {
            mLogger.error("Login failed: " + loginErrorMessage);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Login", "Login failed: " + loginErrorMessage);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return "";
        }
    }

    public String logout()
    {
        mLogger.info("loginManager.logout()");

        loginManager.logout();

        {
            EscenicService escenicService;

            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                escenicService = (EscenicService) session.getAttribute("escenicService");

                if (!isAuthenticated || escenicService == null)
                {
                    mLogger.error("It should never happen this error. isAuthenticated: " + isAuthenticated + ", escenicService: " + escenicService);


                    return "/login?faces-redirect=true";
                }
            }

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Logout", "Logout. UserName: " + escenicService.getUserName());
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }

        return "/login?faces-redirect=true";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
