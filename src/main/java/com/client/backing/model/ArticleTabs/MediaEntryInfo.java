package com.client.backing.model.ArticleTabs;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 25/02/14
 * Time: 10:35
 * To change this template use File | Settings | File Templates.
 */
public class MediaEntryInfo implements Serializable {
    private String mrid;
    private int height;
    private int width;
    private String uri;
    private String mimeType;
    private String status;

    public String getMrid() {
        return mrid;
    }

    public void setMrid(String mrid) {
        this.mrid = mrid;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
