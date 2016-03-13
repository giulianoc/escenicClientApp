package com.client.service.util;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 21/11/15
 * Time: 15:24
 * To change this template use File | Settings | File Templates.
 */
public class ComplexColumnModel implements Serializable {

    private String header;
    private String property;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
