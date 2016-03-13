package com.client.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.client.backing.model.ArticleTabs.Article;
import com.client.backing.model.ArticleTabs.MediaEntryInfo;
import com.client.backing.model.ArticleTabs.MediaInfo;
import com.client.backing.model.Configuration.Configuration;
import com.client.backing.model.ArticleTabs.ImageInfo;
import com.client.backing.model.ArticleTabs.HistoryLog;
import com.client.backing.model.ArticleTabs.MimeTypes;
import com.client.backing.model.SearchResult.ArticleTableData;
import com.client.service.util.ComplexValue;
import com.client.service.util.HttpGetInfo;
import com.client.backing.model.common.ContentType;
import com.client.service.util.EscenicCache;
import com.client.backing.model.common.State;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 21/01/14
 * Time: 11:03
 * To change this template use File | Settings | File Templates.
 */
public class EscenicService implements Serializable {

    private Logger mLogger = Logger.getLogger(this.getClass());

    final static public String configurationFileName = "config.properties";

    private String version;
    private String rsiInternalAPPPath;
    private String administrators;
    private String buildNumber;

    private boolean escenicDisabled;

    private String escenicWebServicesHost = null;
    private int escenicWebServicesPort = -1;
    private int escenicWebServicesServerTimeoutInMilliSeconds = -1;

    private String solrHost = null;
    private int solrPort = -1;
    private int solrServerTimeoutInMilliSeconds = -1;
    private String solrBaseURL;

    private String userName = null;
    private String password = null;
    private Configuration configuration;

    private boolean socksProxy = true;
    private String socksProxyHost = null;
    private int socksProxyPort = -1;

    private XPath xPath;


