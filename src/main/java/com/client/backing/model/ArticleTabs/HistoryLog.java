package com.client.backing.model.ArticleTabs;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 16/11/15
 * Time: 10:43
 * To change this template use File | Settings | File Templates.
 */
public class HistoryLog {

    private Date updated;
    private String state;
    private String author;

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
