package com.client.backing.model.Chat;

import com.client.backing.model.ArticleTabs.LockChanged;
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
 * Date: 03/12/15
 * Time: 11:47
 * To change this template use File | Settings | File Templates.
 */
@PushEndpoint(value = "/locks")
public class LocksPushEndPoint {

    private Logger mLogger = Logger.getLogger(this.getClass());

    @OnMessage(encoders = {JSONEncoder.class})
    public LockChanged onMessage(LockChanged lockChanged)
    {
        mLogger.info("onMessage (/locks). Id: " + lockChanged.getId() + ", KeyField: " + lockChanged.getKeyField());

        return lockChanged;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus e)
    {
        mLogger.info("onOpen (/locks). r: " + r + ", e: " + e);
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus e)
    {
        mLogger.info("onClose (/locks). r: " + r + ", e: " + e);
    }
}
