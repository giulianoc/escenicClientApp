package com.client.backing.model.SearchResult;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 22/11/15
 * Time: 09:27
 * To change this template use File | Settings | File Templates.
 */
public class ArticleTableData {

    private Long objectId;
    private String publication;
    private String title;
    private String state;
    private String homePage;
    private Date published;
    private Date startTime;
    private String contentType;


    public String toString ()
    {
        String toString = "ArticleTableData. objectId: " + objectId
            + ", publication: " + publication
            + ", title: " + title
            + ", state: " + state
            + ", homePage: " + homePage
            + ", published: " + published
            + ", startTime: " + startTime
            + ", contentType: " + contentType;

        return toString;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getPublication() {
        return publication;
    }

    public void setPublication(String publication) {
        this.publication = publication;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
