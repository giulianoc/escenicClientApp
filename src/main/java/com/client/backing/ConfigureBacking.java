package com.client.backing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.client.backing.model.Configuration.Configuration;
import com.client.backing.model.Configuration.FieldConfiguration;
import com.client.service.EscenicField;
import com.client.service.EscenicService;
import com.client.service.EscenicType;
import com.client.service.util.HttpGetInfo;
import com.client.backing.model.common.ContentType;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.FlowEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 20/12/15
 * Time: 12:40
 * To change this template use File | Settings | File Templates.
 */
@ManagedBean
@ViewScoped
public class ConfigureBacking implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private String rsiInternalAPPPath;
    private String configurationsDirectory;

    private boolean skip;

    private List<String> groupNames = new ArrayList<>();
    private String selectedGroupName;

    private String newGroupName;
    private String userNameToBeAddedOrRemoved;
    private String sectionUniqueName;

    private Configuration configuration = new Configuration();

    private List<ContentType> contentTypesToBeSelected = new ArrayList<>();


    private HashMap<String,HashMap<String,EscenicType>> contentTypeModels = new HashMap<>();

    @ManagedProperty(value="#{articleSearchBacking}")
    private ArticleSearchBacking articleSearchBacking;


    @PostConstruct
    public void init()
    {
        {
            List<ContentType> contentTypes = articleSearchBacking.getContentTypes();

            contentTypesToBeSelected.clear();

            for (ContentType contentType : contentTypes)
            {
                // if (!contentType.getType().startsWith("widget_"))
                contentTypesToBeSelected.add(contentType);
            }
        }

        // groupNames
        try
        {
            groupNames.clear();

            {
                FacesContext context = FacesContext.getCurrentInstance();

                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");
                rsiInternalAPPPath = escenicService.getRsiInternalAPPPath();
                configurationsDirectory = (rsiInternalAPPPath + "/groupsConfiguration");
            }

            File folder = new File(configurationsDirectory);
            FileUtils.forceMkdir(folder);
            File[] listOfFiles = folder.listFiles();

            for (int i = 0; i < listOfFiles.length; i++)
            {
                if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".conf"))
                    groupNames.add(listOfFiles[i].getName().substring(0, listOfFiles[i].getName().indexOf(".conf")));
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void setArticleSearchBacking(ArticleSearchBacking articleSearchBacking) {
        this.articleSearchBacking = articleSearchBacking;
    }

    public String onFlowProcess(FlowEvent event)
    {
        // mLogger.error("onFlowProcess. event.getOldStep: " + event.getOldStep() + ", event.getNewStep: " + event.getNewStep());

        if (isSkip())
        {
            setSkip(false);

            return "fieldsConfigurations";
        }
        else
        {
            if (event.getOldStep().equalsIgnoreCase("groupConfigure"))
            {
                String configurationPathName = null;

                try
                {
                    mLogger.info("loadConfiguration ...");

                    File directory = new File(configurationsDirectory);

                    configurationPathName = configurationsDirectory + "/" + getSelectedGroupName() + ".conf";

                    mLogger.info("Loading configuration. configurationPathName: " + configurationPathName);

                    File file = new File(configurationPathName);
                    if (file.exists())
                    {
                        String configurationJson = FileUtils.readFileToString(file);

                        if (configurationJson != null && !configurationJson.equalsIgnoreCase(""))
                        {
                            mLogger.error("Configuration: " + configurationJson);

                            Gson gson = new Gson();
                            Type epgListType = new TypeToken<Configuration>() {}.getType();
                            configuration = gson.fromJson(configurationJson, epgListType);

                            if (configuration == null)
                                configuration = new Configuration();
                            else
                            {
                                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                                    "Configuration", "Configuration read successfully.");
                                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                                FacesContext context = FacesContext.getCurrentInstance();
                                context.addMessage(null, message);
                            }
                        }
                        else
                            configuration = new Configuration();
                    }
                    else
                        configuration = new Configuration();

                    configuration.setGroupName(getSelectedGroupName());
                }
                catch (Exception e)
                {
                    mLogger.error("Read configuration error. configurationPathName: " + configurationPathName + ", Exception: " + e.getMessage());

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Configuration", "Read configuration failed.");
                    // RequestContext.getCurrentInstance().showMessageInDialog(message);
                    FacesContext context = FacesContext.getCurrentInstance();
                    context.addMessage(null, message);
                }
            }
            else if (event.getOldStep().equalsIgnoreCase("contentTypeConfigure"))
            {
                if (configuration.getSelectedContentTypes().size() == 0)
                {
                    mLogger.error("The validation should guarantee at least one selected content type");

                    return event.getNewStep();
                }

                for (ContentType contentType: configuration.getSelectedContentTypes())
                {
                    List<FieldConfiguration> fieldConfigurations = configuration.getFieldsConfigurations().get(contentType.getType());
                    if (fieldConfigurations == null)
                        configuration.getFieldsConfigurations().put(contentType.getType(), new ArrayList<FieldConfiguration>());

                    HashMap<String,EscenicType> contentTypeModel = contentTypeModels.get(contentType.getType());
                    if (contentTypeModel == null)
                        contentTypeModels.put(contentType.getType(), getContentTypeModel(contentType));
                }

                // selectedContentType = selectedContentTypes.get(0);
            }

            return event.getNewStep();
        }
    }

    public void newGroupName()
    {
        try {
            if (getNewGroupName() == null)
            {
                mLogger.error("No New Group name found");

                return;
            }

            String configurationPathName = configurationsDirectory + "/" + getNewGroupName() + ".conf";
            File file = new File(configurationPathName);

            FileUtils.touch(file);

            groupNames.add(newGroupName);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void deleteGroupName()
    {
        try {
            if (getSelectedGroupName() == null)
            {
                mLogger.error("No selected Group name found");

                return;
            }

            int indexToBeRemoved;

            for (indexToBeRemoved = 0; indexToBeRemoved < groupNames.size(); indexToBeRemoved++)
            {
                if (groupNames.get(indexToBeRemoved).equalsIgnoreCase(getSelectedGroupName()))
                    break;
            }

            if (indexToBeRemoved == groupNames.size())
            {
                mLogger.error("Group name '" + getSelectedGroupName() + "' was not found");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Configuration",
                        "Group name '" + getSelectedGroupName() + "' was not found");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            groupNames.remove(indexToBeRemoved);

            String configurationPathName = configurationsDirectory + "/" + getSelectedGroupName() + ".conf";
            File file = new File(configurationPathName);

            FileUtils.forceDelete(file);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void addUserNameToGroup()
    {
        try {
            if (getUserNameToBeAddedOrRemoved() == null)
            {
                mLogger.error("No User name found");

                return;
            }

            boolean userNameFound = false;
            for (String user : configuration.getGroupUsers())
            {
                if (user.equalsIgnoreCase(getUserNameToBeAddedOrRemoved()))
                {
                    userNameFound = true;

                    break;
                }
            }

            if (userNameFound)
            {
                mLogger.error("User name '" + getUserNameToBeAddedOrRemoved() + "' is already present");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Configuration",
                        "User name '" + getUserNameToBeAddedOrRemoved() + "' is already present");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            Configuration localConfiguration = EscenicService.getUserConfiguration(getUserNameToBeAddedOrRemoved(),
                rsiInternalAPPPath);

            if (localConfiguration != null)
            {
                mLogger.error("UserName " + getUserNameToBeAddedOrRemoved() + " is already present in the " + localConfiguration.getGroupName() + " group");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Configuration",
                    "UserName " + getUserNameToBeAddedOrRemoved() + " is already present in the " + localConfiguration.getGroupName() + " group");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            configuration.getGroupUsers().add(getUserNameToBeAddedOrRemoved());
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void removeUserNameFromGroup()
    {
        try {
            if (getUserNameToBeAddedOrRemoved() == null)
            {
                mLogger.error("No User name found");

                return;
            }

            int indexToBeRemoved;

            for (indexToBeRemoved = 0; indexToBeRemoved < configuration.getGroupUsers().size(); indexToBeRemoved++)
            {
                if (configuration.getGroupUsers().get(indexToBeRemoved).equalsIgnoreCase(getUserNameToBeAddedOrRemoved()))
                    break;
            }

            if (indexToBeRemoved == configuration.getGroupUsers().size())
            {
                mLogger.error("No User name '" + getUserNameToBeAddedOrRemoved() + "' was found");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Configuration",
                        "No User name '" + getUserNameToBeAddedOrRemoved() + "' was found");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            configuration.getGroupUsers().remove(indexToBeRemoved);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void addSectionUniqueName()
    {
        try {
            if (getSectionUniqueName() == null)
            {
                mLogger.error("No Section Unique Name found");

                return;
            }

            boolean sectionUniqueNameFound = false;
            for (String sectionUniqueName : configuration.getSectionsUniqueNames())
            {
                if (sectionUniqueName.equalsIgnoreCase(getSectionUniqueName()))
                {
                    sectionUniqueNameFound = true;

                    break;
                }
            }

            if (sectionUniqueNameFound)
            {
                mLogger.error("Section Unique Name '" + getSectionUniqueName() + "' is already present");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Configuration",
                        "Section Unique Name '" + getSectionUniqueName() + "' is already present");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            configuration.getSectionsUniqueNames().add(getSectionUniqueName());
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void deleteSectionUniqueName()
    {
        try {
            if (getSectionUniqueName() == null)
            {
                mLogger.error("No Section Unique Name found");

                return;
            }

            int indexToBeRemoved;

            for (indexToBeRemoved = 0; indexToBeRemoved < configuration.getSectionsUniqueNames().size(); indexToBeRemoved++)
            {
                if (configuration.getSectionsUniqueNames().get(indexToBeRemoved).equalsIgnoreCase(getSectionUniqueName()))
                    break;
            }

            if (indexToBeRemoved == configuration.getSectionsUniqueNames().size())
            {
                mLogger.error("No Section Unique Name '" + getSectionUniqueName() + "' was found");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Configuration",
                        "No Section Unique Name '" + getSectionUniqueName() + "' was found");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            configuration.getSectionsUniqueNames().remove(indexToBeRemoved);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getNewGroupName() {
        return newGroupName;
    }

    public void setNewGroupName(String newGroupName) {
        this.newGroupName = newGroupName;
    }

    public String getUserNameToBeAddedOrRemoved() {
        return userNameToBeAddedOrRemoved;
    }

    public void setUserNameToBeAddedOrRemoved(String userNameToBeAddedOrRemoved) {
        this.userNameToBeAddedOrRemoved = userNameToBeAddedOrRemoved;
    }

    public String getSectionUniqueName() {
        return sectionUniqueName;
    }

    public void setSectionUniqueName(String sectionUniqueName) {
        this.sectionUniqueName = sectionUniqueName;
    }

    public List<String> getGroupNames() {
        return groupNames;
    }

    public void setGroupNames(List<String> groupNames) {
        this.groupNames = groupNames;
    }

    public String getSelectedGroupName() {
        return selectedGroupName;
    }

    public void setSelectedGroupName(String selectedGroupName) {
        this.selectedGroupName = selectedGroupName;
    }

    public List<ContentType> getContentTypesToBeSelected() {
        return contentTypesToBeSelected;
    }

    public void setContentTypesToBeSelected(List<ContentType> contentTypesToBeSelected) {
        this.contentTypesToBeSelected = contentTypesToBeSelected;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    private HashMap<String,EscenicType> getContentTypeModel(ContentType contentType)
    {
        HashMap<String,EscenicType> contentTypeModel = null;
        HashMap<String,EscenicField> linksFields = new HashMap<>();

        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            boolean modelCacheToBeUsed = true;
            HttpGetInfo httpGetPayloadInfo = escenicService.getXML(contentType.getUrl(), "<model - details>",
                "application/vnd.escenic.content-description, application/vnd.vizrt.model+xml", modelCacheToBeUsed);
            if (httpGetPayloadInfo == null || httpGetPayloadInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + contentType.getUrl());

                return contentTypeModel;
            }

            String sPayloadModelReturned = httpGetPayloadInfo.getReturnedBody();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document modelDocument = builder.parse(new java.io.ByteArrayInputStream(sPayloadModelReturned.getBytes()));

            Node contentDescriptorNode = (Node) escenicService.getxPath().evaluate(
                "com.escenic.domain.ContentDescriptor", modelDocument, XPathConstants.NODE);

            contentTypeModel = escenicService.getPayloadModel2(contentDescriptorNode, linksFields);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }

        return contentTypeModel;
    }

    public List<String> getKeyFields(ContentType contentType)
    {
        List<String> keyFields = new ArrayList<>();

        HashMap<String,EscenicType> contentTypeModel = contentTypeModels.get(contentType.getType());

        for (String keyField : contentTypeModel.keySet())
        {
            EscenicType escenicType = contentTypeModel.get(keyField);

            if (escenicType.getVisibility() && !isFieldAlreadyPresent(contentType, escenicType.getKeyField()))
            {
                keyFields.add(escenicType.getKeyField());
            }
        }

        return keyFields;
    }

    public void keyFieldChanged(ContentType contentType, int rowId)
    {
        HashMap<String,EscenicType> contentTypeModel = contentTypeModels.get(contentType.getType());
        List<FieldConfiguration> fieldConfigurations = configuration.getFieldsConfigurations().get(contentType.getType());

        FieldConfiguration fieldConfiguration = fieldConfigurations.get(rowId);

        String newKeyField = fieldConfiguration.getKeyField();
        boolean keyFieldFound = false;

        for (String keyField: contentTypeModel.keySet())
        {
            EscenicType escenicType = contentTypeModel.get(keyField);

            if (escenicType.getKeyField().equalsIgnoreCase(newKeyField))
            {
                fieldConfiguration.setLabel(escenicType.getLabel());
                fieldConfiguration.setGroupLabel(escenicType.getEscenicTypeGroup().getLabel());

                keyFieldFound = true;

                break;
            }
        }

        if (!keyFieldFound)
        {
            mLogger.error("KeyField " + newKeyField + " was not found");
        }
    }

    public void addEmptyNewConfigurationField(ContentType contentType)
    {
        mLogger.error("contentType: " + contentType.getType());
        HashMap<String,EscenicType> contentTypeModel = contentTypeModels.get(contentType.getType());
        List<FieldConfiguration> fieldConfigurations = configuration.getFieldsConfigurations().get(contentType.getType());

        EscenicType escenicType = null;
        boolean keyFieldFound = false;
        for (String keyField : contentTypeModel.keySet())
        {
            escenicType = contentTypeModel.get(keyField);

            if (escenicType.getVisibility() && !isFieldAlreadyPresent(contentType, escenicType.getKeyField()))
            {
                keyFieldFound = true;

                break;
            }
        }

        if (!keyFieldFound)
        {
            mLogger.error("KeyField not found");

            return;
        }

        FieldConfiguration fieldConfiguration = new FieldConfiguration();

        fieldConfiguration.setGroupLabel(escenicType.getEscenicTypeGroup().getLabel());
        fieldConfiguration.setKeyField(escenicType.getKeyField());
        fieldConfiguration.setLabel(escenicType.getLabel());
        fieldConfiguration.setMandatory(true);

        fieldConfigurations.add(fieldConfiguration);
    }

    public boolean isFieldAlreadyPresent(ContentType contentType, String keyField)
    {
        List<FieldConfiguration> fieldConfigurations = configuration.getFieldsConfigurations().get(contentType.getType());

        for (FieldConfiguration fieldConfiguration: fieldConfigurations)
        {
            if (fieldConfiguration.getKeyField().equalsIgnoreCase(keyField))
                return true;
        }

        return false;
    }

    public void save()
    {
        String configurationPathName = null;

        try
        {
            mLogger.info("saveConfiguration ...");

            File directory = new File(configurationsDirectory);
            FileUtils.forceMkdir(directory);

            configurationPathName = configurationsDirectory + "/" + getSelectedGroupName() + ".conf";

            Gson gson = new Gson();
            Type configurationType = new TypeToken<Configuration>() {}.getType();
            String configurationJson = gson.toJson(configuration, configurationType);

            mLogger.error("Configuration: " + configurationJson);

            mLogger.info("Saving configuration. configurationPathName: " + configurationPathName);

            File file = new File(configurationPathName);
            FileUtils.writeStringToFile(file, configurationJson);
        }
        catch (Exception e)
        {
            mLogger.error("Write configuration error. configurationPathName: " + configurationPathName + ", Exception: " + e.getMessage());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Configuration", "Saving configuration failed.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "Configuration", "Configuration saved successfully.");
        // RequestContext.getCurrentInstance().showMessageInDialog(message);
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, message);
    }
}
