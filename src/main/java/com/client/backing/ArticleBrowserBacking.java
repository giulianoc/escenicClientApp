package com.client.backing;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 23/10/15
 * Time: 16:29
 * To change this template use File | Settings | File Templates.
 */
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import com.client.backing.model.ArticleTabs.Article;
import com.client.backing.model.ArticleTabs.MimeTypes;
import com.client.backing.model.ArticleTabs.NewArticle;
import com.client.service.EscenicSection;
import com.client.service.EscenicService;
import com.client.backing.model.common.ContentType;
import com.client.backing.model.common.State;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.primefaces.event.*;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

@ManagedBean
@ViewScoped
public class ArticleBrowserBacking implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private TreeNode root;
    private TreeNode [] multipleSelectedTreeNode;

    private HashMap<String,EscenicSection> sections = new HashMap<>();

    private NewArticle newArticle = new NewArticle();

    @ManagedProperty(value="#{articleSearchBacking}")
    private ArticleSearchBacking articleSearchBacking;

    @ManagedProperty(value="#{articlesTabsBacking}")
    private ArticlesTabsBacking articlesTabsBacking;


    @PostConstruct
    public void init()
    {
        root = new DefaultTreeNode("Escenic Tree", null); // non visible

        loadSections(root);

        /*
        EscenicSectionItemData escenicSectionItemData = new EscenicSectionItemData(
            null, null, "Root", "Root", null, null, null, null, null, null, null, null, null, null);
        escenicSectionItemData.setNodeType(TreeItemData.NodeType.ROOT_NODE);

        TreeNode visibleRoot = new DefaultTreeNode(escenicSectionItemData, root);

        TreeNode dummyNode = new DefaultTreeNode("DummyNode", visibleRoot);
        */
    }

    public ArticleSearchBacking getArticleSearchBacking() {
        return articleSearchBacking;
    }

    public void setArticleSearchBacking(ArticleSearchBacking articleSearchBacking) {
        this.articleSearchBacking = articleSearchBacking;
    }

    public ArticlesTabsBacking getArticlesTabsBacking() {
        return articlesTabsBacking;
    }

    public void setArticlesTabsBacking(ArticlesTabsBacking articlesTabsBacking) {
        this.articlesTabsBacking = articlesTabsBacking;
    }

    private String getSelectedPublications ()
    {
        String selectedPublications = "<all>";

        if (multipleSelectedTreeNode == null || multipleSelectedTreeNode.length == 0)
            return selectedPublications;

        for (int index = 0; index < multipleSelectedTreeNode.length; index++)
        {
            EscenicSection escenicSection = (EscenicSection) (multipleSelectedTreeNode[index]).getData();

            if (selectedPublications.equalsIgnoreCase("<all>"))
                selectedPublications = escenicSection.getPublicationTitle();
            else
            {
                if (!selectedPublications.contains(escenicSection.getPublicationTitle()))
                    selectedPublications += (" " + escenicSection.getPublicationTitle());
            }
        }

        return selectedPublications;
    }

    private String getSelectedSectionsLabels ()
    {
        String selectedSectionsLabels = "<all>";

        if (multipleSelectedTreeNode == null || multipleSelectedTreeNode.length == 0)
            return selectedSectionsLabels;

        for (int index = 0; index < multipleSelectedTreeNode.length; index++)
        {
            EscenicSection escenicSection = (EscenicSection) (multipleSelectedTreeNode[index]).getData();

            if (selectedSectionsLabels.equalsIgnoreCase("<all>"))
                selectedSectionsLabels = escenicSection.getName();
            else
                selectedSectionsLabels += (" - " + escenicSection.getName());
        }

        return selectedSectionsLabels;
    }

    private String getSelectedSectionsIds ()
    {
        String selectedSectionsIds = "";

        if (multipleSelectedTreeNode == null || multipleSelectedTreeNode.length == 0)
            return selectedSectionsIds;

        for (int index = 0; index < multipleSelectedTreeNode.length; index++)
        {
            EscenicSection escenicSection = (EscenicSection) (multipleSelectedTreeNode[index]).getData();

            if (selectedSectionsIds.equalsIgnoreCase(""))
                selectedSectionsIds = String.valueOf(escenicSection.getId());
            else
                selectedSectionsIds += (" " + escenicSection.getId());
        }

        return selectedSectionsIds;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode root) {
        this.root = root;
    }

    public TreeNode[] getMultipleSelectedTreeNode() {
        return multipleSelectedTreeNode;
    }

    public void setMultipleSelectedTreeNode(TreeNode[] multipleSelectedTreeNode) {
        this.multipleSelectedTreeNode = multipleSelectedTreeNode;
    }

    public void onNodeSelect(NodeSelectEvent event)
    {
        EscenicSection escenicSection = (EscenicSection) event.getTreeNode().getData();

        mLogger.info("onNodeSelect. getDctermsIdentifier: " + escenicSection.getId());

        articleSearchBacking.setSelectedPublications(getSelectedPublications());
        articleSearchBacking.setSelectedSectionsLabels(getSelectedSectionsLabels());
        articleSearchBacking.setSelectedSectionsIds(getSelectedSectionsIds());

        /*
        if (currentSectionItemData.getNodeType() == TreeItemData.NodeType.ESCENIC_NODE)
        {
            articleSearchBacking.setSectionId(currentSectionItemData.getDctermsIdentifier());
            articleSearchBacking.setSearchURLTemplate(currentSectionItemData.getSearchURLTemplate());
        }
        */
    }

    public void onNodeUnSelect(NodeUnselectEvent event)
    {
        EscenicSection escenicSection = (EscenicSection) event.getTreeNode().getData();

        /*
        if (currentSectionItemData.getNodeType() == TreeItemData.NodeType.ESCENIC_NODE)
        {
            articleSearchBacking.setSectionId("");
            articleSearchBacking.setSearchURLTemplate("");
        }
        */
    }

    public void onNodeExpand(NodeExpandEvent event)
    {
        try {
            // mLogger.info("Node Data ::" + event.getTreeNode().getData() + " :: Expanded");
            TreeNode currentTreeNode = event.getTreeNode();

            loadSections(currentTreeNode);
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public void onNodeCollapse(NodeCollapseEvent event){
        // mLogger.info("Node Data ::"+event.getTreeNode().getData()+" :: Collapsed");
    }

    public NewArticle getNewArticle() {
        return newArticle;
    }

    public void setNewArticle(NewArticle newArticle) {
        this.newArticle = newArticle;
    }

    public String getNewArticleAllowBinaryContentTypes()
    {
        String fileExtension = MimeTypes.getImageFileExtensions().substring(1, MimeTypes.getImageFileExtensions().length() - 1);
        fileExtension += ("|" + MimeTypes.getVideoFileExtensions().substring(1, MimeTypes.getVideoFileExtensions().length() - 1));
        fileExtension += ("|" + MimeTypes.getAudioFileExtensions().substring(1, MimeTypes.getAudioFileExtensions().length() - 1));

        return ("/(\\.|\\/)(" + fileExtension + ")$/");
    /*
        ContentType selectedContentType = getContentType(getNewArticle().getContentType());

        if (selectedContentType == null)
        {
            mLogger.error("No ContentType found for the '" + getNewArticle().getContentType() + "' selectedContentType");

            return null;
        }

        mLogger.info("selectedContentType: " + selectedContentType.getType());

        if (selectedContentType.getType().equalsIgnoreCase("picture"))
        {
            // MimeTypes.getImageFileExtensions()
            return "/(\\.|\\/)(gif|jpe?g|png)$/";
        }
        else
            return null;
        */
    }

    public void handleFileBinaryUpload(FileUploadEvent event)
    {
        /*
        ContentType selectedContentType = getContentType(getNewArticle().getContentType());

        if (selectedContentType == null)
        {
            mLogger.error("No ContentType found for the '" + selectedContentType + "' getNewArticleContentType()");

            return;
        }

        if (!selectedContentType.getType().equalsIgnoreCase("gallery") && getNewArticle().getBinaries().size() >= 1)
        {
            mLogger.error("Only gallery could upload more than one binary");

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Content", "Only gallery could upload more than one binary");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }
        */

        mLogger.info("Uploaded binary file name: " + event.getFile().getFileName());

        // save
        File binaryFile = null;
        {
            InputStream input = null;
            OutputStream output = null;

            try {
                File fPathToUpload;

                {
                    FacesContext context = FacesContext.getCurrentInstance();
                    HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                    // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                    EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

                    String sPathToUpload = escenicService.getRsiInternalAPPPath();

                    sPathToUpload = (sPathToUpload + "/" + escenicService.getUserName() + "/uploads/");

                    fPathToUpload = new File(sPathToUpload);
                    FileUtils.forceMkdir(fPathToUpload);
                }

                String filename = FilenameUtils.getName(event.getFile().getFileName());
                String basename = FilenameUtils.getBaseName(filename) + "_";
                String extension = "." + FilenameUtils.getExtension(filename);
                binaryFile = File.createTempFile(basename, extension, fPathToUpload);

                input = event.getFile().getInputstream();
                output = new FileOutputStream(binaryFile);

                IOUtils.copy(input, output);
            }
            catch (Exception e)
            {
                mLogger.error("Exception: " + e.getMessage());

                return;
            }
            finally
            {
                if (input != null)
                    IOUtils.closeQuietly(input);
                if (output != null)
                    IOUtils.closeQuietly(output);
            }
        }

        if (binaryFile != null)
            getNewArticle().getBinaries().add(binaryFile);
    }

    public void removedUploadedBinary(int rowIndex)
    {
        mLogger.info("Remove uploaded binary. rowIndex: " + rowIndex);

        File binaryFile = getNewArticle().getBinaries().get(rowIndex);

        getNewArticle().getBinaries().remove(rowIndex);

        if (binaryFile != null)
        {
            mLogger.info("Delete of the file '" + binaryFile.getAbsolutePath() + "'");

            if(!binaryFile.delete())
                mLogger.error("Delete of the file '" + binaryFile.getAbsolutePath() + "' field");
        }
    }

    public void resetNewArticle()
    {
        mLogger.info("Reset New Article");
        newArticle = new NewArticle();
    }

    public List<String> getContentTypesLabels()
    {
        List<String> contentTypesLabels = articleSearchBacking.getContentTypesLabels();

        EscenicService escenicService;
        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            escenicService = (EscenicService) session.getAttribute("escenicService");
        }

        if (escenicService.isAdministrator())
            return contentTypesLabels;
        else
        {
            List<String> filteredContentTypesLabels = new ArrayList<>();

            for (ContentType contentType: escenicService.getConfiguration().getSelectedContentTypes())
            {
                // we could check if the content type is still present checking it with articleSearchBacking.getContentTypesLabels()
                filteredContentTypesLabels.add(contentType.getLabel());
            }

            return filteredContentTypesLabels;
        }
    }

    public void newArticle()
    {
        mLogger.info("New Article");

        List<State> states = articleSearchBacking.getStates();

        if (states == null)
        {
            mLogger.error("contentTypes/states is null");

            return;
        }

        EscenicService escenicService;
        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            // Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            escenicService = (EscenicService) session.getAttribute("escenicService");
        }

        if (multipleSelectedTreeNode == null || multipleSelectedTreeNode.length == 0)
        {
            mLogger.error("No Section node selected");

            return;
        }

        EscenicSection escenicSection = (EscenicSection) (multipleSelectedTreeNode[0]).getData();

        if (!escenicService.isAdministrator())
        {
            if (escenicService.getConfiguration().isReadOnly() ||
                !escenicService.getConfiguration().isAllowedSection(escenicSection.getUniqueName()))
            {
                mLogger.error("No rights to create a content");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "No rights to create a content");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }
        }

        String localNewArticleContentType = getNewArticle().getContentType();
        String localNewArticleState = getNewArticle().getState();
        String localNewArticleTitle = getNewArticle().getTitle();
        String localNewArticleGalleryType = getNewArticle().getGalleryType();
        List<File> localNewArticleBinaries = new ArrayList<>(getNewArticle().getBinaries());

        resetNewArticle();

        if (localNewArticleContentType == null || localNewArticleContentType.equalsIgnoreCase(""))
        {
            mLogger.error("newArticleContentType is wrong"
                + ", localNewArticleContentType: " + localNewArticleContentType
            );

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Content type has to be selected");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        if (localNewArticleState == null || localNewArticleState.equalsIgnoreCase(""))
        {
            mLogger.error("newArticleState is wrong"
                    + ", localNewArticleState: " + localNewArticleState
            );

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "State has to be selected");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        if (localNewArticleTitle == null || localNewArticleTitle.equalsIgnoreCase(""))
        {
            mLogger.error("newArticleTitle is wrong"
                    + ", localNewArticleTitle: " + localNewArticleTitle
            );

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Title has to be filled in");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        if (localNewArticleGalleryType == null || localNewArticleGalleryType.equalsIgnoreCase(""))
        {
            mLogger.error("localNewArticleGalleryType is wrong"
                    + ", localNewArticleGalleryType: " + localNewArticleGalleryType
            );

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Gallery Type has to be selected");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        ContentType selectedContentType = getContentType(localNewArticleContentType);
        if (selectedContentType == null)
        {
            mLogger.error("No ContentType found for the '" + selectedContentType + "' localNewArticleContentType");

            return;
        }

        State selectedState = null;

        {
            for (State state: states)
            {
                if (state.getLabel().equalsIgnoreCase(localNewArticleState))
                {
                    selectedState = state;

                    break;
                }
            }

            if (selectedState == null)
            {
                mLogger.error("No State found for the '" + selectedState + "' selectedState");

                return;
            }
        }

        List<String> newContentItemURLs = new ArrayList<>();
        List<String> newGalleryContentItemURLs = new ArrayList<>();
        ContentType selectedGalleryContentType = null;

        if (selectedContentType.getType().equalsIgnoreCase("picture") ||
            selectedContentType.getType().equalsIgnoreCase("keyframe"))
        {
            if (localNewArticleBinaries.size() == 0)
            {
                mLogger.error("At least one binary has to be uploaded in case of '" + selectedContentType.getType() + "'");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Content", "At least one binary has to be uploaded in case of '" + selectedContentType.getType() + "'");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            for (File binaryFile: localNewArticleBinaries)
            {
                String fileNameExtension;
                if (binaryFile.getName().lastIndexOf('.') == -1)
                {
                    mLogger.error("No file extension found. binary file name: " + binaryFile.getName());

                    continue;
                }

                fileNameExtension = binaryFile.getName().substring(binaryFile.getName().lastIndexOf('.') + 1);

                if (!MimeTypes.isImageFileExtension(fileNameExtension))
                {
                    mLogger.error("Wrong file name extension for the '" + selectedContentType.getType() + "' type");

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Content", "'" + binaryFile.getName() + "' discarded because it is not an image");
                    FacesContext context = FacesContext.getCurrentInstance();
                    context.addMessage(null, message);

                    continue;
                }

                mLogger.info("New Article. ContentType: " + selectedContentType.getType() +
                        ", title: " + localNewArticleTitle +
                        ", state: " + localNewArticleState +
                        ", section: " + escenicSection.getSummary() +
                        ", binaryFile: " + binaryFile.getAbsolutePath());

                String newContentItemURL = escenicService.newArticle(selectedContentType, selectedState,
                    localNewArticleTitle, binaryFile, escenicSection, null, null);

                if (newContentItemURL != null)
                    newContentItemURLs.add(newContentItemURL);

                mLogger.info("Delete of the file '" + binaryFile.getAbsolutePath() + "'");

                if(!binaryFile.delete())
                    mLogger.error("Delete of the file '" + binaryFile.getAbsolutePath() + "' field");
            }
        }
        else if (selectedContentType.getType().equalsIgnoreCase("transcodableVideo") ||
            selectedContentType.getType().equalsIgnoreCase("migrationVideo") ||
            selectedContentType.getType().equalsIgnoreCase("programmeVideo") ||
            selectedContentType.getType().equalsIgnoreCase("vmeVideo") ||
            selectedContentType.getType().equalsIgnoreCase("migrationVideo"))
        {
            if (localNewArticleBinaries.size() == 0)
            {
                mLogger.error("At least one binary has to be uploaded in case of '" + selectedContentType.getType() + "'");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Content", "At least one binary has to be uploaded in case of '" + selectedContentType.getType() + "'");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            for (File binaryFile: localNewArticleBinaries)
            {
                String fileNameExtension;
                if (binaryFile.getName().lastIndexOf('.') == -1)
                {
                    mLogger.error("No file extension found. binary file name: " + binaryFile.getName());

                    continue;
                }

                fileNameExtension = binaryFile.getName().substring(binaryFile.getName().lastIndexOf('.') + 1);

                if (!MimeTypes.isVideoFileExtension(fileNameExtension))
                {
                    mLogger.error("Wrong file name extension for the '" + selectedContentType.getType() + "' type");

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Content", "'" + binaryFile.getName() + "' discarded because it is not a video");
                    FacesContext context = FacesContext.getCurrentInstance();
                    context.addMessage(null, message);

                    continue;
                }

                mLogger.info("New Article. ContentType: " + selectedContentType.getType() +
                        ", title: " + localNewArticleTitle +
                        ", state: " + localNewArticleState +
                        ", section: " + escenicSection.getSummary() +
                        ", binaryFile: " + binaryFile.getAbsolutePath());

                String newContentItemURL = escenicService.newArticle(selectedContentType, selectedState,
                        localNewArticleTitle, binaryFile, escenicSection, null, null);

                if (newContentItemURL != null)
                    newContentItemURLs.add(newContentItemURL);

                mLogger.info("Delete of the file '" + binaryFile.getAbsolutePath() + "'");

                if(!binaryFile.delete())
                    mLogger.error("Delete of the file '" + binaryFile.getAbsolutePath() + "' field");
            }
        }
        else if (selectedContentType.getType().equalsIgnoreCase("gallery"))
        {
            for (File binaryFile: localNewArticleBinaries)
            {
                String fileNameExtension;
                if (binaryFile.getName().lastIndexOf('.') == -1)
                {
                    mLogger.error("No file extension found. binary file name: " + binaryFile.getName());

                    continue;
                }

                fileNameExtension = binaryFile.getName().substring(binaryFile.getName().lastIndexOf('.') + 1);

                if (localNewArticleGalleryType.equalsIgnoreCase("Image"))
                {
                    selectedGalleryContentType = getContentType("picture");
                    if (selectedGalleryContentType == null)
                    {
                        mLogger.error("No ContentType found for 'picture'");

                        return;
                    }

                    if (!MimeTypes.isImageFileExtension(fileNameExtension))
                    {
                        mLogger.error("Wrong file name extension for the '" + selectedContentType.getType() + "' type");

                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Content", "'" + binaryFile.getName() + "' discarded because it is not an 'image'");
                        FacesContext context = FacesContext.getCurrentInstance();
                        context.addMessage(null, message);

                        continue;
                    }
                }
                else if (localNewArticleGalleryType.equalsIgnoreCase("Video"))
                {
                    selectedGalleryContentType = getContentType("transcodableVideo");
                    if (selectedGalleryContentType == null)
                    {
                        mLogger.error("No ContentType found for 'transcodableVideo'");

                        return;
                    }

                    if (!MimeTypes.isVideoFileExtension(fileNameExtension))
                    {
                        mLogger.error("Wrong file name extension for the '" + selectedContentType.getType() + "' type");

                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Content", "'" + binaryFile.getName() + "' discarded because it is not a 'video'");
                        FacesContext context = FacesContext.getCurrentInstance();
                        context.addMessage(null, message);

                        continue;
                    }
                }
                else if (localNewArticleGalleryType.equalsIgnoreCase("Audio"))
                {
                    selectedGalleryContentType = getContentType("transcodableAudio");
                    if (selectedGalleryContentType == null)
                    {
                        mLogger.error("No ContentType found for 'transcodableAudio'");

                        return;
                    }

                    if (!MimeTypes.isAudioFileExtension(fileNameExtension))
                    {
                        mLogger.error("Wrong file name extension for the '" + selectedContentType.getType() + "' type");

                        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Content", "'" + binaryFile.getName() + "' discarded because it is not an 'audio'");
                        FacesContext context = FacesContext.getCurrentInstance();
                        context.addMessage(null, message);

                        continue;
                    }
                }
                else
                {
                    mLogger.error("Wrong localNewArticleGalleryType: " + localNewArticleGalleryType);

                    return;
                }

                mLogger.info("New Article. ContentType: " + selectedGalleryContentType.getType() +
                        ", title: " + localNewArticleTitle +
                        ", state: " + localNewArticleState +
                        ", section: " + escenicSection.getSummary() +
                        ", binaryFile: " + binaryFile.getAbsolutePath());

                String newContentItemURL = escenicService.newArticle(selectedGalleryContentType, selectedState,
                        localNewArticleTitle, binaryFile, escenicSection, null, null);

                if (newContentItemURL != null)
                    newGalleryContentItemURLs.add(newContentItemURL);

                mLogger.info("Delete of the file '" + binaryFile.getAbsolutePath() + "'");

                if(!binaryFile.delete())
                    mLogger.error("Delete of the file '" + binaryFile.getAbsolutePath() + "' field");
            }

            mLogger.info("New Article. ContentType: " + selectedContentType.getType() +
                    ", title: " + localNewArticleTitle +
                    ", state: " + localNewArticleState +
                    ", section: " + escenicSection.getSummary());

            String newContentItemURL = escenicService.newArticle(selectedContentType, selectedState,
                    localNewArticleTitle, null, escenicSection, selectedGalleryContentType, newGalleryContentItemURLs);

            if (newContentItemURL != null)
                newContentItemURLs.add(newContentItemURL);
        }
        else
        {
            mLogger.info("New Article. ContentType: " + selectedContentType.getType() +
                    ", title: " + localNewArticleTitle +
                    ", state: " + localNewArticleState +
                    ", section: " + escenicSection.getSummary());

            String newContentItemURL = escenicService.newArticle(selectedContentType, selectedState,
                localNewArticleTitle, null, escenicSection, null, null);

            if (newContentItemURL != null)
                newContentItemURLs.add(newContentItemURL);
        }

        if (newContentItemURLs.size() == 0)
        {
            mLogger.error("Creation of the content failed");

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Content", "Creation of the content failed");
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);

            return;
        }

        for (String newContentItemURL: newContentItemURLs)
        {
            String articleId = null;
            {
                int begingOfId = newContentItemURL.lastIndexOf('/');
                if (begingOfId == -1)
                {
                    mLogger.error("Wrong newContentItemURL: " + newContentItemURL);

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Content", "Creation of the content failed");
                    FacesContext context = FacesContext.getCurrentInstance();
                    context.addMessage(null, message);

                    return;
                }

                articleId = newContentItemURL.substring(begingOfId + 1);
            }

            if (articleId == null)
            {
                mLogger.error("articleId is null");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Content", "Creation of the content failed");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            Article detailedArticle = escenicService.getArticleContentDetails(articleId, true);

            if (detailedArticle == null)
            {
                mLogger.error("detailedArticle is null. Id: " + articleId);

                return;
            }

            articlesTabsBacking.addTab(detailedArticle);
        }

        for (String newContentItemURL: newGalleryContentItemURLs)
        {
            String articleId = null;
            {
                int begingOfId = newContentItemURL.lastIndexOf('/');
                if (begingOfId == -1)
                {
                    mLogger.error("Wrong newContentItemURL: " + newContentItemURL);

                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Content", "Creation of the content failed");
                    FacesContext context = FacesContext.getCurrentInstance();
                    context.addMessage(null, message);

                    return;
                }

                articleId = newContentItemURL.substring(begingOfId + 1);
            }

            if (articleId == null)
            {
                mLogger.error("articleId is null");

                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Content", "Creation of the content failed");
                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, message);

                return;
            }

            Article detailedArticle = escenicService.getArticleContentDetails(articleId, true);

            if (detailedArticle == null)
            {
                mLogger.error("detailedArticle is null. Id: " + articleId);

                return;
            }

            articlesTabsBacking.addTab(detailedArticle);
        }

        mLogger.info("Content created successfully");

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Content", "Content created successfully.");
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, message);
    }

    private void loadSections(TreeNode treeNode)
    {
        try {
            EscenicSection currentEscenicSection;

            if (treeNode == root)
                currentEscenicSection = null;
            else
                currentEscenicSection = (EscenicSection) treeNode.getData();

            String escenicURL;

            if (currentEscenicSection == null)
                escenicURL = "";
            else
                escenicURL = currentEscenicSection.getDownHref();

            mLogger.info("getSections(" + escenicURL + ")");

            EscenicService escenicService;
            {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

                Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
                escenicService = (EscenicService) session.getAttribute("escenicService");

                if (!isAuthenticated || escenicService == null)
                {
                    mLogger.error("It should never happen this error. isAuthenticated: " + isAuthenticated + ", escenicService: " + escenicService);

                    throw new Exception("It should never happen this error. isAuthenticated: " + isAuthenticated + ", escenicService: " + escenicService);
                }
            }

            List<EscenicSection> escenicSectionList = escenicService.getSections(escenicURL, true);

            {
                String displayName;

                mLogger.info("Received " + escenicSectionList.size() + " sections");

                // remove the dummy tree node and/or the other nodes if already filled
                treeNode.getChildren().clear();

                for (EscenicSection escenicSection: escenicSectionList)
                {
                    sections.put(escenicSection.getId(), escenicSection);

                    if (currentEscenicSection == null)  // root
                        displayName = escenicSection.getName() + " (" +
                            escenicSection.getPublicationTitle() + ")";
                    else
                        displayName = escenicSection.getName();

                    escenicSection.setTreeDisplayName(displayName);

                    TreeNode sectionTreeNode = new DefaultTreeNode(escenicSection, treeNode);

                    // dummy node
                    TreeNode dummyNode = new DefaultTreeNode("", sectionTreeNode);
                }
            }
        }
        catch (Exception e)
        {
            mLogger.error("Exception: " + e.getMessage());
        }
    }

    public EscenicSection getEscenicSectionById(String sectionId)
    {
        return sections.get(sectionId);
    }

    public ContentType getContentType (String contentTypeLabel)
    {
        ContentType selectedContentType = null;
        List<ContentType> contentTypes = articleSearchBacking.getContentTypes();

        if (contentTypeLabel == null)
            return null;

        for (ContentType contentType: contentTypes)
        {
            if (contentType.getLabel().equalsIgnoreCase(contentTypeLabel))
            {
                selectedContentType = contentType;

                break;
            }
        }

        return selectedContentType;
    }
}