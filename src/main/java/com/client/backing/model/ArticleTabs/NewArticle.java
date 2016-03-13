package com.client.backing.model.ArticleTabs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 11/12/15
 * Time: 21:55
 * To change this template use File | Settings | File Templates.
 */
public class NewArticle {
    private String contentType;
    private String title;
    private String state;
    private List<File> binaries = new ArrayList<>();
    private String galleryType;


    public List<String> getGalleryTypesLabels()
    {
        List<String> galleryTypesLabels = new ArrayList<>();

        galleryTypesLabels.add("Image");
        galleryTypesLabels.add("Video");
        galleryTypesLabels.add("Audio");

        return galleryTypesLabels;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getGalleryType() {
        return galleryType;
    }

    public void setGalleryType(String galleryType) {
        this.galleryType = galleryType;
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

    public List<File> getBinaries() {
        return binaries;
    }

    public void setBinaries(List<File> binaries) {
        this.binaries = binaries;
    }
}
