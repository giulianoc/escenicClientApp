package com.client.service;

import com.client.backing.model.ArticleTabs.ImageInfo;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 14/11/15
 * Time: 22:29
 * To change this template use File | Settings | File Templates.
 */
public class EscenicLink {

    private static final Logger mLogger = Logger.getLogger(EscenicLink.class);

    public enum ChangeType {
        ESCENIC_NOCHANGE,
        ESCENIC_REMOVED,
        ESCENIC_ADDED,

        // the only possible change is about the home_section.
        ESCENIC_MODIFIED
    }

    private String rel;
    private String group;
    private String href;
    private Long id;
    private String type;
    private String model;
    private String title;
    private ImageInfo thumbnailImageInfo;
    private String state;
    private ChangeType changeType;


    public EscenicLink()
    {
        changeType = ChangeType.ESCENIC_NOCHANGE;
    }

    public String toString()
    {
        String toString = "EscenicLink. rel: " + rel
            + ", group: " + group
            + ", href: " + href
            + ", id: " + id
            + ", type: " + type
            + ", model: " + model
            + ", title: " + title
            + ", state: " + state
            + ", changeType: " + changeType;

        return toString;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;

        if (href != null)
        {
            int begingOfId = href.lastIndexOf('/');
            if (begingOfId != -1)
            {
                try {
                    id = Long.parseLong(href.substring(begingOfId + 1));
                }
                catch (Exception e)
                {
                    // log commented because most of the href does not end with a number
                    // mLogger.error("href: " + href + ", Exception: " + e.getMessage());
                }
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ImageInfo getThumbnailImageInfo() {
        return thumbnailImageInfo;
    }

    public void setThumbnailImageInfo(ImageInfo thumbnailImageInfo) {
        this.thumbnailImageInfo = thumbnailImageInfo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
}
