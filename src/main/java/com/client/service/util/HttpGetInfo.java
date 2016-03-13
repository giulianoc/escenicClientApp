package com.client.service.util;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 19/11/15
 * Time: 10:18
 * To change this template use File | Settings | File Templates.
 */
public class HttpGetInfo {
    private String returnedBody;
    private String ETagHeader;

    public String getReturnedBody() {
        return returnedBody;
    }

    public void setReturnedBody(String returnedBody) {
        this.returnedBody = returnedBody;
    }

    public String getETagHeader() {
        return ETagHeader;
    }

    public void setETagHeader(String ETagHeader) {
        this.ETagHeader = ETagHeader;
    }
}
