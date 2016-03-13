package com.client.backing;

import com.client.backing.model.ArticleTabs.EscenicTab;
import com.client.backing.model.ArticleTabs.Article;
import com.client.backing.model.SearchResult.ArticleTableData;
import com.client.service.EscenicService;
import org.apache.log4j.Logger;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabCloseEvent;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 25/10/15
 * Time: 08:42
 * To change this template use File | Settings | File Templates.
 */
@ManagedBean
@ViewScoped
public class ArticlesTabsBacking extends TimerTask implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private List<EscenicTab> tabs;
    private int activeTabIndex;
    private EscenicService savedEscenicService; // used only in the run (timer) method

    private int maxTabsNumber = 40;

    @ManagedProperty(value="#{lazySearchResultViewBacking}")
    LazySearchResultViewBacking lazySearchResultViewBacking;


    public void setLazySearchResultViewBacking(LazySearchResultViewBacking lazySearchResultViewBacking) {
        this.lazySearchResultViewBacking = lazySearchResultViewBacking;
    }

    @PostConstruct
    public void init() {
        tabs = new ArrayList<>();

        {
            FacesContext context = FacesContext.getCurrentInstance();
            mLogger.info("context: " + context);
            mLogger.info("context.getExternalContext: " + context.getExternalContext());
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
            mLogger.info("session: " + session);

            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            savedEscenicService = (EscenicService) session.getAttribute("escenicService");

            if (!isAuthenticated || savedEscenicService == null)
            {
                mLogger.error("It should never happen this error. isAuthenticated: " + isAuthenticated + ", escenicService: " + savedEscenicService);


                return;
            }
        }

        Timer checkArticleUpdatesTimer = new Timer();

        checkArticleUpdatesTimer.schedule(this, 1000 * 10, 1000 * 10);
    }

    @Override
    public void run()
    {
        try {
            mLogger.info("Checking active content if updated. activeTabIndex: " + activeTabIndex);

            if (activeTabIndex >= 0 && activeTabIndex < tabs.size())
            {
                EscenicTab activeEscenicTab = tabs.get(activeTabIndex);
                Article activeArticle = activeEscenicTab.getArticle();

                if (activeArticle == null)
                {
                    mLogger.error("activeArticle is null");

                    return;
                }

                activeArticle.refreshLocks(savedEscenicService);

//                I cannot get the session here because I do not have any HTTP request
//                and, in this case, FacesContext.getCurrentInstance is null
//                EscenicService escenicService;
//
//                {
//                    FacesContext context = FacesContext.getCurrentInstance();
//                    mLogger.info("context: " + context);
//                    mLogger.info("context.getExternalContext: " + context.getExternalContext());
//                    HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
//                    mLogger.info("session: " + session);
//
//                    Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
//                    escenicService = (EscenicService) session.getAttribute("escenicService");
//
//                    if (!isAuthenticated || escenicService == null)
//                    {
//                        mLogger.error("It should never happen this error. isAuthenticated: " + isAuthenticated + ", escenicService: " + escenicService);
//
//
//                        return;
//                    }
//                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void deleteArticle()
    {
        try {
            mLogger.info("Deleting content...");

            if (activeTabIndex >= 0 && activeTabIndex < tabs.size())
            {
                EscenicTab activeEscenicTab = tabs.get(activeTabIndex);
                Article activeArticle = activeEscenicTab.getArticle();

                mLogger.info("Active article deleting...");
                {
                    EscenicService escenicService;

                    FacesContext context = FacesContext.getCurrentInstance();
                    HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                    // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                    escenicService = (EscenicService) session.getAttribute("escenicService");

                    escenicService.deleteArticle(activeArticle);

                    tabs.remove(activeTabIndex);

                    // setActiveTabIndex(tabs.size() - 1);
                }
            }
            else
            {
                mLogger.error("No content active in the view. activeTabIndex: " + activeTabIndex);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Delete Content",
                        "No content active in the view. activeTabIndex: " + activeTabIndex);
                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void saveArticle()
    {
        try {
            mLogger.info("Saving content...");

            if (activeTabIndex >= 0 && activeTabIndex < tabs.size())
            {
                EscenicTab activeEscenicTab = tabs.get(activeTabIndex);
                Article activeArticle = activeEscenicTab.getArticle();

                {
                    EscenicService escenicService;

                    FacesContext context = FacesContext.getCurrentInstance();
                    HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                    // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                    escenicService = (EscenicService) session.getAttribute("escenicService");

                    if (!escenicService.isAdministrator())
                    {
                        if (escenicService.getConfiguration().isReadOnly() ||
                            !escenicService.getConfiguration().isAllowedSection(activeArticle.getHomeEscenicSection().getUniqueName()))
                        {
                            mLogger.error("No rights to save the content");

                            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                    "Content", "No rights to save the content");
                            // FacesContext context = FacesContext.getCurrentInstance();
                            context.addMessage(null, message);

                            return;
                        }
                    }
                }

                {
                    mLogger.info("checking if at least one field was modified");

                    if (!activeArticle.isArticleModified())
                    {
                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Save Content",
                            "No field was modified");
                        FacesContext context = FacesContext.getCurrentInstance();
                        context.addMessage(null, message);

                        return;
                    }
                }

                mLogger.info("Active article saving...");
                activeArticle.save();
            }
            else
            {
                mLogger.error("No content active in the view. activeTabIndex: " + activeTabIndex);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "Save Content",
                    "No content active in the view. activeTabIndex: " + activeTabIndex);
                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void searchResultsRowSelect(SelectEvent event)
    {
        addTab(((ArticleTableData) event.getObject()).getObjectId());
    }

    public void addTab(Long objectId)
    {
        mLogger.info("addTab (objectId: " + objectId + ")");

        if (tabs.size() >= maxTabsNumber)
        {
            mLogger.error("Max number of tabs is reached. MaxTabsNumber: " + maxTabsNumber);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Contents tabs", "Reached the max number of tabs. Call the administrator if more tabs are needed");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        // ArticleTableData articleTableData = lazySearchResultViewBacking.getSelectedArticleTableData();
        // objectId = articleTableData.getObjectId();

        // mLogger.error(articleTableData.toString());
        int tabIndex = lookForTab (String.valueOf(objectId));

        mLogger.info("addTab. tabIndex: " + tabIndex + ", id: " + objectId);
        if (tabIndex == -1)
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


                    return;
                }
            }

            Article detailedArticle = escenicService.getArticleContentDetails(
                String.valueOf(objectId), true);

            if (detailedArticle == null)
            {
                mLogger.error("detailedArticle is null. Id: " + objectId);

                return;
            }

            EscenicTab escenicTab = new EscenicTab();
            escenicTab.setArticle(detailedArticle);
            tabs.add(escenicTab);

            setActiveTabIndex(tabs.size() - 1);
        }
        else
        {
            setActiveTabIndex(tabIndex);
        }
    }

    public void addTab(Article article)
    {
        if (tabs.size() >= maxTabsNumber)
        {
            mLogger.error("Max number of tabs is reached. MaxTabsNumber: " + maxTabsNumber);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Contents tabs", "Reached the max number of tabs. Call the administrator if more tabs are needed");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        int tabIndex = lookForTab (article.getMetadataFields().get("com.escenic.displayId").getStringValue());

        mLogger.info("addTab. tabIndex: " + tabIndex + ", id: " + article.getMetadataFields().get("com.escenic.displayId").getStringValue());
        if (tabIndex == -1)
        {
            EscenicTab escenicTab = new EscenicTab();
            escenicTab.setArticle(article);
            tabs.add(escenicTab);

            setActiveTabIndex(tabs.size() - 1);
        }
        else
        {
            setActiveTabIndex(tabIndex);
        }
    }

    /*
    public void refreshLocks()
    {
        mLogger.info("Received refreshLocks");


        if (activeTabIndex >= tabs.size())
        {
            mLogger.error("Wrong activeTabIndex: " + activeTabIndex + ", tabs.size: " + tabs.size());

            return;
        }

        EscenicTab activeEscenicTab = tabs.get(activeTabIndex);
        Article activeArticle = activeEscenicTab.getArticle();

        activeArticle.refreshLocks();
    }
    */

    public void refreshArticle()
    {
        mLogger.info("Received refreshed");


        if (activeTabIndex >= tabs.size())
        {
            mLogger.error("Wrong activeTabIndex: " + activeTabIndex + ", tabs.size: " + tabs.size());

            return;
        }

        EscenicTab activeEscenicTab = tabs.get(activeTabIndex);
        Article activeArticle = activeEscenicTab.getArticle();

        mLogger.info("Remove all the locks of the article. ID: " + activeArticle.getMetadataFields().get("com.escenic.displayId").getStringValue());
        activeArticle.removeLocks();

        mLogger.info("Reload the article. ID: " + activeArticle.getMetadataFields().get("com.escenic.displayId").getStringValue());
        activeArticle.reload();
    }

    private int lookForTab(String id)
    {
        int tabIndex = -1;

        for (int index = 0; index < tabs.size(); index++)
        {
            if (tabs.get(index).getId().equalsIgnoreCase(id))
            {
                tabIndex = index;

                break;
            }
        }

        return tabIndex;
    }

    /*
    private String getTitleForNewTab(String baseTitle)
    {
        int counter = 0;
        boolean tabTitleAlreadyUsed = false;
        String titleForNewTab = null;

        while (counter < maxTabsNumber)
        {
            tabTitleAlreadyUsed = false;

            if (counter == 0)
                titleForNewTab = baseTitle;
            else
                titleForNewTab = baseTitle + " (" + counter + ")";

            for (EscenicTab tab: tabs)
            {
                if (tab.getTitle().equalsIgnoreCase(titleForNewTab))
                {
                    tabTitleAlreadyUsed = true;

                    break;
                }
            }

            if (!tabTitleAlreadyUsed)
                break;

            counter++;
        }

        if (tabTitleAlreadyUsed)
            return null;
        else
            return titleForNewTab;
    }
    */

    public void remove(EscenicTab tab) {
        tabs.remove(tab);
    }

    public List<EscenicTab> getTabs() {
        return tabs;
    }

    public void setTabs(List<EscenicTab> tabs) {
        this.tabs = tabs;
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }

    public void onTabClose(TabCloseEvent event)
    {
        // mLogger.error("onTabClose. event.getTab().getTitle(): " + event.getTab().getTitle());

        for (EscenicTab escenicTab: tabs)
        {
            if (escenicTab.getTabHeaderTitle().equalsIgnoreCase(event.getTab().getTitle()))
            {
                Article activeArticle = escenicTab.getArticle();

                if (activeArticle != null)
                {
                    mLogger.info("Remove all the locks of the article. ID: " + activeArticle.getMetadataFields().get("com.escenic.displayId").getStringValue());
                    activeArticle.removeLocks();
                }

                tabs.remove(escenicTab);
                mLogger.info("Tab removed. Title: " + event.getTab().getTitle());

                break;
            }
        }
    }

    public void closeAll()
    {
        tabs.clear();
    }

    public void closeOthers()
    {
        if (activeTabIndex < 0 || activeTabIndex >= tabs.size())
        {
            mLogger.info("closeOthers. Wrong activeTabIndex: " + activeTabIndex);

            return;
        }

        EscenicTab escenicTabToBeRemained = tabs.get(activeTabIndex);
        int indexToBeRemoved = 0;
        boolean tabFound = false;

        for (EscenicTab escenicTab: tabs)
        {
            if (escenicTab == escenicTabToBeRemained)
            {
                tabFound = true;

                break;
            }

            indexToBeRemoved++;
        }

        if (tabFound)
        {
            if (indexToBeRemoved < 0 || indexToBeRemoved >= tabs.size())
            {
                mLogger.info("closeOthers. Wrong indexToBeRemoved: " + indexToBeRemoved);

                return;
            }

            tabs.remove(indexToBeRemoved);
            mLogger.info("Tab removed. Title: " + escenicTabToBeRemained.getTabHeaderTitle());
        }
        else
        {
            mLogger.error("Tab not found. Title: " + escenicTabToBeRemained.getTabHeaderTitle());
        }
    }
}
