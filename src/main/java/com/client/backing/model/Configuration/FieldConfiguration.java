package com.client.backing.model.Configuration;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 20/12/15
 * Time: 17:10
 * To change this template use File | Settings | File Templates.
 */
public class FieldConfiguration implements Serializable {

    // logger commented because it causes a stack overflow to gson
    // May be if we declare it as static, gson will work as well
    // private final Logger mLogger = Logger.getLogger(this.getClass());

    private String groupLabel;
    private String keyField;
    private String label;
    private boolean mandatory;


    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public String getKeyField() {
        return keyField;
    }

    public void setKeyField(String keyField) {
        this.keyField = keyField;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}
