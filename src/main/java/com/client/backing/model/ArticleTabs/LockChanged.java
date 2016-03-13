package com.client.backing.model.ArticleTabs;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 03/12/15
 * Time: 12:10
 * To change this template use File | Settings | File Templates.
 */
public class LockChanged {
    private String id;
    private String keyField;

    public String toString()
    {
        String toString = "LockChanged. id: " + id + ", keyField: " + keyField;

        return toString;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKeyField() {
        return keyField;
    }

    public void setKeyField(String keyField) {
        this.keyField = keyField;
    }
}
