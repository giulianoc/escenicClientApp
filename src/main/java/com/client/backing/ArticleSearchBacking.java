package com.client.backing;

import com.client.service.EscenicService;
import com.client.backing.model.common.ContentType;
import com.client.backing.model.common.State;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 22/10/15
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */

@ManagedBean
@ViewScoped
public class ArticleSearchBacking implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private String dialogProfileName;
    private String selectedProfile;

    private String textToSearch;
    private String selectedTextToSearchType;
    private List<String> textToSearchTypeList;

    private String selectedPublications;
    private String selectedSectionsLabels;
    private String selectedSectionsIds;

    private Date publishStartTime;
    private Date publishEndTime;
    private Date expireStartTime;
    private Date expireEndTime;
    private Date lastModifiedStartTime;
    private Date lastModifiedEndTime;
    private Date activateStartTime;
    private Date activateEndTime;
    private Date startTime;
    private Date endTime;

    private boolean includeSubSections;

    private String selectedOrderBy;
    private List<String> orderByList;
    private String selectedOrderType;
    private List<String> orderTypeList;

    private String[] selectedContentTypes;
    private List<ContentType> contentTypes;
    private List<String> contentTypesLabels;

    private String[] selectedState;
    private List<State> states;
    private List<String> statesLabels;

    private String tagsURI;
    private String selectedTagsSearchType;
    private List<String> tagsSearchTypeList;

    private String infoMessage;

    public ArticleSearchBacking()
    {
        setIncludeSubSections(true);

        {
            states = new ArrayList<>();

            {
                State state = new State();
                state.setLabel("Draft");
                state.setState("draft");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Submitted");
                state.setState("submitted");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Approved");
                state.setState("approved");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Published");
                state.setState("published");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Draft (with published)");
                state.setState("draft-published");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Submitted (with published)");
                state.setState("submitted-published");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Approved (with published)");
                state.setState("approved-published");

                states.add(state);
            }

            {
                State state = new State();
                state.setLabel("Deleted");
                state.setState("deleted");

                states.add(state);
            }

            statesLabels = new ArrayList<>();
            for (State state : states)
            {
                statesLabels.add(state.getLabel());
            }
        }

        {
            // setSelectedState(getStates().toArray(new String[getStates().size()]));

            // All minus Deleted (it is equivalent to 'active')
            String [] statesSelected = new String[7];
            statesSelected[0] = "Draft";
            statesSelected[1] = "Submitted";
            statesSelected[2] = "Approved";
            statesSelected[3] = "Published";
            statesSelected[4] = "Draft (with published)";
            statesSelected[5] = "Submitted (with published)";
            statesSelected[6] = "Approved (with published)";

            setSelectedState(statesSelected);
        }

        {
            Calendar calendar = Calendar.getInstance();
            Date now = calendar.getTime();
            calendar.add(Calendar.YEAR, 20);
            Date future = calendar.getTime();
            Date unixEpoch = new Date(0);

            // setPublishStartTime(unixEpoch);
            // setPublishEndTime(now);

            // setExpireStartTime(now);
            // setExpireEndTime(future);

            // setActivateStartTime(unixEpoch);
            // setActivateEndTime(now);
        }

        orderByList = new ArrayList<String>();
        orderByList.add("Published Date");
        orderByList.add("Start Date");
        orderByList.add("Id");
        orderByList.add("Creator Username");
        orderByList.add("Last Edited By");
        orderByList.add("State");
        orderByList.add("Content Type");
        orderByList.add("Creation Date");
        orderByList.add("Activation Date");
        orderByList.add("Expiration Date");
        orderByList.add("Last Modified Date");

        orderTypeList = new ArrayList<String>();
        orderTypeList.add("Ascending");
        orderTypeList.add("Descending");

        setSelectedOrderType("Descending");

        textToSearchTypeList = new ArrayList<>();
        textToSearchTypeList.add("All");
        textToSearchTypeList.add("AtLeastOne");
        textToSearchTypeList.add("ExactPhrase");
        textToSearchTypeList.add("Without");

        setSelectedTextToSearchType("All");

        tagsSearchTypeList = new ArrayList<>();
        tagsSearchTypeList.add("All");
        tagsSearchTypeList.add("AtLeastOne");

        // content types
        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            contentTypes = escenicService.getAllContentTypes();

            contentTypesLabels = new ArrayList<>();
            for (ContentType contentType : contentTypes)
            {
                contentTypesLabels.add(contentType.getLabel());
            }
            Collections.sort(contentTypesLabels);
        }
    }

    public void saveProfile()
    {
        String profilePathName = "";

        try {
            mLogger.info("saveProfile ...");

            if (getDialogProfileName() == null || getDialogProfileName().equalsIgnoreCase(""))
            {
                mLogger.error("Write profile error. profileName: " + getDialogProfileName());

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Saving Profile failed.");
                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            String rsiInternalAPPPath;
            String profilesDirectory;
            ArticleBrowserBacking articleBrowserBacking;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                rsiInternalAPPPath = escenicService.getRsiInternalAPPPath();

                profilesDirectory = (rsiInternalAPPPath + "/searchProfiles/" + escenicService.getUserName());

                File file = new File(profilesDirectory);
                FileUtils.forceMkdir(file);

                ValueExpression valueExpression = context.getApplication().getExpressionFactory().createValueExpression(
                    context.getELContext(), "#{articleBrowserBacking}", ArticleBrowserBacking.class);
                articleBrowserBacking = (ArticleBrowserBacking) valueExpression.getValue(context.getELContext());
            }

            profilePathName = profilesDirectory + "/" + getDialogProfileName() + ".search";

            Properties properties = new Properties();

            properties.setProperty("TextToSearch", getTextToSearch());
            properties.setProperty("TextToSearchType", getSelectedTextToSearchType());
            properties.setProperty("States", Arrays.toString(getSelectedState()));
            properties.setProperty("ContentTypes", Arrays.toString(getSelectedContentTypes()));

            {
                if (getSelectedPublications() == null)
                    properties.setProperty("Publication", "");
                else
                    properties.setProperty("Publication", getSelectedPublications());

                if (getSelectedSectionsLabels() == null)
                    properties.setProperty("SectionsLabels", "");
                else
                    properties.setProperty("SectionsLabels", getSelectedSectionsLabels());

                if (getSelectedSectionsIds() == null)
                    properties.setProperty("SectionsIds", "");
                else
                    properties.setProperty("SectionsIds", getSelectedSectionsIds());
            }

            properties.setProperty("IncludeSubSections", String.valueOf(isIncludeSubSections()));
            properties.setProperty("OrderBy", getSelectedOrderBy());
            properties.setProperty("OrderType", getSelectedOrderType());
            if (getPublishStartTime() != null)
                properties.setProperty("PublishStart", simpleDateFormat.format(getPublishStartTime()));
            else
                properties.setProperty("PublishStart", "");
            if (getPublishEndTime() != null)
                properties.setProperty("PublishEnd", simpleDateFormat.format(getPublishEndTime()));
            else
                properties.setProperty("PublishEnd", "");
            if (getExpireStartTime() != null)
                properties.setProperty("ExpireStart", simpleDateFormat.format(getExpireStartTime()));
            else
                properties.setProperty("ExpireStart", "");
            if (getExpireEndTime() != null)
                properties.setProperty("ExpireEnd", simpleDateFormat.format(getExpireEndTime()));
            else
                properties.setProperty("ExpireEnd", "");
            if (getLastModifiedStartTime() != null)
                properties.setProperty("LastModifiedStart", simpleDateFormat.format(getLastModifiedStartTime()));
            else
                properties.setProperty("LastModifiedStart", "");
            if (getLastModifiedEndTime() != null)
                properties.setProperty("LastModifiedEnd", simpleDateFormat.format(getLastModifiedEndTime()));
            else
                properties.setProperty("LastModifiedEnd", "");
            if (getActivateStartTime() != null)
                properties.setProperty("ActivateStart", simpleDateFormat.format(getActivateStartTime()));
            else
                properties.setProperty("ActivateStart", "");
            if (getActivateEndTime() != null)
                properties.setProperty("ActivateEnd", simpleDateFormat.format(getActivateEndTime()));
            else
                properties.setProperty("ActivateEnd", "");
            if (getStartTime() != null)
                properties.setProperty("Start", simpleDateFormat.format(getStartTime()));
            else
                properties.setProperty("Start", "");
            if (getEndTime() != null)
                properties.setProperty("End", simpleDateFormat.format(getEndTime()));
            else
                properties.setProperty("End", "");
            properties.setProperty("TagsURI", getTagsURI());
            properties.setProperty("TagsSearchType", getSelectedTagsSearchType());

            mLogger.info("Saving profile. ProfilePathName: " + profilePathName);

            File file = new File(profilePathName);
            OutputStream out = new FileOutputStream(file);

            properties.store(out, "Article search parameters");
        }
        catch (Exception e)
        {
            mLogger.error("Write profile error. profilePathName: " + profilePathName + ", Exception: " + e.getMessage());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Saving Profile failed.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Profile", "Profile saved successfully.");
        // RequestContext.getCurrentInstance().showMessageInDialog(message);
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, message);
    }

    public void deleteProfile()
    {
        mLogger.info("deleteProfile ...");

        if (getSelectedProfile() == null || getSelectedProfile().equalsIgnoreCase(""))
        {
            mLogger.error("Delete profile error. profileName: " + getSelectedProfile());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Deleting Profile failed.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        String rsiInternalAPPPath;
        String profilesDirectory;

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            rsiInternalAPPPath = escenicService.getRsiInternalAPPPath();

            profilesDirectory = (rsiInternalAPPPath + "/searchProfiles/" + escenicService.getUserName());
        }

        String profilePathName = profilesDirectory + "/" + getSelectedProfile() + ".search";

        mLogger.info("Deleting profile. ProfilePathName: " + profilePathName);

        File file = new File(profilePathName);
        try {
            FileUtils.forceDelete(file);
        }
        catch (Exception e)
        {
            mLogger.error("Delete profile error. profilePathName: " + profilePathName + ", Exception: " + e.getMessage());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Deleting Profile failed.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Profile", "Profile removed successfully.");
        // RequestContext.getCurrentInstance().showMessageInDialog(message);
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, message);
    }

    public List<String> getProfiles()
    {
        mLogger.info("getProfiles ...");

        String rsiInternalAPPPath;
        String profilesDirectory;

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            rsiInternalAPPPath = escenicService.getRsiInternalAPPPath();

            profilesDirectory = (rsiInternalAPPPath + "/searchProfiles/" + escenicService.getUserName());
        }

        List<String> results = new ArrayList<String>();
        String fileName;
        File[] files = new File(profilesDirectory).listFiles();

        if (files != null)
        {
            for (File file: files)
            {
                fileName = file.getName();

                if (file.isFile() && fileName.endsWith(".search"))
                {
                    results.add(fileName.substring(0, fileName.length() - ".search".length()));
                }
            }
        }

        return results;
    }

    public void loadProfile()
    {
        mLogger.info("setProfile: " + getSelectedProfile());

        if (getSelectedProfile() == null || getSelectedProfile().equalsIgnoreCase(""))
        {
            mLogger.error("Write profile error. profileName: " + getSelectedProfile());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Loading Profile failed.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        String rsiInternalAPPPath;
        String profilesDirectory;
        ArticleBrowserBacking articleBrowserBacking;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            rsiInternalAPPPath = escenicService.getRsiInternalAPPPath();

            profilesDirectory = (rsiInternalAPPPath + "/searchProfiles/" + escenicService.getUserName());

            try {
                File file = new File(profilesDirectory);
                FileUtils.forceMkdir(file);
            }
            catch (Exception e)
            {
                mLogger.error("Mkdir error. profilesDirectory: " + profilesDirectory + ", Exception: " + e.getMessage());

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Saving Profile failed.");
                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                FacesContext contextAgain = FacesContext.getCurrentInstance();
                contextAgain.addMessage(null, message);
            }

            ValueExpression valueExpression = context.getApplication().getExpressionFactory().createValueExpression(
                    context.getELContext(), "#{articleBrowserBacking}", ArticleBrowserBacking.class);
            articleBrowserBacking = (ArticleBrowserBacking) valueExpression.getValue(context.getELContext());
        }

        String profilePathName = profilesDirectory + "/" + getSelectedProfile() + ".search";

        mLogger.info("Loading profile. ProfilePathName: " + profilePathName);

        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(profilePathName));

            setTextToSearch(properties.getProperty("TextToSearch"));
            setSelectedTextToSearchType(properties.getProperty("TextToSearchType"));

            String [] selectedState = properties.getProperty("States").replace("[", "").replace("]", "").split(", ");
            if (selectedState == null || (selectedState.length == 1 && selectedState[0].trim().equalsIgnoreCase("")))
                setSelectedState(null);
            else
                setSelectedState(selectedState);

            String [] selectedContentTypes = properties.getProperty("ContentTypes").replace("[", "").replace("]", "").split(", ");
            if (selectedContentTypes == null || (selectedContentTypes.length == 1 && selectedContentTypes[0].trim().equalsIgnoreCase("")))
                setSelectedContentTypes(null);
            else
                setSelectedContentTypes(selectedContentTypes);

            {
                setSelectedPublications(properties.getProperty("Publication"));

                setSelectedSectionsLabels(properties.getProperty("SectionsLabels"));
                setSelectedSectionsIds(properties.getProperty("SectionsIds"));
            }

            setIncludeSubSections(Boolean.valueOf(properties.getProperty("IncludeSubSections")));
            setSelectedOrderBy(properties.getProperty("OrderBy"));
            setSelectedOrderType(properties.getProperty("OrderType"));

            String dateTime = properties.getProperty("PublishStart");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setPublishStartTime (simpleDateFormat.parse(dateTime));
            else
                setPublishStartTime (null);

            dateTime = properties.getProperty("PublishEnd");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setPublishEndTime(simpleDateFormat.parse(dateTime));
            else
                setPublishEndTime(null);

            dateTime = properties.getProperty("ExpireStart");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setExpireStartTime(simpleDateFormat.parse(dateTime));
            else
                setExpireStartTime(null);

            dateTime = properties.getProperty("ExpireEnd");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setExpireEndTime(simpleDateFormat.parse(dateTime));
            else
                setExpireEndTime(null);

            dateTime = properties.getProperty("LastModifiedStart");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setLastModifiedStartTime(simpleDateFormat.parse(dateTime));
            else
                setLastModifiedStartTime(null);

            dateTime = properties.getProperty("LastModifiedEnd");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setLastModifiedEndTime(simpleDateFormat.parse(dateTime));
            else
                setLastModifiedEndTime(null);

            dateTime = properties.getProperty("ActivateStart");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setActivateStartTime(simpleDateFormat.parse(dateTime));
            else
                setActivateStartTime(null);

            dateTime = properties.getProperty("ActivateEnd");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setActivateEndTime(simpleDateFormat.parse(dateTime));
            else
                setActivateEndTime(null);

            dateTime = properties.getProperty("Start");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setStartTime(simpleDateFormat.parse(dateTime));
            else
                setStartTime(null);

            dateTime = properties.getProperty("End");
            if (dateTime != null && !dateTime.equalsIgnoreCase(""))
                setEndTime(simpleDateFormat.parse(dateTime));
            else
                setEndTime(null);

            setTagsURI(properties.getProperty("TagsURI"));
            setSelectedTagsSearchType(properties.getProperty("TagsSearchType"));
        }
        catch (Exception e)
        {
            mLogger.error("Load profile error. profilePathName: " + profilePathName + ", Exception: " + e.getMessage());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Profile", "Loading Profile failed.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }

        // in order to have again the 'Select profile' entry
        setSelectedProfile(null);

        // FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Profile", "Profile loaded successfully.");
        // RequestContext.getCurrentInstance().showMessageInDialog(message);
    }

    public void reset()
    {
        setTextToSearch("");
        setSelectedTextToSearchType("All");

        {
            // All minus Deleted (it is equivalent to 'active')
            String [] statesSelected = new String[7];
            statesSelected[0] = "Draft";
            statesSelected[1] = "Submitted";
            statesSelected[2] = "Approved";
            statesSelected[3] = "Published";
            statesSelected[4] = "Draft (with published)";
            statesSelected[5] = "Submitted (with published)";
            statesSelected[6] = "Approved (with published)";

            setSelectedState(statesSelected);
        }

        setSelectedContentTypes(null);

        setSelectedPublications(null);

        setSelectedSectionsLabels(null);
        setSelectedSectionsIds(null);
        setIncludeSubSections(true);

        setSelectedOrderBy("Published Date");
        setSelectedOrderType("Descending");

        setPublishStartTime(null);
        setPublishEndTime(null);

        setExpireStartTime(null);
        setExpireEndTime(null);

        setLastModifiedStartTime(null);
        setLastModifiedEndTime(null);

        setActivateStartTime(null);
        setActivateEndTime(null);

        setStartTime(null);
        setEndTime(null);

        setTagsURI(null);
        setSelectedTagsSearchType("All");
    }

    public String getDialogProfileName() {
        return dialogProfileName;
    }

    public void setDialogProfileName(String dialogProfileName) {
        this.dialogProfileName = dialogProfileName;
    }

    public String getSelectedProfile() {
        // mLogger.info("getSelectedProfile: " + selectedProfile);
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

    public String getTextToSearch() {
        // mLogger.info("getTextToSearch. textToSearch: " + textToSearch);
        return textToSearch;
    }

    public void setTextToSearch(String textToSearch) {
        // mLogger.info("setTextToSearch. textToSearch: " + textToSearch);
        this.textToSearch = textToSearch;
    }

    public String getSelectedTextToSearchType() {
        // mLogger.info("getSelectedTextToSearchType. selectedTextToSearchType: " + selectedTextToSearchType);
        return selectedTextToSearchType;
    }

    public void setSelectedTextToSearchType(String selectedTextToSearchType) {
        this.selectedTextToSearchType = selectedTextToSearchType;
    }

    public String getSelectedPublications() {
        return selectedPublications;
    }

    public void setSelectedPublications(String selectedPublications) {
        this.selectedPublications = selectedPublications;
        // mLogger.info("selectedPublications: " + selectedPublications);
    }

    public String getSelectedSectionsLabels() {
        return selectedSectionsLabels;
    }

    public void setSelectedSectionsLabels(String selectedSectionsLabels) {
        this.selectedSectionsLabels = selectedSectionsLabels;
        // mLogger.info("selectedSectionsLabels: " + selectedSectionsLabels);
    }

    public String getSelectedSectionsIds() {
        return selectedSectionsIds;
    }

    public void setSelectedSectionsIds(String selectedSectionsIds) {
        this.selectedSectionsIds = selectedSectionsIds;
        // mLogger.info("selectedSectionsIds: " + selectedSectionsIds);
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public void setInfoMessage(String infoMessage) {
        this.infoMessage = infoMessage;
    }

    public Date getPublishStartTime() {
        return publishStartTime;
    }

    public void setPublishStartTime(Date publishStartTime) {
        this.publishStartTime = publishStartTime;
    }

    public Date getPublishEndTime() {
        return publishEndTime;
    }

    public void setPublishEndTime(Date publishEndTime) {
        this.publishEndTime = publishEndTime;
    }

    public Date getExpireStartTime() {
        return expireStartTime;
    }

    public void setExpireStartTime(Date expireStartTime) {
        this.expireStartTime = expireStartTime;
    }

    public Date getExpireEndTime() {
        return expireEndTime;
    }

    public void setExpireEndTime(Date expireEndTime) {
        this.expireEndTime = expireEndTime;
    }

    public Date getLastModifiedStartTime() {
        return lastModifiedStartTime;
    }

    public void setLastModifiedStartTime(Date lastModifiedStartTime) {
        this.lastModifiedStartTime = lastModifiedStartTime;
    }

    public Date getLastModifiedEndTime() {
        return lastModifiedEndTime;
    }

    public void setLastModifiedEndTime(Date lastModifiedEndTime) {
        this.lastModifiedEndTime = lastModifiedEndTime;
    }

    public Date getActivateStartTime() {
        return activateStartTime;
    }

    public void setActivateStartTime(Date activateStartTime) {
        this.activateStartTime = activateStartTime;
    }

    public Date getActivateEndTime() {
        return activateEndTime;
    }

    public void setActivateEndTime(Date activateEndTime) {
        this.activateEndTime = activateEndTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public boolean isIncludeSubSections() {
        // mLogger.info("isIncludeSubSections. includeSubSections: " + includeSubSections);
        return includeSubSections;
    }

    public void setIncludeSubSections(boolean includeSubSections) {
        // mLogger.info("setIncludeSubSections. includeSubSections: " + includeSubSections);
        this.includeSubSections = includeSubSections;
    }

    public String[] getSelectedContentTypes() {
        // mLogger.info("getSelectedContentTypes: " + selectedContentTypes);
        return selectedContentTypes;
    }

    public void setSelectedContentTypes(String[] selectedContentTypes)
    {
        if (selectedContentTypes == null)
            this.selectedContentTypes = new String[0];
        else
            this.selectedContentTypes = selectedContentTypes;
    }

    public List<ContentType> getContentTypes() {
        return contentTypes;
    }

    public List<String> getContentTypesLabels()
    {
        return contentTypesLabels;
    }

    public String[] getSelectedState() {
        return selectedState;
    }

    public void setSelectedState(String[] selectedState) {
        if (selectedState == null)
            this.selectedState = new String[0];
        else
            this.selectedState = selectedState;
    }

    public List<String> getStatesLabels()
    {
        return statesLabels;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    public String getSelectedOrderBy() {
        // mLogger.info("getSelectedOrderBy. selectedOrderBy: " + selectedOrderBy);
        return selectedOrderBy;
    }

    public void setSelectedOrderBy(String selectedOrderBy) {
        this.selectedOrderBy = selectedOrderBy;
    }

    public List<String> getOrderByList() {
        return orderByList;
    }

    public void setOrderByList(List<String> orderByList) {
        this.orderByList = orderByList;
    }

    public String getSelectedOrderType() {
        // mLogger.info("getSelectedOrderType. selectedOrderType: " + selectedOrderType);
        return selectedOrderType;
    }

    public void setSelectedOrderType(String selectedOrderType) {
        this.selectedOrderType = selectedOrderType;
    }

    public List<String> getOrderTypeList() {
        return orderTypeList;
    }

    public void setOrderTypeList(List<String> orderTypeList) {
        this.orderTypeList = orderTypeList;
    }

    public String getTagsURI() {
        // mLogger.info("getTagsURI. tagsURI: " + tagsURI);
        return tagsURI;
    }

    public void setTagsURI(String tagsURI) {
        this.tagsURI = tagsURI;
    }

    public String getSelectedTagsSearchType() {
        // mLogger.info("getSelectedTagsSearchType. selectedTagsSearchType: " + selectedTagsSearchType);
        return selectedTagsSearchType;
    }

    public void setSelectedTagsSearchType(String selectedTagsSearchType) {
        this.selectedTagsSearchType = selectedTagsSearchType;
    }

    public List<String> getTextToSearchTypeList() {
        return textToSearchTypeList;
    }

    public void setTextToSearchTypeList(List<String> textToSearchTypeList) {
        this.textToSearchTypeList = textToSearchTypeList;
    }

    public List<String> getTagsSearchTypeList() {
        return tagsSearchTypeList;
    }

    public void setTagsSearchTypeList(List<String> tagsSearchTypeList) {
        this.tagsSearchTypeList = tagsSearchTypeList;
    }

}