    public EscenicService(String userName, String password)
    {
        {
            xPath = XPathFactory.newInstance().newXPath();

            xPath.setNamespaceContext(new NamespaceContext()
            {
                public String getNamespaceURI(String prefix)
                {
                    String result = null;

                    if ("vdf".equals(prefix))
                        result = "http://www.vizrt.com/types";
                    else if ("dcterms".equals(prefix))
                        result = "http://purl.org/dc/terms/";
                    else if ("app".equals(prefix))
                        result = "http://www.w3.org/2007/app";

                    /*
                    if ("atom".equals(prefix)) {
                        result = "http://www.w3.org/2005/Atom";
                    } else if ("vaext".equals(prefix)) {
                        result = "http://www.vizrt.com/atom-ext";
                    }
                    } else if ("core".equals(prefix)) {
                        result = "http://ns.vizrt.com/ardome/core";
                    } else if ("vizid".equals(prefix)) {
                        result = "http://www.vizrt.com/opensearch/vizid";
                    } else if ("vizmedia".equals(prefix)) {
                        result = "http://www.vizrt.com/opensearch/mediatype";
                    } else if ("opensearch".equals(prefix)) {
                        result = "http://a9.com/-/spec/opensearch/1.1";
                    } else if ("acl".equals(prefix)) {
                        result = "http://www.vizrt.com/2012/acl";
                    } else if ("mam".equals(prefix)) {
                        result = "http://www.vizrt.com/2010/mam";
                    }
                    } else if ("playout".equals(prefix)) {
                        result = "http://ns.vizrt.com/ardome/playout";
                    } else if ("media".equals(prefix)) {
                        result = "http://search.yahoo.com/mrss/";
                    }
                    } else if ("at".equals(prefix)) {
                        result = "http://purl.org/atompub/tombstones/1.0";
                    } else if ("md".equals(prefix)) {
                        result = "http://ns.vizrt.com/ardome/metadata";
                    } else if ("playlist".equals(prefix)) {
                        result = "http://ns.vizrt.com/ardome/playlist";
                    }
                    */

                    if (result == null)
                        mLogger.error("naming space missing. Prefix: " + prefix);

                    return result;
                }

                // Dummy implementations – not used!
                public Iterator getPrefixes(String val) {
                    return null;
                }

                public String getPrefix(String uri) {
                    return null;
                }
            });
        }

        {
            try
            {
                Properties properties = new Properties();
                InputStream inputStream = EscenicService.class.getClassLoader().getResourceAsStream(configurationFileName);
                if (inputStream == null)
                {
                    mLogger.error("Configuration file not found. ConfigurationFileName: " + configurationFileName);

                    return;
                }
                properties.load(inputStream);

                escenicDisabled = Boolean.valueOf(properties.getProperty("escenic.disabled"));

                {
                    version = properties.getProperty("yaec.version");
                    if (version == null)
                    {
                        mLogger.error("No 'yaec.version'.host found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }

                    rsiInternalAPPPath = properties.getProperty("yaec.localPath");
                    if (rsiInternalAPPPath == null)
                    {
                        mLogger.error("No 'yaec.localPath'.host found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }

                    administrators = properties.getProperty("yaec.administrators");
                    if (administrators == null)
                    {
                        mLogger.error("No 'yaec.administrators'.host found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                }

                {
                    ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance()
                            .getExternalContext().getContext();
                    String absoluteWebAppRealPath = ctx.getRealPath("/");

                    String buildNumberAbsolutePathName = absoluteWebAppRealPath + "WEB-INF/classes/build.number";

                    mLogger.info("buildNumberAbsolutePathName: " + buildNumberAbsolutePathName);

                    File file = new File(buildNumberAbsolutePathName);
                    buildNumber = FileUtils.readFileToString(file, "UTF-8");
                }

                {
                    escenicWebServicesHost = properties.getProperty("escenic.webservices.host");
                    if (escenicWebServicesHost == null)
                    {
                        mLogger.error("No escenic.webservices.host found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }

                    String tmpEscenicWebServicesPort = properties.getProperty("escenic.webservices.port");
                    if (tmpEscenicWebServicesPort == null)
                    {
                        mLogger.error("No escenic.webservices.port found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                    escenicWebServicesPort = Long.valueOf(tmpEscenicWebServicesPort).intValue();

                    String tmpEscenicWebServicesServerTimeoutInMilliSeconds = properties.getProperty("escenic.webservices.serverTimeoutInMilliSeconds");
                    if (tmpEscenicWebServicesServerTimeoutInMilliSeconds == null)
                    {
                        mLogger.error("No escenic.webservices.serverTimeoutInMilliSeconds found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                    escenicWebServicesServerTimeoutInMilliSeconds = Long.valueOf(tmpEscenicWebServicesServerTimeoutInMilliSeconds).intValue();
                }

                {
                    solrHost = properties.getProperty("solr.host");
                    if (solrHost == null)
                    {
                        mLogger.error("No solr.host found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }

                    String tmpSolrPort = properties.getProperty("solr.port");
                    if (tmpSolrPort == null)
                    {
                        mLogger.error("No solr.port found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                    solrPort = Long.valueOf(tmpSolrPort).intValue();

                    String tmpSolrServerTimeoutInMilliSeconds = properties.getProperty("solr.serverTimeoutInMilliSeconds");
                    if (tmpSolrServerTimeoutInMilliSeconds == null)
                    {
                        mLogger.error("No solr.serverTimeoutInMilliSeconds found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                    solrServerTimeoutInMilliSeconds = Long.valueOf(tmpSolrServerTimeoutInMilliSeconds).intValue();

                    solrBaseURL = properties.getProperty("solr.baseURL");
                    if (solrBaseURL == null)
                    {
                        mLogger.error("No solr.baseURL found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                }

                String tmpSocksProxy = properties.getProperty("socks.proxy");
                if (tmpSocksProxy == null)
                {
                    mLogger.error("No socks.proxy found. ConfigurationFileName: " + configurationFileName);

                    return;
                }
                socksProxy = Boolean.valueOf(tmpSocksProxy).booleanValue();

                if (socksProxy)
                {
                    socksProxyHost = properties.getProperty("socks.proxy.host");
                    if (socksProxyHost == null)
                    {
                        mLogger.error("No socks.proxy.host found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }

                    String tmpSocksProxyPort = properties.getProperty("socks.proxy.port");
                    if (tmpSocksProxyPort == null)
                    {
                        mLogger.error("No socks.proxy.port found. ConfigurationFileName: " + configurationFileName);

                        return;
                    }
                    socksProxyPort = Long.valueOf(tmpSocksProxyPort).intValue();
                }
            }
            catch (Exception e)
            {
                mLogger.error("Problems to load the configuration file. Exception: " + e.getMessage() + ", ConfigurationFileName: " + configurationFileName);

                return;
            }
        }

        this.userName = userName;
        this.password = password;

        if (!isAdministrator())
        {
            configuration = getUserConfiguration(getUserName(), rsiInternalAPPPath);
            if (configuration == null)
            {
                mLogger.info("UserName '" + getUserName() + "' not found in any group. Set the default configuration");

                configuration = new Configuration();
            }
            else
            {
                mLogger.info("UserName '" + getUserName() + "' found in the " + configuration.getGroupName() + " group");
            }
        }
        else
        {
            mLogger.info("UserName '" + getUserName() + "' is administrator");

            configuration = new Configuration();
        }
    }

    public static Configuration getUserConfiguration(String userName, String rsiInternalAPPPath)
    {
        Configuration localConfiguration = null;
        boolean configurationFound = false;

        try
        {
            Logger.getLogger(EscenicService.class).info("Looking for the configuration of the the user '" + userName + "'");

            String configurationsDirectory = (rsiInternalAPPPath + "/groupsConfiguration");

            File folder = new File(configurationsDirectory);
            FileUtils.forceMkdir(folder);

            File[] listOfFiles = folder.listFiles();

            for (int i = 0; i < listOfFiles.length; i++)
            {
                if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".conf"))
                {
                    Logger.getLogger(EscenicService.class).info("Loading the configuration: " + listOfFiles[i].getName());

                    File file = new File(listOfFiles[i].getAbsolutePath());
                    if (file.exists())
                    {
                        String configurationJson = FileUtils.readFileToString(file);

                        if (configurationJson != null && !configurationJson.equalsIgnoreCase(""))
                        {
                            Logger.getLogger(EscenicService.class).error("Configuration: " + configurationJson);

                            Gson gson = new Gson();
                            Type epgListType = new TypeToken<Configuration>() {}.getType();
                            localConfiguration = gson.fromJson(configurationJson, epgListType);

                            if (localConfiguration != null)
                            {
                                int userNameIndex;

                                for (userNameIndex = 0; userNameIndex < localConfiguration.getGroupUsers().size(); userNameIndex++)
                                {
                                    if (localConfiguration.getGroupUsers().get(userNameIndex).equalsIgnoreCase(userName))
                                        break;
                                }

                                if (userNameIndex == localConfiguration.getGroupUsers().size())
                                {
                                    // mLogger.error("No User name '" + getUserNameToBeAddedOrRemoved() + "' was found");

                                    continue;
                                }

                                configurationFound = true;

                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Logger.getLogger(EscenicService.class).error("getUserConfiguration error. Exception: " + e.getMessage());
        }

        return configurationFound ? localConfiguration : null;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getUserName() {
        return userName;
    }

    public String getVersion() {
        return version;
    }

    public String getRsiInternalAPPPath() {
        return rsiInternalAPPPath;
    }

    public boolean isAdministrator()
    {
        return administrators.contains(getUserName());
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public String getEscenicWebServicesHost() {
        return escenicWebServicesHost;
    }

    public int getEscenicWebServicesPort() {
        return escenicWebServicesPort;
    }

    public XPath getxPath() {
        return xPath;
    }

    static public String isEscenicUserValid(String localUserName, String localPassword)
    {
        String localEscenicWebServicesHost;
        int localEscenicWebServicesPort;
        boolean localSocksProxy;
        String localSocksProxyHost;
        int localSocksProxyPort;
        int localEscenicWebServicesServerTimeoutInMilliSeconds;

        {
            try
            {
                Properties properties = new Properties();
                InputStream inputStream = EscenicService.class.getClassLoader().getResourceAsStream(configurationFileName);
                if (inputStream == null)
                {
                    String errorMessage = "Configuration file not found. ConfigurationFileName: " + configurationFileName;

                    Logger.getLogger(EscenicService.class).error(errorMessage);

                    return errorMessage;
                }
                properties.load(inputStream);

                {
                    boolean escenicDisabled = Boolean.valueOf(properties.getProperty("escenic.disabled"));

                    if (escenicDisabled)
                    {
                        Logger.getLogger(EscenicService.class).error("Escenic service disabled");

                        return null;
                    }
                }

                localEscenicWebServicesHost = properties.getProperty("escenic.webservices.host");
                if (localEscenicWebServicesHost == null)
                {
                    String errorMessage = "No escenic.webservices.host found. ConfigurationFileName: " + configurationFileName;
                    Logger.getLogger(EscenicService.class).error(errorMessage);

                    return errorMessage;
                }

                String tmpEscenicWebServicesPort = properties.getProperty("escenic.webservices.port");
                if (tmpEscenicWebServicesPort == null)
                {
                    String errorMessage = "No escenic.webservices.port found. ConfigurationFileName: " + configurationFileName;
                    Logger.getLogger(EscenicService.class).error(errorMessage);

                    return errorMessage;
                }
                localEscenicWebServicesPort = Long.valueOf(tmpEscenicWebServicesPort).intValue();

                String tmpEscenicWebServicesServerTimeoutInMilliSeconds = properties.getProperty("escenic.webservices.serverTimeoutInMilliSeconds");
                if (tmpEscenicWebServicesServerTimeoutInMilliSeconds == null)
                {
                    String errorMessage = "No escenic.webservices.serverTimeoutInMilliSeconds found. ConfigurationFileName: " + configurationFileName;
                    Logger.getLogger(EscenicService.class).error(errorMessage);

                    return errorMessage;
                }
                localEscenicWebServicesServerTimeoutInMilliSeconds = Long.valueOf(tmpEscenicWebServicesServerTimeoutInMilliSeconds).intValue();

                String tmpSocksProxy = properties.getProperty("socks.proxy");
                if (tmpSocksProxy == null)
                {
                    String errorMessage = "No socks.proxy found. ConfigurationFileName: " + configurationFileName;
                    Logger.getLogger(EscenicService.class).error(errorMessage);

                    return errorMessage;
                }
                localSocksProxy = Boolean.valueOf(tmpSocksProxy).booleanValue();

                if (localSocksProxy)
                {
                    localSocksProxyHost = properties.getProperty("socks.proxy.host");
                    if (localSocksProxyHost == null)
                    {
                        String errorMessage = "No socks.proxy.host found. ConfigurationFileName: " + configurationFileName;
                        Logger.getLogger(EscenicService.class).error(errorMessage);

                        return errorMessage;
                    }

                    String tmpSocksProxyPort = properties.getProperty("socks.proxy.port");
                    if (tmpSocksProxyPort == null)
                    {
                        String errorMessage = "No socks.proxy.port found. ConfigurationFileName: " + configurationFileName;
                        Logger.getLogger(EscenicService.class).error(errorMessage);

                        return errorMessage;
                    }
                    localSocksProxyPort = Long.valueOf(tmpSocksProxyPort).intValue();
                }
                else
                {
                    localSocksProxyHost = null;
                    localSocksProxyPort = -1;
                }
            }
            catch (Exception e)
            {
                String errorMessage = "Problems to load the configuration file. Exception: " + e.getMessage() + ", ConfigurationFileName: " + configurationFileName;
                Logger.getLogger(EscenicService.class).error(errorMessage);

                return errorMessage;
            }
        }

        String url  = "http://" + localEscenicWebServicesHost + ":" + localEscenicWebServicesPort + "/webservice/escenic/section/ROOT/subsections";

        try {
            HttpResponse response = null;

            if (localUserName == null || localPassword == null)
            {
                String errorMessage = "userName and/or escenicPassword not initialized";
                Logger.getLogger(EscenicService.class).error(errorMessage);

                return errorMessage;
            }

            {
                if (localSocksProxy)
                {
                    System.setProperty("socksProxyHost", localSocksProxyHost);
                    System.setProperty("socksProxyPort", String.valueOf(localSocksProxyPort));
                }

                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(localUserName, localPassword));

                RequestConfig requestConfig = RequestConfig.custom()
                        .setSocketTimeout(localEscenicWebServicesServerTimeoutInMilliSeconds)
                        .setConnectTimeout(localEscenicWebServicesServerTimeoutInMilliSeconds)
                        .build();

                CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).
                        setDefaultRequestConfig(requestConfig).build();

                HttpGet httpget = new HttpGet(url);

                Logger.getLogger(EscenicService.class).info("Escenic URL: " + url + ", Timeout: " + localEscenicWebServicesServerTimeoutInMilliSeconds);

                response = httpClient.execute(httpget);
            }

            if (response.getStatusLine().getStatusCode() != 200)
            {
                String errorMessage = "'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode();
                Logger.getLogger(EscenicService.class).error(errorMessage);

                return errorMessage;
            }

            HttpEntity entity = response.getEntity();

            if (entity == null)
            {
                String errorMessage = "'" + url + "' failed. HttpEntity is null";
                Logger.getLogger(EscenicService.class).error(errorMessage);

                return errorMessage;
            }

            return null;
        }
        catch (Exception e)
        {
            String errorMessage = "'" + url + "' failed. Exception: " + e;
            Logger.getLogger(EscenicService.class).error(errorMessage);

            return errorMessage;
        }
    }

    public List<ContentType> getAllContentTypes()
    {
        String escenicURL  = "http://" + escenicWebServicesHost + ":" + escenicWebServicesPort + "/webservice/publication/rsi/content-descriptions";

        List<ContentType> contentTypes = new ArrayList<>();

        mLogger.info("getAllContentTypes. escenicURL: " + escenicURL);

        try {
            boolean cacheToBeUsed = true;

            HttpGetInfo httpGetInfo = getXML(escenicURL, "<contentTypes>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + escenicURL);

                return null;
            }

            String sTextReturned = httpGetInfo.getReturnedBody();

            String urls[] = sTextReturned.split("\\r?\\n");
            String contentTypeLabel;
            String contentTypeValue;

            for (int indexContentType = 0; indexContentType < urls.length; indexContentType++)
            {
                contentTypeValue = urls[indexContentType].substring(urls[indexContentType].lastIndexOf("/") + 1);

                if (contentTypeValue.equalsIgnoreCase("migrationAudio"))
                    contentTypeLabel = "Audio (Migration)";
                else if (contentTypeValue.equalsIgnoreCase("programmeAudio"))
                    contentTypeLabel = "Audio (Program)";
                else if (contentTypeValue.equalsIgnoreCase("transcodableAudio"))
                    contentTypeLabel = "Audio (Transcodable)";
                else if (contentTypeValue.equalsIgnoreCase("vmeAudio"))
                    contentTypeLabel = "Audio (VME)";
                else if (contentTypeValue.equalsIgnoreCase("banner"))
                    contentTypeLabel = "Banner";
                else if (contentTypeValue.equalsIgnoreCase("gallery"))
                    contentTypeLabel = "Gallery";
                else if (contentTypeValue.equalsIgnoreCase("banner"))
                    contentTypeLabel = "Banner";
                else if (contentTypeValue.equalsIgnoreCase("keyframe"))
                    contentTypeLabel = "Key Frame";
                else if (contentTypeValue.equalsIgnoreCase("livestreaming"))
                    contentTypeLabel = "Live Streaming";
                else if (contentTypeValue.equalsIgnoreCase("picture"))
                    contentTypeLabel = "Picture";
                else if (contentTypeValue.equalsIgnoreCase("programme"))
                    contentTypeLabel = "Program";
                else if (contentTypeValue.equalsIgnoreCase("series"))
                    contentTypeLabel = "Series";
                else if (contentTypeValue.equalsIgnoreCase("story"))
                    contentTypeLabel = "Story";
                else if (contentTypeValue.equalsIgnoreCase("migrationVideo"))
                    contentTypeLabel = "Video (Migration)";
                else if (contentTypeValue.equalsIgnoreCase("programmeVideo"))
                    contentTypeLabel = "Video (Program)";
                else if (contentTypeValue.equalsIgnoreCase("segmentedProgrammeVideo"))
                    contentTypeLabel = "Video (Segmented Program)";
                else if (contentTypeValue.equalsIgnoreCase("transcodableVideo"))
                    contentTypeLabel = "Video (Transcodable)";
                else if (contentTypeValue.equalsIgnoreCase("vmeVideo"))
                    contentTypeLabel = "Video (VME)";
                else
                    contentTypeLabel = contentTypeValue;

                ContentType contentType = new ContentType(contentTypeLabel, contentTypeValue, urls[indexContentType]);

                contentTypes.add(contentType);
            }
        }
        catch (Exception e) {

            mLogger.error("getAllContentTypes. escenicURL: " + escenicURL + " failed. Exception: " + e);

            return null;    // throw new Exception(methodName + " (" + contentIdURL + ") failed. Exception: " + e);
        }


        return contentTypes;
    }

    public Long getContentItems (
            int startIndex, int pageSize,
            String wordsToSearch, String wordsToSearchType,
            String publications, String sections, boolean includeSubSections,
            Date publishStartTime, Date publishEndTime,
            Date expireStartTime, Date expireEndTime,
            Date lastModifiedStartTime, Date lastModifiedEndTime,
            Date activateStartTime, Date activateEndTime,
            Date startTime, Date endTime,
            String orderBy, String orderType,
            String [] contentTypes,
            String [] states,
            String tagsURI, String tagsSearchType,
            List<ArticleTableData> articleTableDataList)
    {
        String qUrlParameter = "";
        String fqUrlParameter = "";
        String sortUrlParameter = "";
        String startUrlParameter = "";
        String rowsUrlParameter = "";
        String flUrlParameter = "";
        Long totalSearchItems = new Long(0);


        mLogger.info("getContentItems. startIndex: " + startIndex +
                ", pageSize: " + pageSize +
                ", wordsToSearch: " + wordsToSearch +
                ", wordsToSearchType: " + wordsToSearchType +
                ", publications: " + publications +
                ", sectionId: " + sections +
                ", includeSubSections: " + includeSubSections +
                ", PublishStartTime: " + publishStartTime +
                ", PublishEndTime: " + publishEndTime +
                ", ExpireStartTime: " + expireStartTime +
                ", ExpireEndTime: " + expireEndTime +
                ", LastModifiedStartTime: " + lastModifiedStartTime +
                ", LastModifiedEndTime: " + lastModifiedEndTime +
                ", ActivateStartTime: " + activateStartTime +
                ", ActivateEndTime: " + activateEndTime +
                ", startTime: " + startTime +
                ", endTime: " + endTime +
                ", orderBy: " + orderBy +
                ", orderType: " + orderType +
                ", contentTypes length: " + (contentTypes == null ? "null" : contentTypes.length) +
                ", states length: " + (states == null ? "null" : states.length) +
                ", tagsURI: " + tagsURI +
                ", tagsSearchType: " + tagsSearchType
        );

        if (escenicDisabled)
        {
            mLogger.error("Escenic service disabled");

            return totalSearchItems;
        }

        if (pageSize <= 0)
        {
            mLogger.error("Wrong input parameters");

            return totalSearchItems;
        }

        try {
            if (wordsToSearch != null && !wordsToSearch.trim().equalsIgnoreCase("") &&
                    wordsToSearchType != null && !wordsToSearchType.trim().equalsIgnoreCase(""))
            {
                String qQueryString = getQueryString(wordsToSearchType, wordsToSearch);

                qUrlParameter = java.net.URLEncoder.encode(qQueryString, "UTF-8");
                mLogger.info("q url parameter: " + qQueryString + ", encoded: " + qUrlParameter);
            }
            else
            {
                qUrlParameter = java.net.URLEncoder.encode("*:*", "UTF-8");
                mLogger.info("q url parameter: " + "*:*" + ", encoded: " + qUrlParameter);
            }

            {
                // Publication currentPublication;
                String tagFieldName = "classification";
                String filterQuery = "";

                if (publications != null && !publications.trim().equalsIgnoreCase(""))
                {
                    filterQuery += ("(publication: " + getQueryString("AtLeastOne", publications) + ") ");
                }

                // mLogger.info("3. sections: " + sections + ", includeSubSections: " + includeSubSections);
                if (sections != null && !sections.trim().equalsIgnoreCase(""))
                {
                    if (includeSubSections)
                        filterQuery += ("+(section: " + getQueryString("AtLeastOne", sections) + ") ");
                    else
                        filterQuery += ("+(home_section: " + getQueryString("AtLeastOne", sections) + ") ");
                }

                {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    if (publishStartTime != null && publishEndTime != null)
                    {
                        filterQuery += ("+(publishdate: [" + dateFormat.format(publishStartTime) + " TO " + dateFormat.format(publishEndTime) + "]) ");
                    }
                    else if (publishStartTime == null && publishEndTime != null)
                    {
                        filterQuery += ("+(publishdate: [* TO " + dateFormat.format(publishEndTime) + "]) ");
                    }
                    else if (publishStartTime != null && publishEndTime == null)
                    {
                        filterQuery += ("+(publishdate: [" + dateFormat.format(publishStartTime) + " TO *]) ");
                    }
                }

                {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    if (expireStartTime != null && expireEndTime != null)
                    {
                        filterQuery += ("+(expiredate: [" + dateFormat.format(expireStartTime) + " TO " + dateFormat.format(expireEndTime) + "]) ");
                    }
                    else if (expireStartTime == null && expireEndTime != null)
                    {
                        filterQuery += ("+(expiredate: [* TO " + dateFormat.format(expireEndTime) + "]) ");
                    }
                    else if (expireStartTime != null && expireEndTime == null)
                    {
                        filterQuery += ("+(expiredate: [" + dateFormat.format(expireStartTime) + " TO *]) ");
                    }
                }

                {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    if (lastModifiedStartTime != null && lastModifiedEndTime != null)
                    {
                        filterQuery += ("+(lastmodifieddate: [" + dateFormat.format(lastModifiedStartTime) + " TO " + dateFormat.format(lastModifiedEndTime) + "]) ");
                    }
                    else if (lastModifiedStartTime == null && lastModifiedEndTime != null)
                    {
                        filterQuery += ("+(lastmodifieddate: [* TO " + dateFormat.format(lastModifiedEndTime) + "]) ");
                    }
                    else if (lastModifiedStartTime != null && lastModifiedEndTime == null)
                    {
                        filterQuery += ("+(lastmodifieddate: [" + dateFormat.format(lastModifiedStartTime) + " TO *]) ");
                    }
                }

                {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    if (activateStartTime != null && activateEndTime != null)
                    {
                        filterQuery += ("+(activatedate: [" + dateFormat.format(activateStartTime) + " TO " + dateFormat.format(activateEndTime) + "]) ");
                    }
                    else if (activateStartTime == null && activateEndTime != null)
                    {
                        filterQuery += ("+(activatedate: [* TO " + dateFormat.format(activateEndTime) + "]) ");
                    }
                    else if (activateStartTime != null && activateEndTime == null)
                    {
                        filterQuery += ("+(activatedate: [" + dateFormat.format(activateStartTime) + " TO *]) ");
                    }
                }

                {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    if (startTime != null && endTime != null)
                    {
                        filterQuery += ("+(starttime_date: [" + dateFormat.format(startTime) + " TO " + dateFormat.format(endTime) + "]) ");
                    }
                    else if (startTime == null && endTime != null)
                    {
                        filterQuery += ("+(starttime_date: [* TO " + dateFormat.format(endTime) + "]) ");
                    }
                    else if (startTime != null && endTime == null)
                    {
                        filterQuery += ("+(starttime_date: [" + dateFormat.format(startTime) + " TO *]) ");
                    }
                }

                if (contentTypes != null && contentTypes.length > 0)
                {
                    String value = "";

                    for (int index = 0; index < contentTypes.length; index++)
                    {
                        value += contentTypes[index];

                        if (index != contentTypes.length - 1)
                            value += " ";
                    }

                    filterQuery += ("+(contenttype: " + getQueryString("AtLeastOne", value) + ") ");
                }

                if (states != null && states.length > 0)
                {
                    String value = "";

                    for (int index = 0; index < states.length; index++)
                    {
                        value += states[index];

                        if (index != states.length - 1)
                            value += " ";
                    }

                    filterQuery += ("+(state: " + getQueryString("AtLeastOne", value) + ") ");
                }

                if (tagFieldName != null && !tagFieldName.trim().equalsIgnoreCase("") &&
                        tagsURI != null && !tagsURI.trim().equalsIgnoreCase("") &&
                        tagsSearchType != null && !tagsSearchType.trim().equalsIgnoreCase(""))
                {
                    List<URI> tagsIdentifiers = new ArrayList<>();

                    StringTokenizer tokenizer = new StringTokenizer(tagsURI, " ");
                    if (tokenizer.countTokens() > 0)
                    {
                        while (tokenizer.hasMoreTokens())
                            tagsIdentifiers.add(new URI(tokenizer.nextToken()));
                    }

                    filterQuery += ("+(" + tagFieldName + ": " + getTagsQueryString(tagsSearchType, tagsIdentifiers) + ") ");
                }

                fqUrlParameter = java.net.URLEncoder.encode(filterQuery, "UTF-8");
                mLogger.info("fq url parameter: " + filterQuery + ", encoded: " + fqUrlParameter);
            }

            {
                String sortQuery;

                if (orderBy != null)
                {
                    if (orderBy.equalsIgnoreCase("Published Date"))
                        sortQuery = "publishdate ";
                    else if (orderBy.equalsIgnoreCase("Start Date"))
                        sortQuery = "starttime_date ";
                    else if (orderBy.equalsIgnoreCase("Id"))
                        sortQuery = "id ";
                    else if (orderBy.equalsIgnoreCase("Creator Username"))
                        sortQuery = "creator_username ";
                    else if (orderBy.equalsIgnoreCase("Last Edited By"))
                        sortQuery = "last_edited_by ";
                    else if (orderBy.equalsIgnoreCase("State"))
                        sortQuery = "state ";
                    else if (orderBy.equalsIgnoreCase("Content Type"))
                        sortQuery = "contenttype ";
                    else if (orderBy.equalsIgnoreCase("Creation Date"))
                        sortQuery = "creationdate ";
                    else if (orderBy.equalsIgnoreCase("Activation Date"))
                        sortQuery = "activatedate ";
                    else if (orderBy.equalsIgnoreCase("Expiration Date"))
                        sortQuery = "expiredate ";
                    else if (orderBy.equalsIgnoreCase("Last Modified Date"))
                        sortQuery = "lastmodifieddate ";
                    else
                        sortQuery = "publishdate ";

                    if (orderType != null)
                    {
                        if (orderType.equalsIgnoreCase("Ascending"))
                            sortQuery += "asc";
                        else if (orderType.equalsIgnoreCase("Descending"))
                            sortQuery += "desc";
                        else
                            sortQuery += "desc";
                    }
                    else
                        sortQuery += "desc";
                }
                else
                    sortQuery = "publishdate desc";

                sortUrlParameter = java.net.URLEncoder.encode(sortQuery, "UTF-8");
                mLogger.info("sort url parameter: " + sortQuery + ", encoded: " + sortUrlParameter);
            }

            startUrlParameter = String.valueOf(startIndex);
            mLogger.info("start url parameter: " + startUrlParameter);

            rowsUrlParameter = String.valueOf(pageSize);
            mLogger.info("rows url parameter: " + rowsUrlParameter);

            {
                String flQuery = "objectid,publication,title,contenttype,publishdate,home_section_name,starttime_date,state";
                flUrlParameter = java.net.URLEncoder.encode(flQuery, "UTF-8");
                mLogger.info("fl url parameter: " + flQuery + ", encoded: " + flUrlParameter);
            }

            // i.e.: http://internal.publishing.rsi.ch:8180/solr/collection1/select?q=fff&fq=%28publication%3A+%28rsi+OR+tvsvizzera+%29%29+%2B%28section%3A+%285+OR+8569+%29%29+%2B%28publishdate%3A+[2015-04-06T22%3A00%3A00Z+TO+2015-11-12T23%3A00%3A00Z]%29+%2B%28expiredate%3A+[2015-11-17T23%3A00%3A00Z+TO+2017-05-23T22%3A00%3A00Z]%29+%2B%28activatedate%3A+[2014-05-31T22%3A00%3A00Z+TO+2016-05-18T22%3A00%3A00Z]%29+%2B%28state%3A+%28draft+OR+submitted+OR+approved+OR+published+OR+draft-published+OR+submitted-published+OR+approved-published+%29%29&sort=publishdate+desc&fl=id%2Ctitle&wt=json&indent=true
            String url = "http://" + solrHost + ":" + solrPort + solrBaseURL + "/select";
            url += ("?wt=json");
            url += ("&start=" + startUrlParameter);
            url += ("&rows=" + rowsUrlParameter);
            url += ("&fl=" + flUrlParameter);
            if (!qUrlParameter.equalsIgnoreCase(""))
                url += ("&q=" + qUrlParameter);
            if (!fqUrlParameter.equalsIgnoreCase(""))
                url += ("&fq=" + fqUrlParameter);
            if (!sortUrlParameter.equalsIgnoreCase(""))
                url += ("&sort=" + sortUrlParameter);

            mLogger.info("Solr URL: " + url);
            String json = loadContentItems(url);
            // mLogger.info("Solr URL: " + url + ", Result: " + json);

            JSONObject jsonObject = new JSONObject(json);
            JSONObject jsonResponse = jsonObject.getJSONObject("response");

            totalSearchItems = jsonResponse.getLong("numFound");

            JSONArray jsonArrayArticles = jsonResponse.getJSONArray("docs");

            for (int index = 0; index < jsonArrayArticles.length(); index++)
            {
                JSONObject jsonObjectArticle = jsonArrayArticles.getJSONObject(index);

                ArticleTableData articleTableData = new ArticleTableData();

                try
                {
                    articleTableData.setObjectId(jsonObjectArticle.getLong("objectid"));
                }
                catch (Exception ex)
                {
                    mLogger.info("objectid not found. Exception: " + ex.getMessage());

                    continue;
                }

                try
                {
                    articleTableData.setPublication(jsonObjectArticle.getString("publication"));
                }
                catch (Exception ex)
                {
                    mLogger.info("publication not found. Exception: " + ex.getMessage());
                }

                try
                {
                    articleTableData.setTitle(jsonObjectArticle.getString("title"));
                }
                catch (Exception ex)
                {
                    mLogger.info("title not found. Exception: " + ex.getMessage());
                }

                try
                {
                    articleTableData.setState(jsonObjectArticle.getString("state"));
                }
                catch (Exception ex)
                {
                    mLogger.info("state not found. Exception: " + ex.getMessage());
                }

                try
                {
                    String fieldValue = jsonObjectArticle.getString("contenttype");
                    // fieldValue = jsonObjectArticle.getString("typeName");
                    if (fieldValue != null)
                    {
                        articleTableData.setContentType(fieldValue);
                    }
                }
                catch (Exception ex)
                {
                    mLogger.info("contenttype not found. Exception: " + ex.getMessage());
                }

                try
                {
                    String fieldValue = jsonObjectArticle.getString("publishdate");
                    Date dateValue = null;
                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    {
                        try {
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                            dateValue = dateFormat.parse(fieldValue);
                        }
                        catch (Exception exx)
                        {
                            // in some case I saw the format: "4000-05-08T15:07:12Z"
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                            dateValue = dateFormat.parse(fieldValue);
                        }
                    }

                    articleTableData.setPublished(dateValue);
                }
                catch (Exception ex)
                {
                    mLogger.info("publishDate not found. Exception: " + ex.getMessage());
                }

                try
                {
                    String fieldValue = jsonObjectArticle.getString("home_section_name");
                    articleTableData.setHomePage(fieldValue != null ? fieldValue : "");
                }
                catch (Exception ex)
                {
                    mLogger.info("home_section_name not found. Exception: " + ex.getMessage());
                }

                try
                {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                    String fieldValue = jsonObjectArticle.getString("starttime_date");
                    Date dateValue = null;
                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    {
                        dateValue = dateFormat.parse(fieldValue);
                    }

                    articleTableData.setStartTime(dateValue);
                }
                catch (Exception ex)
                {
                    // mLogger.info("starttime_date not found. Exception: " + ex.getMessage());
                }

                // mLogger.info("Article returned. Id: " + article.getId());

                articleTableDataList.add(articleTableData);
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Content Items", "Exception: " + e.getMessage());
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null)
                context.addMessage(null, message);
        }


        return totalSearchItems;
    }

    public Article getArticleContentDetails (String id, boolean cacheToBeUsed)
    {
        String escenicUrl = null;
        Article newDetailedArticle = null;


        if (id == null)
        {
            mLogger.error("Wrong input parameters");

            return null;
        }

        mLogger.info("getArticleContentDetails. id: " + id);

        if (escenicDisabled)
        {
            mLogger.error("Escenic service disabled");

            return null;
        }

        try {
            HashMap<String,EscenicField> metadataFields = null;
            HashMap<String,EscenicField> linksFields = new HashMap<>();
            HashMap<String,EscenicField> publicationLinksFields = null;
            List<String> stateTransitions = null;
            String contentType = null;

            escenicUrl = "http://" + escenicWebServicesHost + ":" + escenicWebServicesPort +
                "/webservice/escenic/content/" + id;

            HttpGetInfo httpGetInfo = getXML(escenicUrl, "<article type>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + escenicUrl);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            // loading
            {
                try
                {
                    // Node document = (Node) xPath.evaluate("/", new InputSource(new StringReader(sXMLReturned)), XPathConstants.NODE);

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    // factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                    /*
                    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new InputSource(new StringReader(sXMLReturned)));
                    */

                    // payload fields and payload model
                    {

                        Node payloadNode = (Node) xPath.evaluate("/entry/content/payload", document, XPathConstants.NODE);
                        HashMap<String,EscenicType> payloadModel;

                        {
                            // load the model associated to the payload
                            // <vdf:payload model="http://internal.publishing.production.rsi.ch:8080/webservice/publication/rsi/escenic/model/story">

                            String modelURL = (String) xPath.evaluate("@model", payloadNode, XPathConstants.STRING);
                            if (modelURL == null || modelURL.lastIndexOf('/') == -1)
                            {
                                mLogger.error("Wrong model URL: " + modelURL);

                                return null;
                            }


                            contentType = modelURL.substring(modelURL.lastIndexOf('/') + 1);

                            boolean modelCacheToBeUsed = true;
                            HttpGetInfo httpGetPayloadInfo = getXML(modelURL, "<model - details>",
                                "application/vnd.escenic.content-description, application/vnd.vizrt.model+xml",
                                modelCacheToBeUsed);
                            if (httpGetPayloadInfo == null || httpGetPayloadInfo.getReturnedBody() == null)
                            {
                                mLogger.error("getXML failed. URL: " + modelURL);

                                return null;
                            }
                            /*
                            HttpGetInfo httpGetPayloadInfo = getXML(modelURL, "<model>", null, modelCacheToBeUsed);
                            if (httpGetPayloadInfo == null || httpGetPayloadInfo.getReturnedBody() == null)
                            {
                                mLogger.error("getXML failed. URL: " + modelURL);

                                return null;
                            }
                            */

                            String sPayloadModelReturned = httpGetPayloadInfo.getReturnedBody();
                            // mLogger.error("Model: " + sPayloadModelReturned);

                            // DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            // factory.setNamespaceAware(true);
                            // DocumentBuilder builder = factory.newDocumentBuilder();
                            Document modelDocument = builder.parse(new java.io.ByteArrayInputStream(sPayloadModelReturned.getBytes()));

                            // vdf removed because setNamespaceAware is false
                            /*
                            Node schemaNode = (Node) xPath.evaluate("/model/schema", modelDocument, XPathConstants.NODE);

                            payloadModel = getPayloadModel(schemaNode);
                            */

                            Node contentDescriptorNode = (Node) xPath.evaluate("com.escenic.domain.ContentDescriptor", modelDocument, XPathConstants.NODE);
                            payloadModel = getPayloadModel2(contentDescriptorNode, linksFields);
                        }

                        metadataFields = getPayloadFields(document, payloadNode, payloadModel);
                    }

                    // entry fields
                    /*
                    {
                        Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                        metadataFields = getEntryFields(id.longValue(), entryNode, metadataFields);
                    }
                    */

                    // link fields and history logs
                    {
                        Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                        linksFields = getEntryLinksFields(id, entryNode, linksFields);
                        publicationLinksFields = getPublicationLinksFields(id, entryNode);
                    }

                    {
                        Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                        getEscenicMetadata(id, entryNode, metadataFields, linksFields);
                    }

                    // states
                    {
                        Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                        String stateTransitionsHRef = xPath.evaluate("control/state/@href", entryNode);
                        if (stateTransitionsHRef == null || stateTransitionsHRef.equalsIgnoreCase(""))
                        {
                            mLogger.error("stateTransitionsHref cannot be null or empty. stateTransitionsHRef: " +
                                stateTransitionsHRef);

                            return null;
                        }
                        stateTransitions = getStateTransitions(stateTransitionsHRef);
                    }
                    if (stateTransitions == null)
                    {
                        mLogger.error("stateTransitions cannot be null");

                        return null;
                    }
                }
                catch (Exception e)
                {
                    mLogger.error("Exception: " + e.getMessage() + ", sXMLReturned: " + sXMLReturned);

                    // throw e;
                }
            }

            /*
            // look for media entries
            MediaInfo mediaInfo = null;
            {
                mLogger.info("Loading media-entry-info...");

                String linkKeyField = "http://www.vizrt.com/types/relation/media-entry-info" + "-";
                EscenicField escenicField = linksFields.get(linkKeyField);

                if (escenicField != null)
                {
                    List<EscenicLink> listLinksField = escenicField.getListLinkValues();

                    if (listLinksField != null && listLinksField.size() > 0 &&
                        listLinksField.get(0).getHref() != null)
                        mediaInfo = getMediaInfo(listLinksField.get(0).getHref(), cacheToBeUsed);
                }
            }
            */

            newDetailedArticle = new Article(contentType,
                metadataFields, linksFields, publicationLinksFields,
                stateTransitions, httpGetInfo.getReturnedBody(), httpGetInfo.getETagHeader());
        }
        catch (Exception e)
        {
            mLogger.error("getArticleContentDetails (" + escenicUrl + ") failed. Exception: " + e);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Get Content Details", escenicUrl + " failed. Exception: " + e);
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null)
                context.addMessage(null, message);
        }


        return newDetailedArticle;
    }

    public MediaInfo getMediaInfo(String mediaEntryInfoURL, boolean cacheToBeUsed)
    {
        mLogger.info("getMediaEntriesInfo. mediaEntryInfoURL: " + mediaEntryInfoURL);

        MediaInfo mediaInfo = new MediaInfo();

        try {
            HttpGetInfo httpGetInfo = getXML(mediaEntryInfoURL, "<media info>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + mediaEntryInfoURL);

                return null;
            }

            String sJSONReturned = httpGetInfo.getReturnedBody();

            JSONObject jsonMediaInfo = new JSONObject(sJSONReturned);

            mediaInfo.setTranscodingState(jsonMediaInfo.getString("transcodingState"));
            mediaInfo.setProgress(jsonMediaInfo.getInt("progress"));
            mediaInfo.setGroup(jsonMediaInfo.getString("group"));
            try {
                mediaInfo.setMessage(jsonMediaInfo.getString("message"));
            }
            catch (Exception ex)
            {
                // mLogger.error("Exception: " + ex.getMessage());
            }

            if (mediaInfo.getMessage() != null)
            {
                // error is present and no more data regarding the videos are presents
                // "transcodingState":"FailedState"

                mLogger.error("Video failed. Message: " + mediaInfo.getMessage());

                return mediaInfo;
            }

            mediaInfo.setId(jsonMediaInfo.getLong("id"));
            mediaInfo.setExternalReference(jsonMediaInfo.getString("external-reference"));
            mediaInfo.setStatus(jsonMediaInfo.getString("status"));
            mediaInfo.setDuration(jsonMediaInfo.getInt("duration"));

            boolean videoPresent = false;

            try {
                JSONArray jsonVideos = jsonMediaInfo.getJSONArray("video");

                for (int index = 0; index < jsonVideos.length(); index++)
                {
                    MediaEntryInfo mediaEntryInfo = new MediaEntryInfo();

                    mediaEntryInfo.setMrid(jsonVideos.getJSONObject(index).getString("mrid"));
                    mediaEntryInfo.setHeight(jsonVideos.getJSONObject(index).getInt("height"));
                    mediaEntryInfo.setWidth(jsonVideos.getJSONObject(index).getInt("width"));
                    mediaEntryInfo.setUri(jsonVideos.getJSONObject(index).getString("uri"));
                    mediaEntryInfo.setMimeType(jsonVideos.getJSONObject(index).getString("mime-type"));
                    mediaEntryInfo.setStatus(jsonVideos.getJSONObject(index).getString("status"));

                    mediaInfo.getMediaEntriesInfo().add(mediaEntryInfo);
                }

                videoPresent = true;
            }
            catch (Exception ex)
            {
                // mLogger.error("Exception: " + ex.getMessage());
                videoPresent = false;
            }

            if (videoPresent)
            {
                mediaInfo.setMediaType(MediaInfo.MediaType.MEDIA_VIDEO);
            }
            else
            {
                boolean audioPresent = false;

                try {
                    JSONArray jsonAudios = jsonMediaInfo.getJSONArray("audio");

                    for (int index = 0; index < jsonAudios.length(); index++)
                    {
                        MediaEntryInfo mediaEntryInfo = new MediaEntryInfo();

                        mediaEntryInfo.setMrid(jsonAudios.getJSONObject(index).getString("mrid"));
                        mediaEntryInfo.setUri(jsonAudios.getJSONObject(index).getString("uri"));
                        mediaEntryInfo.setMimeType(jsonAudios.getJSONObject(index).getString("mime-type"));
                        mediaEntryInfo.setStatus(jsonAudios.getJSONObject(index).getString("status"));

                        mediaInfo.getMediaEntriesInfo().add(mediaEntryInfo);
                    }

                    audioPresent = true;
                }
                catch (Exception ex)
                {
                    // mLogger.error("Exception: " + ex.getMessage());
                    audioPresent = false;
                }

                if (audioPresent)
                {
                    mediaInfo.setMediaType(MediaInfo.MediaType.MEDIA_AUDIO);
                }
                else
                {
                    mediaInfo.setMediaType(MediaInfo.MediaType.MEDIA_NONE);
                }
            }
        }
        catch (Exception e) {

            mLogger.error("getMediaInfo. mediaEntryInfoURL: " + mediaEntryInfoURL + " failed. Exception: " + e);

            return null;    // throw new Exception(methodName + " (" + contentIdURL + ") failed. Exception: " + e);
        }


        return mediaInfo;
    }

    public HashMap<String,EscenicLock> getExternalLocks(String articleId, String lockCollectionURL)
            throws Exception
    {
        mLogger.info("getExternalLocks. lockCollectionURL: " + lockCollectionURL);

        HashMap<String,EscenicLock> externalLockHashMap = new HashMap<>();
        boolean cacheToBeUsed = false;

        try {
            HttpGetInfo httpGetInfo = getXML(lockCollectionURL, "<locks>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + lockCollectionURL);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                // Node document = (Node) xPath.evaluate("/", new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + sXMLReturned)), XPathConstants.NODE);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String fieldValue;

                NodeList locksNodeList = (NodeList) xPath.evaluate("/feed/entry", document, XPathConstants.NODESET);

                for (int lockIndex = 0; lockIndex < locksNodeList.getLength(); lockIndex++)
                {
                    Node lockEntry = locksNodeList.item(lockIndex);

                    EscenicLock escenicLock = new EscenicLock();

                    escenicLock.setId(xPath.evaluate("id/text()", lockEntry));
                    escenicLock.setTitle(xPath.evaluate("title/text()", lockEntry));
                    escenicLock.setUserName(getUserName(xPath.evaluate("author/uri/text()", lockEntry)));
                    escenicLock.setContent(xPath.evaluate("content/text()", lockEntry));
                    escenicLock.setFragment(xPath.evaluate("fragment/text()", lockEntry));
                    escenicLock.setSummary(xPath.evaluate("summary/text()", lockEntry));

                    // Expected: 2010-12-05T04:49:18.000Z
                    fieldValue = xPath.evaluate("expires/text()", lockEntry);
                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                        escenicLock.setExpires(dateFormat.parse(fieldValue));

                    // Expected: 2010-12-05T04:49:18.000Z
                    fieldValue = xPath.evaluate("updated/text()", lockEntry);
                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                        escenicLock.setUpdated(dateFormat.parse(fieldValue));

                    mLogger.info(escenicLock.toString());

                    externalLockHashMap.put(escenicLock.getFragment(), escenicLock);
                }

                mLogger.info("Locks number found for the article " + articleId + ": " + locksNodeList.getLength());
            }
        }
        catch (Exception e)
        {
            mLogger.error("getExternalLocks. lockCollectionURL: " + lockCollectionURL + " failed. Exception: " + e);

            return null;    // throw new Exception(methodName + " (" + contentIdURL + ") failed. Exception: " + e);
        }


        return externalLockHashMap;
    }


    public String getUserName(String personURL)
            throws Exception
    {
        mLogger.info("getUserName. personURL: " + personURL);

        String userName = null;
        boolean cacheToBeUsed = true;

        try {
            if (personURL == null)
            {
                mLogger.error("Wrong input parameter. personURL: " + personURL);

                return null;
            }

            HttpGetInfo httpGetInfo = getXML(personURL, "<person>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + personURL);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                // Node document = (Node) xPath.evaluate("/", new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + sXMLReturned)), XPathConstants.NODE);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                userName = xPath.evaluate("/entry/content/payload/field[@name='com.escenic.username']/value/text()", document);

                mLogger.info("PersonURL: " + personURL + ", UserName: " + userName);
            }
        }
        catch (Exception e)
        {
            mLogger.error("getUserName. personURL " + personURL + " failed. Exception: " + e);

            return null;    // throw new Exception(methodName + " (" + contentIdURL + ") failed. Exception: " + e);
        }


        return userName;
    }


    public void completeEscenicLinkInformation (EscenicLink escenicLink)
    {
        mLogger.info("completeEscenicLinkInformation. articleURL: " + escenicLink.getHref());

        try {
            boolean cacheToBeUsed = true;

            HttpGetInfo httpGetInfo = getXML(escenicLink.getHref(), "<article>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + escenicLink.getHref());

                return;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                EscenicField escenicField;

                // Node document = (Node) xPath.evaluate("/", new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + sXMLReturned)), XPathConstants.NODE);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */

                Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                {
                    if (escenicLink.getTitle() == null || escenicLink.getTitle().equalsIgnoreCase(""))
                    {
                        escenicLink.setTitle(xPath.evaluate("title/text()", entryNode));
                    }

                    if (escenicLink.getState() == null || escenicLink.getState().equalsIgnoreCase(""))
                    {
                        escenicLink.setState(xPath.evaluate("control/state[@name]", entryNode));
                    }
                }

                if (escenicLink.getThumbnailImageInfo() == null)
                {
                    ImageInfo imageInfo = getImageInfo(entryNode);

                    if (imageInfo != null)
                        escenicLink.setThumbnailImageInfo(imageInfo);
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            return;
        }

        return;
    }

    public ImageInfo getImageInfo(String articleURL, boolean cacheToBeUsed)
    {
        mLogger.info("getImageInfo. articleURL: " + articleURL);

        ImageInfo imageInfo = null;

        try
        {
            HttpGetInfo httpGetInfo = getXML(articleURL, "<article>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + articleURL);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                // Node document = (Node) xPath.evaluate("/", new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + sXMLReturned)), XPathConstants.NODE);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */

                Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                imageInfo = getImageInfo(entryNode);
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            return null;
        }

        return imageInfo;
    }

    public ImageInfo getImageInfo(Node entryNode)
    {
        ImageInfo imageInfo = null;

        try {
            String thumbnailHref = xPath.evaluate("link[@rel='thumbnail']/@href", entryNode);

            if (thumbnailHref != null && thumbnailHref.lastIndexOf('/') != -1)
            {
                String articleId = null;
                try {
                    articleId = thumbnailHref.substring(thumbnailHref.lastIndexOf('/') + 1);
                }
                catch (Exception ex)
                {
                    mLogger.error("href: " + thumbnailHref + ", Exception: " + ex.getMessage());
                }

                imageInfo = new ImageInfo();
                imageInfo.setUrl(thumbnailHref);
                if (articleId != null)
                    imageInfo.setId(articleId);
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }

        return imageInfo;
    }

    public List<EscenicSection> getSections(String escenicURL, boolean cacheToBeUsed) throws Exception
    {
        String methodName = "getSections";
        String localEscenicURL = "n. a.";

        List<EscenicSection> escenicSectionList = new ArrayList<>();

        mLogger.info("Called: " + methodName + "(" + escenicURL + ")");

        try
        {
            if (escenicDisabled)
            {
                mLogger.error("Escenic service disabled");

                return escenicSectionList;
            }

            if (escenicURL == null || escenicURL.equals(""))
                localEscenicURL  = "http://" + escenicWebServicesHost + ":" + escenicWebServicesPort + "/webservice/escenic/section/ROOT/subsections";
            else
                localEscenicURL = escenicURL;

            HttpGetInfo httpGetInfo = getXML(localEscenicURL, "<sections>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + localEscenicURL);

                return escenicSectionList;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */

                NodeList entryNodeList = (NodeList) xPath.evaluate("/feed/entry", document, XPathConstants.NODESET);


                for (int entryIndex = 0; entryIndex < entryNodeList.getLength(); entryIndex++)
                {
                    // mLogger.info("Retrieving section " + entryIndex);

                    Node entry = entryNodeList.item(entryIndex);

                    EscenicSection escenicSection = new EscenicSection();

                    escenicSection.setHref(xPath.evaluate("id/text()", entry));

                    escenicSection.setId(xPath.evaluate("identifier/text()", entry));

                    escenicSection.setHomeSection(false);

                    escenicSection.setName(xPath.evaluate("content/payload/field[@name='com.escenic.sectionName']/value/text()", entry));
                    escenicSection.setUniqueName(xPath.evaluate("content/payload/field[@name='com.escenic.uniqueName']/value/text()", entry));
                    escenicSection.setSectionParameters(xPath.evaluate("content/payload/field[@name='com.escenic.sectionParameters']/value/text()", entry));

                    escenicSection.setSummary(xPath.evaluate("summary/text()", entry));

                    escenicSection.setDownHref(xPath.evaluate("link[@rel='down']/@href", entry));

                    escenicSection.setContentItemsHref(xPath.evaluate("link[@rel='http://www.vizrt.com/types/relation/content-items']/@href", entry));

                    escenicSection.setPublicationTitle(xPath.evaluate("link[@rel='http://www.vizrt.com/types/relation/publication']/@title", entry));

                    escenicSectionList.add(escenicSection);
                }
            }
        }
        catch (Exception e)
        {

            mLogger.error(methodName + " (" + localEscenicURL + ") failed. Exception: " + e);

            mLogger.info("End call: " + methodName + "(" + escenicURL + ")");

            throw new Exception(methodName + " (" + localEscenicURL + ") failed. Exception: " + e);
        }


        mLogger.info("End call: " + methodName + "(" + escenicURL + ")");

        return escenicSectionList;
    }

    public EscenicSection getSection (String sectionURL, boolean cacheToBeUsed)
    {
        mLogger.info("getSection. sectionURL: " + sectionURL);

        EscenicSection escenicSection = new EscenicSection();

        try {
            HttpGetInfo httpGetInfo = getXML(sectionURL, "<section>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + sectionURL);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            // setting galleryImage fields: id and href
            {
                EscenicField escenicField;

                // Node document = (Node) xPath.evaluate("/", new InputSource(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + sXMLReturned)), XPathConstants.NODE);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */

                Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                {
                    escenicSection.setSummary(xPath.evaluate("summary/text()", entryNode));

                    escenicSection.setId(xPath.evaluate("identifier/text()", entryNode));
                    // escenicSection.setHomeSection(systemFields.get("metadata:home-section").getBooleanValue());

                    escenicSection.setDownHref(xPath.evaluate("link[@rel='down']/@href", entryNode));

                    escenicSection.setPublicationTitle(xPath.evaluate("link[@rel='http://www.vizrt.com/types/relation/publication']/@title", entryNode));
                }

                Node payloadNode = (Node) xPath.evaluate("/entry/content/payload", document, XPathConstants.NODE);
                HashMap<String,EscenicType> payloadModel;

                {
                    // load the model associated to the payload
                    // <vdf:payload model="http://internal.publishing.production.rsi.ch:8080/webservice/publication/rsi/escenic/model/story">

                    String modelURL = (String) xPath.evaluate("@model", payloadNode, XPathConstants.STRING);

                    if (modelURL == null)
                    {
                        mLogger.error("model payload was not found");

                        return null;
                    }

                    // HttpGetInfo httpGetPayloadInfo = getXML(modelURL, "<model>", null, true /* cacheToBeUsed */);
                    HttpGetInfo httpGetPayloadInfo = getXML(modelURL, "<model - details>",
                        "application/vnd.escenic.content-description, application/vnd.vizrt.model+xml", true /* cacheToBeUsed */);
                    if (httpGetPayloadInfo == null || httpGetPayloadInfo.getReturnedBody() == null)
                    {
                        mLogger.error("getXML failed. URL: " + modelURL);

                        return null;
                    }

                    String sPayloadModelReturned = httpGetPayloadInfo.getReturnedBody();

                    // DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    // factory.setNamespaceAware(true);
                    // DocumentBuilder builder = factory.newDocumentBuilder();
                    Document modelDocument = builder.parse(new java.io.ByteArrayInputStream(sPayloadModelReturned.getBytes()));

                    // vdf removed because setNamespaceAware is false
                    // Node schemaNode = (Node) xPath.evaluate("/model/schema", modelDocument, XPathConstants.NODE);

                    // to get much more detail information the same model URL
                    // has to be called using:
                    // Accept: application/vnd.escenic.content-description, application/vnd.vizrt.model+xml
                    Node contentDescriptorNode = (Node) xPath.evaluate("com.escenic.domain.ContentDescriptor", modelDocument, XPathConstants.NODE);
                    HashMap<String,EscenicField> linksFields = new HashMap<>();
                    payloadModel = getPayloadModel2(contentDescriptorNode, linksFields);
                    // payloadModel = getPayloadModel(schemaNode);
                }

                HashMap<String,EscenicField> payloadFields = getPayloadFields(document, payloadNode, payloadModel);

                escenicSection.setName(payloadFields.get("com.escenic.sectionName").getStringValue());
                escenicSection.setUniqueName(payloadFields.get("com.escenic.uniqueName").getStringValue());
                escenicSection.setSectionParameters(payloadFields.get("com.escenic.sectionParameters").getStringValue());

            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            return null;
        }

        return escenicSection;
    }

    synchronized public HttpGetInfo getXML(String localEscenicURL, String helpText,
        String acceptHeader, boolean cacheToBeUsed)
    {
        EscenicCache escenicCache = EscenicCache.getInstance();

        HttpGetInfo httpGetInfo = null;

        if (cacheToBeUsed)
            httpGetInfo = escenicCache.getXML(localEscenicURL);

        if (httpGetInfo == null)
        {
            httpGetInfo = loadXMLFromEscenic(localEscenicURL, acceptHeader, helpText);

            if (httpGetInfo == null)
            {
                mLogger.error("It was not able to load the XML. localEscenicURL: " + localEscenicURL + ")");

                return null;
            }

            escenicCache.storeXML(localEscenicURL, httpGetInfo);
        }

        return httpGetInfo;
    }

    private HttpGetInfo loadXMLFromEscenic(String url, String acceptHeader, String helpText)
    {
        HttpGetInfo httpGetInfo = null;

        try {
            HttpResponse response = null;

            if (userName == null || password == null)
            {
                mLogger.error("userName and/or password not initialized");

                return null;
            }

            {
                if (socksProxy)
                {
                    System.setProperty("socksProxyHost", socksProxyHost);
                    System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
                }

                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(userName, password));

                RequestConfig requestConfig = RequestConfig.custom()
                        .setSocketTimeout(escenicWebServicesServerTimeoutInMilliSeconds)
                        .setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds)
                        .build();

                CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).
                        setDefaultRequestConfig(requestConfig).build();


                // connection.setRequestProperty("Accept-Charset", "UTF-8");

                HttpGet httpget = new HttpGet(url);

                if (acceptHeader != null)
                {
                    mLogger.info("Header. " + "Accept: " + acceptHeader);
                    httpget.setHeader("Accept", acceptHeader);
                }

                mLogger.info("Escenic URL: " + url + ", Timeout: " + escenicWebServicesServerTimeoutInMilliSeconds + ", helpText: " + helpText);

                response = httpClient.execute(httpget);
            }

            if (response.getStatusLine().getStatusCode() != 200)
            {
                mLogger.error("'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode());

                throw new Exception("'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode());
            }

            HttpEntity entity = response.getEntity();

            if (entity == null)
            {
                mLogger.error("'" + url + "' failed. HttpEntity is null");

                throw new Exception("'" + url + "' failed. HttpEntity is null");
            }

            httpGetInfo = new HttpGetInfo();

            InputStream in = entity.getContent();
            String xml = IOUtils.toString(in, "UTF-8");
            //xml = EntityUtils.toString(entity);

            // xml = xml.replaceAll("\n", " ");
            // mLogger.info("httpget returned XML: " + xml);

            httpGetInfo.setReturnedBody(xml);
            Header header = response.getFirstHeader("ETag");
            if (header != null)
                httpGetInfo.setETagHeader(header.getValue());
        }
        catch (Exception e)
        {
            mLogger.error("'" + url + "' failed. Exception: " + e);

            return null;
        }

        return httpGetInfo;
    }

    public InputStream loadBinaryFromEscenic(String url)
    {
        InputStream in = null;

        try {
            HttpResponse response = null;

            if (userName == null || password == null)
            {
                mLogger.error("userName and/or password not initialized");

                return null;
            }

            {
                if (socksProxy)
                {
                    System.setProperty("socksProxyHost", socksProxyHost);
                    System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
                }

                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(userName, password));

                RequestConfig requestConfig = RequestConfig.custom()
                        .setSocketTimeout(escenicWebServicesServerTimeoutInMilliSeconds)
                        .setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds)
                        .build();

                CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).
                        setDefaultRequestConfig(requestConfig).build();


                // connection.setRequestProperty("Accept-Charset", "UTF-8");

                HttpGet httpget = new HttpGet(url);

                mLogger.info("Escenic URL: " + url + ", Timeout: " + escenicWebServicesServerTimeoutInMilliSeconds);

                response = httpClient.execute(httpget);
            }

            if (response.getStatusLine().getStatusCode() != 200)
            {
                mLogger.error("'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode());

                throw new Exception("'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode());
            }

            HttpEntity entity = response.getEntity();

            if (entity == null)
            {
                mLogger.error("'" + url + "' failed. HttpEntity is null");

                throw new Exception("'" + url + "' failed. HttpEntity is null");
            }

            in = entity.getContent();
        }
        catch (Exception e)
        {
            mLogger.error("'" + url + "' failed. Exception: " + e);

            return null;
        }

        return in;
    }

    private String loadContentItems(String url)
    {
        String json = null;

        try {
            HttpResponse response = null;

            {
                if (socksProxy)
                {
                    System.setProperty("socksProxyHost", socksProxyHost);
                    System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
                }

                RequestConfig requestConfig = RequestConfig.custom()
                        .setSocketTimeout(solrServerTimeoutInMilliSeconds)
                        .setConnectTimeout(solrServerTimeoutInMilliSeconds)
                        .build();

                CloseableHttpClient httpClient = HttpClientBuilder.create().
                        setDefaultRequestConfig(requestConfig).build();

                HttpGet httpget = new HttpGet(url);

                mLogger.info("ContentItems URL: " + url + ", Timeout: " + solrServerTimeoutInMilliSeconds);

                response = httpClient.execute(httpget);
            }

            if (response.getStatusLine().getStatusCode() != 200)
            {
                mLogger.error("'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode());

                throw new Exception("'" + url + "' failed. Status code from escenic: " + response.getStatusLine().getStatusCode());
            }

            HttpEntity entity = response.getEntity();

            if (entity == null)
            {
                mLogger.error("'" + url + "' failed. HttpEntity is null");

                throw new Exception("'" + url + "' failed. HttpEntity is null");
            }

            InputStream in = entity.getContent();
            json = IOUtils.toString(in, "UTF-8");
            // json = EntityUtils.toString(entity);

            // mLogger.info("httpget returned JSON: " + json);
        }
        catch (Exception e)
        {
            mLogger.error("'" + url + "' failed. Exception: " + e);

            return null;
        }

        return json;
    }

    public String lockResource(String lockCollectionURL, String fragment)
    {
        mLogger.info("lockResource. lockCollectionURL: " + lockCollectionURL + ", fragment: " + fragment);

        String privateLockURL = null;
        // String httpPostReturn = "";

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            Element entryElement = doc.createElement("entry");
            doc.appendChild(entryElement);

            Attr xmlns = doc.createAttribute("xmlns");
            xmlns.setValue("http://www.w3.org/2005/Atom");
            entryElement.setAttributeNode(xmlns);

            Attr xmlnsMetadata = doc.createAttribute("xmlns:metadata");
            xmlnsMetadata.setValue("http://xmlns.escenic.com/2010/atom-metadata");
            entryElement.setAttributeNode(xmlnsMetadata);

            Element summaryElement = doc.createElement("summary");
            summaryElement.appendChild(doc.createTextNode("Lock created by: " + getUserName() + " from WS"));
            entryElement.appendChild(summaryElement);

            Attr type = doc.createAttribute("type");
            type.setValue("text");
            summaryElement.setAttributeNode(type);

            Element fragmentElement = doc.createElement("metadata:fragment");
            fragmentElement.appendChild(doc.createTextNode(fragment));
            entryElement.appendChild(fragmentElement);

            String postBodyRequest;
            {
                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, result);

                postBodyRequest = writer.toString();
            }

            mLogger.info("POST XML: " + postBodyRequest);

            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            URL uURL = new URL(lockCollectionURL);
            HttpURLConnection con = (HttpURLConnection)uURL.openConnection();
            con.setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds);
            con.setDoOutput(true); // false because I do not need to append any data to this request

            con.setRequestMethod("POST");

            // String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes("utf-8"));
            String encoded = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes("utf-8"));
            con.setRequestProperty("Authorization", "Basic " + encoded);
            mLogger.info("Header. " + "Authorization: " + "Basic " + encoded);

            con.setRequestProperty("User-Agent", "RSIInternalAPP");
            mLogger.info("Header. " + "User-Agent: " + "RSIInternalAPP");

            con.setRequestProperty("Host", escenicWebServicesHost + ":" + escenicWebServicesPort);
            mLogger.info("Header. " + "Host: " + escenicWebServicesHost + ":" + escenicWebServicesPort);

            int clength = postBodyRequest.getBytes().length;
            if(clength > 0)
            {
                con.setRequestProperty("Content-Type", "application/atom+xml; type=entry");
                mLogger.info("Header. " + "Content-Type: " + "application/atom+xml; type=entry");

                con.setRequestProperty("Content-Length", String.valueOf(clength));
                mLogger.info("Header. " + "Content-Length: " + String.valueOf(clength));

                con.setDoInput(true); // false means the response is ignored

                // con.getOutputStream().write(postBodyRequest.getBytes(), 0, clength);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postBodyRequest);
                wr.flush();
                wr.close();
            }

            int responseCode = con.getResponseCode();
            privateLockURL = con.getHeaderField("Location");
            // mLogger.error("responseCode: " + responseCode + ", privateLockURL: " + privateLockURL);

            if (responseCode != 201 || privateLockURL == null)
            {
                mLogger.error("Lock for the fragment " + fragment + " failed. responseCode: " + responseCode + ", privateLockURL: " + privateLockURL);

                return null;
            }

            /*
            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            boolean createdSuccessful = false;
            while ((line = rd.readLine()) != null)
            {
                if (line.contains("201 Created"))
                    createdSuccessful = true;
                else if (line.startsWith("Location: "))
                    privateLockURL = line.substring("Location: ".length() + 1);

                httpPostReturn += (line + "\n");
            }
            rd.close();

            if (!createdSuccessful || privateLockURL == null)
            {
                mLogger.error("Lock for the fragment " + fragment + " failed. httpPostReturn: " + httpPostReturn);

                return null;
            }
            */

            mLogger.info("Resource locked successfully, responseCode: " + responseCode + ", privateLockURL: " + privateLockURL);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Fragment '" + fragment + "' locked successfully.");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null)
                context.addMessage(null, message);
        }
        catch(Exception e)
        {
            mLogger.error("httpPost exception: " + e + ", URL: " + lockCollectionURL);

            return null;
        }

        return privateLockURL;
    }

    public String newArticle(ContentType contentType, State state, String title,
        File binaryFile, EscenicSection escenicSection,
        ContentType galleryContentType, List<String> galleryContentItemURLs)
    {
        String newContentItemURL = null;
        String binaryArticleURL = null;

        try
        {
            String linkMimeType = null;

            if (binaryFile != null)
            {
                linkMimeType = MimeTypes.getMimeType(binaryFile.getName());
                if (linkMimeType == null)
                {
                    mLogger.error("No able to build the link mime type. FileName: " + binaryFile.getAbsolutePath());

                    return null;
                }

                binaryArticleURL = getBinaryURL(binaryFile, linkMimeType);

                if (binaryArticleURL == null)
                {
                    mLogger.error("binaryArticleURL is null");

                    return null;
                }
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            Element entryElement = doc.createElement("entry");
            doc.appendChild(entryElement);

            Attr xmlnsAttribute = doc.createAttribute("xmlns");
            xmlnsAttribute.setValue("http://www.w3.org/2005/Atom");
            entryElement.setAttributeNode(xmlnsAttribute);

            Attr appAttribute = doc.createAttribute("xmlns:app");
            appAttribute.setValue("http://www.w3.org/2007/app");
            entryElement.setAttributeNode(appAttribute);

            Attr vdfAttribute = doc.createAttribute("xmlns:vdf");
            vdfAttribute.setValue("http://www.vizrt.com/types");
            entryElement.setAttributeNode(vdfAttribute);

            Attr metadataAttribute = doc.createAttribute("xmlns:metadata");
            metadataAttribute.setValue("http://xmlns.escenic.com/2010/atom-metadata");
            entryElement.setAttributeNode(metadataAttribute);

            Attr dctermsAttribute = doc.createAttribute("xmlns:dcterms");
            dctermsAttribute.setValue("http://purl.org/dc/terms/");
            entryElement.setAttributeNode(dctermsAttribute);

            {
                Element titleElement = doc.createElement("title");
                titleElement.appendChild(doc.createTextNode(title));
                entryElement.appendChild(titleElement);

                Attr typeAttribute = doc.createAttribute("type");
                typeAttribute.setValue("text");
                titleElement.setAttributeNode(typeAttribute);
            }

            if (state.getState().equalsIgnoreCase("draft"))
            {
                Element appControlElement = doc.createElement("app:control");
                entryElement.appendChild(appControlElement);

                Element appDraftElement = doc.createElement("app:draft");
                appDraftElement.appendChild(doc.createTextNode("yes"));
                appControlElement.appendChild(appDraftElement);
            }
            else
            {
                Element appControlElement = doc.createElement("app:control");
                entryElement.appendChild(appControlElement);

                Element vaextStateElement = doc.createElement("vaext:state");
                vaextStateElement.appendChild(doc.createTextNode(state.getState()));
                appControlElement.appendChild(vaextStateElement);

                Attr vaextAttribute = doc.createAttribute("xmlns:vaext");
                vaextAttribute.setValue("http://www.vizrt.com/atom-ext");
                vaextStateElement.setAttributeNode(vaextAttribute);
            }

            {
                Element contentElement = doc.createElement("content");
                entryElement.appendChild(contentElement);

                Attr typeAttribute = doc.createAttribute("type");
                typeAttribute.setValue("application/vnd.vizrt.payload+xml");
                contentElement.setAttributeNode(typeAttribute);

                {
                    Element vdfPayloadElement = doc.createElement("vdf:payload");
                    contentElement.appendChild(vdfPayloadElement);

                    Attr modelAttribute = doc.createAttribute("model");
                    modelAttribute.setValue(contentType.getUrl());
                    vdfPayloadElement.setAttributeNode(modelAttribute);

                    {
                        // HttpGetInfo httpGetPayloadInfo = getXML(contentType.getUrl(), "<model>", null, true /* cacheToBeUsed */);
                        HttpGetInfo httpGetPayloadInfo = getXML(contentType.getUrl(), "<model - details>",
                                "application/vnd.escenic.content-description, application/vnd.vizrt.model+xml", true /* cacheToBeUsed */);
                        if (httpGetPayloadInfo == null || httpGetPayloadInfo.getReturnedBody() == null)
                        {
                            mLogger.error("getXML failed. URL: " + contentType.getUrl());

                            return null;
                        }

                        String sPayloadModelReturned = httpGetPayloadInfo.getReturnedBody();

                        Document modelDocument = docBuilder.parse(new java.io.ByteArrayInputStream(
                            sPayloadModelReturned.getBytes()));

                        // vdf removed because setNamespaceAware is false
                        // Node schemaNode = (Node) xPath.evaluate("/model/schema", modelDocument, XPathConstants.NODE);

                        // to get much more detail information the same model URL
                        // has to be called using:
                        // Accept: application/vnd.escenic.content-description, application/vnd.vizrt.model+xml
                        Node contentDescriptorNode = (Node) xPath.evaluate("com.escenic.domain.ContentDescriptor", modelDocument, XPathConstants.NODE);
                        HashMap<String,EscenicField> linksFields = new HashMap<>();
                        HashMap<String,EscenicType> payloadModel = getPayloadModel2(contentDescriptorNode, linksFields);
                        // HashMap<String,EscenicType> payloadModel = getPayloadModel(schemaNode);

                        for (String modelKey: payloadModel.keySet())
                        {
                            EscenicType escenicType = payloadModel.get(modelKey);

                            if (binaryFile != null && binaryArticleURL != null && linkMimeType != null &&
                                escenicType.getKeyField().equalsIgnoreCase("binary"))
                            {
                                // binary is added below

                                continue;
                            }

                            Element vdfFieldElement = doc.createElement("vdf:field");
                            vdfPayloadElement.appendChild(vdfFieldElement);

                            Attr nameAttribute = doc.createAttribute("name");
                            nameAttribute.setValue(escenicType.getKeyField());
                            vdfFieldElement.setAttributeNode(nameAttribute);

                            if (escenicType.getType() == EscenicType.Type.ESCENIC_NESTEDSTRINGTYPE ||
                                escenicType.getType() == EscenicType.Type.ESCENIC_NESTEDCOMPLEXTYPE)
                            {
                                Element vdfListElement = doc.createElement("vdf:list");
                                vdfFieldElement.appendChild(vdfListElement);
                            }
                            else // EscenicType.Type.ESCENIC_SIMPLE
                            {
                                // title is used for most of the types
                                if (escenicType.getKeyField().equalsIgnoreCase("title"))
                                {
                                    Element vdfValueElement = doc.createElement("vdf:value");
                                    vdfValueElement.appendChild(doc.createTextNode(title));
                                    vdfFieldElement.appendChild(vdfValueElement);
                                }
                                // name is used for the keyframe type
                                else if (escenicType.getKeyField().equalsIgnoreCase("name"))
                                {
                                    Element vdfValueElement = doc.createElement("vdf:value");
                                    vdfValueElement.appendChild(doc.createTextNode(title));
                                    vdfFieldElement.appendChild(vdfValueElement);
                                }
                                else
                                {
                                    /*
                                    Element vdfValueElement = doc.createElement("vdf:value");
                                    // to evaluate if value represents the default value and if we want to set a default
                                    // vdfValueElement.appendChild(doc.createTextNode(escenicType.getValue()));
                                    vdfFieldElement.appendChild(vdfValueElement);
                                    */
                                }
                            }
                        }

                        if (binaryFile != null && binaryArticleURL != null && linkMimeType != null)
                        {
                            Element vdfFieldElement = doc.createElement("vdf:field");
                            vdfPayloadElement.appendChild(vdfFieldElement);

                            Attr nameAttribute = doc.createAttribute("name");
                            nameAttribute.setValue("binary");
                            vdfFieldElement.setAttributeNode(nameAttribute);

                            Element vdfValueElement = doc.createElement("vdf:value");
                            vdfFieldElement.appendChild(vdfValueElement);

                            Element linkElement = doc.createElement("link");
                            vdfValueElement.appendChild(linkElement);

                            Attr relAttribute = doc.createAttribute("rel");
                            relAttribute.setValue("edit-media");
                            linkElement.setAttributeNode(relAttribute);

                            Attr hrefAttribute = doc.createAttribute("href");
                            hrefAttribute.setValue(binaryArticleURL);
                            linkElement.setAttributeNode(hrefAttribute);

                            Attr linkTypeAttribute = doc.createAttribute("type");
                            linkTypeAttribute.setValue(linkMimeType);
                            linkElement.setAttributeNode(linkTypeAttribute);

                            Attr linkTitleAttribute = doc.createAttribute("title");
                            linkTitleAttribute.setValue(binaryFile.getName());
                            linkElement.setAttributeNode(linkTitleAttribute);
                        }
                    }
                }
            }

            // in case of gallery, management of the contents to be added into the gallery
            if (galleryContentType != null && galleryContentItemURLs != null && galleryContentItemURLs.size() > 0)
            {
                for (String contentItemURL: galleryContentItemURLs)
                {
                    Element linkElement = doc.createElement("link");
                    entryElement.appendChild(linkElement);

                    Attr relAttribute = doc.createAttribute("rel");
                    relAttribute.setValue("related");
                    linkElement.setAttributeNode(relAttribute);

                    Attr hrefAttribute = doc.createAttribute("href");
                    hrefAttribute.setValue(contentItemURL);
                    linkElement.setAttributeNode(hrefAttribute);

                    Attr linkTypeAttribute = doc.createAttribute("type");
                    linkTypeAttribute.setValue("application/atom+xml; type=entry");
                    linkElement.setAttributeNode(linkTypeAttribute);

                    Attr groupAttribute = doc.createAttribute("metadata:group");
                    groupAttribute.setValue("pictureRel");
                    linkElement.setAttributeNode(groupAttribute);

                    if (galleryContentType.getType().equalsIgnoreCase("picture"))
                    {
                        ImageInfo imageInfo = getImageInfo(contentItemURL, true /* cacheToBeUsed */);

                        Attr thumbnailAttribute = doc.createAttribute("metadata:thumbnail");
                        thumbnailAttribute.setValue(imageInfo.getUrl());
                        linkElement.setAttributeNode(thumbnailAttribute);
                    }

                    Element vdfPayloadElement = doc.createElement("vdf:payload");
                    linkElement.appendChild(vdfPayloadElement);

                    Attr modelAttribute = doc.createAttribute("model");
                    modelAttribute.setValue(galleryContentType.getUrl());
                    vdfPayloadElement.setAttributeNode(modelAttribute);

                    Element vdfFieldElement = doc.createElement("vdf:field");
                    vdfPayloadElement.appendChild(vdfFieldElement);

                    Attr nameAttribute = doc.createAttribute("name");
                    nameAttribute.setValue("title");
                    vdfFieldElement.setAttributeNode(nameAttribute);

                    Element vdfValueElement = doc.createElement("vdf:value");
                    vdfValueElement.appendChild(doc.createTextNode(title));
                    vdfFieldElement.appendChild(vdfValueElement);
                }

                Element appControlElement = doc.createElement("app:control");
                entryElement.appendChild(appControlElement);

                Element vaextStateElement = doc.createElement("vaext:state");
                vaextStateElement.appendChild(doc.createTextNode(state.getState()));
                appControlElement.appendChild(vaextStateElement);

                Attr vaextAttribute = doc.createAttribute("xmlns:vaext");
                vaextAttribute.setValue("http://www.vizrt.com/atom-ext");
                vaextStateElement.setAttributeNode(vaextAttribute);
            }

            String postBodyRequest;
            {
                DOMSource domSource = new DOMSource(doc);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.transform(domSource, result);

                postBodyRequest = writer.toString();
            }

            mLogger.info("POST XML: " + postBodyRequest);

            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            URL uURL = new URL(escenicSection.getContentItemsHref());
            mLogger.info("URL: " + escenicSection.getContentItemsHref());
            HttpURLConnection con = (HttpURLConnection)uURL.openConnection();
            con.setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds);
            con.setDoOutput(true); // false because I do not need to append any data to this request

            con.setRequestMethod("POST");

            // String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes("utf-8"));
            String encoded = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes("utf-8"));
            con.setRequestProperty("Authorization", "Basic " + encoded);
            mLogger.info("Header. " + "Authorization: " + "Basic " + encoded);

            con.setRequestProperty("User-Agent", "RSIInternalAPP");
            mLogger.info("Header. " + "User-Agent: " + "RSIInternalAPP");

            con.setRequestProperty("Host", escenicWebServicesHost + ":" + escenicWebServicesPort);
            mLogger.info("Header. " + "Host: " + escenicWebServicesHost + ":" + escenicWebServicesPort);

            con.setRequestProperty("Accept", "*/*");
            mLogger.info("Header. " + "Accept: " + "*/*");

            int clength = postBodyRequest.getBytes().length;
            if(clength > 0)
            {
                con.setRequestProperty("Content-Type", "application/atom+xml");
                mLogger.info("Header. " + "Content-Type: " + "application/atom+xml");

                con.setRequestProperty("Content-Length", String.valueOf(clength));
                mLogger.info("Header. " + "Content-Length: " + String.valueOf(clength));

                con.setDoInput(true); // false means the response is ignored

                // con.getOutputStream().write(postBodyRequest.getBytes(), 0, clength);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postBodyRequest);
                wr.flush();
                wr.close();
            }

            int responseCode = con.getResponseCode();
            newContentItemURL = con.getHeaderField("Location");
            // mLogger.error("responseCode: " + responseCode + ", privateLockURL: " + privateLockURL);

            if (responseCode != 201 || newContentItemURL == null)
            {
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                String httpPutReturn = "";
                int maxLines = 3;
                int lineIndex = 0;
                while ((line = rd.readLine()) != null && lineIndex++ < maxLines)
                {
                    httpPutReturn += (line + "\n");
                }
                rd.close();

                mLogger.error("Creating the new content (" + escenicSection.getContentItemsHref() + ") failed. responseCode: " + responseCode + ", " + con.getResponseMessage() + ", body: " + httpPutReturn);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Content", "Creating failed. responseCode: " + responseCode + ", Error: " + httpPutReturn);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return null;
            }

            mLogger.info("Article created successfully, responseCode: " + responseCode + ", newContentItemURL: " + newContentItemURL);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Content '" + contentType.getLabel() + "' created successfully.");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }
        catch(Exception e)
        {
            mLogger.error("httpPost exception: " + e + ", URL: " + escenicSection.getContentItemsHref());

            return null;
        }

        return newContentItemURL;
    }

    public String getBinaryURL(File binaryFile, String linkMimeType)
    {
        String binaryURL = null;

        try
        {
            mLogger.info("POST (binary): " + binaryFile.getAbsolutePath());

            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            String sURL = "http://" + escenicWebServicesHost + ":" + escenicWebServicesPort + "/webservice/escenic/binary";
            URL uURL = new URL(sURL);
            mLogger.info("URL: " + sURL);
            HttpURLConnection con = (HttpURLConnection)uURL.openConnection();
            con.setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds);
            con.setDoOutput(true); // false because I do not need to append any data to this request

            con.setRequestMethod("POST");

            // String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes("utf-8"));
            String encoded = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes("utf-8"));
            con.setRequestProperty("Authorization", "Basic " + encoded);
            mLogger.info("Header. " + "Authorization: " + "Basic " + encoded);

            con.setRequestProperty("User-Agent", "RSIInternalAPP");
            mLogger.info("Header. " + "User-Agent: " + "RSIInternalAPP");

            con.setRequestProperty("Host", escenicWebServicesHost + ":" + escenicWebServicesPort);
            mLogger.info("Header. " + "Host: " + escenicWebServicesHost + ":" + escenicWebServicesPort);

            long clength = binaryFile.length();
            if(clength > 0)
            {
                con.setRequestProperty("Content-Type", linkMimeType);
                mLogger.info("Header. " + "Content-Type: " + linkMimeType);

                con.setRequestProperty("Content-Length", String.valueOf(clength));
                mLogger.info("Header. " + "Content-Length: " + String.valueOf(clength));

                con.setDoInput(true); // false means the response is ignored

                OutputStream output = con.getOutputStream();
                InputStream file = new FileInputStream(binaryFile);

                {
                    byte[] buffer = new byte[1024 * 10];
                    int length;
                    while ((length = file.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    output.flush();
                    output.close();
                }
            }

            int responseCode = con.getResponseCode();

            binaryURL = con.getHeaderField("Location");
            // mLogger.error("responseCode: " + responseCode + ", privateLockURL: " + privateLockURL);

            if (responseCode != 201 || binaryURL == null)
            {
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                String httpPutReturn = "";
                int maxLines = 3;
                int lineIndex = 0;
                while ((line = rd.readLine()) != null && lineIndex++ < maxLines)
                {
                    httpPutReturn += (line + "\n");
                }
                rd.close();

                mLogger.error("Creating the new content (" + sURL + ") failed. responseCode: " + responseCode + ", " + con.getResponseMessage() + ", body: " + httpPutReturn);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Content", "Creating failed. responseCode: " + responseCode + ", Error: " + httpPutReturn);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return null;
            }

            mLogger.info("Binary Article created successfully, responseCode: " + responseCode + ", binaryURL: " + binaryURL);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Binary article created successfully.");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }
        catch(Exception e)
        {
            mLogger.error("httpPost exception: " + e);

            return null;
        }

        return binaryURL;
    }

    public void deleteArticle(Article article)
        throws Exception
    {
        if (article.getLinksFields() == null ||
                article.getLinksFields().get("self-") == null ||
                article.getLinksFields().get("self-").getListLinkValues() == null ||
                article.getLinksFields().get("self-").getListLinkValues().size() == 0 ||
                article.getLinksFields().get("self-").getListLinkValues().get(0) == null ||
                article.getLinksFields().get("self-").getListLinkValues().get(0).getHref() == null)
        {
            mLogger.error("Content URL was not found");

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Content", "Content URL was not found");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        String url = article.getLinksFields().get("self-").getListLinkValues().get(0).getHref();
        String id = article.getMetadataFields().get("com.escenic.displayId").getStringValue();


        try {
            if (userName == null || password == null)
            {
                mLogger.error("userName and/or password not initialized");

                throw new Exception("userName and/or password not initialized");
            }

            if (url == null)
            {
                mLogger.error("url is null");

                throw new Exception("url is null");
            }

            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            URL uURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection)uURL.openConnection();
            con.setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds);
            con.setDoOutput(false); // false because I do not need to append any data to this request
            con.setDoInput(true); // false means the response is ignored

            con.setRequestMethod("DELETE");

            // String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes("utf-8"));
            String encoded = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes("utf-8"));
            con.setRequestProperty("Authorization", "Basic " + encoded);
            mLogger.info("Header. " + "Authorization: " + "Basic " + encoded);

            con.setRequestProperty("User-Agent", "RSIInternalAPP");
            mLogger.info("Header. " + "User-Agent: " + "RSIInternalAPP");

            con.setRequestProperty("Host", escenicWebServicesHost + ":" + escenicWebServicesPort);
            mLogger.info("Header. " + "Host: " + escenicWebServicesHost + ":" + escenicWebServicesPort);

            int responseCode = con.getResponseCode();

            if (responseCode != 200)
            {
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                String httpPutReturn = "";
                int maxLines = 3;
                int lineIndex = 0;
                while ((line = rd.readLine()) != null && lineIndex++ < maxLines)
                {
                    httpPutReturn += (line + "\n");
                }
                rd.close();

                mLogger.error("Delete of '" + url + "' failed. Status code from escenic: " + responseCode);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Content", "Delete of the Content " + id + " failed. responseCode: " + responseCode + ", Error: " + httpPutReturn);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                throw new Exception("'" + url + "' failed. Status code from escenic: " + responseCode);
            }

            mLogger.info("Article deleted successfully, url: " + url + ", responseCode: " + responseCode);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Content '" + id + "' deleted successfully.");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }
        catch (Exception e)
        {
            mLogger.error("'" + url + "' failed. Exception: " + e);
        }

        return;
    }

    public void saveArticle(String id, String eTagHeader, List<String> privateLocksURLs, String xmlNewArticleSource)
            throws Exception
    {

        // String httpPutReturn = "";
        String escenicUrl = "";

        try {

            /*
            if (escenicWebServicesHost.contains("production"))
            {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Functionality enabled only on staging.");
                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }
            */

            escenicUrl = "http://" + escenicWebServicesHost + ":" + escenicWebServicesPort +
                    "/webservice/escenic/content/" + id;

            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            URL uURL = new URL(escenicUrl);
            HttpURLConnection con = (HttpURLConnection)uURL.openConnection();
            con.setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds);
            con.setDoOutput(true);

            con.setRequestMethod("PUT");

            // String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes("utf-8"));
            String encoded = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes("utf-8"));
            con.setRequestProperty("Authorization", "Basic " + encoded);
            mLogger.info("Header. " + "Authorization: " + "Basic " + encoded);

            con.setRequestProperty("User-Agent", "RSIInternalAPP");
            mLogger.info("Header. " + "User-Agent: " + "RSIInternalAPP");

            con.setRequestProperty("Host", escenicWebServicesHost + ":" + escenicWebServicesPort);
            mLogger.info("Header. " + "Host: " + escenicWebServicesHost + ":" + escenicWebServicesPort);

            con.setRequestProperty("If-Match", eTagHeader);
            mLogger.info("Header. " + "If-Match: " + eTagHeader);

            int clength = xmlNewArticleSource.getBytes().length;
            if(clength > 0)
            {
                con.setRequestProperty("Content-Type", "application/atom+xml; type=entry");
                mLogger.info("Header. " + "Content-Type: " + "application/atom+xml; type=entry");

                con.setRequestProperty("Content-Length", String.valueOf(clength));
                mLogger.info("Header. " + "Content-Length: " + String.valueOf(clength));

                for (String privateLockURL : privateLocksURLs)
                {
                    con.setRequestProperty("X-Escenic-Locks", privateLockURL);
                    mLogger.info("Header. " + "X-Escenic-Locks: " + privateLockURL);
                }

                con.setDoInput(true);

                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(xmlNewArticleSource);
                wr.flush();
                wr.close();
            }

            int responseCode = con.getResponseCode();
            // mLogger.error("responseCode: " + responseCode);

            if (responseCode != 200 && responseCode != 204)
            {
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                String httpPutReturn = "";
                int maxLines = 3;
                int lineIndex = 0;
                while ((line = rd.readLine()) != null && lineIndex++ < maxLines)
                {
                    httpPutReturn += (line + "\n");
                }
                rd.close();

                mLogger.error("Saving of the content failed. responseCode: " + responseCode + ", " + con.getResponseMessage() + ", body: " + httpPutReturn);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Content", "Saving failed. responseCode: " + responseCode + ", Error: " + httpPutReturn);
                // RequestContext.getCurrentInstance().showMessageInDialog(message);
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                throw new Exception("Saving failed. responseCode: " + responseCode + ", Error: " + httpPutReturn);
            }

            mLogger.info("Content saved successfully, responseCode: " + responseCode);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Content saved successfully.");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
        }
        catch(Exception e)
        {
            mLogger.error("httpPut exception: " + e.getMessage() + ", URL: " + escenicUrl);

            throw new Exception("httpPut exception: " + e.getMessage() + ", URL: " + escenicUrl);
        }
    }

    public void removeLock(String publicLockURL)
    {
        mLogger.info("removeLock: " + publicLockURL);

        try
        {
            if (socksProxy)
            {
                System.setProperty("socksProxyHost", socksProxyHost);
                System.setProperty("socksProxyPort", String.valueOf(socksProxyPort));
            }

            URL uURL = new URL(publicLockURL);
            HttpURLConnection con = (HttpURLConnection)uURL.openConnection();
            con.setConnectTimeout(escenicWebServicesServerTimeoutInMilliSeconds);
            con.setDoOutput(false); // false because I do not need to append any data to this request

            con.setRequestMethod("DELETE");

            // String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes("utf-8"));
            String encoded = DatatypeConverter.printBase64Binary((userName + ":" + password).getBytes("utf-8"));
            con.setRequestProperty("Authorization", "Basic " + encoded);
            mLogger.info("Header. " + "Authorization: " + "Basic " + encoded);

            con.setRequestProperty("User-Agent", "RSIInternalAPP");
            mLogger.info("Header. " + "User-Agent: " + "RSIInternalAPP");

            con.setRequestProperty("Host", escenicWebServicesHost + ":" + escenicWebServicesPort);
            mLogger.info("Header. " + "Host: " + escenicWebServicesHost + ":" + escenicWebServicesPort);

            con.setDoInput(true); // false means the response is ignored

            if (con.getResponseCode() != 204)
            {
                mLogger.error("HTTP DELETE failed. responseCode: " + con.getResponseCode() + ", " + con.getResponseMessage());

                return;
            }

            mLogger.info("Lock removed successfully. PublicLockURL: " + publicLockURL);
        }
        catch(Exception e)
        {
            mLogger.error("httpDelete (" + publicLockURL + ") exception: " + e);

            // throw e;
        }
    }

    /*
    private String getSearchURLTemplate (String contentItemsURL, HashSet<String> urlsRequested) throws Exception {
        String methodName = "getSearchURLTemplate";

        mLogger.info("Called: " + methodName + "(" + contentItemsURL + "<urlsRequested>" + ")");

        String searchXMLURL;
        String searchURLTemplate = null;

        try {

            String sXMLReturned = getXML(contentItemsURL);
            if (sXMLReturned == null)
            {
                mLogger.error("getXML failed. URL: " + contentItemsURL);

                return searchURLTemplate;
            }

            {
                Document docXMLMetaData;
                NodeList nodeURLElementMetaData;

                // docXMLMetaData =
                // DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(isBodyResponse);
                docXMLMetaData = DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(
                                sXMLReturned)));

                urlsRequested.add(contentItemsURL);

                // feed
                mLogger.info("Looking for the 'feed' tag");
                nodeURLElementMetaData = docXMLMetaData
                        .getElementsByTagName("feed");
                if (nodeURLElementMetaData.getLength() == 0){
                    mLogger.error(methodName + " (" + contentItemsURL + ") failed. No 'feed' tag found. Wrong XML. (XML Returned: "
                            + sXMLReturned + ")");

                    throw new Exception(methodName + " (" + contentItemsURL + ") failed. No 'feed' tag found. Wrong XML. (XML Returned: " + sXMLReturned + ")");
                }

                searchXMLURL = getFirstLinkDetails((Element) nodeURLElementMetaData.item(0), "href", "search");
                if (searchXMLURL == null || searchXMLURL.equals("")){
                    mLogger.error(methodName + " (" + contentItemsURL + ") failed. No 'search' link found. Wrong XML. (XML Returned: "
                            + sXMLReturned + ")");

                    // throw new Exception(methodName + " (" + contentItemsURL + ") failed. No 'search' link found. Wrong XML. (XML Returned: " + sXMLReturned + ")");
                }
            }
        } catch (Exception e) {

            mLogger.error(methodName + " (" + contentItemsURL + ") failed. Exception: " + e);

            throw new Exception(methodName + " (" + contentItemsURL + ") failed. Exception: " + e);
        }

        try {

            String sXMLReturned = getXML(searchXMLURL);
            if (sXMLReturned == null)
            {
                mLogger.error("getXML failed. URL: " + searchXMLURL);

                return searchURLTemplate;
            }

            {
                Document docXMLMetaData;
                NodeList nodeURLElementMetaData;
                Element eURLElement;
                String type;

                // docXMLMetaData =
                // DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(isBodyResponse);
                docXMLMetaData = DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(
                                sXMLReturned)));

                urlsRequested.add(searchXMLURL);

                // feed
                mLogger.info("Looking for the 'OpenSearchDescription' tag");
                nodeURLElementMetaData = docXMLMetaData
                        .getElementsByTagName("OpenSearchDescription");
                if (nodeURLElementMetaData.getLength() == 0){
                    mLogger.error(methodName + " (" + searchXMLURL + ") failed. No 'OpenSearchDescription' tag found. Wrong XML. (XML Returned: "
                            + sXMLReturned + ")");

                    throw new Exception(methodName + " (" + searchXMLURL + ") failed. No 'OpenSearchDescription' tag found. Wrong XML. (XML Returned: " + sXMLReturned + ")");
                }

                nodeURLElementMetaData = docXMLMetaData
                        .getElementsByTagName("Url");

                for (int urlIndex = 0; urlIndex < nodeURLElementMetaData.getLength(); urlIndex++) {
                    if (nodeURLElementMetaData.item(urlIndex).getNodeType() != Node.ELEMENT_NODE) {
                        mLogger.error(methodName + " (" + searchXMLURL + ") failed. Wrong XML. (XML Returned: "
                                + sXMLReturned + ")");

                        throw new Exception(
                                methodName + " (" + searchXMLURL + ") failed. Wrong XML. (XML Returned: " + sXMLReturned + ")");
                    }

                    eURLElement = (Element) nodeURLElementMetaData.item(urlIndex);

                    type = eURLElement.getAttribute("type");
                    if (type != null && type.equals("application/atom+xml"))
                    {
                        searchURLTemplate = eURLElement.getAttribute("template");
                        if (searchURLTemplate != null && !searchURLTemplate.equals(""))
                        {
                            break;
                        }
                        else {
                            searchURLTemplate = null;
                        }
                    }
                }
            }
        } catch (Exception e) {

            mLogger.error(methodName + " (" + searchXMLURL + ") failed. Exception: " + e);

            throw new Exception(methodName + " (" + searchXMLURL + ") failed. Exception: " + e);
        }

        return searchURLTemplate;
    }
    */

    private String getFirstLinkDetails (Element eLinkParentElement, String fieldName, String relName) throws Exception {
        NodeList nodeLinkElementMetaData;
        String rel;
        String fieldValue;
        String linkFieldValue;
        Element eLinkElement;

        linkFieldValue = null;
        nodeLinkElementMetaData = eLinkParentElement.getElementsByTagName("link");

        for (int linkIndex = 0; linkIndex < nodeLinkElementMetaData.getLength(); linkIndex++) {
            if (nodeLinkElementMetaData.item(linkIndex).getNodeType() != Node.ELEMENT_NODE) {
                mLogger.error("getLinkDetails failed. Wrong XML.");

                throw new Exception("getLinkDetails failed. Wrong XML.");
            }

            eLinkElement = (Element) nodeLinkElementMetaData.item(linkIndex);

            rel = eLinkElement.getAttribute("rel");
            if (rel != null && rel.endsWith(relName))
            {
                fieldValue = eLinkElement.getAttribute(fieldName);
                if (fieldValue != null && !fieldValue.equals(""))
                {
                    linkFieldValue = fieldValue;

                    break;
                }
            }
        }

        return linkFieldValue;
    }

    /*
    public static String serializeDoc(Node doc)
    {
        StringWriter outText = new StringWriter();
        StreamResult sr = new StreamResult(outText);
        Properties oprops = new Properties();
        oprops.put(OutputKeys.METHOD, "xml");
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
            t.setOutputProperties(oprops);
            t.transform(new DOMSource(doc), sr);
        } catch (Exception e) {
            System.out.println(e);
        }

        // it will be like: <?xml version="1.0" encoding="UTF-8"?><vdf:value> .... </vdf:value> or
        // <?xml version="1.0" encoding="UTF-8"?><vdf:value/>

        // mLogger.info("outText.toString: " + outText.toString());

        String value;
        if (outText.toString().indexOf("<vdf:value/>") != -1)
            value = "";
        else
            value = outText.toString().substring(
                    outText.toString().indexOf("<vdf:value>") + "<vdf:value>".length(),
                    outText.toString().length() - "</vdf:value>".length());


        return value;
    }
    */

    /*
    private String getChildValue(NodeList nNodes, String sTagName, boolean returnSerialized)
    {
        Node nNode;
        int iIndex;
        String sNodeValue = new String("");

        try {
            for (iIndex = 0; iIndex < nNodes.getLength(); iIndex++) {
                nNode = nNodes.item(iIndex);

                if (nNode.getNodeType() == Node.ELEMENT_NODE
                        && ((Element) nNode).getTagName().equals(sTagName)) {
                    if (returnSerialized)
                        sNodeValue = serializeDoc(nNode);
                    else
                        sNodeValue = nNode.getFirstChild().getNodeValue();

// mLogger.error("NODE (" + sTagName + "): " + ((Element) nNode).getTagName() + ", Value: " + serializeDoc(nNode));
                    break;
                }
            }

            if (iIndex < nNodes.getLength())
                return sNodeValue;
            else
                return null;
        } catch (Exception e) {
            // mLogger.error("getChildValue. " + e);

            return null;
        }
    }
    */

    /*
    private String getFieldValue(NodeList nNodes, String sFieldName)
    {
        Node nNode;
        int iIndex;
        String sFieldValue = null;

        try {
            for (iIndex = 0; iIndex < nNodes.getLength(); iIndex++) {
                nNode = nNodes.item(iIndex);

                if (nNode.getNodeType() == Node.ELEMENT_NODE
                        && ((Element) nNode).getTagName().equals("vdf:field") &&
                        (((Element) nNode).getAttribute("name")).equals(sFieldName)) {
// mLogger.error("ATTRIBUTE: " + ((Element) nNode).getAttribute("name"));
                    sFieldValue = getChildValue(((Element) nNode).getChildNodes(), "vdf:value", true);
// mLogger.error("FIELD VALUE: " + sFieldValue);

                    break;
                }
// mLogger.error("ATTRIBUTE: " + ((Element) nNode).getAttribute("name"));
            }

            return sFieldValue;
        } catch (Exception e) {
            // mLogger.error("getChildValue. " + e);

            return null;
        }
    }
    */

    /*
    private List<String> getListValues(NodeList nPayloadsNodes, String sFieldName)
    {
        Node nNode;
        int iPayloadIndex;
        int iFieldIndex;
        String sFieldValue;
        List<String> sListValues = new ArrayList<>();

        try {
            for (iPayloadIndex = 0; iPayloadIndex < nPayloadsNodes.getLength(); iPayloadIndex++)
            {
                NodeList nFieldsNodes = ((Element) nPayloadsNodes.item(iPayloadIndex)).getChildNodes();

                for (iFieldIndex = 0; iFieldIndex < nFieldsNodes.getLength(); iFieldIndex++)
                {
                    nNode = nFieldsNodes.item(iFieldIndex);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE
                            && ((Element) nNode).getTagName().equals("vdf:field") &&
                            (((Element) nNode).getAttribute("name")).equals(sFieldName))
                    {
    // mLogger.error("ATTRIBUTE: " + ((Element) nNode).getAttribute("name"));
                        sFieldValue = getChildValue(((Element) nNode).getChildNodes(), "vdf:value", true);
    // mLogger.error("FIELD VALUE: " + sFieldValue);

                        if (sFieldValue != null)
                            sListValues.add(sFieldValue);
                    }
    // mLogger.error("ATTRIBUTE: " + ((Element) nNode).getAttribute("name"));
                }
            }

            return sListValues;
        }
        catch (Exception e)
        {
            // mLogger.error("getChildValue. " + e);

            return null;
        }
    }
    */

    /*
    private HashMap<String,EscenicType> getPayloadModel(Node schemaNode)
    {
        HashMap<String,EscenicType> payloadModel = new HashMap<>();

        String keyField = null;

        try
        {
//            <vdf:model>
//                <vdf:schema>
//                    <vdf:fielddef name="disableAutomaticFieldsEdit" label="Disabilita aggiornamenti di title, subhead, leadtext e body" mediatype="text/plain" xsdtype="boolean">
//                        <vdf:value>false</vdf:value>
//                    </vdf:fielddef>
//                    <vdf:fielddef name="title" label="Title" mediatype="text/plain" xsdtype="string"/>
//                    ...
//                    <vdf:fielddef name="mesoTags" label="MESO Tags">
//                        <vdf:listdef>
//                            <vdf:schema>
//                                <vdf:fielddef name="mesoTags" label="MESO Tags" mediatype="text/plain" xsdtype="string"/>
//                            </vdf:schema>
//                        </vdf:listdef>
//                        <vdf:value>[]</vdf:value>
//                    </vdf:fielddef>
//                    ...
//                </vdf:schema>
//            </vdf:model>
            NodeList fieldDefNodeList = (NodeList) xPath.evaluate("fielddef", schemaNode, XPathConstants.NODESET);

            for (int fieldDefIndex = 0; fieldDefIndex < fieldDefNodeList.getLength(); fieldDefIndex++)
            {
                Node fieldDefNode = fieldDefNodeList.item(fieldDefIndex);
                fieldDefNode.getParentNode().removeChild(fieldDefNode); // Removes the node before evaluating for better performance

                keyField = (String) xPath.evaluate("@name", fieldDefNode, XPathConstants.STRING);

                EscenicType escenicType = new EscenicType();

                Node listDefNode = (Node) xPath.evaluate("listdef", fieldDefNode, XPathConstants.NODE);
                String xsdtype = (String) xPath.evaluate("@xsdtype", fieldDefNode, XPathConstants.STRING);
                String label = (String) xPath.evaluate("@label", fieldDefNode, XPathConstants.STRING);
                String visibility = (String) xPath.evaluate("@visibility", fieldDefNode, XPathConstants.STRING);
                Node choiceNode = (Node) xPath.evaluate("choice", fieldDefNode, XPathConstants.NODE);

                escenicType.setKeyField(keyField);

                if (label == null || label.equalsIgnoreCase(""))
                    escenicType.setLabel(keyField);
                else
                    escenicType.setLabel(label);

                if (visibility == null || visibility.equalsIgnoreCase(""))
                    escenicType.setVisibility(true);
                else
                {
                    if (visibility.equalsIgnoreCase("hidden"))
                        escenicType.setVisibility(false);
                    else
                    {
                        mLogger.error("Unknown visibility value: " + visibility);
                        escenicType.setVisibility(true);
                    }
                }

                if (listDefNode == null)
                {
                    if (xsdtype == null)
                        mLogger.error("Scenario Unknown. keyField: " + keyField);
                    else
                    {
                        escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                        escenicType.setXsdType(xsdtype);
                        escenicType.setValueIfUnset((String) xPath.evaluate("value/text()", fieldDefNode, XPathConstants.STRING));

                        if (choiceNode != null)
                        {
                            NodeList alternativesNodeList = (NodeList) xPath.evaluate("alternative", choiceNode, XPathConstants.NODESET);
                            Node collectionNode = (Node) xPath.evaluate("collection", choiceNode, XPathConstants.NODE);

                            if (alternativesNodeList != null && alternativesNodeList.getLength() > 0)
                            {
//                                    <vdf:fielddef name="twitterAccount" label="Twitter account" mediatype="text/plain" xsdtype="string">
//                                        <vdf:choice scope="limit">
//                                            <vdf:alternative/>
//                                            <vdf:alternative label="Vizrt">
//                                                <vdf:value>vizrt</vdf:value>
//                                            </vdf:alternative>
//                                        </vdf:choice>
//                                    </vdf:fielddef>

                                Map<String,String> alternativesList = new HashMap<>();

                                for (int alternativeIndex = 0; alternativeIndex < alternativesNodeList.getLength(); alternativeIndex++)
                                {
                                    Node alternativeNode = alternativesNodeList.item(alternativeIndex);

                                    String alternativeLabel = (String) xPath.evaluate("@label", alternativeNode, XPathConstants.STRING);
                                    String alternativeValue = (String) xPath.evaluate("value/text()", alternativeNode, XPathConstants.STRING);

                                    if (alternativeLabel != null && alternativeValue != null &&
                                        !alternativeLabel.equalsIgnoreCase("") && !alternativeValue.equalsIgnoreCase(""))
                                    {
                                        // mLogger.error("EscenicType. keyField: " + keyField + ", alternativeLabel: " + alternativeLabel + ", alternativeValue: " + alternativeValue);

                                        alternativesList.put(alternativeLabel, alternativeValue);
                                    }
                                }
                                if (alternativesList.size() > 0)
                                    escenicType.setAlternativesList(alternativesList);
                                // mLogger.error("escenicType.getLabel: " + escenicType.getLabel() + ", escenicType.getAlternativesList(): " + escenicType.getAlternativesList());
                            }
                            else if (collectionNode != null)
                            {
//                                <vdf:choice scope="limit">
//                                    <vdf:collection src="/webservice-extensions/wf/collection/story-fields.jsp" select="content"/>
//                                </vdf:choice>

                                mLogger.error("keyField: " + keyField + ". The scenario vdf:choice -> vdf:collection is not managed");
                            }
                            else
                            {
                                mLogger.error("keyField: " + keyField + ". This scenario is not managed");
                            }
                        }
                    }
                }
                else
                {
//                    <vdf:fielddef name="newsKeywords" label="News Keywords">
//                        <vdf:listdef>
//                            <vdf:schema>
//                                <vdf:fielddef name="newsKeywords" label="News Keywords" mediatype="text/plain" xsdtype="string"/>
//                            </vdf:schema>
//                        </vdf:listdef>
//                        <vdf:value>[]</vdf:value>
//                    </vdf:fielddef>

                    Node nestedSchemaNode = (Node) xPath.evaluate("schema", listDefNode, XPathConstants.NODE);

                    if (nestedSchemaNode == null)
                    {
                        mLogger.error("Scenario Unknown. keyField: " + keyField);
                    }
                    else
                    {
                        escenicType.setType(EscenicType.Type.ESCENIC_NESTEDCOMPLEXTYPE);
                        escenicType.setNestedType(getPayloadModel(nestedSchemaNode));
                    }
                }

                // mLogger.info(escenicType.toString());

                payloadModel.put(keyField, escenicType);
            }
        }
        catch (Exception e)
        {
            mLogger.error("keyField: " + keyField + ", Exception: " + e.getMessage());
        }

        return payloadModel;
    }
    */

    public HashMap<String,EscenicType> getPayloadModel2(Node contentDescriptorNode,
        HashMap<String,EscenicField> linksFields)
    {
        HashMap<String,EscenicType> payloadModel = new HashMap<>();
        String propertyGroupName = null;

        try
        {
            /*
            <com.escenic.domain.ContentDescriptor>
                <mVersion>541850954</mVersion>
                <mPropertyDescriptors>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>title</mName>
                        <mLabel>Title</mLabel>
                        <mDescription>The title of the article</mDescription>
                        <mType>java.lang.String</mType>
                        ....
                    <com.escenic.domain.SingleSelectionPropertyDescriptor>
                        <mName>image_icon</mName>
                        <mLabel>Image</mLabel>
                        <mDescription>Is the article contains image</mDescription>
                        <mType>java.lang.Boolean</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>mesoTags</mName>
                        <mLabel>MESO Tags</mLabel>
                        <mDescription>The tags used by the Social Media filter</mDescription>
                        <mType>java.util.List</mType>
                        <mElementDescriptor>
                            <mName>mesoTags</mName>
                            <mLabel>MESO Tags</mLabel>
                            <mDescription>The tags used by the Social Media filter</mDescription>
                            <mType>java.lang.String</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>lead</mName>
                        <mLabel>Lead</mLabel>
                        <mType>java.util.List</mType>
                        <mElementDescriptor>
                            <mName>relation</mName>
                            <mLabel>Relation</mLabel>
                            <mType>com.escenic.domain.ContentSummary</mType>
                    <com.escenic.domain.SpecializedMapPropertyDescriptor>
                        <mName>com.escenic.sections</mName>
                        <mLabel>Sections</mLabel>
                        <mType>java.util.Map</mType>
                    <com.escenic.domain.StatePropertyDescriptor>
                        <mName>com.escenic.state</mName>
                        <mLabel>State</mLabel>
                        <mType>com.escenic.abdera.vaext.State</mType>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>com.escenic.geotag</mName>
                        <mLabel>Geotag</mLabel>
                        <mType>java.lang.String</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>nonPremiumFields</mName>
                        <mLabel>Non premium fields</mLabel>
                        <mDescription>The article's non premium fields to display</mDescription>
                        <mType>java.util.List</mType>
                        <mElementDescriptor class="com.escenic.domain.collection.CollectionPropertyDescriptor">
                            <mName>nonPremiumFields</mName>
                            <mLabel>Non premium fields</mLabel>
                            <mDescription>The article's non premium fields to display</mDescription>
                            <mType>com.escenic.domain.collection.CollectionValue</mType>
                            <mSource>/webservice-extensions/wf/collection/story-fields.jsp</mSource>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>twitterShare</mName>
                        <mLabel>Twitter</mLabel>
                        <mDescription>This complex fields content twitter sharing status.</mDescription>
                        <mType>java.util.List</mType>
                        <mElementDescriptor class="com.escenic.domain.MapPropertyDescriptor">
                            <mName>twitterShare</mName>
                            <mLabel>Twitter</mLabel>
                            <mDescription>This complex fields content twitter sharing status.</mDescription>
                            <mType>java.util.Map</mType>
                            <mProperties>
                                <entry>
                                    <string>shareontwitter</string>
                                    <com.escenic.domain.SingleSelectionPropertyDescriptor>
                                        <mName>shareOnTwitter</mName>
                                        <mLabel>Share on Twitter</mLabel>
                                        <mDescription></mDescription>
                                        <mType>java.lang.Boolean</mType>
                                <entry>
                                    <string>tweettext</string>
                                    <com.escenic.domain.PropertyDescriptor>
                                        <mName>tweetText</mName>
                                        <mLabel>Tweet text</mLabel>
                                        <mDescription>The tweet text of the article</mDescription>
                                        <mType>java.lang.String</mType>
                                <entry>
                                    <string>istweeted</string>
                                    <com.escenic.domain.SingleSelectionPropertyDescriptor>
                                        <mName>isTweeted</mName>
                                        <mLabel>Is Tweeted?</mLabel>
                                        <mDescription>This field determines if the content is already shared in twitter.</mDescription>
                                        <mType>java.lang.Boolean</mType>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>resultCenterID</mName>
                        <mLabel>Result center ID</mLabel>
                        <mType>java.math.BigDecimal</mType>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>pushDate</mName>
                        <mLabel>Push Date</mLabel>
                        <mType>java.util.Date</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>tags</mName>
                        <mLabel>Tag</mLabel>
                        <mType>java.util.List</mType>
                        <mElementDescriptor class="com.escenic.domain.MapPropertyDescriptor">
                            <mName>Payload</mName>
                            <mLabel>Payload</mLabel>
                            <mType>java.util.Map</mType>
                            <mProperties>
                                <entry>
                                    <string>relevance</string>
                                    <com.escenic.domain.PropertyDescriptor>
                                        <mName>relevance</mName>
                                        <mLabel>Tag relevance</mLabel>
                                        <mType>java.lang.Double</mType>
                                        <mDefaultValue class="double">1.0</mDefaultValue>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>com.escenic.displayUri</mName>
                        <mLabel>URL</mLabel>
                        <mType>java.net.URI</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>com.escenic.authors</mName>
                        <mLabel>Authors</mLabel>
                        <mType>java.util.List</mType>
                        <mElementDescriptor>
                            <mName>relation</mName>
                            <mLabel>Relation</mLabel>
                            <mType>com.escenic.domain.Link</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>com.escenic.tags</mName>
                        <mLabel>com.escenic.tags</mLabel>
                        <mType>java.util.List</mType>
                        <mElementDescriptor>
                            <mName>tag</mName>
                            <mLabel>Tag</mLabel>
                            <mType>com.escenic.domain.TagRelation</mType>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>com.escenic.homeSection</mName>
                        <mLabel>Home section</mLabel>
                        <mType>com.escenic.domain.Link</mType>
                    <com.escenic.domain.SpecializedMapPropertyDescriptor>
                        <mName>com.escenic.localHomeSections</mName>
                        <mLabel>Local home sections</mLabel>
                        <mType>java.util.Map</mType>
                        <mKeyDescriptor>
                            <mName>publication</mName>
                            <mLabel>Publication</mLabel>
                            <mType>java.net.URI</mType>
                        <mValueDescriptor>
                            <mName>section</mName>
                            <mLabel>Section</mLabel>
                            <mType>com.escenic.domain.Link</mType>
                    <com.escenic.domain.PropertyDescriptor>
                        <mName>com.escenic.priority</mName>
                        <mLabel>Priority</mLabel>
                        <mType>java.lang.Integer</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>com.escenic.inlineRelations</mName>
                        <mLabel>Relations to inlined objects</mLabel>
                        <mType>java.util.List</mType>
                        <mModuleSupport>
                            <mModules>
                                <com.escenic.module.ModuleImpl>
                                    <mCanContainModulesSupport>
                                    <mAttributes/>
                                    <mElementName>hidden</mElementName>
                        <mElementDescriptor>
                            <mName>inlineRelation</mName>
                            <mLabel>Inlinerelation</mLabel>
                            <mType>com.escenic.domain.ContentSummary</mType>
                    <com.escenic.domain.ListPropertyDescriptor>
                        <mName>related</mName>
                        <mLabel>Related</mLabel>
                        <mType>java.util.List</mType>
                        <mElementDescriptor reference="../../com.escenic.domain.ListPropertyDescriptor[2]/mElementDescriptor"/>
                <mPropertyGroups>
                    <com.escenic.domain.PropertyGroup>
                        <mName>default</mName>
                        <mLabel>Default</mLabel>
                        <mDescription>The main content fields</mDescription>
                        <mPropertyDescriptors>
                            <com.escenic.domain.PropertyDescriptor reference="../../../../mPropertyDescriptors/com.escenic.domain.PropertyDescriptor"/>
                            <com.escenic.domain.PropertyDescriptor reference="../../../../mPropertyDescriptors/com.escenic.domain.PropertyDescriptor[2]"/>
                            <com.escenic.domain.PropertyDescriptor reference="../../../../mPropertyDescriptors/com.escenic.domain.PropertyDescriptor[3]"/>
                            ...
                    <com.escenic.domain.PropertyGroup>
                        <mName>metadata</mName>
                        <mLabel>Metadata</mLabel>
                        <mDescription>The advanced content fields</mDescription>
                        <mPropertyDescriptors>
                            <com.escenic.domain.PropertyDescriptor reference="../../../../mPropertyDescriptors/com.escenic.domain.PropertyDescriptor[10]"/>
                            <com.escenic.domain.ListPropertyDescriptor reference="../../../../mPropertyDescriptors/com.escenic.domain.ListPropertyDescriptor[5]"/>
                <mContentType>story</mContentType>
                <mLabel>Story</mLabel>
                <mDescription>A story</mDescription>
                <mId>http://internal.publishing.production.rsi.ch:8080/webservice/publication/rsi/escenic/model/story</mId>
            */
            NodeList propertyDescriptorsNodeList = (NodeList) xPath.evaluate("mPropertyDescriptors/*", contentDescriptorNode, XPathConstants.NODESET);

            payloadModel = getPayloadModel2_HashMap (propertyDescriptorsNodeList, linksFields);

            NodeList propertyGroupsNodeList = (NodeList) xPath.evaluate("mPropertyGroups/*", contentDescriptorNode, XPathConstants.NODESET);

            for (int propertyGroupIndex = 0; propertyGroupIndex < propertyGroupsNodeList.getLength(); propertyGroupIndex++)
            {
                Node propertyGroupNode = propertyGroupsNodeList.item(propertyGroupIndex);

                EscenicTypeGroup escenicTypeGroup = new EscenicTypeGroup();

                propertyGroupName = xPath.evaluate("mName/text()", propertyGroupNode);
                escenicTypeGroup.setName(propertyGroupName);

                String groupLabel = xPath.evaluate("mLabel/text()", propertyGroupNode);
                if (groupLabel.equalsIgnoreCase("COM.ESCENIC.METADATA"))
                    groupLabel = "Escenic Metadata";
                else if (groupLabel.equalsIgnoreCase("COM.ESCENIC.TAGSCHEME"))
                    groupLabel = "Escenic Tag";
                escenicTypeGroup.setLabel(groupLabel);

                escenicTypeGroup.setDescription(xPath.evaluate("mDescription/text()", propertyGroupNode));

                NodeList propertyDescriptorReferencesNodeList = (NodeList) xPath.evaluate("mPropertyDescriptors/*", propertyGroupNode, XPathConstants.NODESET);

                for (int propertyDescriptorIndex = 0; propertyDescriptorIndex < propertyDescriptorReferencesNodeList.getLength();
                    propertyDescriptorIndex++)
                {
                    Node propertyDescriptorNode = propertyDescriptorReferencesNodeList.item(propertyDescriptorIndex);

                    String reference = xPath.evaluate("@reference", propertyDescriptorNode);
                    if (reference == null || reference.equalsIgnoreCase(""))
                    {
                        mLogger.error("reference was not found. Group name: " + escenicTypeGroup.getName());

                        continue;
                    }

                    String keyField = xPath.evaluate(reference + "/mName/text()", propertyDescriptorNode);
                    if (keyField == null || keyField.equalsIgnoreCase(""))
                    {
                        mLogger.error("keyField was not found. Group name: " + escenicTypeGroup.getName());

                        continue;
                    }

                    EscenicType escenicType = payloadModel.get(keyField);
                    if (escenicType == null)
                    {
                        // if this field is contained in the linksFields, this is not an error
                        if (!linksFields.containsKey(keyField))
                            mLogger.error("keyField '" + keyField + "' was not found. Group name: " + escenicTypeGroup.getName());

                        continue;
                    }

                    mLogger.info("Field '" + keyField + "' added to the '" + escenicTypeGroup.getLabel() + "' group");
                    escenicType.setEscenicTypeGroup(escenicTypeGroup);
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("propertyGroupName: " + propertyGroupName + ", Exception: " + e.getMessage());
        }

        return payloadModel;
    }

    private HashMap<String,EscenicType> getPayloadModel2_HashMap (NodeList propertyDescriptorsNodeList,
        HashMap<String,EscenicField> linksFields)
    {
        HashMap<String,EscenicType> payloadModel = new HashMap<>();

        String keyField = null;

        try
        {
            EscenicTypeGroup defaultEscenicTypeGroup = new EscenicTypeGroup();

            defaultEscenicTypeGroup.setName("default group");
            defaultEscenicTypeGroup.setLabel("Default Group");
            defaultEscenicTypeGroup.setDescription("Default Group");

            for (int propertyDescriptorIndex = 0; propertyDescriptorIndex < propertyDescriptorsNodeList.getLength(); propertyDescriptorIndex++)
            {
                Node propertyDescriptorNode = propertyDescriptorsNodeList.item(propertyDescriptorIndex);

                EscenicType escenicType = new EscenicType();

                escenicType.setEscenicTypeGroup(defaultEscenicTypeGroup);

                if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.PropertyDescriptor"))
                {
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);
                    String label = xPath.evaluate("mLabel/text()", propertyDescriptorNode);
                    String type = xPath.evaluate("mType/text()", propertyDescriptorNode);
                    Node visibilityNode = (Node) xPath.evaluate(
                            "mModuleSupport/mModules/com.escenic.module.ModuleImpl/mElementName[text() = 'hidden']", propertyDescriptorNode,
                            XPathConstants.NODE);
                    Node elementNameNodeOfValueIfUnset = (Node) xPath.evaluate(
                            "mModuleSupport/mModules/com.escenic.module.ModuleImpl/mElementName[text() = 'value-if-unset']", propertyDescriptorNode,
                            XPathConstants.NODE);
                    String valueIfUnset = null;
                    if (elementNameNodeOfValueIfUnset != null)
                    {
                        valueIfUnset = xPath.evaluate("../mContent/text()", elementNameNodeOfValueIfUnset);
                    }

                    escenicType.setKeyField(keyField);

                    if (label == null || label.equalsIgnoreCase(""))
                        escenicType.setLabel(keyField);
                    else
                        escenicType.setLabel(label);

                    if (visibilityNode == null)
                        escenicType.setVisibility(true);
                    else
                    {
                        escenicType.setVisibility(false);
                    }

                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);

                    if (type.equalsIgnoreCase("java.lang.String"))
                        escenicType.setXsdType("string");
                    else if (type.equalsIgnoreCase("java.math.BigDecimal"))
                        escenicType.setXsdType("decimal");
                    else if (type.equalsIgnoreCase("java.util.Date"))
                        escenicType.setXsdType("datetime");
                    else if (type.equalsIgnoreCase("java.lang.Double"))
                        escenicType.setXsdType("decimal");
                    else if (type.equalsIgnoreCase("java.net.URI"))
                        escenicType.setXsdType("string");
                    else if (type.equalsIgnoreCase("java.lang.Integer"))
                        escenicType.setXsdType("long");
                    else if (type.equalsIgnoreCase("com.escenic.domain.Link"))
                        escenicType.setXsdType("link");
                    else
                    {
                        mLogger.error("Unknown model type: " + type);

                        continue;
                    }

                    escenicType.setValueIfUnset(valueIfUnset);

                    NodeList optionsNodeList = (NodeList) xPath.evaluate("mOptions/*", propertyDescriptorNode, XPathConstants.NODESET);
                    if (optionsNodeList != null)
                    {
                        Map<String,String> alternativesList = new HashMap<>();

                        for (int optionIndex = 0; optionIndex < optionsNodeList.getLength(); optionIndex++)
                        {
                            Node optionNode = optionsNodeList.item(optionIndex);

                            /*
                            if (optionNode.getNodeName().equalsIgnoreCase("null"))
                                alternativesList.put("<empty>", "<empty>");
                            else */
                            if (optionNode.getNodeName().equalsIgnoreCase("com.escenic.domain.EnumerationOption"))
                            {
                                String optionValue = xPath.evaluate("mValue/text()", optionNode);
                                String optionLabel = xPath.evaluate("mLabel/text()", optionNode);

                                if (optionLabel != null && optionValue != null &&
                                        !optionLabel.equalsIgnoreCase("") && !optionValue.equalsIgnoreCase(""))
                                {
                                    alternativesList.put(optionLabel, optionValue);
                                }
                            }

                        }

                        if (alternativesList.size() > 0)
                            escenicType.setAlternativesList(alternativesList);
                    }
                }
                else if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.SingleSelectionPropertyDescriptor"))
                {
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);
                    String label = xPath.evaluate("mLabel/text()", propertyDescriptorNode);
                    String type = xPath.evaluate("mType/text()", propertyDescriptorNode);
                    Node visibilityNode = (Node) xPath.evaluate(
                            "mModuleSupport/mModules/com.escenic.module.ModuleImpl/mElementName[text() = 'hidden']", propertyDescriptorNode,
                            XPathConstants.NODE);
                    Node elementNameNodeOfValueIfUnset = (Node) xPath.evaluate(
                            "mModuleSupport/mModules/com.escenic.module.ModuleImpl/mElementName[text() = 'value-if-unset']", propertyDescriptorNode,
                            XPathConstants.NODE);
                    String valueIfUnset = null;
                    if (elementNameNodeOfValueIfUnset != null)
                    {
                        valueIfUnset = xPath.evaluate("../mContent/text()", elementNameNodeOfValueIfUnset);
                    }

                    escenicType.setKeyField(keyField);

                    if (label == null || label.equalsIgnoreCase(""))
                        escenicType.setLabel(keyField);
                    else
                        escenicType.setLabel(label);

                    if (visibilityNode == null)
                        escenicType.setVisibility(true);
                    else
                    {
                        escenicType.setVisibility(false);
                    }

                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);

                    if (type.equalsIgnoreCase("java.lang.String"))
                        escenicType.setXsdType("string");
                    else if (type.equalsIgnoreCase("java.lang.Boolean"))
                        escenicType.setXsdType("boolean");
                    else
                    {
                        mLogger.error("Unknown model type: " + type);

                        continue;
                    }

                    escenicType.setValueIfUnset(valueIfUnset);

                    NodeList optionsNodeList = (NodeList) xPath.evaluate("mOptions/*", propertyDescriptorNode, XPathConstants.NODESET);
                    if (optionsNodeList != null)
                    {
                        Map<String,String> alternativesList = new HashMap<>();

                        for (int optionIndex = 0; optionIndex < optionsNodeList.getLength(); optionIndex++)
                        {
                            Node optionNode = optionsNodeList.item(optionIndex);

                            if (optionNode.getNodeName().equalsIgnoreCase("null"))
                                alternativesList.put("null", "null");
                            else if (optionNode.getNodeName().equalsIgnoreCase("com.escenic.domain.EnumerationOption"))
                            {
                                String optionValue = xPath.evaluate("mValue/text()", optionNode);
                                String optionLabel = xPath.evaluate("mLabel/text()", optionNode);

                                if (optionLabel != null && optionValue != null &&
                                        !optionLabel.equalsIgnoreCase("") && !optionValue.equalsIgnoreCase(""))
                                {
                                    alternativesList.put(optionLabel, optionValue);
                                }
                            }

                        }

                        if (alternativesList.size() > 0)
                            escenicType.setAlternativesList(alternativesList);
                    }
                }
                else if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.MultipleSelectionPropertyDescriptor"))
                {
                    // list of 'named' booleans that it was moved in a list of complex where
                    // each element is an enumeration of all the possible options

                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);

                    String label = xPath.evaluate("mLabel/text()", propertyDescriptorNode);
                    String type = xPath.evaluate("mType/text()", propertyDescriptorNode);
                    Node visibilityNode = (Node) xPath.evaluate(
                            "mModuleSupport/mModules/com.escenic.module.ModuleImpl/mElementName[text() = 'hidden']", propertyDescriptorNode,
                            XPathConstants.NODE);


                    escenicType.setKeyField(keyField);

                    if (label == null || label.equalsIgnoreCase(""))
                        escenicType.setLabel(keyField);
                    else
                        escenicType.setLabel(label);

                    if (visibilityNode == null)
                        escenicType.setVisibility(true);
                    else
                    {
                        escenicType.setVisibility(false);
                    }

                    {
                        HashMap<String,EscenicType> stringListModel = new HashMap<>();

                        {
                            EscenicType nestedEscenicType = new EscenicType();

                            nestedEscenicType.setKeyField(keyField);

                            if (label == null || label.equalsIgnoreCase(""))
                                nestedEscenicType.setLabel(keyField);
                            else
                                nestedEscenicType.setLabel(label);

                            nestedEscenicType.setVisibility(true);

                            nestedEscenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);

                            nestedEscenicType.setXsdType("string");

                            nestedEscenicType.setValueIfUnset(null);

                            {
                                NodeList optionsNodeList = (NodeList) xPath.evaluate("mOptions/*", propertyDescriptorNode, XPathConstants.NODESET);
                                if (optionsNodeList != null)
                                {
                                    Map<String,String> alternativesList = new HashMap<>();

                                    for (int optionIndex = 0; optionIndex < optionsNodeList.getLength(); optionIndex++)
                                    {
                                        Node optionNode = optionsNodeList.item(optionIndex);

                                        String optionValue = xPath.evaluate("mValue/text()", optionNode);
                                        String optionLabel = xPath.evaluate("mLabel/text()", optionNode);

                                        if (optionLabel != null && optionValue != null &&
                                                !optionLabel.equalsIgnoreCase("") && !optionValue.equalsIgnoreCase(""))
                                        {
                                            alternativesList.put(optionLabel, optionValue);
                                        }
                                    }
                                    if (alternativesList.size() > 0)
                                        nestedEscenicType.setAlternativesList(alternativesList);
                                }
                            }

                            stringListModel.put(keyField, nestedEscenicType);
                        }

                        escenicType.setNestedType(stringListModel);
                        escenicType.setType(EscenicType.Type.ESCENIC_NESTEDSTRINGTYPE);
                    }

                    escenicType.setValueIfUnset(null);
                }
                else if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.ListPropertyDescriptor"))
                {
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);

                    String label = xPath.evaluate("mLabel/text()", propertyDescriptorNode);
                    String type = xPath.evaluate("mType/text()", propertyDescriptorNode);
                    Node visibilityNode = (Node) xPath.evaluate(
                            "mModuleSupport/mModules/com.escenic.module.ModuleImpl/mElementName[text() = 'hidden']", propertyDescriptorNode,
                            XPathConstants.NODE);
                    Node mElementDescriptorNode;
                    {
                        mElementDescriptorNode = (Node) xPath.evaluate("mElementDescriptor", propertyDescriptorNode, XPathConstants.NODE);

                        String elementDescriptorReference = xPath.evaluate("@reference", mElementDescriptorNode);
                        if (elementDescriptorReference != null && !elementDescriptorReference.equalsIgnoreCase(""))
                            mElementDescriptorNode = (Node) xPath.evaluate(elementDescriptorReference, mElementDescriptorNode, XPathConstants.NODE);
                    }
                    String listType = xPath.evaluate("mType/text()", mElementDescriptorNode);


                    escenicType.setKeyField(keyField);

                    if (label == null || label.equalsIgnoreCase(""))
                        escenicType.setLabel(keyField);
                    else
                        escenicType.setLabel(label);

                    if (visibilityNode == null)
                        escenicType.setVisibility(true);
                    else
                    {
                        escenicType.setVisibility(false);
                    }

                    if (type.equalsIgnoreCase("java.util.List") && listType != null)
                    {
                        if (listType.equalsIgnoreCase("java.lang.String"))
                        {
                            HashMap<String,EscenicType> stringListdModel = new HashMap<>();

                            {
                                EscenicType nestedEscenicType = new EscenicType();

                                nestedEscenicType.setKeyField(keyField);

                                if (label == null || label.equalsIgnoreCase(""))
                                    nestedEscenicType.setLabel(keyField);
                                else
                                    nestedEscenicType.setLabel(label);

                                nestedEscenicType.setVisibility(true);

                                nestedEscenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);

                                nestedEscenicType.setXsdType("string");

                                nestedEscenicType.setValueIfUnset(null);

                                stringListdModel.put(keyField, nestedEscenicType);
                            }

                            escenicType.setNestedType(stringListdModel);
                            escenicType.setType(EscenicType.Type.ESCENIC_NESTEDSTRINGTYPE);
                        }
                        else if (listType.equalsIgnoreCase("com.escenic.domain.ContentSummary"))
                        {
                            if (linksFields != null && escenicType.getVisibility())
                            {
                                String linkKey = "related";
                                linkKey += "-";
                                linkKey += keyField;

                                EscenicField escenicField = new EscenicField();
                                {
                                    List<EscenicLink> listLinkValues = new ArrayList<>();

                                    escenicField.setReadOnly(false);
                                    escenicField.setValueType(EscenicField.ValueType.ESCENIC_LISTLINKVALUE);

                                    escenicField.setListLinkValues(listLinkValues);

                                    escenicField.setFieldName(linkKey);
                                    escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                                }

                                // mLogger.error("linksFields.put(linkKey: " + linkKey + ", escenicField: " + escenicField + ")");
                                linksFields.put(linkKey, escenicField);
                            }
                            else
                            {
                                mLogger.info("KeyField: " + keyField +
                                    ". linksFields not filled. linksFields: " + linksFields +
                                    ", visibility: " + escenicType.getVisibility());
                            }

                            continue;
                        }
                        else if (listType.equalsIgnoreCase("com.escenic.domain.Link"))
                        {
                            mLogger.warn("KeyField: " + keyField + ". Unmanaged model type: " + listType +
                                ". This is java.lang.String -> com.escenic.domain.Link. The management should be added since it is already managed the Property -> com.escenic.domain.Link");

                            continue;
                        }
                        else if (listType.equalsIgnoreCase("com.escenic.domain.collection.CollectionValue"))
                        {
                            String source = xPath.evaluate("mSource/text()", mElementDescriptorNode);

                            mLogger.warn("KeyField: " + keyField + ". Unmanaged model type: " + listType + ", source: " + source);

                            continue;
                        }
                        else if (listType.equalsIgnoreCase("java.util.Map"))
                        {
                            NodeList nestedPropertyDescriptorsNodeList = (NodeList) xPath.evaluate(
                                "mProperties/entry/*[starts-with(name(), 'com.escenic.domain')]",
                                mElementDescriptorNode, XPathConstants.NODESET);

                            mLogger.info("KeyField: " + keyField + ". Managed the nested type");

                            escenicType.setNestedType(getPayloadModel2_HashMap (nestedPropertyDescriptorsNodeList, null));
                            escenicType.setType(EscenicType.Type.ESCENIC_NESTEDCOMPLEXTYPE);
                        }
                        else if (listType.equalsIgnoreCase("com.escenic.domain.TagRelation"))
                        {
                            mLogger.warn("KeyField: " + keyField + ". Unmanaged model type: " + listType);

                            continue;
                        }
                        else
                        {
                            mLogger.error("KeyField: " + keyField + ". Unknown model list type: " + listType);

                            continue;
                        }
                    }
                    else
                    {
                        mLogger.error("KeyField: " + keyField + ". Unknown model type/listType: " + type + "/" + listType);

                        continue;
                    }

                    escenicType.setValueIfUnset(null);
                }
                else if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.StatePropertyDescriptor"))
                {
                    // this represents the 'state' of the article and it is managed as com.escenic.domain.SingleSelectionPropertyDescriptor
                    // using java.lang.String as type, state URIs as options
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);
                    String label = xPath.evaluate("mLabel/text()", propertyDescriptorNode);

                    escenicType.setKeyField(keyField);

                    if (label == null || label.equalsIgnoreCase(""))
                        escenicType.setLabel(keyField);
                    else
                        escenicType.setLabel(label);

                    escenicType.setVisibility(true);

                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);

                    escenicType.setXsdType("string");

                    escenicType.setValueIfUnset(null);

                    /*
                        commented mStateURIs are the URIs but this field is used to save the state (just string and not URI)

                    NodeList optionsNodeList = (NodeList) xPath.evaluate("mStateURIs/uri", propertyDescriptorNode, XPathConstants.NODESET);
                    if (optionsNodeList != null)
                    {
                        Map<String,String> alternativesList = new HashMap<>();

                        for (int optionIndex = 0; optionIndex < optionsNodeList.getLength(); optionIndex++)
                        {
                            Node optionNode = optionsNodeList.item(optionIndex);

                            String optionValue = xPath.evaluate("text()", optionNode);
                            String optionLabel = optionValue;

                            if (optionLabel != null && optionValue != null &&
                                    !optionLabel.equalsIgnoreCase("") && !optionValue.equalsIgnoreCase(""))
                            {
                                alternativesList.put(optionLabel, optionValue);
                            }
                        }

                        if (alternativesList.size() > 0)
                            escenicType.setAlternativesList(alternativesList);
                    }
                    */
                }
                else if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.SpecializedMapPropertyDescriptor"))
                {
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);

                    mLogger.warn("KeyField: " + keyField + ". Unmanaged model nodeName: " + propertyDescriptorNode.getNodeName());

                    continue;
                }
                else if (propertyDescriptorNode.getNodeName().equalsIgnoreCase("com.escenic.domain.collection.CollectionPropertyDescriptor"))
                {
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);

                    mLogger.warn("KeyField: " + keyField + ". Unmanaged model nodeName: " + propertyDescriptorNode.getNodeName());

                    continue;
                }
                else
                {
                    keyField = xPath.evaluate("mName/text()", propertyDescriptorNode);

                    mLogger.error("KeyField: " + keyField + ". Unknown model nodeName: " + propertyDescriptorNode.getNodeName());

                    continue;
                }

                mLogger.info("escenicType. " + escenicType.toString());

                payloadModel.put(keyField, escenicType);
            }
        }
        catch (Exception e)
        {
            mLogger.error("keyField: " + keyField + ", Exception: " + e.getMessage());
        }

        return payloadModel;
    }

    /*
    private HashMap<String,EscenicField> getEntryFields(Long articleId, Node entryNode,
        HashMap<String,EscenicField> metadataField)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        HashMap<String,EscenicField> entryFields;

        if (metadataField == null)
            entryFields = new HashMap<>();
        else
            entryFields = metadataField;

        try
        {
            EscenicTypeGroup entryEscenicTypeGroup = new EscenicTypeGroup();

            entryEscenicTypeGroup.setName("Entry medatata");
            entryEscenicTypeGroup.setLabel("Entry medatata");
            entryEscenicTypeGroup.setDescription("Entry medatata");

            {
                EscenicField escenicField = new EscenicField();

                String fieldName = "entry_title";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Title");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("string");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("title/text()", entryNode);

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_STRINGVALUE);
                escenicField.setStringValue(fieldValue);
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                // dcterms removed because setNamespaceAware is false
                String fieldName = "entry_dcterms:identifier";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Identifier");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("long");
                    escenicType.setValueIfUnset("0");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("identifier/text()", entryNode);

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_LONGVALUE);
                if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    escenicField.setLongValue(Long.valueOf(fieldValue));
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                // dcterms removed because setNamespaceAware is false
                String fieldName = "entry_dcterms:created";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Created");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("datetime");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("created/text()", entryNode);

                // Expected: 2014-12-11T11:30:18.000Z

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_DATEVALUE);
                if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    escenicField.setDateValue(dateFormat.parse(fieldValue));
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                String fieldName = "entry_published";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Published");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("datetime");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("published/text()", entryNode);

                // Expected: 2014-12-11T11:30:18.000Z

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_DATEVALUE);
                if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    escenicField.setDateValue(dateFormat.parse(fieldValue));
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                String fieldName = "entry_app:edited";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Edited");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("datetime");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("edited/text()", entryNode);

                // Expected: 2014-12-11T11:30:18.000Z

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_DATEVALUE);
                if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    escenicField.setDateValue(dateFormat.parse(fieldValue));
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                String fieldName = "entry_updated";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Updated");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("datetime");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("updated/text()", entryNode);

                // Expected: 2014-12-11T11:30:18.000Z

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_DATEVALUE);
                if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                    escenicField.setDateValue(dateFormat.parse(fieldValue));
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();
                String fieldName = "entry_id";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Id");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("string");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("id/text()", entryNode);

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_STRINGVALUE);
                escenicField.setStringValue(fieldValue);
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                String fieldName = "entry_summary";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("Summary");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("string");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("summary/text()", entryNode);

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_STRINGVALUE);
                escenicField.setStringValue(fieldValue);
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }

            {
                EscenicField escenicField = new EscenicField();

                String fieldName = "entry_state";

                EscenicType escenicType = new EscenicType();
                {
                    escenicType.setKeyField(fieldName);
                    escenicType.setLabel("State");
                    escenicType.setType(EscenicType.Type.ESCENIC_SIMPLE);
                    escenicType.setVisibility(true);
                    escenicType.setXsdType("string");
                    escenicType.setEscenicTypeGroup(entryEscenicTypeGroup);
                }

                String fieldValue = xPath.evaluate("control/state/@name", entryNode);

                escenicField.setEscenicType(escenicType);
                escenicField.setValueType(EscenicField.ValueType.ESCENIC_STRINGVALUE);
                escenicField.setStringValue(fieldValue);
                escenicField.setReadOnly(true);

                escenicField.setFieldName(fieldName);
                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info(escenicField.toString());

                entryFields.put(fieldName, escenicField);
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            // throw e;
        }

        return entryFields;
    }
    */

    private void getEscenicMetadata (String articleId, Node entryNode,
        HashMap<String,EscenicField> metadataField, HashMap<String,EscenicField> linksFields)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));


        try
        {
            for (String keyField: metadataField.keySet())
            {
                EscenicField escenicField = metadataField.get(keyField);

                if (escenicField == null)
                {
                    mLogger.error("EscenicField is null. keyField: '" + keyField + "'");

                    continue;
                }

                // mLogger.error("keyField: " + keyField);
                // mLogger.error("escenicField: " + escenicField.toString());

                /*
                if (escenicField.getEscenicType() != null ||
                    escenicField.getEscenicType().getEscenicTypeGroup() != null ||
                    escenicField.getEscenicType().getEscenicTypeGroup().getName() != null)
                    mLogger.error("FieldKey: " + escenicField.getFieldName() + ", group: " + escenicField.getEscenicType().getEscenicTypeGroup().getName());
                */

                if (escenicField.getEscenicType() == null ||
                    escenicField.getEscenicType().getEscenicTypeGroup() == null ||
                    escenicField.getEscenicType().getEscenicTypeGroup().getName() == null ||
                    (!escenicField.getEscenicType().getEscenicTypeGroup().getName().equalsIgnoreCase("COM.ESCENIC.METADATA") &&
                    !escenicField.getEscenicType().getEscenicTypeGroup().getName().equalsIgnoreCase("default"))
                    )
                    continue;

                if (escenicField.getEscenicType().getEscenicTypeGroup().getName().equalsIgnoreCase("COM.ESCENIC.METADATA"))
                {
                    if (escenicField.getFieldName().equalsIgnoreCase("publication"))
                    {
                        // this should be already readonly, anyway it is forced to be readonly
                        escenicField.setReadOnly(true);

                        // in the model it is defined as java.net.URI
                        escenicField.setStringValue(xPath.evaluate("publication[@href]", entryNode));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.priority"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.homeSection"))
                    {
                        // this should be already readonly, anyway it is forced to be readonly
                        escenicField.setReadOnly(true);
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.displayUri"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        String linkKey = "alternate";
                        linkKey += "-";
                        linkKey += "";

                        escenicField.setStringValue(linksFields.get(linkKey).getStringValue());
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.expireDate"))
                    {
                        String fieldValue = xPath.evaluate("expires/text()", entryNode);

                        // Expected: 2014-12-11T11:30:18.000Z

                        if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                            escenicField.setDateValue(dateFormat.parse(fieldValue));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.createdBy"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        escenicField.setStringValue(xPath.evaluate("author/name/text()", entryNode));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.displayId"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        escenicField.setStringValue(xPath.evaluate("identifier/text()", entryNode));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.publishedDate"))
                    {
                        String fieldValue = xPath.evaluate("updated/text()", entryNode);

                        // Expected: 2014-12-11T11:30:18.000Z

                        if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                            escenicField.setDateValue(dateFormat.parse(fieldValue));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.source"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        escenicField.setStringValue(xPath.evaluate("reference[@source]", entryNode));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.createDate"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        String fieldValue = xPath.evaluate("created/text()", entryNode);

                        // Expected: 2014-12-11T11:30:18.000Z

                        if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                            escenicField.setDateValue(dateFormat.parse(fieldValue));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.activateDate"))
                    {
                        String fieldValue = xPath.evaluate("available/text()", entryNode);

                        // Expected: 2014-12-11T11:30:18.000Z

                        if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                            escenicField.setDateValue(dateFormat.parse(fieldValue));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.sourceid"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        escenicField.setStringValue(xPath.evaluate("reference[@sourceid]", entryNode));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.displayName"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.firstPublishDate"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.previewUri"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        String linkKey = "http://www.vizrt.com/types/relation/preview";
                        linkKey += "-";
                        linkKey += "";

                        escenicField.setStringValue(linksFields.get(linkKey).getStringValue());
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.lastModifiedDate"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        String fieldValue = xPath.evaluate("edited/text()", entryNode);

                        // Expected: 2014-12-11T11:30:18.000Z

                        if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                            escenicField.setDateValue(dateFormat.parse(fieldValue));
                    }
                    else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.lockURI"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        String linkKey = "http://www.vizrt.com/types/relation/lock";
                        linkKey += "-";
                        linkKey += "";

                        escenicField.setStringValue(linksFields.get(linkKey).getStringValue());
                    }
                    else
                    {
                        mLogger.error("field belonging to the 'COM.ESCENIC.METADATA' group unknown: " + escenicField.getFieldName());
                    }
                }
                else // if (escenicField.getEscenicType().getEscenicTypeGroup().getName().equalsIgnoreCase("default"))
                {
                    if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.state"))
                    {
                        // forced to be readonly
                        escenicField.setReadOnly(true);

                        escenicField.setStringValue(xPath.evaluate("control/state/@name", entryNode));
                    }
                    else
                    {
                        // mLogger.error("field belonging to the 'default' group unknown: " + escenicField.getFieldName());
                    }
                }

                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            // throw e;
        }
    }

    private List<String> getStateTransitions(String stateTransitionsHRef)
    {
        mLogger.info("getStateTransitions. stateTransitionsHRef: " + stateTransitionsHRef);

        List<String> stateTransitions = new ArrayList<>();


        try {
            boolean cacheToBeUsed = true;

            HttpGetInfo httpGetInfo = getXML(stateTransitionsHRef, "<stateTransitions>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + stateTransitionsHRef);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */
                NodeList statesNodeList = (NodeList) xPath.evaluate("/state/actions/action", document, XPathConstants.NODESET);

                // mLogger.error("entriesNodeList.getLength: " + entriesNodeList.getLength());
                for (int stateIndex = 0; stateIndex < statesNodeList.getLength(); stateIndex++)
                {
                    Node stateNode = statesNodeList.item(stateIndex);

                    String state = xPath.evaluate("text()", stateNode);

                    stateTransitions.add(state);
                }
            }
        }
        catch (Exception e) {

            mLogger.error("getStateTransitions. stateTransitionsHRef: " + stateTransitionsHRef + " failed. Exception: " + e);

            return null;    // throw new Exception(methodName + " (" + contentIdURL + ") failed. Exception: " + e);
        }

        return stateTransitions;
    }

    private HashMap<String,EscenicField> getPayloadFields(Document document, Node payloadNode,
        HashMap<String,EscenicType> payloadModel)
    {
        String fieldName = null;
        HashMap<String,EscenicField> payloadFields = new HashMap<>();
        EscenicField lastEscenicField = null;

        try
        {
            NodeList payloadFieldsNodeList = (NodeList) xPath.evaluate("field", payloadNode, XPathConstants.NODESET);

            // mLogger.info("entry/content/payload/field payloadFieldsNodeList.getLength: " + payloadFieldsNodeList.getLength());

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            /*
            for (String keyFieldModel : payloadModel.keySet())
            {
                mLogger.info("keyFieldModel: " + keyFieldModel + ", " + payloadModel.get(keyFieldModel));
            }
            */

            for (int iPayloadFieldIndex = 0; iPayloadFieldIndex < payloadFieldsNodeList.getLength(); iPayloadFieldIndex++)
            {
                Node fieldNode = payloadFieldsNodeList.item(iPayloadFieldIndex);
                fieldNode.getParentNode().removeChild(fieldNode); // Removes the node before evaluating for better performance

                fieldName = xPath.evaluate("@name", fieldNode);

                Node valueNode = (Node) xPath.evaluate("value", fieldNode, XPathConstants.NODE);

                EscenicType escenicType = payloadModel.get(fieldName);
                if (escenicType == null)
                {
                    // in case of reserved Escenic field the error is not logged
                    // if (fieldName.equalsIgnoreCase("com.escenic.tags"))
                    //    continue;

                    // shopLink, datasource, binary, serverURL, alternates, video, audio, facebookLink, twitterLink
                    mLogger.error("escenicType is null. fieldName: " + fieldName);

                    continue;
                }

                EscenicField escenicField = EscenicField.createEmptyEscenicField(fieldName, escenicType);
                if (escenicField == null)
                {
                    mLogger.error("escenicField is null. fieldName: " + fieldName);

                    continue;
                }

                lastEscenicField = escenicField;

                // mLogger.info("Processing fieldName: " + fieldName + ", escenicType.getType: " + escenicType.getType());

                if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_STRINGVALUE)
                {
                    String sFieldValue = null;

                    if (valueNode != null)
                    {
                        if (fieldName.equalsIgnoreCase("body"))
                        {
                            // encodeBodyNodeToBePresented will make sure we have a div root a child
                            Article.encodeBodyNodeToBePresented(document, valueNode, xPath);

                            {
                                Logger.getLogger(Article.class).info("Transform DOM to String...");
                                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                Transformer transformer = transformerFactory.newTransformer();
                                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

                                DOMSource source = new DOMSource(valueNode.getFirstChild());
                                StringWriter writer = new StringWriter();
                                StreamResult result = new StreamResult(writer);

                                transformer.transform(source, result);

                                sFieldValue = writer.getBuffer().toString();
                            }

                            // mLogger.error("sFieldValue: " + sFieldValue);
                        }
                        else
                            sFieldValue = xPath.evaluate("text()", valueNode);
                    }
                    else
                    {
                        // the default value is used if present
                        if (escenicType.getValueIfUnset() != null && !escenicType.getValueIfUnset().equalsIgnoreCase(""))
                            sFieldValue = escenicType.getValueIfUnset();
                    }

                    // mLogger.info("fieldName: " + fieldName + ", sFieldValue: " + sFieldValue + ", escenicType.getXsdType: " + escenicType.getXsdType());

                    escenicField.setStringValue(sFieldValue);
                }
                else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_BOOLEANVALUE)
                {
                    String fieldValue = null;
                    Boolean bFieldValue = null;

                    if (valueNode != null)
                        fieldValue = xPath.evaluate("text()", valueNode);

                    // mLogger.info("fieldName: " + fieldName + ", fieldValue: " + fieldValue + ", escenicType.getXsdType: " + escenicType.getXsdType());

                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                        bFieldValue = Boolean.valueOf(fieldValue);

                    escenicField.setBooleanValue(bFieldValue);
                }
                else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_DATEVALUE)
                {
                    String fieldValue = null;
                    Date dFieldValue = null;

                    if (valueNode != null)
                        fieldValue = xPath.evaluate("text()", valueNode);

                    // mLogger.info("fieldName: " + fieldName + ", fieldValue: " + fieldValue + ", escenicType.getXsdType: " + escenicType.getXsdType());

                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                        dFieldValue = dateFormat.parse(fieldValue);

                    escenicField.setDateValue(dFieldValue);
                }
                else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_DECIMALVALUE)
                {
                    String fieldValue = null;
                    Float fFieldValue = null;

                    if (valueNode != null)
                        fieldValue = xPath.evaluate("text()", valueNode);

                    // mLogger.info("fieldName: " + fieldName + ", fieldValue: " + fieldValue + ", escenicType.getXsdType: " + escenicType.getXsdType());

                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                        fFieldValue = Float.valueOf(fieldValue);

                    escenicField.setDecimalValue(fFieldValue);
                }
                else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LONGVALUE)
                {
                    String fieldValue = null;
                    Long lFieldValue = null;

                    if (valueNode != null)
                        fieldValue = xPath.evaluate("text()", valueNode);

                    // mLogger.info("fieldName: " + fieldName + ", fieldValue: " + fieldValue + ", escenicType.getXsdType: " + escenicType.getXsdType());

                    if (fieldValue != null && !fieldValue.equalsIgnoreCase(""))
                        lFieldValue = Long.valueOf(fieldValue);

                    escenicField.setLongValue(lFieldValue);
                }
                else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LINKVALUE)
                {
                    Node linkNode = (Node) xPath.evaluate("link", valueNode, XPathConstants.NODE);

                    if (linkNode != null)
                    {
                        String type = xPath.evaluate("@type", linkNode);

                        if (MimeTypes.isImageMimeType(type))
                        {
                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", linkNode));
                                escenicLink.setHref(xPath.evaluate("@href", linkNode));
                                escenicLink.setType(type);
                                escenicLink.setTitle(xPath.evaluate("@title", linkNode));

                                // we will set the href as thumbnail
                                if (escenicLink.getHref() != null && !escenicLink.getHref().equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(escenicLink.getHref());

                                    int begingOfId = escenicLink.getHref().lastIndexOf('/');
                                    if (begingOfId != -1)
                                    {
                                        imageInfo.setId(escenicLink.getHref().substring(begingOfId + 1));
                                    }

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }


                                /* It seems the below fields are missing in this scenario (<vdf:value> <link ...)
                                escenicLink.setGroup(xPath.evaluate("@group", linkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", linkNode));

                                */
                            }

                            escenicField.setLinkValue(escenicLink);
                        }
                        else if (MimeTypes.isVideoMimeType(type))
                        {
                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", linkNode));
                                escenicLink.setHref(xPath.evaluate("@href", linkNode));
                                escenicLink.setType(type);
                                escenicLink.setTitle(xPath.evaluate("@title", linkNode));

                                // we will set the href as thumbnail
                                /*
                                if (escenicLink.getHref() != null && !escenicLink.getHref().equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(escenicLink.getHref());

                                    int begingOfId = escenicLink.getHref().lastIndexOf('/');
                                    if (begingOfId != -1)
                                    {
                                        imageInfo.setId(escenicLink.getHref().substring(begingOfId + 1));
                                    }

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }
                                */

                                /* It seems the below fields are missing in this scenario (<vdf:value> <link ...)
                                escenicLink.setGroup(xPath.evaluate("@group", linkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", linkNode));

                                */
                            }

                            escenicField.setLinkValue(escenicLink);
                        }
                        else if (MimeTypes.isAudioMimeType(type))
                        {
                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", linkNode));
                                escenicLink.setHref(xPath.evaluate("@href", linkNode));
                                escenicLink.setType(type);
                                escenicLink.setTitle(xPath.evaluate("@title", linkNode));

                                // we will set the href as thumbnail
                                /*
                                if (escenicLink.getHref() != null && !escenicLink.getHref().equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(escenicLink.getHref());

                                    int begingOfId = escenicLink.getHref().lastIndexOf('/');
                                    if (begingOfId != -1)
                                    {
                                        imageInfo.setId(escenicLink.getHref().substring(begingOfId + 1));
                                    }

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }
                                */

                                /* It seems the below fields are missing in this scenario (<vdf:value> <link ...)
                                escenicLink.setGroup(xPath.evaluate("@group", linkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", linkNode));

                                */
                            }

                            escenicField.setLinkValue(escenicLink);
                        }
                        else
                        {
                            mLogger.error("Link Type unknown: " + type);
                        }
                    }
                }
                else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTCOMPLEXVALUE)
                {
                    if (escenicType.getType() == EscenicType.Type.ESCENIC_NESTEDSTRINGTYPE)
                    {
                        /* It manages this scenario:
                            <vdf:field name="mesoTags">
                                <vdf:list>
                                    <vdf:payload>
                                        <vdf:field name="mesoTags">
                                            <vdf:value>pippo</vdf:value>
                                        </vdf:field>
                                    </vdf:payload>
                                    <vdf:payload>
                                        <vdf:field name="mesoTags">
                                            <vdf:value>pluto</vdf:value>
                                        </vdf:field>
                                    </vdf:payload>
                                </vdf:list>
                            </vdf:field>

                            or

                            <vdf:field name="mesoTags"/>
                         */

                        Node listNode = (Node) xPath.evaluate("list", fieldNode, XPathConstants.NODE);
                        NodeList valuesNodeList = null;
                        if (listNode != null)
                            valuesNodeList = (NodeList) xPath.evaluate("payload/field/value", listNode, XPathConstants.NODESET);

                        if (valuesNodeList != null && valuesNodeList.getLength() > 0)
                        {
                            List<ComplexValue> listComplexValue = new ArrayList<>();

                            HashMap<String,EscenicType> nestedPayloadModel = escenicType.getNestedType();
                            EscenicType nestedEscenicType = nestedPayloadModel.get(fieldName);
                            if (nestedEscenicType == null)
                            {
                                mLogger.error("nestedEscenicType is null. fieldName: " + fieldName);

                                continue;
                            }

                            for (int payloadIndex = 0; payloadIndex < valuesNodeList.getLength(); payloadIndex++)
                            {
                                Node stringValueNode = valuesNodeList.item(payloadIndex);

                                ComplexValue complexValue = new ComplexValue();
                                HashMap<String,EscenicField> nestedPayloadFields = new HashMap<>();

                                {
                                    complexValue.setInitialEscenicField(escenicField);

                                    EscenicField nestedEscenicField = EscenicField.createEmptyEscenicField(fieldName, nestedEscenicType);
                                    if (nestedEscenicField == null)
                                    {
                                        mLogger.error("nestedEscenicField is null. fieldName: " + fieldName);

                                        continue;
                                    }

                                    nestedEscenicField.setStringValue(xPath.evaluate("text()", stringValueNode));
                                    nestedEscenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                                    nestedPayloadFields.put(fieldName, nestedEscenicField);
                                    complexValue.setMetadataFields(nestedPayloadFields);
                                }

                                listComplexValue.add(complexValue);
                            }

                            escenicField.setListComplexValue(listComplexValue);
                        }
                        else
                        {
                            if (listNode != null && listNode.getChildNodes().getLength() == 0)
                            {
                                // empty field
                            /* We have like:
                            <vdf:field name="newsKeywords">
                                <vdf:list/>
                            </vdf:field>
                            */
                            }
                            else
                            {

                                // empty field
                            /* We have like:
                            <vdf:field name="newsKeywords" />
                            */
                            }
                        }
                    }
                    else // if (escenicType.getType() == EscenicType.Type.ESCENIC_NESTEDCOMPLEXTYPE)
                    {
                        /* It manages this scenario:
                            <vdf:field name="facebookShare">
                                <vdf:list>
                                    <vdf:payload>
                                        <vdf:field name="facebookShare">
                                            <vdf:field name="facebookAccount">
                                                <vdf:value>vizrt</vdf:value>
                                            </vdf:field>
                                            <vdf:field name="facebookTitle">
                                                <vdf:value>aaa</vdf:value>
                                            </vdf:field>
                                            <vdf:field name="facebookSummary">
                                                <vdf:value>bbb</vdf:value>
                                            </vdf:field>
                                        </vdf:field>
                                    </vdf:payload>
                                </vdf:list>
                            </vdf:field>
                         */
                        Node valuesListNode = (Node) xPath.evaluate("list", fieldNode, XPathConstants.NODE);
                        NodeList valuesListPlayloadNodeList = null;
                        if (valuesListNode != null)
                            valuesListPlayloadNodeList = (NodeList) xPath.evaluate("payload/field", valuesListNode, XPathConstants.NODESET);

                        if (valuesListPlayloadNodeList != null && valuesListPlayloadNodeList.getLength() > 0)
                        {
                            List<ComplexValue> listComplexValue = new ArrayList<>();

                            for (int payloadIndex = 0; payloadIndex < valuesListPlayloadNodeList.getLength(); payloadIndex++)
                            {
                                Node valuesListPlayload = valuesListPlayloadNodeList.item(payloadIndex);

                                ComplexValue complexValue = new ComplexValue();
                                complexValue.setInitialEscenicField(escenicField);

                                complexValue.setMetadataFields(getPayloadFields(document, valuesListPlayload, escenicType.getNestedType()));

                                listComplexValue.add(complexValue);
                            }

                            escenicField.setListComplexValue(listComplexValue);
                        }
                        else
                        {
                            if (valuesListNode != null && valuesListNode.getChildNodes().getLength() == 0)
                            {
                                // empty field
                                /* We have like:
                                <vdf:field name="newsKeywords">
                                    <vdf:list/>
                                </vdf:field>
                                */
                            }
                            else
                            {

                                // empty field
                                /* We have like:
                                <vdf:field name="newsKeywords" />
                                */
                            }
                        }
                    }
                }
                else
                {
                    mLogger.error("fieldName: " + fieldName + ", Unknown ValueType: " + escenicField.getValueType());

                    continue;
                }

                escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

                // mLogger.info("fieldName: " + fieldName + ", " + escenicField.toString());

                payloadFields.put(fieldName, escenicField);
            }

            // add more fields if present in the model and not present in the article
            {
                for (String modelKeyField: payloadModel.keySet())
                {
                    if (payloadFields.get(modelKeyField) == null)
                    {

                        EscenicType escenicType = payloadModel.get(modelKeyField);

                        EscenicField escenicField = EscenicField.createEmptyEscenicField (modelKeyField, escenicType);

                        if (escenicField == null)
                        {
                            mLogger.error("escenicField was not created");

                            continue;
                        }

                        payloadFields.put(modelKeyField, escenicField);

                        mLogger.info("Added the modelKeyField " + modelKeyField + " because it was missing in the article");
                    }
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("fieldName: " + fieldName +
                (lastEscenicField == null ? "" : (", escenicField.getValueType: " + lastEscenicField.getValueType())) +
                ", Exception: " + e.getMessage());

            // throw e;
        }

        return payloadFields;
    }

    private HashMap<String,EscenicField> getEntryLinksFields(String articleId, Node entryNode,
        HashMap<String,EscenicField> linksFields)
    {
        try
        {
            {
                // Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

                mLogger.info("getEntryLinksFields. articleId: " + articleId);

                NodeList linksNodeList = (NodeList) xPath.evaluate("link", entryNode, XPathConstants.NODESET);
                String linkKey;

                for (int linkIndex = 0; linkIndex < linksNodeList.getLength(); linkIndex++)
                {
                    Node linkNode = linksNodeList.item(linkIndex);

                    linkKey = xPath.evaluate("@rel", linkNode);
                    linkKey += "-";
                    linkKey += xPath.evaluate("@group", linkNode);

                    // we are sure the related are already present (with EscenicLink null) because added
                    // when the model is parsed

                    EscenicField escenicField = linksFields.get(linkKey);
                    if (escenicField == null)
                        escenicField = new EscenicField();

                    EscenicLink escenicLink = new EscenicLink();
                    {
                        escenicLink.setRel(xPath.evaluate("@rel", linkNode));
                        escenicLink.setGroup(xPath.evaluate("@group", linkNode));
                        escenicLink.setHref(xPath.evaluate("@href", linkNode));
                        escenicLink.setType(xPath.evaluate("@type", linkNode));
                        escenicLink.setModel(xPath.evaluate("payload/@model", linkNode));
                        escenicLink.setTitle(xPath.evaluate("payload/field[@name='title']/value/text()", linkNode));

                        String thumbnailURL = xPath.evaluate("@thumbnail", linkNode);
                        if (thumbnailURL != null && !thumbnailURL.equalsIgnoreCase(""))
                        {
                            ImageInfo imageInfo = new ImageInfo();

                            imageInfo.setUrl(thumbnailURL);

                            int begingOfId = thumbnailURL.lastIndexOf('/');
                            if (begingOfId != -1)
                                imageInfo.setId(thumbnailURL.substring(begingOfId + 1));

                            escenicLink.setThumbnailImageInfo(imageInfo);
                        }
                    }

                    escenicField.setReadOnly(false);
                    escenicField.setValueType(EscenicField.ValueType.ESCENIC_LISTLINKVALUE);

                    List<EscenicLink> listLinkValues = escenicField.getListLinkValues();
                    if (listLinkValues == null)
                        listLinkValues = new ArrayList<>();
                    listLinkValues.add(escenicLink);
                    escenicField.setListLinkValues(listLinkValues);

                    escenicField.setFieldName(linkKey);
                    escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                    // mLogger.info(escenicField.toString());

                    linksFields.put(linkKey, escenicField);
                }

                // sections
                {
                    String homeSectionURL = null;

                    String homeSectionKey = "http://www.vizrt.com/types/relation/home-section" + "-";
                    EscenicField homeEscenicField = linksFields.get(homeSectionKey);
                    if (homeEscenicField != null &&
                            homeEscenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTLINKVALUE &&
                            homeEscenicField.getListLinkValues().size() > 0 &&
                            homeEscenicField.getListLinkValues().get(0).getHref() != null &&
                            !homeEscenicField.getListLinkValues().get(0).getHref().equalsIgnoreCase(""))
                    {
                        homeSectionURL = homeEscenicField.getListLinkValues().get(0).getHref();
                    }

                    if (homeSectionURL == null)
                    {
                        Node homeSectionLinkNode = (Node) xPath.evaluate("publication/link[@rel='http://www.vizrt.com/types/relation/home-section']", entryNode, XPathConstants.NODE);

                        if (homeSectionLinkNode != null)
                        {
                            linkKey = xPath.evaluate("@rel", homeSectionLinkNode);
                            linkKey += "-";
                            linkKey += xPath.evaluate("@group", homeSectionLinkNode);

                            EscenicField escenicField = linksFields.get(linkKey);
                            if (escenicField == null)
                                escenicField = new EscenicField();

                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", homeSectionLinkNode));
                                escenicLink.setHref(xPath.evaluate("@href", homeSectionLinkNode));
                                homeSectionURL = escenicLink.getHref();
                                escenicLink.setType(xPath.evaluate("@type", homeSectionLinkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", homeSectionLinkNode));
                                escenicLink.setTitle(xPath.evaluate("payload/field[@name='title']/value/text()", homeSectionLinkNode));

                                String thumbnailURL = xPath.evaluate("@thumbnail", homeSectionLinkNode);
                                if (thumbnailURL != null && !thumbnailURL.equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(thumbnailURL);

                                    int begingOfId = thumbnailURL.lastIndexOf('/');
                                    if (begingOfId != -1)
                                        imageInfo.setId(thumbnailURL.substring(begingOfId + 1));

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }
                            }

                            escenicField.setReadOnly(false);
                            escenicField.setValueType(EscenicField.ValueType.ESCENIC_LISTLINKVALUE);

                            List<EscenicLink> listLinkValues = escenicField.getListLinkValues();
                            if (listLinkValues == null)
                                listLinkValues = new ArrayList<>();
                            listLinkValues.add(escenicLink);
                            escenicField.setListLinkValues(listLinkValues);

                            escenicField.setFieldName(linkKey);
                            escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                            // mLogger.info(escenicField.toString());

                            linksFields.put(linkKey, escenicField);
                        }
                    }

                    {
                        NodeList sectionsLinksNodeList = (NodeList) xPath.evaluate("publication/link[@rel='http://www.vizrt.com/types/relation/section']", entryNode, XPathConstants.NODESET);

                        for (int linkIndex = 0; linkIndex < sectionsLinksNodeList.getLength(); linkIndex++)
                        {
                            Node sectionLinkNode = sectionsLinksNodeList.item(linkIndex);

                            linkKey = xPath.evaluate("@rel", sectionLinkNode);
                            linkKey += "-";
                            linkKey += xPath.evaluate("@group", sectionLinkNode);

                            EscenicField escenicField = linksFields.get(linkKey);
                            if (escenicField == null)
                                escenicField = new EscenicField();

                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", sectionLinkNode));
                                escenicLink.setHref(xPath.evaluate("@href", sectionLinkNode));
                                if (homeSectionURL != null && escenicLink.getHref().equalsIgnoreCase(homeSectionURL))
                                    continue;
                                escenicLink.setType(xPath.evaluate("@type", sectionLinkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", sectionLinkNode));
                                escenicLink.setTitle(xPath.evaluate("payload/field[@name='title']/value/text()", sectionLinkNode));

                                String thumbnailURL = xPath.evaluate("@thumbnail", sectionLinkNode);
                                if (thumbnailURL != null && !thumbnailURL.equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(thumbnailURL);

                                    int begingOfId = thumbnailURL.lastIndexOf('/');
                                    if (begingOfId != -1)
                                        imageInfo.setId(thumbnailURL.substring(begingOfId + 1));

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }
                            }

                            escenicField.setReadOnly(false);
                            escenicField.setValueType(EscenicField.ValueType.ESCENIC_LISTLINKVALUE);

                            List<EscenicLink> listLinkValues = escenicField.getListLinkValues();
                            if (listLinkValues == null)
                                listLinkValues = new ArrayList<>();
                            listLinkValues.add(escenicLink);
                            escenicField.setListLinkValues(listLinkValues);

                            escenicField.setFieldName(linkKey);
                            escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                            // mLogger.info(escenicField.toString());

                            linksFields.put(linkKey, escenicField);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            // throw e;
        }

        return linksFields;
    }

    private HashMap<String,EscenicField> getPublicationLinksFields(String articleId, Node entryNode)
    {
        HashMap<String,EscenicField> publicationLinksFields = new HashMap<>();

        try
        {
            {
                // sections
                {
                    String linkKey;
                    String homeSectionURL = null;

                    {
                        Node homeSectionLinkNode = (Node) xPath.evaluate("publication/link[@rel='http://www.vizrt.com/types/relation/home-section']", entryNode, XPathConstants.NODE);

                        if (homeSectionLinkNode != null)
                        {
                            linkKey = xPath.evaluate("@rel", homeSectionLinkNode);
                            linkKey += "-";
                            linkKey += xPath.evaluate("@group", homeSectionLinkNode);

                            EscenicField escenicField = publicationLinksFields.get(linkKey);
                            if (escenicField == null)
                                escenicField = new EscenicField();

                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", homeSectionLinkNode));
                                escenicLink.setHref(xPath.evaluate("@href", homeSectionLinkNode));
                                homeSectionURL = escenicLink.getHref();
                                escenicLink.setType(xPath.evaluate("@type", homeSectionLinkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", homeSectionLinkNode));
                                escenicLink.setTitle(xPath.evaluate("payload/field[@name='title']/value/text()", homeSectionLinkNode));

                                String thumbnailURL = xPath.evaluate("@thumbnail", homeSectionLinkNode);
                                if (thumbnailURL != null && !thumbnailURL.equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(thumbnailURL);

                                    int begingOfId = thumbnailURL.lastIndexOf('/');
                                    if (begingOfId != -1)
                                        imageInfo.setId(thumbnailURL.substring(begingOfId + 1));

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }
                            }

                            escenicField.setReadOnly(false);
                            escenicField.setValueType(EscenicField.ValueType.ESCENIC_LISTLINKVALUE);

                            List<EscenicLink> listLinkValues = escenicField.getListLinkValues();
                            if (listLinkValues == null)
                                listLinkValues = new ArrayList<>();
                            listLinkValues.add(escenicLink);
                            escenicField.setListLinkValues(listLinkValues);

                            escenicField.setFieldName(linkKey);
                            escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                            // mLogger.info(escenicField.toString());

                            publicationLinksFields.put(linkKey, escenicField);
                        }
                    }

                    {
                        // retrieve all the sections without considering the one relative to the home_section
                        NodeList sectionsLinksNodeList = (NodeList) xPath.evaluate("publication/link[@rel='http://www.vizrt.com/types/relation/section']", entryNode, XPathConstants.NODESET);

                        for (int linkIndex = 0; linkIndex < sectionsLinksNodeList.getLength(); linkIndex++)
                        {
                            Node sectionLinkNode = sectionsLinksNodeList.item(linkIndex);

                            linkKey = xPath.evaluate("@rel", sectionLinkNode);
                            linkKey += "-";
                            linkKey += xPath.evaluate("@group", sectionLinkNode);

                            EscenicField escenicField = publicationLinksFields.get(linkKey);
                            if (escenicField == null)
                                escenicField = new EscenicField();

                            EscenicLink escenicLink = new EscenicLink();
                            {
                                escenicLink.setRel(xPath.evaluate("@rel", sectionLinkNode));
                                escenicLink.setHref(xPath.evaluate("@href", sectionLinkNode));
                                if (homeSectionURL != null && escenicLink.getHref().equalsIgnoreCase(homeSectionURL))
                                    continue;
                                escenicLink.setType(xPath.evaluate("@type", sectionLinkNode));
                                escenicLink.setModel(xPath.evaluate("payload/@model", sectionLinkNode));
                                escenicLink.setTitle(xPath.evaluate("payload/field[@name='title']/value/text()", sectionLinkNode));

                                String thumbnailURL = xPath.evaluate("@thumbnail", sectionLinkNode);
                                if (thumbnailURL != null && !thumbnailURL.equalsIgnoreCase(""))
                                {
                                    ImageInfo imageInfo = new ImageInfo();

                                    imageInfo.setUrl(thumbnailURL);

                                    int begingOfId = thumbnailURL.lastIndexOf('/');
                                    if (begingOfId != -1)
                                        imageInfo.setId(thumbnailURL.substring(begingOfId + 1));

                                    escenicLink.setThumbnailImageInfo(imageInfo);
                                }
                            }

                            escenicField.setReadOnly(false);
                            escenicField.setValueType(EscenicField.ValueType.ESCENIC_LISTLINKVALUE);

                            List<EscenicLink> listLinkValues = escenicField.getListLinkValues();
                            if (listLinkValues == null)
                                listLinkValues = new ArrayList<>();
                            listLinkValues.add(escenicLink);
                            escenicField.setListLinkValues(listLinkValues);

                            escenicField.setFieldName(linkKey);
                            escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);
                            // mLogger.info(escenicField.toString());

                            publicationLinksFields.put(linkKey, escenicField);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            // throw e;
        }

        return publicationLinksFields;
    }

    public List<HistoryLog> getHistoryLogs(String historyLogURL)
    {
        mLogger.info("getHistoryLogs. historyLogURL: " + historyLogURL);

        List<HistoryLog> historyLogs = new ArrayList<>();

        try {
            boolean cacheToBeUsed = false;

            HttpGetInfo httpGetInfo = getXML(historyLogURL, "<historyLog>", null, cacheToBeUsed);
            if (httpGetInfo == null || httpGetInfo.getReturnedBody() == null)
            {
                mLogger.error("getXML failed. URL: " + historyLogURL);

                return null;
            }

            String sXMLReturned = httpGetInfo.getReturnedBody();

            {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new java.io.ByteArrayInputStream(sXMLReturned.getBytes()));
                /*
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(new StringReader(sXMLReturned)));
                */
                NodeList entriesNodeList = (NodeList) xPath.evaluate("/feed/entry", document, XPathConstants.NODESET);

                // mLogger.error("entriesNodeList.getLength: " + entriesNodeList.getLength());
                for (int entryIndex = 0; entryIndex < entriesNodeList.getLength(); entryIndex++)
                {
                    Node entryNode = entriesNodeList.item(entryIndex);

                    HistoryLog historyLog = new HistoryLog();

                    {
                        String fieldValue = xPath.evaluate("updated/text()", entryNode);

                        // Expected: 2014-12-11T11:30:18.000Z

                        historyLog.setUpdated(dateFormat.parse(fieldValue));
                    }

                    historyLog.setState(xPath.evaluate("control/state/@name", entryNode));

                    historyLog.setAuthor(xPath.evaluate("author/name/text()", entryNode));

                    // mLogger.error("Updated: " + historyLog.getUpdated() + ", State: " + historyLog.getState() + ", Author: " + historyLog.getAuthor());

                    historyLogs.add(historyLog);
                }
            }
        }
        catch (Exception e) {

            mLogger.error("getHistoryLogs. historyLogURL: " + historyLogURL + " failed. Exception: " + e);

            return null;    // throw new Exception(methodName + " (" + contentIdURL + ") failed. Exception: " + e);
        }


        return historyLogs;
    }

    private String getQueryString(final String operator, final String words)
    {
        StringBuilder result = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(words, " ", false);

        // mLogger.info("getQueryString(). operator: " + operator + ", words: " + words);

        if (tokenizer.countTokens() > 0)
        {
            if (operator.equalsIgnoreCase("Without"))
            {
                while (tokenizer.hasMoreTokens())
                {
                    result.append("-").append("(").append(tokenizer.nextToken()).append(") ");
                }
            }
            else if (operator.equalsIgnoreCase("All"))
            {
                result.append("(").append(tokenizer.nextToken()).append(" ");

                while (tokenizer.hasMoreTokens())
                {
                    result.append("+ ").append(tokenizer.nextToken()).append(" ");
                }

                result.append(")");
            }
            else if (operator.equalsIgnoreCase("AtLeastOne"))
            {
                result.append("(").append(tokenizer.nextToken()).append(" ");


                while (tokenizer.hasMoreTokens())
                {
                    result.append("OR ").append(tokenizer.nextToken()).append(" ");
                }

                result.append(")");
            }
            else
            {
                mLogger.error("Unknown operator: " + operator);
            }
        }

        return result.toString();
    }

    private String getTagsQueryString(final String operator, final List<URI> tagsIdentifiers)
    {
        StringBuilder result = new StringBuilder();

        if (tagsIdentifiers.size() > 0)
        {
            if (operator.equalsIgnoreCase("All"))
            {
                result.append("(").append(tagsIdentifiers.get(0));

                for (int index = 1; index < tagsIdentifiers.size(); index++)
                {
                    result.append("+ \"").append(tagsIdentifiers.get(index)).append("\" ");
                }

                result.append(")");
            }
            else if (operator.equalsIgnoreCase("AtLeastOne"))
            {
                result.append("(").append(tagsIdentifiers.get(0));

                for (int index = 1; index < tagsIdentifiers.size(); index++)
                {
                    result.append("OR \"").append(tagsIdentifiers.get(index)).append("\" ");
                }

                result.append(")");
            }
            else
            {
                mLogger.error("Unknown operator: " + operator);
            }
        }

        return result.toString();
    }
}