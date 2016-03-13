package com.client.service;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 30/11/15
 * Time: 21:42
 * To change this template use File | Settings | File Templates.
 */
public class EscenicSection {

    private String id;
    private String name;
    private Boolean homeSection;
    private String uniqueName;
    private String summary;
    private String sectionParameters;
    private String href;
    private String downHref;
    private String contentItemsHref;
    private String publicationTitle;
    private String treeDisplayName;

    // refer the field (link) relate to this section
    private EscenicField escenicField;

    @Override
    public String toString() {
        return treeDisplayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public Boolean getHomeSection() {
        return homeSection;
    }

    public void setHomeSection(Boolean homeSection) {
        this.homeSection = homeSection;
    }

    public String getSectionParameters() {
        return sectionParameters;
    }

    public void setSectionParameters(String sectionParameters) {
        this.sectionParameters = sectionParameters;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public EscenicField getEscenicField() {
        return escenicField;
    }

    public void setEscenicField(EscenicField escenicField) {
        this.escenicField = escenicField;
    }

    public String getDownHref() {
        return downHref;
    }

    public void setDownHref(String downHref) {
        this.downHref = downHref;
    }

    public String getContentItemsHref() {
        return contentItemsHref;
    }

    public void setContentItemsHref(String contentItemsHref) {
        this.contentItemsHref = contentItemsHref;
    }

    public String getPublicationTitle() {
        return publicationTitle;
    }

    public void setPublicationTitle(String publicationTitle) {
        this.publicationTitle = publicationTitle;
    }

    public String getTreeDisplayName() {
        return treeDisplayName;
    }

    public void setTreeDisplayName(String treeDisplayName) {
        this.treeDisplayName = treeDisplayName;
    }
}
