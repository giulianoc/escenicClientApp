package com.client.backing.model.ArticleTabs;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 09/11/15
 * Time: 16:54
 * To change this template use File | Settings | File Templates.
 */
public class ImageInfo {
    private String id;
    private String url;
    private String cachedPath;
    private String title;
    private String description;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCachedPath() {
        return cachedPath;
    }

    public void setCachedPath(String cachedPath) {
        this.cachedPath = cachedPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
