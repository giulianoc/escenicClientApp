package com.client.backing.model.ArticleTabs;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 15/11/15
 * Time: 04:59
 * To change this template use File | Settings | File Templates.
 */
public class MediaInfo {

    public enum MediaType {
        MEDIA_AUDIO,
        MEDIA_VIDEO,
        MEDIA_NONE
    }

    private MediaType mediaType;
    private Long id;
    private String externalReference;
    private String group;
    private String status;
    private String message;

    private int duration;
    private String transcodingState;
    private int progress;

    private Vector<MediaEntryInfo> mediaEntriesInfo = new Vector<>();

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTranscodingState() {
        return transcodingState;
    }

    public void setTranscodingState(String transcodingState) {
        this.transcodingState = transcodingState;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Vector<MediaEntryInfo> getMediaEntriesInfo() {
        return mediaEntriesInfo;
    }

    public void setMediaEntriesInfo(Vector<MediaEntryInfo> mediaEntriesInfo) {
        this.mediaEntriesInfo = mediaEntriesInfo;
    }
}
