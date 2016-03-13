package com.client.backing.model.Chat;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 05/12/15
 * Time: 16:11
 * To change this template use File | Settings | File Templates.
 */
public class MessageInfo {
    private String from;
    private String to;
    private String message;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
