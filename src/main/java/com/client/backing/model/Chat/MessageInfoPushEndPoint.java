package com.client.backing.model.Chat;

import org.apache.log4j.Logger;
import org.primefaces.push.EventBus;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 04/12/15
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
@PushEndpoint(value = "/user")
public class MessageInfoPushEndPoint {

    private Logger mLogger = Logger.getLogger(this.getClass());

    @OnMessage(encoders = {JSONEncoder.class})
    public MessageInfo onMessage(MessageInfo messageInfo)
    {
        mLogger.info("onMessage (/user). From: " + messageInfo.getFrom() + ", To: " + messageInfo.getTo() + ", Message: " + messageInfo.getMessage());

        return messageInfo;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus e)
    {
        mLogger.info("onOpen (/user). r: " + r + ", e: " + e);
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus e)
    {
        mLogger.info("onClose (/user). r: " + r + ", e: " + e);
    }
}
