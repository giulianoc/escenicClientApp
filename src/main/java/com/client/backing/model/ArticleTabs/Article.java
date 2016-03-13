package com.client.backing.model.ArticleTabs;

import com.client.backing.ArticleBrowserBacking;
import com.client.backing.model.Configuration.Configuration;
import com.client.backing.model.Configuration.FieldConfiguration;
import com.client.backing.model.SearchResult.ArticleTableData;
import com.client.service.*;
import com.client.service.util.ComplexValue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.DragDropEvent;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.w3c.dom.*;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 22/10/15
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */
public class Article implements Serializable {

    /*
    public enum ContainerType {
        CONTAINER_ENTRY,
        CONTAINER_PAYLOAD
    }
    */
    private final Logger mLogger = Logger.getLogger(this.getClass());

    private String xmlSource;
    private String eTagHeader;

    // retrieved from content details
    private List<EscenicTypeGroup> escenicTypeGroups = new ArrayList<>();
    private String selectedEscenicTypeGroupLabel;

    private HashMap<String,EscenicField> metadataFields;
    private HashMap<String,EscenicField> linksFields;
    private String selectedRelatedLinkKey;

    private HashMap<String,EscenicField> publicationLinksFields;

    List<String> stateTransitions;
    private String selectedNewState;

    private List<HistoryLog> historyLogs = null;
    private List<EscenicSection> sections = null;
    private String lockCollectionURL = null;

    private String sectionsLockName = "com.escenic.sections";
    private String myPrivateSectionsLockURL = null;
    private EscenicLock sectionsExternalLock = null;

    private MediaInfo mediaInfo = null;
    private MediaEntryInfo defaultMediaEntry;
    private EscenicLink binaryLinkField;
    private boolean binaryLinkFieldVideoAudio;

    // retrieved by solr
    private String contentType;

    private Date _dLastRetentionOnPictureFiles = null;
    private int _iDeletePictureFilesPeriodInSeconds = 60 * 60 * 24 * 1; // every day
    private int _iDeletePictureFileRetentionInSeconds = 60 * 60 * 24 * 7; // retention of a week


    public Article()
    {
        metadataFields = new HashMap<>();
        linksFields = new HashMap<>();
        publicationLinksFields = new HashMap<>();
    }

