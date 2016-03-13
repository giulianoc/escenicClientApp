package com.client.backing.model.common;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 29/10/15
 * Time: 07:25
 * To change this template use File | Settings | File Templates.
 */
public class ContentType implements Serializable {

    // logger commented because it causes a stack overflow to gson
    // May be if we declare it as static, gson will work as well
    // private final Logger mLogger = Logger.getLogger(this.getClass());

    private String label;
    private String type;
    private String url;

    public ContentType(String label, String type, String url)
    {
        this.label = label;
        this.type = type;
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object obj) {
        return ((ContentType) obj).getType().equalsIgnoreCase(getType());
    }
}
