package com.client.service;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 26/11/15
 * Time: 09:40
 * To change this template use File | Settings | File Templates.
 */
public class EscenicLock implements Serializable {

    // Public URL
    private String id;
    private String title;
    private String fragment;
    private String userName;
    private Date updated;
    private Date expires;
    private String summary;
    private String content;

    public String toString()
    {
        String toString = "EscenicLock. id: " + id
            + ", title: " + title
            + ", fragment: " + fragment
            + ", userName: " + userName
            + ", updated: " + updated
            + ", expires: " + expires
            + ", summary: " + summary
            + ", content: " + content;

        return toString;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