    public Article(
       String contentType,
       HashMap<String,EscenicField> metadataFields,
       HashMap<String,EscenicField> linksFields,
       HashMap<String,EscenicField> publicationLinksFields,
       List<String> stateTransitions,
       String xmlSource,
       String eTagHeader)
    {
        this.contentType = contentType;

        {
            if (metadataFields != null)
                this.metadataFields = metadataFields;
            else
                this.metadataFields = new HashMap<>();

            binaryLinkField = null;

            for (String keyField: metadataFields.keySet())
            {
                EscenicField escenicField = metadataFields.get(keyField);

                if (escenicField.getEscenicType() == null ||
                    escenicField.getEscenicType().getEscenicTypeGroup() == null)
                {
                    mLogger.error("This is not possible!!!");

                    continue;
                }

                if (!escenicTypeGroups.contains(escenicField.getEscenicType().getEscenicTypeGroup()))
                {
                    escenicTypeGroups.add(escenicField.getEscenicType().getEscenicTypeGroup());
                }

                if (escenicField.getFieldName().equalsIgnoreCase("binary") &&
                    escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LINKVALUE &&
                    escenicField.getLinkValue() != null)
                    binaryLinkField = escenicField.getLinkValue();
            }

            if (escenicTypeGroups.size() == 0)
            {
                mLogger.error("At least one group must be present");

                return;
            }

            selectedEscenicTypeGroupLabel = escenicTypeGroups.get(0).getLabel();
        }

        if (linksFields != null)
            this.linksFields = linksFields;
        else
            this.linksFields = new HashMap<>();

        if (publicationLinksFields != null)
            this.publicationLinksFields = publicationLinksFields;
        else
            this.publicationLinksFields = new HashMap<>();

        this.stateTransitions = stateTransitions;
        selectedNewState = null;

        this.xmlSource = xmlSource;
        this.eTagHeader = eTagHeader;

        // lock URL
        {
            String lockKey = "http://www.vizrt.com/types/relation/lock" + "-";
            EscenicField escenicField = linksFields.get(lockKey);
            if (escenicField != null &&
                    escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTLINKVALUE &&
                    escenicField.getListLinkValues().size() > 0 &&
                    escenicField.getListLinkValues().get(0).getHref() != null &&
                    !escenicField.getListLinkValues().get(0).getHref().equalsIgnoreCase(""))
            {
                lockCollectionURL = escenicField.getListLinkValues().get(0).getHref();
            }
        }

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            refreshLocks(escenicService);
        }
    }

    private void initWith(Article article)
    {
        stateTransitions = article.getStateTransitions();
        selectedNewState = null;

        xmlSource = article.getXmlSource();
        eTagHeader = article.geteTagHeader();

        {
            metadataFields = article.getMetadataFields();

            escenicTypeGroups.clear();

            binaryLinkField = null;

            for (String keyField: metadataFields.keySet())
            {
                EscenicField escenicField = metadataFields.get(keyField);

                if (escenicField.getEscenicType() == null ||
                        escenicField.getEscenicType().getEscenicTypeGroup() == null)
                {
                    mLogger.error("This is not possible!!!");

                    continue;
                }

                if (!escenicTypeGroups.contains(escenicField.getEscenicType().getEscenicTypeGroup()))
                {
                    escenicTypeGroups.add(escenicField.getEscenicType().getEscenicTypeGroup());
                }

                if (escenicField.getFieldName().equalsIgnoreCase("binary") &&
                        escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LINKVALUE &&
                        escenicField.getLinkValue() != null)
                    binaryLinkField = escenicField.getLinkValue();
            }

            if (escenicTypeGroups.size() == 0)
            {
                mLogger.error("At least one group must be present");

                return;
            }

            selectedEscenicTypeGroupLabel = escenicTypeGroups.get(0).getLabel();
        }

        linksFields = article.getLinksFields();
        publicationLinksFields = article.getPublicationLinksFields();

        historyLogs = article.getHistoryLogs();
        sections = null;
        lockCollectionURL = article.getLockCollectionURL();

        myPrivateSectionsLockURL = null;
        sectionsExternalLock = null;

        mediaInfo = null;
        // private MediaEntryInfo defaultMediaEntry;
    }

    public List<String> getStateTransitions() {
        List<String> localStateTransitions = new ArrayList<>();

        String currentState = metadataFields.get("com.escenic.state").getStringValue();

        for (String state: stateTransitions)
        {
            if (state.equalsIgnoreCase(currentState))
                continue;

            localStateTransitions.add(state);
        }

        return localStateTransitions;
    }

    public void setStateTransitions(List<String> stateTransitions) {
        this.stateTransitions = stateTransitions;
    }

    public String getSelectedNewState() {
        return selectedNewState;
    }

    public void setSelectedNewState(String selectedNewState) {
        this.selectedNewState = selectedNewState;
    }

    public EscenicLink getBinaryLinkField() {

        if (binaryLinkField != null)
        {
            if (binaryLinkField.getThumbnailImageInfo() != null &&
                    binaryLinkField.getThumbnailImageInfo().getUrl() != null &&
                    !binaryLinkField.getThumbnailImageInfo().getUrl().equalsIgnoreCase("") &&
                    binaryLinkField.getThumbnailImageInfo().getCachedPath() == null)
            {
                binaryLinkField.getThumbnailImageInfo().setCachedPath(
                        getAndCachePicture(binaryLinkField.getThumbnailImageInfo().getUrl(),
                                binaryLinkField.getThumbnailImageInfo().getId()));
            }
        }

        return binaryLinkField;
    }

    public void setBinaryLinkField(EscenicLink binaryLinkField) {
        this.binaryLinkField = binaryLinkField;
    }

    public boolean isBinaryLinkFieldVideoAudio() {
        if (binaryLinkField != null)
            return MimeTypes.isAudioMimeType(binaryLinkField.getType()) || MimeTypes.isVideoMimeType(binaryLinkField.getType());
        else
            return false;
    }

    public void setBinaryLinkFieldVideoAudio(boolean binaryLinkFieldVideoAudio) {
        this.binaryLinkFieldVideoAudio = binaryLinkFieldVideoAudio;
    }

    public boolean isArticleModified()
    {
        boolean isArticleModified = false;


        if (selectedNewState != null && !selectedNewState.equalsIgnoreCase(""))
        {
            isArticleModified = true;

            mLogger.info("'State' changed");
        }

        {
            for (String fieldKey: metadataFields.keySet())
            {
                if (metadataFields.get(fieldKey).getChangeType() != EscenicField.ChangeType.ESCENIC_NOCHANGE)
                {
                    isArticleModified = true;

                    mLogger.info("Field changed (metadataFields). Name: " + fieldKey + ", ChangeType: " + metadataFields.get(fieldKey).getChangeType());

                    // break;
                }
            }

            for (String fieldKey: linksFields.keySet())
            {
                if (linksFields.get(fieldKey).getChangeType() != EscenicField.ChangeType.ESCENIC_NOCHANGE)
                {
                    isArticleModified = true;

                    mLogger.info("Field changed (linksFields). Name: " + fieldKey + ", ChangeType: " + linksFields.get(fieldKey).getChangeType());

                    // break;
                }
            }
        }

        // if (!isArticleModified)
        {
            if (sections != null)
            {
                for (EscenicSection escenicSection: sections)
                {
                    // if (!isArticleModified)
                    //    break;

                    if (escenicSection.getEscenicField() == null)
                    {
                        isArticleModified = true;

                        mLogger.info("New section was added. Summary: " + escenicSection.getSummary());

                        // break;
                    }
                    else
                    {
                        for (EscenicLink escenicLink: escenicSection.getEscenicField().getListLinkValues())
                        {
                            if (escenicLink.getHref().equalsIgnoreCase(escenicSection.getHref()))
                            {
                                if (escenicLink.getChangeType() != EscenicLink.ChangeType.ESCENIC_NOCHANGE)
                                {
                                    isArticleModified = true;

                                    mLogger.info("Section " + escenicSection.getName() + " was " + escenicLink.getChangeType());

                                    // break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return isArticleModified;
    }

    public static void encodeBodyNodeToBePresented(Document document, Node bodyValueNode, XPath xPath)
    {
        try
        {
            HttpServletRequest origRequest = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
            // ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

            // img processing
            {
                NodeList imgNodeList = ((Element) bodyValueNode).getElementsByTagName("img");

                for (int imgIndex = 0; imgIndex < imgNodeList.getLength(); imgIndex++)
                {
                    Node imgNode = imgNodeList.item(imgIndex);

                    Attr srcAttribute = ((Element) imgNode).getAttributeNode("src");

                    String src = xPath.evaluate("@src", imgNode);

                    String articleId = null;
                    {
                        int begingOfId = src.lastIndexOf('/');
                        if (begingOfId != -1)
                            articleId = src.substring(begingOfId + 1);
                    }

                    if (articleId != null)
                    {
                        String cachedPath = getAndCachePicture(src, articleId);

                        if (cachedPath != null)
                        {
                            String localSrc = "http://" + origRequest.getServerName() + ":" + origRequest.getServerPort() +
                                origRequest.getContextPath() + "/resources/" + cachedPath;
                            // Logger.getLogger(Article.class).error(localSrc);

                            Attr dataSrcAttribute = document.createAttribute("data-src");
                            dataSrcAttribute.setValue(src);
                            ((Element) imgNode).setAttributeNode(dataSrcAttribute);

                            srcAttribute.setValue(localSrc);
                        }
                    }
                }
            }

            // Make sure the body has a 'div' tag as root (it would not be saved if there is no a root 'div' tag)
            {
                // Logger.getLogger(Article.class).error("bodyValueNode.getChildNodes().getLength(): " + bodyValueNode.getChildNodes().getLength());
                // Logger.getLogger(Article.class).error("bodyValueNode.getFirstChild().getNodeName(): " + bodyValueNode.getFirstChild().getNodeName());
                if (bodyValueNode.getChildNodes().getLength() != 1 ||
                    (bodyValueNode.getChildNodes().getLength() == 1 && !bodyValueNode.getFirstChild().getNodeName().equalsIgnoreCase("div")))
                {
                    Element divElement = document.createElement("div");

                    Attr xmlnsAttribute = document.createAttribute("xmlns");
                    xmlnsAttribute.setValue("http://www.w3.org/1999/xhtml");
                    divElement.setAttributeNode(xmlnsAttribute);

                    while (bodyValueNode.hasChildNodes())
                    {
                        divElement.appendChild(bodyValueNode.getFirstChild());
                        bodyValueNode.removeChild(bodyValueNode.getFirstChild());
                    }

                    bodyValueNode.appendChild(divElement);
                }
            }
        }
        catch (Exception e)
        {
            Logger.getLogger(Article.class).error("Exception: " + e.getMessage());
        }
    }

    public Element decodeBodyToBeSaved(String body)
    {
        Element bodyElement = null;
        String localBody;

        try
        {
            localBody = "<div xmlns=\"http://www.w3.org/1999/xhtml\">" + body + "</div>";
            String unescapedBody = StringEscapeUtils.unescapeHtml4(localBody.replaceAll("<p> </p>", "<p></p>"));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new java.io.ByteArrayInputStream(unescapedBody.getBytes()));

            bodyElement = document.getDocumentElement();

            // img processing
            {
                NodeList imgNodeList = bodyElement.getElementsByTagName("img");

                for (int imgIndex = 0; imgIndex < imgNodeList.getLength(); imgIndex++)
                {
                    Node imgNode = imgNodeList.item(imgIndex);

                    Attr srcAttribute = ((Element) imgNode).getAttributeNode("src");
                    Attr dataSrcAttribute = ((Element) imgNode).getAttributeNode("data-src");

                    if (srcAttribute != null && dataSrcAttribute != null)
                    {
                        String src = dataSrcAttribute.getValue();
                        srcAttribute.setValue(src);

                        ((Element) imgNode).removeAttribute("data-src");
                    }
                }
            }
        }
        catch (Exception e)
        {
            Logger.getLogger(Article.class).error("decodeBodyNodeToBeSaved. Exception: " + e.getMessage());

            bodyElement = null;
        }

        return bodyElement;
    }

    /*
    public String decodeBodyToBeSaved(String body)
    {
        String newBody = null;

        try
        {
            String unescapedBody = "<root>" + StringEscapeUtils.unescapeHtml4(body) + "</root>";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new java.io.ByteArrayInputStream(unescapedBody.getBytes()));

            // img processing
            {
                NodeList imgNodeList = document.getElementsByTagName("img");

                for (int imgIndex = 0; imgIndex < imgNodeList.getLength(); imgIndex++)
                {
                    Node imgNode = imgNodeList.item(imgIndex);

                    Attr srcAttribute = ((Element) imgNode).getAttributeNode("src");
                    Attr dataSrcAttribute = ((Element) imgNode).getAttributeNode("data-src");

                    if (srcAttribute != null && dataSrcAttribute != null)
                    {
                        String src = dataSrcAttribute.getValue();
                        srcAttribute.setValue(src);

                        ((Element) imgNode).removeAttribute("data-src");
                    }
                }
            }

            {
                Logger.getLogger(Article.class).info("Transform DOM to String...");
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                // transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

                Node rootNode = document.getFirstChild();
                newBody = "";
                while (rootNode.hasChildNodes())
                {
                    DOMSource source = new DOMSource(rootNode.getFirstChild());
                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);

                    transformer.transform(source, result);

                    newBody += writer.getBuffer().toString();

                    rootNode.removeChild(rootNode.getFirstChild());
                }
            }
        }
        catch (Exception e)
        {
            Logger.getLogger(Article.class).error("decodeBodyNodeToBeSaved. Exception: " + e.getMessage());

            newBody = null;
        }

        return newBody;
    }
    */

    public String applyChangesToXML(List<String> privateLocksURLs)
    {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            XPath xPath = escenicService.getxPath();

            xmlSource = xmlSource.replaceAll("<p> </p>", "<p></p>");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new java.io.ByteArrayInputStream(xmlSource.getBytes()));

            /*
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xmlSource)));
            */

            Node entryNode = (Node) xPath.evaluate("/entry", document, XPathConstants.NODE);

            String xmlnsVdfAttribute = ((Element) entryNode).getAttribute("xmlns:vdf");
            if (xmlnsVdfAttribute == null || xmlnsVdfAttribute.equalsIgnoreCase(""))
                ((Element) entryNode).setAttribute("xmlns:vdf", "http://www.vizrt.com/types");

            String xmlnsAgeAttribute = ((Element) entryNode).getAttribute("xmlns:age");
            if (xmlnsAgeAttribute == null || xmlnsAgeAttribute.equalsIgnoreCase(""))
                ((Element) entryNode).setAttribute("xmlns:age", "http://purl.org/atompub/age/1.0");

            Node payloadNode = (Node) xPath.evaluate("/entry/content/payload", document, XPathConstants.NODE);

            for (String fieldKey: metadataFields.keySet())
            {
                EscenicField escenicField = metadataFields.get(fieldKey);

                if (escenicField.getChangeType() == EscenicField.ChangeType.ESCENIC_ADDED ||
                        escenicField.getChangeType() == EscenicField.ChangeType.ESCENIC_MODIFIED)
                {
                    if (escenicField.getMyPrivateLockURL() == null)
                    {
                        mLogger.error("The '" + escenicField.getFieldName() + "' field was modified but no Lock is found");

                        continue;
                    }

                    if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_DATEVALUE ||
                            escenicField.getValueType() == EscenicField.ValueType.ESCENIC_DECIMALVALUE ||
                            escenicField.getValueType() == EscenicField.ValueType.ESCENIC_BOOLEANVALUE ||
                            escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LONGVALUE ||
                            escenicField.getValueType() == EscenicField.ValueType.ESCENIC_STRINGVALUE)
                    {
                        String fieldValue;
                        if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_DATEVALUE)
                        {
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

                            if (escenicField.getDateValue() == null)
                                fieldValue = null;
                            else
                                fieldValue = dateFormat.format(escenicField.getDateValue());
                        }
                        else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_BOOLEANVALUE)
                        {
                            fieldValue = String.valueOf(escenicField.getBooleanValue());
                        }
                        else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LONGVALUE)
                        {
                            if (escenicField.getLongValue() == null)
                                fieldValue = null;
                            else
                                fieldValue = String.valueOf(escenicField.getLongValue());
                        }
                        else // if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_STRINGVALUE)
                        {
                            fieldValue = escenicField.getStringValue();
                        }

                        mLogger.info("Changing XML (metadata). FieldName: " + escenicField.getFieldName() + ", FieldValue: " + fieldValue);

                        if (escenicField.getEscenicType().getEscenicTypeGroup().getName().
                            equalsIgnoreCase("COM.ESCENIC.METADATA"))
                        {
                            if (escenicField.getFieldName().equalsIgnoreCase("publication"))
                            {
                                // read only
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.priority"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.homeSection"))
                            {
                                // read only
                                // already managed in the 'section' tab
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.displayUri"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.expireDate"))
                            {
                                Node expireNode = (Node) xPath.evaluate("expires", entryNode, XPathConstants.NODE);

                                if (fieldValue != null)
                                {
                                    if (expireNode == null)
                                    {
                                        Element fieldElement = document.createElement("age:expires");
                                        fieldElement.appendChild(document.createTextNode(fieldValue));
                                        ((Element) entryNode).appendChild(fieldElement);
                                    }
                                    else
                                    {
                                        expireNode.setTextContent(fieldValue);
                                    }
                                }
                                else
                                {
                                    entryNode.removeChild(expireNode);
                                }
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.createdBy"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.displayId"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.publishedDate"))
                            {
                                Node publishedNode = (Node) xPath.evaluate("updated", entryNode, XPathConstants.NODE);

                                if (fieldValue != null)
                                {
                                    if (publishedNode == null)
                                    {
                                        Element fieldElement = document.createElement("updated");
                                        fieldElement.appendChild(document.createTextNode(fieldValue));
                                        ((Element) entryNode).appendChild(fieldElement);
                                    }
                                    else
                                    {
                                        publishedNode.setTextContent(fieldValue);
                                    }
                                }
                                else
                                {
                                    entryNode.removeChild(publishedNode);
                                }
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.source"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.createDate"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.activateDate"))
                            {
                                Node availableNode = (Node) xPath.evaluate("available", entryNode, XPathConstants.NODE);

                                if (fieldValue != null)
                                {
                                    if (availableNode == null)
                                    {
                                        Element fieldElement = document.createElement("dcterms:available");
                                        fieldElement.appendChild(document.createTextNode(fieldValue));
                                        ((Element) entryNode).appendChild(fieldElement);
                                    }
                                    else
                                    {
                                        availableNode.setTextContent(fieldValue);
                                    }
                                }
                                else
                                {
                                    entryNode.removeChild(availableNode);
                                }
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.sourceid"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.displayName"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.firstPublishDate"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.previewUri"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.lastModifiedDate"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else if (escenicField.getFieldName().equalsIgnoreCase("com.escenic.lockURI"))
                            {
                                // forced to be read only
                                mLogger.error("I never saw a field like that and I do not know where to save it");
                            }
                            else
                            {
                                mLogger.error("COM.ESCENIC.METADATA field unknown: " + escenicField.getFieldName());
                            }
                        }
                        else if (escenicField.getEscenicType().getEscenicTypeGroup().getName().
                            equalsIgnoreCase("default") &&
                            escenicField.getFieldName().equalsIgnoreCase("com.escenic.state"))
                        {
                            Node stateNode = (Node) xPath.evaluate("control/state", entryNode, XPathConstants.NODE);
                            if (stateNode == null)
                            {
                                mLogger.error("StateNode (control/state) was not found");

                                return null;
                            }

                            stateNode.appendChild(document.createTextNode(fieldValue));
                        }
                        else
                        {
                            Node fieldNode = (Node) xPath.evaluate("field[@name='" + escenicField.getFieldName() + "']", payloadNode, XPathConstants.NODE);

                            if (fieldNode == null)
                            {
                                Element fieldElement = document.createElement("vdf:field");
                                ((Element) payloadNode).appendChild(fieldElement);

                                Attr nameAttribute = document.createAttribute("name");
                                nameAttribute.setValue(metadataFields.get(fieldKey).getFieldName());
                                fieldElement.setAttributeNode(nameAttribute);

                                Element valueElement = document.createElement("vdf:value");

                                if (escenicField.getFieldName().equalsIgnoreCase("body"))
                                {
                                    if (fieldValue != null)
                                    {
                                        Element bodyElement = decodeBodyToBeSaved(fieldValue);
                                        if (bodyElement == null)
                                            mLogger.error("bodyElement is null");
                                        else
                                        {
                                            /*
                                            while (bodyElement.hasChildNodes())
                                            {
                                                Node bodyAdoptedNode = document.importNode(bodyElement.getFirstChild(), true);
                                                valueElement.appendChild(bodyAdoptedNode);
                                                bodyElement.removeChild(bodyElement.getFirstChild());
                                            }
                                            */
                                            Node bodyAdoptedNode = document.importNode(bodyElement, true);
                                            valueElement.appendChild(bodyAdoptedNode);
                                        }
                                        /*
                                        String body = decodeBodyToBeSaved(fieldValue);
                                        if (body == null)
                                            mLogger.error("body is null");
                                        else
                                            valueElement.appendChild(document.createCDATASection(body));
                                        */
                                    }
                                }
                                else
                                {
                                    if (fieldValue != null)
                                        valueElement.appendChild(document.createTextNode(fieldValue));
                                }
                                fieldElement.appendChild(valueElement);
                            }
                            else
                            {
                                Node valueNode = (Node) xPath.evaluate("value", fieldNode, XPathConstants.NODE);

                                if (valueNode == null)
                                {
                                    Element valueElement = document.createElement("vdf:value");

                                    if (escenicField.getFieldName().equalsIgnoreCase("body"))
                                    {
                                        Element bodyElement = decodeBodyToBeSaved(fieldValue);
                                        if (bodyElement == null)
                                            mLogger.error("bodyNode is null");
                                        else
                                        {
                                            /*
                                            while (bodyElement.hasChildNodes())
                                            {
                                                Node bodyAdoptedNode = document.importNode(bodyElement.getFirstChild(), true);
                                                valueElement.appendChild(bodyAdoptedNode);
                                                bodyElement.removeChild(bodyElement.getFirstChild());
                                            }
                                            */
                                            Node bodyAdoptedNode = document.importNode(bodyElement, true);
                                            valueElement.appendChild(bodyAdoptedNode);
                                        }
                                        /*
                                        String body = decodeBodyToBeSaved(fieldValue);
                                        if (body == null)
                                            mLogger.error("body is null");
                                        else
                                            valueElement.appendChild(document.createCDATASection(body));
                                        */
                                    }
                                    else
                                    {
                                        valueElement.appendChild(document.createTextNode(fieldValue));
                                    }

                                    ((Element) fieldNode).appendChild(valueElement);
                                }
                                else
                                {
                                    if (escenicField.getFieldName().equalsIgnoreCase("body"))
                                    {
                                        Element bodyElement = decodeBodyToBeSaved(fieldValue);
                                        if (bodyElement == null)
                                            mLogger.error("bodyNode is null");
                                        else
                                        {
                                            while (valueNode.hasChildNodes())
                                                valueNode.removeChild(valueNode.getFirstChild());

                                            /*
                                            while (bodyElement.hasChildNodes())
                                            {
                                                Node bodyAdoptedNode = document.importNode(bodyElement.getFirstChild(), true);
                                                valueNode.appendChild(bodyAdoptedNode);
                                                bodyElement.removeChild(bodyElement.getFirstChild());
                                            }
                                            */
                                            Node bodyAdoptedNode = document.importNode(bodyElement, true);
                                            valueNode.appendChild(bodyAdoptedNode);
                                        }
                                        /*
                                        String body = decodeBodyToBeSaved(fieldValue);
                                        if (body == null)
                                            mLogger.error("body is null");
                                        else
                                            valueNode.appendChild(document.createCDATASection(body));
                                        */
                                    }
                                    else
                                    {
                                        ((Element) valueNode).setTextContent(fieldValue);
                                    }
                                }
                            }
                        }

                        privateLocksURLs.add(escenicField.getMyPrivateLockURL());
                    }
                    else if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LINKVALUE)
                    {
                        mLogger.error("This is not possible since this field is set as read only (in EscenicField:createEmptyEscenicField)");
                    }
                }
                else
                {
                    if (escenicField.getMyPrivateLockURL() != null)
                    {
                        mLogger.error("The '" + escenicField.getFieldName() + "' field was NOT modified but Lock is found");

                        continue;
                    }
                }
            }

            for (String fieldKey: linksFields.keySet())
            {
                EscenicField escenicField = linksFields.get(fieldKey);

                List<EscenicLink> escenicLinks = escenicField.getListLinkValues();

                if (escenicField.getChangeType() == EscenicField.ChangeType.ESCENIC_ADDED ||
                    escenicField.getChangeType() == EscenicField.ChangeType.ESCENIC_MODIFIED)
                {
                    if (escenicField.getValueType() != EscenicField.ValueType.ESCENIC_LISTLINKVALUE)
                    {
                        mLogger.error("link that does not have the ESCENIC_LISTLINKVALUE type!!!");

                        continue;
                    }

                    if (escenicField.getMyPrivateLockURL() == null)
                    {
                        mLogger.error("The '" + escenicField.getFieldName() + "' field, was modified but no Lock is found");

                        continue;
                    }

                    for (EscenicLink escenicLink: escenicLinks)
                    {
                        if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_ADDED ||
                            escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_REMOVED)
                        {
                            Node linkNode = (Node) xPath.evaluate("link[@rel='related' and @group='" + escenicLink.getGroup() + "' and @href='" + escenicLink.getHref() + "']", entryNode, XPathConstants.NODE);

                            if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_ADDED)
                            {
                                if (linkNode != null)
                                {
                                    mLogger.error("Link (rel: 'related', group: '" + escenicLink.getGroup() + "', href: '" + escenicLink.getHref() + "') was added but it is already present inside the article XML");

                                    continue;
                                }

                                Element linkElement = document.createElement("link");
                                ((Element) entryNode).appendChild(linkElement);

                                Attr relAttribute = document.createAttribute("rel");
                                relAttribute.setValue("related");
                                linkElement.setAttributeNode(relAttribute);

                                Attr hrefAttribute = document.createAttribute("href");
                                hrefAttribute.setValue(escenicLink.getHref());
                                linkElement.setAttributeNode(hrefAttribute);

                                Attr typeAttribute = document.createAttribute("type");
                                typeAttribute.setValue("application/atom+xml; type=entry");
                                linkElement.setAttributeNode(typeAttribute);

                                Attr groupAttribute = document.createAttribute("metadata:group");
                                groupAttribute.setValue(escenicLink.getGroup());
                                linkElement.setAttributeNode(groupAttribute);

                                if (escenicLink.getModel() != null && !escenicLink.getModel().equalsIgnoreCase(""))
                                {
                                    Element payloadElement = document.createElement("vdf:payload");
                                    linkElement.appendChild(payloadElement);

                                    Attr modelAttribute = document.createAttribute("model");
                                    modelAttribute.setValue(escenicLink.getModel());
                                    payloadElement.setAttributeNode(modelAttribute);

                                    Element fieldElement = document.createElement("vdf:field");
                                    payloadElement.appendChild(fieldElement);

                                    Attr nameAttribute = document.createAttribute("name");
                                    nameAttribute.setValue("title");
                                    fieldElement.setAttributeNode(nameAttribute);

                                    if (escenicLink.getTitle() != null && !escenicLink.getTitle().equalsIgnoreCase(""))
                                    {
                                        Element valueElement = document.createElement("vdf:value");
                                        valueElement.appendChild(document.createTextNode(escenicLink.getTitle()));
                                        fieldElement.appendChild(valueElement);
                                    }

                                    if (escenicLink.getThumbnailImageInfo() != null &&
                                        escenicLink.getThumbnailImageInfo().getUrl() != null &&
                                        !escenicLink.getThumbnailImageInfo().getUrl().equalsIgnoreCase(""))
                                    {
                                        Attr thumbnailAttribute = document.createAttribute("metadata:thumbnail");
                                        thumbnailAttribute.setValue(escenicLink.getThumbnailImageInfo().getUrl());
                                        linkElement.setAttributeNode(thumbnailAttribute);
                                    }
                                }
                            }
                            else if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_REMOVED)
                            {
                                entryNode.removeChild(linkNode);
                            }
                        }
                    }

                    privateLocksURLs.add(escenicField.getMyPrivateLockURL());
                }
            }

            // sections
            if (sections != null)
            {
                Node entryPublication = (Node) xPath.evaluate("/entry/publication", document, XPathConstants.NODE);
                EscenicSection escenicHomeSection = null;

                Node homeSectionNode = (Node) xPath.evaluate("link[@rel='http://www.vizrt.com/types/relation/home-section']", entryPublication, XPathConstants.NODE);
                entryPublication.removeChild(homeSectionNode);

                for (EscenicSection escenicSection: sections)
                {
                    if (escenicSection.getHomeSection())
                    {
                        if (escenicHomeSection != null)
                            mLogger.error("It should never happen, the following two sections are both home_section: " + escenicSection.getSummary() + " and " + escenicHomeSection.getSummary());

                        escenicHomeSection = escenicSection;
                    }

                    if (escenicSection.getEscenicField() == null)
                    {
                        mLogger.info("New section was added. Summary: " + escenicSection.getSummary());

                        {
                            Element linkElement = document.createElement("link");
                            ((Element) entryPublication).appendChild(linkElement);

                            Attr relAttribute = document.createAttribute("rel");
                            relAttribute.setValue("http://www.vizrt.com/types/relation/section");
                            linkElement.setAttributeNode(relAttribute);

                            Attr hrefAttribute = document.createAttribute("href");
                            hrefAttribute.setValue(escenicSection.getHref());
                            linkElement.setAttributeNode(hrefAttribute);

                            Attr titleAttribute = document.createAttribute("title");
                            titleAttribute.setValue(escenicSection.getName());
                            linkElement.setAttributeNode(titleAttribute);

                            Attr typeAttribute = document.createAttribute("type");
                            typeAttribute.setValue("application/atom+xml; type=entry");
                            linkElement.setAttributeNode(typeAttribute);
                        }
                    }
                    else
                    {
                        for (EscenicLink escenicLink: escenicSection.getEscenicField().getListLinkValues())
                        {
                            if (escenicLink.getHref().equalsIgnoreCase(escenicSection.getHref()))
                            {
                                if (escenicLink.getChangeType() != EscenicLink.ChangeType.ESCENIC_NOCHANGE)
                                {
                                    mLogger.info("Section " + escenicSection.getName() + " was " + escenicLink.getChangeType());

                                    if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_REMOVED)
                                    {
                                        Node linkToRemove = (Node) xPath.evaluate("link[@href='" + escenicSection.getHref() + "']", entryPublication, XPathConstants.NODE);

                                        entryPublication.removeChild(linkToRemove);
                                    }
                                    else if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_MODIFIED)
                                    {
                                        // the only possible change is about the home_section.
                                    }
                                    else if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_ADDED)
                                    {
                                        mLogger.error("This should never happen because when a section is added, escenicLink is null");

                                        return null;
                                    }
                                }
                            }
                        }
                    }
                }

                {
                    if (escenicHomeSection == null)
                    {
                        mLogger.error("This should never happen, escenicHomeSection cannot be null");

                        return null;
                    }

                    {
                        Element linkElement = document.createElement("link");
                        ((Element) entryPublication).appendChild(linkElement);

                        Attr relAttribute = document.createAttribute("rel");
                        relAttribute.setValue("http://www.vizrt.com/types/relation/home-section");
                        linkElement.setAttributeNode(relAttribute);

                        Attr hrefAttribute = document.createAttribute("href");
                        hrefAttribute.setValue(escenicHomeSection.getHref());
                        linkElement.setAttributeNode(hrefAttribute);

                        Attr titleAttribute = document.createAttribute("title");
                        titleAttribute.setValue(escenicHomeSection.getName());
                        linkElement.setAttributeNode(titleAttribute);

                        Attr typeAttribute = document.createAttribute("type");
                        typeAttribute.setValue("application/atom+xml; type=entry");
                        linkElement.setAttributeNode(typeAttribute);
                    }


                    // home_section is also a child of entry
                    Node homeSectionLink = (Node) xPath.evaluate("/entry/link[@rel='http://www.vizrt.com/types/relation/home-section']", document, XPathConstants.NODE);

                    if (homeSectionLink != null)
                    {
                        Attr hrefAttribute = ((Element) homeSectionLink).getAttributeNode("href");
                        hrefAttribute.setValue(escenicHomeSection.getHref());

                        Attr titleAttribute = ((Element) homeSectionLink).getAttributeNode("title");
                        titleAttribute.setValue(escenicHomeSection.getName());
                    }
                }

                if (getMyPrivateSectionsLockURL() != null)
                    privateLocksURLs.add(getMyPrivateSectionsLockURL());
            }

            mLogger.info("Transform DOM to String...");
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            // transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(document);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            transformer.transform(source, result);

            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());

            return null;
        }
    }

    /*
    public void ckEditorSaveEventListener(String body)
    {
        mLogger.error("ckEditorAjaxEventListener. body: " + body);
    }
    */

    public void ckEditorChangeEventListener(String body)
    {
        EscenicField bodyEscenicField = metadataFields.get("body");

        valueChangeListener(bodyEscenicField, null);
    }

    public void save()
    {
        List<String> privateLocksURLs = new ArrayList<>();
        String id = getMetadataFields().get("com.escenic.displayId").getStringValue();

        mLogger.info("saveArticle. ID: " + id);

        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

        // check if the state was changed
        {
            String fieldKey = "com.escenic.state";
            EscenicField escenicField = metadataFields.get(fieldKey);
            if (escenicField == null)
            {
                mLogger.error("No EscenicField was found for the " + fieldKey + " field");

                return;
            }

            if (selectedNewState != null && !selectedNewState.equalsIgnoreCase(escenicField.getStringValue()))
            {
                String privateLockURL = escenicService.lockResource(lockCollectionURL, fieldKey);
                if (privateLockURL == null)
                {
                    mLogger.error("Lock failed for the " + fieldKey + " field");

                    return;
                }

                mLogger.info("The " + fieldKey + " field was locked. privateLockURL: " + privateLockURL);

                escenicField.setMyPrivateLockURL(privateLockURL);

                escenicField.setStringValue(selectedNewState);
            }
        }

        String xmlNewArticleSource = applyChangesToXML(privateLocksURLs);

        if (xmlNewArticleSource == null)
        {
            mLogger.error("NEW XML is null. xmlNewArticleSource: " + xmlNewArticleSource);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Content", "Error generating the XML");
            // FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        mLogger.info("NEW XML: " + xmlNewArticleSource);

        try {
            escenicService.saveArticle(id, geteTagHeader(), privateLocksURLs, xmlNewArticleSource);

            removeLocks();

            reload();
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }

    }

    public void reload()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");
        boolean cacheToBeUsed = false;

        Article refreshedArticle = escenicService.getArticleContentDetails(
                getMetadataFields().get("com.escenic.displayId").getStringValue(),
                cacheToBeUsed);

        if (refreshedArticle == null)
        {
            mLogger.error("refreshedArticle is null. Id: " + getMetadataFields().get("com.escenic.displayId").getStringValue());

            return;
        }

        initWith(refreshedArticle);
    }

    static private String getAndCachePicture(String pictureURL, String articleId)
    {

        Logger.getLogger(Article.class).info("getAndCacheJPEG (pictureUrl: " + pictureURL +
                ", articleId: " + articleId);

        if (pictureURL == null)
        {
            Logger.getLogger(Article.class).error("Wrong input parameter");

            return null;
        }

        String pictureFileName;
        if (articleId == null)
            pictureFileName = pictureURL.substring(pictureURL.lastIndexOf('/') + 1) + ".jpg";
        else
            pictureFileName = articleId + ".jpg";

        ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance()
                .getExternalContext().getContext();
        String absoluteWebAppRealPath = ctx.getRealPath("/");
        String relativePathName = "cache/images/" + pictureFileName;

        String pictureAbsolutePathName = absoluteWebAppRealPath + "resources/" + relativePathName;

        Logger.getLogger(Article.class).info("absoluteWebAppRealPath: " + absoluteWebAppRealPath +
                ", relativePathName: " + relativePathName +
                ", pictureAbsolutePathName: " + pictureAbsolutePathName);

        File pictureFile = new File(pictureAbsolutePathName);

        if (pictureFile.exists()) {
            Logger.getLogger(Article.class).info("Picture already cached: " + pictureAbsolutePathName);

            try {
                FileUtils.touch(pictureFile);
            }
            catch (Exception e)
            {
                Logger.getLogger(Article.class).error("FileUtils.touch(" + pictureFile + ") failed. Exception: " + e.getMessage());
            }
        }
        else
        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            Logger.getLogger(Article.class).info("Loading the " + pictureURL + " picture");

                /*
                URL url = new URL(pictureURL);
                BufferedImage sourceImage = ImageIO.read(url);
                */
            try
            {
                InputStream in = escenicService.loadBinaryFromEscenic(pictureURL);

                BufferedImage sourceImage = ImageIO.read(in);

                if (sourceImage == null)
                {
                    Logger.getLogger(Article.class).error("ImageIO.read(" + pictureURL + ") failed");

                    throw new Exception("ImageIO.read(" + pictureURL + ") failed");
                }

                BufferedImage resizedImage;

                {
                    int sourceWidth = sourceImage.getWidth();
                    int sourceHeight = sourceImage.getHeight();

                    int newWidth = sourceWidth;
                    int newHeight = sourceHeight;

                    Logger.getLogger(Article.class).info("Loaded the " + pictureURL + " picture. Type: " + sourceImage.getType());

                    resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB /* sourceImage.getType() */);
                    Graphics2D g = resizedImage.createGraphics();
                    // g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(sourceImage, 0, 0, newWidth, newHeight, 0, 0, sourceWidth, sourceHeight, null);
                    // g.drawImage(sourceImage, 0, 0, newWidth, newHeight, null);
                    g.dispose();
                }

                Logger.getLogger(Article.class).info("Saving the generated picture in " + pictureAbsolutePathName);

                File resizedPictureFile = new File(pictureAbsolutePathName);
                ImageIO.write(resizedImage, "jpg", resizedPictureFile);
            }
            catch (Exception ex)
            {
                Logger.getLogger(Article.class).error("Image processing failed (pictureURL: " + pictureURL + "). Exception: " + ex.getMessage());

                relativePathName = null;
                // throw ex;
            }
        }


        return relativePathName;
    }

    private void cacheRetention()
    {
        ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        String absoluteWebAppRealPath = ctx.getRealPath("/");

        if (_dLastRetentionOnPictureFiles == null ||
                _dLastRetentionOnPictureFiles.getTime() + (_iDeletePictureFilesPeriodInSeconds * 1000) < new Date().getTime()) {
            _dLastRetentionOnPictureFiles = new Date();

            mLogger.info("Run PictureFiles retention period");

            File pictureDirectory = new File(absoluteWebAppRealPath + "resources/cache/images");

            File[] lsPictureFiles = pictureDirectory.listFiles();

            for (int index = 0; index < lsPictureFiles.length; index++) {
                File pictureFileToBeRemoved = lsPictureFiles[index];

                if (pictureFileToBeRemoved.isFile() && pictureFileToBeRemoved.getName().endsWith(".jpg")) {
                    mLogger.info("Picture file to be verified. File: " + pictureFileToBeRemoved.getPath());

                    if (pictureFileToBeRemoved.lastModified() + (_iDeletePictureFileRetentionInSeconds * 1000) < new Date().getTime()) {
                        if (!pictureFileToBeRemoved.delete())
                            mLogger.error("Retention period on picture files. Delete file " + pictureFileToBeRemoved.getPath() + " failed");
                        else
                            mLogger.info("Retention period on picture files. Delete file " + pictureFileToBeRemoved.getPath());
                    }

                }
            }
        }
    }

    public EscenicLock getSectionsExternalLock() {
        return sectionsExternalLock;
    }

    public void setSectionsExternalLock(EscenicLock sectionsExternalLock) {
        this.sectionsExternalLock = sectionsExternalLock;
    }

    public String getLockCollectionURL() {
        return lockCollectionURL;
    }

    public void setLockCollectionURL(String lockCollectionURL) {
        this.lockCollectionURL = lockCollectionURL;
    }

    public HashMap<String, EscenicField> getPublicationLinksFields() {
        return publicationLinksFields;
    }

    public void setPublicationLinksFields(HashMap<String, EscenicField> publicationLinksFields) {
        this.publicationLinksFields = publicationLinksFields;
    }

    public String getXmlSource() {
        return xmlSource;
    }

    public void setXmlSource(String xmlSource) {
        this.xmlSource = xmlSource;
    }

    public String geteTagHeader() {
        return eTagHeader;
    }

    public void seteTagHeader(String eTagHeader) {
        this.eTagHeader = eTagHeader;
    }

    public void removeLocks()
    {
        mLogger.info("removeLocks");

        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

        for (String keyField : getMetadataFields().keySet())
        {
            try
            {
                EscenicField escenicField = getMetadataFields().get(keyField);

                if (escenicField != null && escenicField.getMyPrivateLockURL() != null)
                {
                    mLogger.info("remove lock for the " + escenicField.getFieldName() + " field");
                    escenicService.removeLock(escenicField.getMyPrivateLockURL());
                }
            }
            catch(Exception e)
            {
                mLogger.error("httpDelete exception: " + e);

                // throw e;
            }
        }

        for (String keyField : getLinksFields().keySet())
        {
            try
            {
                EscenicField escenicField = getLinksFields().get(keyField);

                if (escenicField != null && escenicField.getMyPrivateLockURL() != null)
                {
                    mLogger.info("remove lock for the " + escenicField.getFieldName() + " field");
                    escenicService.removeLock(escenicField.getMyPrivateLockURL());
                }
            }
            catch(Exception e)
            {
                mLogger.error("httpDelete exception: " + e);

                // throw e;
            }
        }

        // sections
        try
        {
            if (getMyPrivateSectionsLockURL() != null)
            {
                mLogger.info("remove lock for the 'sections'");
                escenicService.removeLock(getMyPrivateSectionsLockURL());
            }
        }
        catch(Exception e)
        {
            mLogger.error("httpDelete exception: " + e);

            // throw e;
        }
    }

    public void refreshLocks(EscenicService escenicService)
    {
        try {
            if (escenicService == null)
            {
                mLogger.error("refreshLocks. escenicService is null");

                return;
            }

            String articleId = getMetadataFields().get("com.escenic.displayId").getStringValue();

            // escenicService is an argument because this method is called by a Timer and
            //      there is no request and then no session to get escenicService
            // FacesContext context = FacesContext.getCurrentInstance();
            // HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
            // EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            HashMap<String,EscenicLock> externalLockHashMap = escenicService.getExternalLocks(articleId, lockCollectionURL);

            for (String keyField: metadataFields.keySet())
            {
                EscenicField escenicField = metadataFields.get(keyField);

                EscenicLock externalLock = externalLockHashMap.get(escenicField.getFieldName());

                if (externalLock == null)
                {
                    if (escenicField.getExternalLock() != null)
                    {
                        escenicField.setExternalLock(null);

                        LockChanged lockChanged = new LockChanged();
                        lockChanged.setId(articleId);
                        lockChanged.setKeyField(keyField);

                        EventBus eventBus = EventBusFactory.getDefault().eventBus();
                        if (eventBus == null)
                            mLogger.error("eventBus is null");
                        else
                        {
                            mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                            eventBus.publish("/locks", lockChanged);
                        }
                    }
                }
                else
                {
                    if (escenicField.getMyPrivateLockURL() == null)
                    {
                        /*
                        if (externalLock.getAuthorName().equalsIgnoreCase(escenicService.getEscenicAuthorName()))
                        {
                            // if it is my lock, previously done and not removed
                            mLogger.info("The external lock of the " + escenicField.getFieldName() + " field is removed because it is my lock. publicLockURL: " + externalLock.getId());
                            escenicService.removeLock(externalLock.getId());

                            String privateLockURL = escenicService.lockResource(lockCollectionURL, escenicField.getFieldName());
                            mLogger.info("The " + escenicField.getFieldName() + " field was locked. privateLockURL: " + privateLockURL);
                            escenicField.setMyPrivateLockURL(privateLockURL);

                            escenicField.setExternalLock(null);
                        }
                        else
                        */
                        {
                            if (escenicField.getExternalLock() != null &&
                                escenicField.getExternalLock().getUserName().equalsIgnoreCase(externalLock.getUserName()) &&
                                escenicField.getExternalLock().getFragment().equalsIgnoreCase(externalLock.getFragment()))
                            {
                                // lock not changed
                            }
                            else
                            {
                                escenicField.setExternalLock(externalLock);

                                LockChanged lockChanged = new LockChanged();
                                lockChanged.setId(articleId);
                                lockChanged.setKeyField(keyField);

                                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                                if (eventBus == null)
                                    mLogger.error("eventBus is null");
                                else
                                {
                                    mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                                    eventBus.publish("/locks", lockChanged);
                                }
                            }

                            mLogger.info("Article id: " + articleId + ", UserName: " + externalLock.getUserName() + ", " + externalLock.getContent());
                        }
                    }
                    else
                    {
                        if (externalLock.getUserName().equalsIgnoreCase(escenicService.getUserName()))
                        {
                            // if it is my lock

                            if (escenicField.getExternalLock() != null)
                            {
                                escenicField.setExternalLock(null);

                                LockChanged lockChanged = new LockChanged();
                                lockChanged.setId(articleId);
                                lockChanged.setKeyField(keyField);

                                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                                if (eventBus == null)
                                    mLogger.error("eventBus is null");
                                else
                                {
                                    mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                                    eventBus.publish("/locks", lockChanged);
                                }
                            }
                        }
                        else
                        {
                            mLogger.error("It should never happen. There is the fragment '" + escenicField.getFieldName() + "' locked by myself and loccked by " + externalLock.getUserName());
                        }
                    }
                }
            }

            for (String keyField: linksFields.keySet())
            {
                EscenicField escenicField = linksFields.get(keyField);

                /*
                if (keyField.startsWith("related"))
                    mLogger.error("keyField: " + keyField + ", escenicField: " + escenicField);
                */

                if (escenicField == null)
                    continue;

                if (!escenicField.getFieldName().startsWith("related-"))
                    continue;

                // fieldName is like 'related_lead'
                String fieldNameToLock = escenicField.getFieldName().substring(8);

                // mLogger.error("fieldNameToLock: " + fieldNameToLock);
                EscenicLock externalLock = externalLockHashMap.get(fieldNameToLock);
                // mLogger.error("externalLock: " + externalLock);

                if (externalLock == null)
                {
                    if (escenicField.getExternalLock() != null)
                    {
                        escenicField.setExternalLock(null);

                        LockChanged lockChanged = new LockChanged();
                        lockChanged.setId(articleId);
                        lockChanged.setKeyField(keyField);

                        EventBus eventBus = EventBusFactory.getDefault().eventBus();
                        if (eventBus == null)
                            mLogger.error("eventBus is null");
                        else
                        {
                            mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                            eventBus.publish("/locks", lockChanged);
                        }
                    }
                }
                else
                {
                    if (escenicField.getMyPrivateLockURL() == null)
                    {
                        /*
                        if (externalLock.getAuthorName().equalsIgnoreCase(escenicService.getEscenicAuthorName()))
                        {
                            // if it is my lock, previously done and not removed
                            mLogger.info("The external lock of the " + escenicField.getFieldName() + " field is removed because it is my lock. publicLockURL: " + externalLock.getId());
                            escenicService.removeLock(externalLock.getId());

                            String privateLockURL = escenicService.lockResource(lockCollectionURL, escenicField.getFieldName());
                            mLogger.info("The " + escenicField.getFieldName() + " field was locked. privateLockURL: " + privateLockURL);
                            escenicField.setMyPrivateLockURL(privateLockURL);

                            escenicField.setExternalLock(null);
                        }
                        else
                        */
                        {
                            if (escenicField.getExternalLock() != null &&
                                    escenicField.getExternalLock().getUserName().equalsIgnoreCase(externalLock.getUserName()) &&
                                    escenicField.getExternalLock().getFragment().equalsIgnoreCase(externalLock.getFragment()))
                            {
                                // lock not changed
                            }
                            else
                            {
                                escenicField.setExternalLock(externalLock);

                                LockChanged lockChanged = new LockChanged();
                                lockChanged.setId(articleId);
                                lockChanged.setKeyField(keyField);

                                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                                if (eventBus == null)
                                    mLogger.error("eventBus is null");
                                else
                                {
                                    mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                                    eventBus.publish("/locks", lockChanged);
                                }
                            }

                            mLogger.info("Article id: " + articleId + ", UserName: " + externalLock.getUserName() + ", " + externalLock.getContent());
                        }
                    }
                    else
                    {
                        if (externalLock.getUserName().equalsIgnoreCase(escenicService.getUserName()))
                        {
                            // if it is my lock

                            if (escenicField.getExternalLock() != null)
                            {
                                escenicField.setExternalLock(null);

                                LockChanged lockChanged = new LockChanged();
                                lockChanged.setId(articleId);
                                lockChanged.setKeyField(keyField);

                                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                                if (eventBus == null)
                                    mLogger.error("eventBus is null");
                                else
                                {
                                    mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                                    eventBus.publish("/locks", lockChanged);
                                }
                            }
                        }
                        else
                        {
                            mLogger.error("It should never happen. There is the fragment '" + fieldNameToLock + "' locked by myself and locked by " + externalLock.getUserName());
                        }
                    }
                }
            }

            // locks for the sections
            {
                EscenicLock localSectionsExternalLock = externalLockHashMap.get(sectionsLockName);

                if (localSectionsExternalLock == null)
                {
                    if (sectionsExternalLock != null)
                    {
                        sectionsExternalLock = null;

                        LockChanged lockChanged = new LockChanged();
                        lockChanged.setId(articleId);
                        lockChanged.setKeyField(sectionsLockName);

                        EventBus eventBus = EventBusFactory.getDefault().eventBus();
                        {
                            mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                            eventBus.publish("/locks", lockChanged);
                        }
                    }
                }
                else
                {
                    if (myPrivateSectionsLockURL == null)
                    {
                        /*
                        if (sectionsExternalLock.getAuthorName().equalsIgnoreCase(escenicService.getEscenicAuthorName()))
                        {
                            // if it is my lock, previously done and not removed
                            mLogger.info("The external lock of the " + sectionsLockName + " field is removed because it is my lock. publicLockURL: " + sectionsExternalLock.getId());
                            escenicService.removeLock(sectionsExternalLock.getId());

                            myPrivateSectionsLockURL = escenicService.lockResource(lockCollectionURL, sectionsLockName);
                            mLogger.info("The " + sectionsLockName + " field was locked. myPrivateSectionsLockURL: " + myPrivateSectionsLockURL);

                            sectionsExternalLock = null;
                        }
                        else
                        */
                        {
                            if (sectionsExternalLock != null &&
                                    sectionsExternalLock.getUserName().equalsIgnoreCase(localSectionsExternalLock.getUserName()) &&
                                    sectionsExternalLock.getFragment().equalsIgnoreCase(localSectionsExternalLock.getFragment()))
                            {
                                // lock not changed
                            }
                            else
                            {
                                sectionsExternalLock = localSectionsExternalLock;

                                LockChanged lockChanged = new LockChanged();
                                lockChanged.setId(articleId);
                                lockChanged.setKeyField(sectionsLockName);

                                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                                {
                                    mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                                    eventBus.publish("/locks", lockChanged);
                                }
                            }

                            mLogger.info("Article id: " + articleId + ", AuthorName: " + sectionsExternalLock.getUserName() + ", " + sectionsExternalLock.getContent());
                        }
                    }
                    else
                    {
                        if (localSectionsExternalLock.getUserName().equalsIgnoreCase(escenicService.getUserName()))
                        {
                            // if it is my lock

                            if (sectionsExternalLock != null)
                            {
                                sectionsExternalLock = null;

                                LockChanged lockChanged = new LockChanged();
                                lockChanged.setId(articleId);
                                lockChanged.setKeyField(sectionsLockName);

                                EventBus eventBus = EventBusFactory.getDefault().eventBus();
                                {
                                    mLogger.info("Send /locks on the EventBus. " + lockChanged.toString());
                                    eventBus.publish("/locks", lockChanged);
                                }
                            }
                        }
                        else
                        {
                            mLogger.error("It should never happen. There is the fragment '" + sectionsLockName + "' locked by myself and loccked by " + sectionsExternalLock.getUserName());
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void stealLock(EscenicField escenicField)
    {
        mLogger.error("stealLock. FieldName: " + escenicField.getFieldName());

        if (!(escenicField.getExternalLock() != null && escenicField.getMyPrivateLockURL() == null))
        {
            mLogger.error("FieldName: " + escenicField.getFieldName() +
                ". I cannot steal the lock in this scenario. externalLock: " + escenicField.getExternalLock() +
                ", MyPrivateLockURL: " + escenicField.getMyPrivateLockURL());

            return;
        }

        String fieldNameToLock;
        if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTLINKVALUE)
        {
            if (!escenicField.getFieldName().startsWith("related"))
            {
                mLogger.error("stealLock. Field name does not start with 'related': " + escenicField.getFieldName());

                return;
            }

            // fieldName is like 'related_lead'
            fieldNameToLock = escenicField.getFieldName().substring(8);
        }
        else
        {
            fieldNameToLock = escenicField.getFieldName();
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

        mLogger.info("FieldName: " + fieldNameToLock + ". Remove of the external lock:" + escenicField.getExternalLock().getId());
        escenicService.removeLock(escenicField.getExternalLock().getId());
        escenicField.setExternalLock(null);

        mLogger.info("FieldName: " + fieldNameToLock + ". Lock the field");
        String privateLockURL = escenicService.lockResource(lockCollectionURL, fieldNameToLock);
        if (privateLockURL != null)
        {
            mLogger.info("The " + fieldNameToLock + " field was locked. privateLockURL: " + privateLockURL);

            escenicField.setMyPrivateLockURL(privateLockURL);
        }
        else
        {
            mLogger.error("Lock failed for the " + fieldNameToLock + " field");

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Lock failed for the " + fieldNameToLock + " field");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            context.addMessage(null, message);

            return;
        }
    }

    public void stealSectionsLock()
    {
        mLogger.error("stealSectionsLock");

        if (!(sectionsExternalLock != null && myPrivateSectionsLockURL == null))
        {
            mLogger.error("Sections. I cannot steal the lock in this scenario. externalLock: " + sectionsExternalLock +
                    ", MyPrivateSectionsLockURL: " + myPrivateSectionsLockURL);

            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

        mLogger.info("Sections. Remove of the external lock:" + sectionsExternalLock.getId());
        escenicService.removeLock(sectionsExternalLock.getId());
        sectionsExternalLock = null;

        mLogger.info("Sections. Lock the field");
        myPrivateSectionsLockURL = escenicService.lockResource(lockCollectionURL, sectionsLockName);
        if (myPrivateSectionsLockURL != null)
        {
            mLogger.info("The " + sectionsLockName + " field was locked. privateLockURL: " + myPrivateSectionsLockURL);
        }
        else
        {
            mLogger.error("Lock failed for the " + sectionsLockName + " field");

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Lock failed for the " + sectionsLockName + " field");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            context.addMessage(null, message);

            return;
        }
    }

    public void valueChangeListener(EscenicField changedEscenicField, EscenicField initialEscenicFieldOfAComplex)
    {
        EscenicField escenicFieldToLock;

        // mLogger.error("valueChangeListener. changedEscenicField: " + changedEscenicField +
        //    ", initialEscenicFieldOfAComplex: " + initialEscenicFieldOfAComplex);
        /*
        if (escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTCOMPLEXVALUE)
        {
            ComplexValue changedComplexValue = null;

            if (escenicField.getListComplexValue() == null || escenicField.getListComplexValue().size() == 0)
            {
                mLogger.error("It should never happen a complex with no element in his list");

                return;
            }

            for (int complexValueIndex = 0; complexValueIndex < escenicField.getListComplexValue().size() && changedComplexValue == null;
                complexValueIndex++)
            {
                ComplexValue complexValue = escenicField.getListComplexValue().get(complexValueIndex);
                for (String keyField: complexValue.getPayloadFields().keySet())
                {
                    if (complexValue.getPayloadFields().get(keyField) == escenicField)
                    {
                        changedComplexValue = complexValue;

                        break;
                    }
                }
            }

            if (changedComplexValue == null)
            {
                mLogger.error("It should never happen, the EscenicField was not found inside the list of ComplexValues");

                return;
            }

            mLogger.info("Received valueChangeListener. escenicField.getFieldName: " + escenicField.getFieldName() +
                ", Initial Complex Field Name: " + changedComplexValue.getInitialEscenicField().getFieldName());

            escenicFieldToLock = changedComplexValue.getInitialEscenicField();
        }
        else
        {
            mLogger.info("Received valueChangeListener. escenicField.getFieldName: " + escenicField.getFieldName());

            escenicFieldToLock = escenicField;
        }
        */

        if (initialEscenicFieldOfAComplex == null)
        {
            mLogger.info("Received valueChangeListener. escenicField.getFieldName: " + changedEscenicField.getFieldName());

            escenicFieldToLock = changedEscenicField;
        }
        else
        {
            mLogger.info("Received valueChangeListener. escenicField.getFieldName: " + changedEscenicField.getFieldName() +
                    ", Initial Complex Field Name: " + initialEscenicFieldOfAComplex.getFieldName());

            escenicFieldToLock = initialEscenicFieldOfAComplex;
        }

        if (escenicFieldToLock.getMyPrivateLockURL() != null)
        {
            mLogger.error("Field '" + escenicFieldToLock.getFieldName() + "' is already locked by myself");

            return;
        }

        if (escenicFieldToLock.getExternalLock() != null)
        {
            mLogger.error("Field " + escenicFieldToLock.getFieldName() + " is already locked by " + escenicFieldToLock.getExternalLock().getUserName());

            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

        String privateLockURL = escenicService.lockResource(lockCollectionURL, escenicFieldToLock.getFieldName());
        if (privateLockURL != null)
        {
            mLogger.info("The " + escenicFieldToLock.getFieldName() + " field was locked. privateLockURL: " + privateLockURL);

            escenicFieldToLock.setMyPrivateLockURL(privateLockURL);
        }
        else
        {
            mLogger.error("Lock failed for the " + escenicFieldToLock.getFieldName() + " field");

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Lock failed for the " + escenicFieldToLock.getFieldName() + " field");
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            context.addMessage(null, message);

            return;
        }
    }

    public void removeComplexFieldRow(EscenicField escenicField, int rowIdToBeRemoved)
    {
        if (escenicField == null ||
                escenicField.getValueType() != EscenicField.ValueType.ESCENIC_LISTCOMPLEXVALUE ||
                escenicField.getListComplexValue() == null ||
                rowIdToBeRemoved >= escenicField.getListComplexValue().size())
        {
            mLogger.error("Wrong input");

            return;
        }

        escenicField.getListComplexValue().remove(rowIdToBeRemoved);

        mLogger.info("Complex field (row) was removed. Field name: " + escenicField.getFieldName() + ", Row index " + rowIdToBeRemoved);
    }

    public void addEmptyComplexFieldRow(EscenicField complexEscenicField)
    {
        if (complexEscenicField == null ||
                complexEscenicField.getValueType() != EscenicField.ValueType.ESCENIC_LISTCOMPLEXVALUE)
        {
            mLogger.error("Wrong input");

            return;
        }

        ComplexValue emptyComplexValue = EscenicField.createEmptyComplexValue(complexEscenicField, complexEscenicField.getEscenicType().getNestedType());

        List<ComplexValue> listComplexValue = complexEscenicField.getListComplexValue();

        if (listComplexValue == null)
        {
            listComplexValue = new ArrayList<>();
        }

        listComplexValue.add(emptyComplexValue);

        complexEscenicField.setListComplexValue(listComplexValue);

        mLogger.info("Empty complex field (row) was added");
    }

    public void removeSection(String id)
    {
        mLogger.info("removeSection. id: " + id);

        {
            if (sectionsExternalLock != null)
            {
                mLogger.error("Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Content", "Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());
                FacesContext context = FacesContext.getCurrentInstance();
                if (context != null)
                    context.addMessage(null, message);

                return;
            }

            if (myPrivateSectionsLockURL == null)
            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                myPrivateSectionsLockURL = escenicService.lockResource(lockCollectionURL, sectionsLockName);
                if (myPrivateSectionsLockURL != null)
                {
                    mLogger.info("The " + sectionsLockName + " field was locked. myPrivateSectionsLockURL: " + myPrivateSectionsLockURL);
                }
                else
                {
                    mLogger.error("Lock failed for the " + sectionsLockName + " field");

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Content", "Lock failed for the " + sectionsLockName + " field");
                    // RequestContext.getCurrentInstance().showMessageInDialog(message);
                    context.addMessage(null, message);

                    return;
                }
            }
        }

        boolean sectionFound = false;

        if (sections != null)
        {
            for (EscenicSection escenicSection : sections)
            {
                if (sectionFound)
                    break;

                if (escenicSection.getId().equalsIgnoreCase(id))
                {
                    // escenicSection.getEscenicField will return the link field
                    if (escenicSection.getEscenicField() != null)
                    {
                        for (EscenicLink escenicLink : escenicSection.getEscenicField().getListLinkValues())
                        {
                            if (escenicLink.getHref().equalsIgnoreCase(escenicSection.getHref()))
                            {
                                escenicSection.getEscenicField().setChangeType(EscenicField.ChangeType.ESCENIC_MODIFIED);
                                escenicLink.setChangeType(EscenicLink.ChangeType.ESCENIC_REMOVED);

                                sectionFound = true;

                                break;
                            }
                        }
                    }
                    else
                    {
                        // it is a remove of a section just added

                        sectionFound = true;

                        sections.remove(escenicSection);

                        break;
                    }
                }
            }
        }

        if (!sectionFound)
        {
            mLogger.error("Section id " + id + " was not found");

            return;
        }

        mLogger.info("Section id " + id + " was removed");
    }

    public void setAsHomeSection(String id)
    {
        mLogger.info("setAsHomeSection. id: " + id);

        EscenicSection escenicSectionToBeSetAsHome = null;
        EscenicLink escenicLinkRelatedToSectionToBeSetAsHome = null;
        EscenicSection escenicHomeSection = null;
        EscenicLink escenicLinkRelatedToHomeSection = null;

        {
            if (sectionsExternalLock != null)
            {
                mLogger.error("Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Content", "Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());
                FacesContext context = FacesContext.getCurrentInstance();
                if (context != null)
                    context.addMessage(null, message);

                return;
            }

            if (myPrivateSectionsLockURL == null)
            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                myPrivateSectionsLockURL = escenicService.lockResource(lockCollectionURL, sectionsLockName);
                if (myPrivateSectionsLockURL != null)
                {
                    mLogger.info("The " + sectionsLockName + " field was locked. myPrivateSectionsLockURL: " + myPrivateSectionsLockURL);
                }
                else
                {
                    mLogger.error("Lock failed for the " + sectionsLockName + " field");

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Content", "Lock failed for the " + sectionsLockName + " field");
                    // RequestContext.getCurrentInstance().showMessageInDialog(message);
                    context.addMessage(null, message);

                    return;
                }
            }
        }

        if (sections != null)
        {
            for (EscenicSection escenicSection : sections)
            {
                if (escenicSectionToBeSetAsHome != null &&
                    escenicHomeSection != null)
                    break;

                if (escenicSection.getId().equalsIgnoreCase(id) && !escenicSection.getHomeSection())
                {
                    escenicSectionToBeSetAsHome = escenicSection;

                    if (escenicSection.getEscenicField() != null)
                    {
                        boolean escenicLinkFound = false;

                        for (EscenicLink escenicLink : escenicSection.getEscenicField().getListLinkValues())
                        {
                            if (escenicLink.getHref().equalsIgnoreCase(escenicSection.getHref()))
                            {
                                escenicLinkRelatedToSectionToBeSetAsHome = escenicLink;

                                escenicLinkFound = true;

                                break;
                            }
                        }

                        if (!escenicLinkFound)
                        {
                            mLogger.error("EscenicLink was not found");

                            return;
                        }
                    }
                    else
                    {
                        // section just added

                        escenicLinkRelatedToSectionToBeSetAsHome = null;
                    }
                }
                else if (escenicSection.getHomeSection())
                {
                    escenicHomeSection = escenicSection;

                    if (escenicSection.getEscenicField() != null)
                    {
                        boolean escenicLinkFound = false;

                        for (EscenicLink escenicLink : escenicSection.getEscenicField().getListLinkValues())
                        {
                            if (escenicLink.getHref().equalsIgnoreCase(escenicSection.getHref()))
                            {
                                escenicLinkRelatedToHomeSection = escenicLink;

                                escenicLinkFound = true;

                                break;
                            }
                        }

                        if (!escenicLinkFound)
                        {
                            mLogger.error("EscenicLink was not found");

                            return;
                        }
                    }
                    else
                    {
                        escenicLinkRelatedToHomeSection = null;
                    }
                }
            }
        }

        if (!(escenicSectionToBeSetAsHome != null &&
            escenicHomeSection != null))
        {
            mLogger.error("Section not found. id " + id +
                    ", escenicSectionToBeSetAsHome: " + escenicSectionToBeSetAsHome +
                    ", escenicLinkRelatedToSectionToBeSetAsHome: " + escenicLinkRelatedToSectionToBeSetAsHome +
                    ", escenicHomeSection: " + escenicHomeSection +
                    ", escenicLinkRelatedToHomeSection: " + escenicLinkRelatedToHomeSection
            );

            return;
        }

        escenicSectionToBeSetAsHome.setHomeSection(true);
        if (escenicSectionToBeSetAsHome.getEscenicField() != null)
        {
            escenicSectionToBeSetAsHome.getEscenicField().setChangeType(EscenicField.ChangeType.ESCENIC_MODIFIED);
            escenicLinkRelatedToSectionToBeSetAsHome.setChangeType(EscenicLink.ChangeType.ESCENIC_MODIFIED);
        }

        escenicHomeSection.setHomeSection(false);
        if (escenicHomeSection.getEscenicField() != null)
        {
            escenicHomeSection.getEscenicField().setChangeType(EscenicField.ChangeType.ESCENIC_MODIFIED);
            escenicLinkRelatedToHomeSection.setChangeType(EscenicLink.ChangeType.ESCENIC_MODIFIED);
        }

        mLogger.info("Section id " + id + " was set as home");
    }

    public EscenicSection getHomeEscenicSection()
    {
        EscenicSection homeEscenicSection = null;

        for (EscenicSection escenicSection : getSections())
        {
            if (escenicSection.getHomeSection())
            {
                homeEscenicSection = escenicSection;

                break;
            }
        }

        // mLogger.error("getHomeEscenicSection: " + homeEscenicSection);

        return homeEscenicSection;
    }

    public void sectionTreeDropOnSectionsTable()
    {
        try {
            Map<String,String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
            String sectionId = params.get("sectionId");

            mLogger.info("sectionTreeDropOnSectionsTable. sectionId: " + sectionId);

            if (sectionId == null || sectionId.equalsIgnoreCase(""))
            {
                mLogger.error("Wrong sectionId: " + sectionId);

                return;
            }

            {
                if (sectionsExternalLock != null)
                {
                    mLogger.error("Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Content", "Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());
                    FacesContext context = FacesContext.getCurrentInstance();
                    if (context != null)
                        context.addMessage(null, message);

                    return;
                }

                if (myPrivateSectionsLockURL == null)
                {
                    FacesContext context = FacesContext.getCurrentInstance();
                    HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                    EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                    myPrivateSectionsLockURL = escenicService.lockResource(lockCollectionURL, sectionsLockName);
                    if (myPrivateSectionsLockURL != null)
                    {
                        mLogger.info("The " + sectionsLockName + " field was locked. myPrivateSectionsLockURL: " + myPrivateSectionsLockURL);
                    }
                    else
                    {
                        mLogger.error("Lock failed for the " + sectionsLockName + " field");

                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Content", "Lock failed for the " + sectionsLockName + " field");
                        // RequestContext.getCurrentInstance().showMessageInDialog(message);
                        context.addMessage(null, message);

                        return;
                    }
                }
            }

            ArticleBrowserBacking articleBrowserBacking;

            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                ValueExpression valueExpression = context.getApplication().getExpressionFactory().createValueExpression(
                        context.getELContext(), "#{articleBrowserBacking}", ArticleBrowserBacking.class);
                articleBrowserBacking = (ArticleBrowserBacking) valueExpression.getValue(context.getELContext());
            }

            EscenicSection escenicSection = articleBrowserBacking.getEscenicSectionById(sectionId);

            if (escenicSection != null)
                sections.add(escenicSection);
            else
                mLogger.error("escenicSection: " + escenicSection);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public List<EscenicSection> getSections() {

        List<EscenicSection> escenicSectionList;

        if (sections == null)
        {
            sections = new ArrayList<>();

            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            String homeSectionKey = "http://www.vizrt.com/types/relation/home-section" + "-";
            EscenicField homeEscenicField = publicationLinksFields.get(homeSectionKey);
            if (homeEscenicField != null &&
                    homeEscenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTLINKVALUE &&
                    homeEscenicField.getListLinkValues().size() > 0 &&
                    homeEscenicField.getListLinkValues().get(0).getHref() != null &&
                    !homeEscenicField.getListLinkValues().get(0).getHref().equalsIgnoreCase(""))
            {
                EscenicSection homeSection = escenicService.getSection(homeEscenicField.getListLinkValues().get(0).getHref(), true);

                if (homeSection != null)
                {
                    homeSection.setHref(homeEscenicField.getListLinkValues().get(0).getHref());
                    homeSection.setEscenicField(homeEscenicField);
                    homeSection.setHomeSection(true);
                    sections.add(homeSection);
                }
            }

            String sectionKey = "http://www.vizrt.com/types/relation/section" + "-";
            EscenicField sectionEscenicField = publicationLinksFields.get(sectionKey);
            if (sectionEscenicField != null &&
                    sectionEscenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTLINKVALUE &&
                    sectionEscenicField.getListLinkValues().size() > 0)
            {
                for (EscenicLink escenicLink: sectionEscenicField.getListLinkValues())
                {
                    if (escenicLink.getChangeType() != EscenicLink.ChangeType.ESCENIC_REMOVED &&
                            escenicLink.getHref() != null && !escenicLink.getHref().equalsIgnoreCase(""))
                    {
                        EscenicSection section = escenicService.getSection(escenicLink.getHref(), true);

                        if (section != null)
                        {
                            section.setHref(escenicLink.getHref());
                            section.setEscenicField(sectionEscenicField);
                            section.setHomeSection(false);
                            sections.add(section);
                        }
                    }
                }
            }

            escenicSectionList = sections;
        }
        else
        {
            escenicSectionList = new ArrayList<>();

            for (EscenicSection escenicSection: sections)
            {
                if (escenicSection.getEscenicField() != null)
                {
                    for (EscenicLink escenicLink : escenicSection.getEscenicField().getListLinkValues())
                    {
                        if (escenicLink.getHref().equalsIgnoreCase(escenicSection.getHref()))
                        {
                            if (escenicLink.getChangeType() != EscenicLink.ChangeType.ESCENIC_REMOVED)
                            {
                                escenicSectionList.add(escenicSection);

                                break;
                            }
                        }
                    }
                }
                else
                {
                    escenicSectionList.add(escenicSection);
                }
            }
        }

        return escenicSectionList;
    }

    /*
    public void addSelectedSection()
    {
        mLogger.info("addSelectedSection.");

        {
            if (sectionsExternalLock != null)
            {
                mLogger.error("Field " + sectionsLockName + " is already locked by " + sectionsExternalLock.getUserName());

                return;
            }

            if (myPrivateSectionsLockURL == null)
            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                myPrivateSectionsLockURL = escenicService.lockResource(lockCollectionURL, sectionsLockName);

                mLogger.info("The " + sectionsLockName + " field was locked. myPrivateSectionsLockURL: " + myPrivateSectionsLockURL);
            }
        }

        ArticleBrowserBacking articleBrowserBacking;

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            ValueExpression valueExpression = context.getApplication().getExpressionFactory().createValueExpression(
                    context.getELContext(), "#{articleBrowserBacking}", ArticleBrowserBacking.class);
            articleBrowserBacking = (ArticleBrowserBacking) valueExpression.getValue(context.getELContext());
        }

        TreeNode [] sectionsTreeNodes = articleBrowserBacking.getMultipleSelectedTreeNode();

        if (sectionsTreeNodes == null || sectionsTreeNodes.length != 1)
        {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Section", "Wrong selected sections: " + (sectionsTreeNodes == null ? 0 : sectionsTreeNodes.length));
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            mLogger.error("Wrong selected sections: " + (sectionsTreeNodes == null ? 0 : sectionsTreeNodes.length));

            return;
        }

        EscenicSection escenicSection = (EscenicSection) ((sectionsTreeNodes[0]).getData());

        if (escenicSection != null)
            sections.add(escenicSection);
        else
            mLogger.error("escenicSection: " + escenicSection);
    }
    */

    public void articleDropOnRelated(DragDropEvent ddEvent)
    {
        ArticleTableData articleTableData = ((ArticleTableData) ddEvent.getData());

        String linkKey = ddEvent.getComponent().getAttributes().get("linksKey").toString();

        if (linkKey == null || linkKey.length() < 8)
        {
            mLogger.error("articleDropOnRelated. Wrong linkKey: " + linkKey);

            return;
        }

        // fieldName is like 'related_lead'
        String fieldNameToLock = linkKey.substring(8);

        mLogger.info("articleDropOnRelated. Dropped article id " + articleTableData.getObjectId() + " in " + linkKey);

        EscenicField escenicField = linksFields.get(linkKey);

        if (escenicField == null)
        {
            mLogger.error("articleDropOnRelated. escenicField is null");

            return;
        }

        if (escenicField.getExternalLock() != null)
        {
            mLogger.error("Field " + fieldNameToLock + " is already locked by " + escenicField.getExternalLock().getUserName());

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Content", "Field " + fieldNameToLock + " is already locked by " + escenicField.getExternalLock().getUserName());
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null)
                context.addMessage(null, message);

            return;
        }

        if (escenicField.getListLinkValues() != null)
        {
            boolean linkAlreadyPresent = false;

            for (EscenicLink escenicLink : escenicField.getListLinkValues())
            {
                String linkURL = escenicLink.getHref();

                if (linkURL == null)
                {
                    mLogger.error("linkURL is null");

                    continue;
                }

                Long articleId = null;
                {
                    int begingOfId = linkURL.lastIndexOf('/');
                    if (begingOfId != -1)
                        articleId = Long.parseLong(linkURL.substring(begingOfId + 1));
                }

                if (articleId == null)
                {
                    mLogger.error("articleId is null");

                    continue;
                }

                if (articleId == articleTableData.getObjectId())
                {
                    linkAlreadyPresent = true;

                    break;
                }
            }

            if (linkAlreadyPresent)
            {
                mLogger.info("Article (" + articleTableData.getObjectId() + ") is already present among " + linkKey);

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Content", "Content (" + articleTableData.getObjectId() + ") is already present among " + linkKey);
                FacesContext context = FacesContext.getCurrentInstance();
                if (context != null)
                    context.addMessage(null, message);

                return;
            }
        }

        EscenicService escenicService;
        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            escenicService = (EscenicService) session.getAttribute("escenicService");
        }

        // create the EscenicLink
        EscenicLink escenicLink = new EscenicLink();
        {
            String href = "http://" + escenicService.getEscenicWebServicesHost() + ":" +
                escenicService.getEscenicWebServicesPort() + "/webservice/escenic/content/" +
                articleTableData.getObjectId();
            String model = "http://" + escenicService.getEscenicWebServicesHost() + ":" +
                escenicService.getEscenicWebServicesPort() + "/webservice/publication/" +
                articleTableData.getPublication() + "/escenic/model/" + articleTableData.getContentType();

            escenicLink.setRel("related");
            escenicLink.setGroup(fieldNameToLock);
            escenicLink.setHref(href);
            escenicLink.setType("application/atom+xml; type=entry");
            escenicLink.setModel(model);
            escenicLink.setTitle(articleTableData.getTitle());

            boolean cacheToBeUsed = true;
            escenicLink.setThumbnailImageInfo(escenicService.getImageInfo(href, cacheToBeUsed));
        }

        if (escenicField.getListLinkValues() == null)
        {
            List<EscenicLink> listLinkValues = new ArrayList<>();

            escenicField.setListLinkValues(listLinkValues);
        }

        if (escenicField.getMyPrivateLockURL() == null)
        {
            String privateLockURL = escenicService.lockResource(lockCollectionURL, fieldNameToLock);
            if (privateLockURL != null)
            {
                mLogger.info("The " + fieldNameToLock + " field was locked. privateLockURL: " + privateLockURL);

                escenicField.setMyPrivateLockURL(privateLockURL);
            }
            else
            {
                mLogger.error("Lock failed for the " + fieldNameToLock + " field");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Lock failed for the " + fieldNameToLock + " field");
                FacesContext context = FacesContext.getCurrentInstance();
                if (context != null)
                    context.addMessage(null, message);

                return;
            }
        }

        escenicLink.setChangeType(EscenicLink.ChangeType.ESCENIC_ADDED);
        escenicField.getListLinkValues().add(escenicLink);
        escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_MODIFIED);
    }

    public void removeRelatedLink(String linkKey, int rowId)
    {
        EscenicField escenicField = linksFields.get(linkKey);

        if (escenicField == null || escenicField.getListLinkValues() == null)
        {
            mLogger.error("No escenicField available. escenicField: " + escenicField);

            return;
        }

        if (escenicField.getListLinkValues().size() < rowId)
        {
            mLogger.error("No link available. rowId: " + rowId + ", escenicField.getListLinkValues().size(): " + escenicField.getListLinkValues().size());

            return;
        }


        {
            EscenicService escenicService;
            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                escenicService = (EscenicService) session.getAttribute("escenicService");
            }

            if (escenicField.getExternalLock() != null)
            {
                mLogger.error("Field " + escenicField.getFieldName() + " is already locked by " + escenicField.getExternalLock().getUserName());

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Content", "Field " + escenicField.getFieldName() + " is already locked by " + escenicField.getExternalLock().getUserName());
                FacesContext context = FacesContext.getCurrentInstance();
                if (context != null)
                    context.addMessage(null, message);

                return;
            }

            if (escenicField.getMyPrivateLockURL() == null)
            {
                // fieldName is like 'related_lead'
                String fieldNameToLock = escenicField.getFieldName().substring(8);
                String privateLockURL = escenicService.lockResource(lockCollectionURL, fieldNameToLock);
                if (privateLockURL != null)
                {
                    mLogger.info("The " + fieldNameToLock + " field was locked. privateLockURL: " + privateLockURL);

                    escenicField.setMyPrivateLockURL(privateLockURL);
                }
                else
                {
                    mLogger.error("Lock failed for the " + fieldNameToLock + " field");

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Content", "Lock failed for the " + fieldNameToLock + " field");
                    FacesContext context = FacesContext.getCurrentInstance();
                    if (context != null)
                        context.addMessage(null, message);

                    return;
                }
            }
        }

        EscenicLink escenicLink = escenicField.getListLinkValues().get(rowId);
        escenicLink.setChangeType(EscenicLink.ChangeType.ESCENIC_REMOVED);

        escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_MODIFIED);

        mLogger.info("Link index " + rowId + " of " + linkKey + " was removed");
    }

    public String getSelectedRelatedLinkKey() {
        return selectedRelatedLinkKey;
    }

    public void setSelectedRelatedLinkKey(String selectedRelatedLinkKey) {
        this.selectedRelatedLinkKey = selectedRelatedLinkKey;
    }

    public List<String> getRelatedLinksKeys()
    {
        List<String> relatedLinksKeys = new ArrayList<>();

        for (String keyField: linksFields.keySet())
        {
            if (!keyField.startsWith("related-"))
                continue;

            relatedLinksKeys.add(keyField);

            if (selectedRelatedLinkKey == null)
                selectedRelatedLinkKey = keyField;

            if (linksFields.get(keyField) != null)
            {
                for (EscenicLink escenicLink : linksFields.get(keyField).getListLinkValues())
                {
                    if (escenicLink.getChangeType() == EscenicLink.ChangeType.ESCENIC_REMOVED)
                        continue;

                    if (escenicLink.getHref() != null && !escenicLink.getHref().equalsIgnoreCase("") &&
                            (escenicLink.getState() == null || escenicLink.getState().equalsIgnoreCase("") ||
                                    escenicLink.getTitle() == null || escenicLink.getTitle().equalsIgnoreCase("") ||
                                    escenicLink.getThumbnailImageInfo() == null))
                    {
                        FacesContext context = FacesContext.getCurrentInstance();
                        HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                        EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                        escenicService.completeEscenicLinkInformation(escenicLink);
                    }

                    if (escenicLink.getThumbnailImageInfo() != null &&
                        escenicLink.getThumbnailImageInfo().getUrl() != null &&
                        !escenicLink.getThumbnailImageInfo().getUrl().equalsIgnoreCase("") &&
                        escenicLink.getThumbnailImageInfo().getCachedPath() == null &&
                        escenicLink.getThumbnailImageInfo().getId() != null)
                    {
                        escenicLink.getThumbnailImageInfo().setCachedPath(
                                getAndCachePicture(escenicLink.getThumbnailImageInfo().getUrl(),
                                        escenicLink.getThumbnailImageInfo().getId()));
                    }
                }
            }
        }

        cacheRetention();

        return relatedLinksKeys;
    }

    public String getMyPrivateSectionsLockURL() {
        return myPrivateSectionsLockURL;
    }

    public void setMyPrivateSectionsLockURL(String myPrivateSectionsLockURL) {
        this.myPrivateSectionsLockURL = myPrivateSectionsLockURL;
    }

    public MediaInfo getMediaInfo() {

        if (mediaInfo == null)
        {
            String id = getMetadataFields().get("com.escenic.displayId").getStringValue();
            mLogger.info("Loading media-entry-info... Article id: " + id);

            String linkKeyField = "http://www.vizrt.com/types/relation/media-entry-info" + "-";
            EscenicField escenicField = linksFields.get(linkKeyField);

            if (escenicField != null)
            {
                List<EscenicLink> listLinksField = escenicField.getListLinkValues();
                boolean cacheToBeUsed = true;

                if (listLinksField != null && listLinksField.size() > 0 &&
                        listLinksField.get(0).getHref() != null)
                {
                    FacesContext context = FacesContext.getCurrentInstance();
                    HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                    EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                    mediaInfo = escenicService.getMediaInfo(listLinksField.get(0).getHref(), cacheToBeUsed);
                }
            }
        }

        return mediaInfo;
    }

    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    public MediaEntryInfo getDefaultMediaEntry() {
        return defaultMediaEntry;
    }

    public void setDefaultMediaEntry(MediaEntryInfo defaultMediaEntry) {
        this.defaultMediaEntry = defaultMediaEntry;
    }

    /*
    public Collection<MediaSource> getMediaSources()
    {
        Collection<MediaSource> sources = new ArrayList<>();

        for (AudioMediaEntryInfo audioMediaEntryInfo: getAudioMediaEntriesInfo())
        {
            mLogger.info("getMediaSources. URI: " + audioMediaEntryInfo.getUri() + ", MimeType: " + audioMediaEntryInfo.getMimeType());

            sources.add(new MediaSource(audioMediaEntryInfo.getUri(), audioMediaEntryInfo.getMimeType()));
        }

        return sources;
    }

     */
    public List<HistoryLog> getHistoryLogs() {

        if (historyLogs == null)
        {
            String historyLogKey = "http://www.vizrt.com/types/relation/log" + "-";
            EscenicField escenicField = linksFields.get(historyLogKey);
            if (escenicField != null &&
                    escenicField.getValueType() == EscenicField.ValueType.ESCENIC_LISTLINKVALUE &&
                    escenicField.getListLinkValues().size() > 0 &&
                    escenicField.getListLinkValues().get(0).getHref() != null &&
                    !escenicField.getListLinkValues().get(0).getHref().equalsIgnoreCase(""))
            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                historyLogs = escenicService.getHistoryLogs(escenicField.getListLinkValues().get(0).getHref());
            }
        }

        return historyLogs;
    }

    public void setHistoryLogs(List<HistoryLog> historyLogs) {
        this.historyLogs = historyLogs;
    }

    public HashMap<String, EscenicField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(HashMap<String, EscenicField> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public List<EscenicTypeGroup> getEscenicTypeGroups() {
        return escenicTypeGroups;
    }

    public void setEscenicTypeGroups(List<EscenicTypeGroup> escenicTypeGroups) {
        this.escenicTypeGroups = escenicTypeGroups;
    }

    public String getSelectedEscenicTypeGroupLabel() {
        return selectedEscenicTypeGroupLabel;
    }

    public void setSelectedEscenicTypeGroupLabel(String selectedEscenicTypeGroupLabel) {
        this.selectedEscenicTypeGroupLabel = selectedEscenicTypeGroupLabel;
    }

    public List<String> getEscenicTypeGroupLabels() {
        List<String> escenicTypeGroupLabels = new ArrayList<>();

        for (EscenicTypeGroup escenicTypeGroup: escenicTypeGroups)
        {
            escenicTypeGroupLabels.add(escenicTypeGroup.getLabel());
        }

        return escenicTypeGroupLabels;
    }

    public List<String> getMetadataFieldsKeys(String escenicTypeGroupLabel)
    {
        List<String> metadataFieldsKeys = new ArrayList<>();
        EscenicField escenicField;

        // "Escenic Metadata" is the group label of the "COM.ESCENIC.METADATA" group name
        if (escenicTypeGroupLabel.equalsIgnoreCase("Escenic Metadata"))
        {
            String priorityKeyField = "com.escenic.displayId";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "com.escenic.createdBy";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "com.escenic.createDate";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "com.escenic.publishedDate";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "com.escenic.activateDate";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "com.escenic.expireDate";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "com.escenic.lastModifiedDate";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null)
                metadataFieldsKeys.add(priorityKeyField);

            for (String keyField: metadataFields.keySet())
            {
                if (!metadataFields.get(keyField).getEscenicType().getEscenicTypeGroup().
                    getLabel().equalsIgnoreCase("Escenic Metadata"))
                    continue;

                if (keyField.equalsIgnoreCase("com.escenic.displayId") ||
                    keyField.equalsIgnoreCase("com.escenic.createdBy") ||
                    keyField.equalsIgnoreCase("com.escenic.createDate") ||
                    keyField.equalsIgnoreCase("com.escenic.publishedDate") ||
                    keyField.equalsIgnoreCase("com.escenic.activateDate") ||
                    keyField.equalsIgnoreCase("com.escenic.expireDate") ||
                    keyField.equalsIgnoreCase("com.escenic.lastModifiedDate"))
                    continue;

                metadataFieldsKeys.add(keyField);
            }
        }
        else
        {
            String priorityKeyField = "title";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null &&
                escenicField.getEscenicType().getEscenicTypeGroup().getLabel().
                    equalsIgnoreCase(escenicTypeGroupLabel))
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "subtitle";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null &&
                escenicField.getEscenicType().getEscenicTypeGroup().getLabel().
                    equalsIgnoreCase(escenicTypeGroupLabel))
                metadataFieldsKeys.add(priorityKeyField);

            priorityKeyField = "body";
            escenicField = metadataFields.get(priorityKeyField);
            if (escenicField != null &&
                escenicField.getEscenicType().getEscenicTypeGroup().getLabel().
                    equalsIgnoreCase(escenicTypeGroupLabel))
                metadataFieldsKeys.add(priorityKeyField);

            for (String keyField: metadataFields.keySet())
            {
                escenicField = metadataFields.get(keyField);

                if (keyField.equalsIgnoreCase("title") ||
                    keyField.equalsIgnoreCase("subtitle") ||
                    keyField.equalsIgnoreCase("body"))
                    continue;

                // the binary field, if present, will be displayed in the 'Media' tab
                if (escenicField.getFieldName().equalsIgnoreCase("binary"))
                    continue;

                if (!escenicField.getEscenicType().getEscenicTypeGroup().getLabel().
                    equalsIgnoreCase(escenicTypeGroupLabel))
                    continue;

                metadataFieldsKeys.add(keyField);
            }
        }

        return metadataFieldsKeys;
    }

    public List<String> getUserMetadataFieldsKeys()
    {
        List<String> metadataFieldsKeys = new ArrayList<>();
        Configuration configuration;
        {
            EscenicService escenicService;

            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            escenicService = (EscenicService) session.getAttribute("escenicService");

            configuration = escenicService.getConfiguration();
        }

        if (configuration.getFieldsConfigurations().get(contentType) != null &&
            configuration.getFieldsConfigurations().get(contentType).size() > 0)
        {
            for (FieldConfiguration fieldConfiguration : configuration.getFieldsConfigurations().get(contentType))
            {
                EscenicField escenicField = metadataFields.get(fieldConfiguration.getKeyField());

                if (escenicField == null)
                {
                    mLogger.error("The KeyField " + fieldConfiguration.getKeyField() + " was not found");

                    continue;
                }

                escenicField.getEscenicType().setLabel(fieldConfiguration.getLabel());
                // fieldConfiguration.isMandatory() is not managed yet

                metadataFieldsKeys.add(fieldConfiguration.getKeyField());
            }
        }

        return metadataFieldsKeys;
    }

    public HashMap<String, EscenicField> getLinksFields() {
        return linksFields;
    }

    public void setLinksFields(HashMap<String, EscenicField> linksFields) {
        this.linksFields = linksFields;
    }

    public String getContentType() {
        // mLogger.info("getContentType. contentType: " + contentType.getType() + "-" + contentType.getLabel());

        return contentType;
    }

    public void setContentType(String contentType) {
        // mLogger.info("setContentType. contentType: " + contentType.getType() + "-" + contentType.getLabel());
        this.contentType = contentType;
    }

/*
    .xhtml:
    <p:fileUpload mode="advanced" fileUploadListener="#{tab.gallery.uploadedMSWordFile}"
        label="Append text from a MS WORD document (.docx)" skinSimple="true" dragDropSupport="true"
        allowTypes="/(\.|\/)(docx)$/" update="galleryBodyEditor" />

    public void uploadedMSWordFile(FileUploadEvent fileUploadEvent)
    {
        UploadedFile uploadedFile = fileUploadEvent.getFile();
        String htmlPathName;

        mLogger.info("uploadedMSWordFile. Uploaded File Name Is: " + uploadedFile.getFileName() + ", Uploaded File Size: " + uploadedFile.getSize());


        try {
            {
                EscenicService escenicService;

                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                escenicService = (EscenicService) session.getAttribute("escenicService");

                htmlPathName = "/tmp/" + escenicService.getEscenicUserName() + ".html";
            }

            // String docPathName = "/Users/multi/Downloads/briefing-guida-programmi.docx";
            // String htmlDocPathName = "/Users/multi/Downloads/briefing-guida-programmi.html";

            // https://code.google.com/p/xdocreport/wiki/XWPFConverterXHTML

            // 1) Load DOCX into XWPFDocument
            // InputStream in= new FileInputStream(new File(docPathName));
            InputStream in = uploadedFile.getInputstream();
            XWPFDocument document = new XWPFDocument(in);

// 2) Prepare XHTML options (here we set the IURIResolver to load images from a "word/media" folder)
            XHTMLOptions options = XHTMLOptions.create().URIResolver(new FileURIResolver(new File("/tmp")));

// 3) Convert XWPFDocument to XHTML
            OutputStream out = new FileOutputStream(new File(htmlPathName));
            XHTMLConverter.getInstance().convert(document, out, options);

            File htmlFile = new File(htmlPathName);
            String sGeneratedHtml = FileUtils.readFileToString(htmlFile);

            setBody(getBody() + sGeneratedHtml);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }

    }
    */


}
