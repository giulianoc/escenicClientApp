package com.client.service;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 14/12/15
 * Time: 12:01
 * To change this template use File | Settings | File Templates.
 */
public class EscenicTypeGroup implements Serializable {
    private String name;
    private String label;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
