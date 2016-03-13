package com.client.backing;

import com.client.backing.model.Chat.MessageInfo;
import com.client.service.EscenicService;
import org.apache.log4j.Logger;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 01/11/15
 * Time: 06:04
 * To change this template use File | Settings | File Templates.
 */

@ManagedBean
@ViewScoped
public class ChatBacking implements Serializable {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    public enum ChannelType {
        broadcast,
        user
    }

    private List<ChannelType> channelTypeList;
    private ChannelType channelType;
    private String message;
    private String allChat;
    private String userChannel;

    private String userName;
    private EventBus eventBus;


    public ChatBacking()
    {
        channelTypeList = new ArrayList<>();
        channelTypeList.add(ChannelType.broadcast);
        channelTypeList.add(ChannelType.user);

        setMessage("<br />");
        setAllChat("<br />");

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            EscenicService escenicService = (EscenicService) session.getAttribute("escenicService");

            userName = escenicService.getUserName();
        }

        eventBus = EventBusFactory.getDefault().eventBus();
    }

    public void receivedMessage(ActionEvent actionEvent)
    {

        Map<String,String> requestParameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        // mLogger.info("receivedMessage. requestParameterMap: " + requestParameterMap);

        String from = requestParameterMap.get("from");
        String to = requestParameterMap.get("to");
        String message = requestParameterMap.get("message");

        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

        String messageToBeAddedToTheChat = "<br /><br />" + simpleDateFormat.format(now) + ". From " + from +
            " to " + to + "<hr>" + message;

        mLogger.info("receivedMessage. messageToBeAddedToTheChat: " + messageToBeAddedToTheChat);

        setAllChat(messageToBeAddedToTheChat + (getAllChat() == null ? "" : getAllChat()));

        FacesMessage fmMessage = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "From " + from, message);
        // RequestContext.getCurrentInstance().showMessageInDialog(message);
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, fmMessage);
    }

    public void sendMessage()
    {
        if (eventBus == null)
        {
            mLogger.error("eventBus is null");

            return;
        }

        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

        mLogger.info("Chat. sendMessage. ChannelType: " + channelType + ", userChannel: " + userChannel + ", message: " + getMessage());

        String messageToBeAddedToTheChat = "<br /><br />" + simpleDateFormat.format(now) + ". From " + userName +
            " to " + (getChannelType() == ChannelType.broadcast ? "broadcast" : getUserChannel()) +
            "<hr>" + getMessage();

        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessage(getMessage());
        messageInfo.setFrom(userName);

        if (channelType == ChannelType.broadcast)
        {
            messageInfo.setTo("broadcast");

            eventBus.publish("/user", messageInfo);

            setAllChat(messageToBeAddedToTheChat + (getAllChat() == null ? "" : getAllChat()));

            setMessage("<br />");
        }
        else if (channelType == ChannelType.user && userChannel != null && !userChannel.equalsIgnoreCase(""))
        {
            messageInfo.setTo(getUserChannel());

            eventBus.publish("/user", messageInfo);

            setAllChat(messageToBeAddedToTheChat + (getAllChat() == null ? "" : getAllChat()));

            setMessage("<br />");
        }
        else
            mLogger.error("Send Chat, wrong parameters. channelType: " + channelType + ", userChannel: " + userChannel);
    }

    public void sendMessageToUnlock(String destinationUserName, String articleId, String fieldName)
    {
        if (eventBus == null)
        {
            mLogger.error("eventBus is null");

            return;
        }

        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

        mLogger.info("Chat. sendMessageToUnlock. destinationUserName: " + destinationUserName +
            ", articleId: " + articleId + ", fieldName: " + fieldName);

        String messageToUnlock = "Dear " + destinationUserName + ", could you please unlock the '" + fieldName +
            "' field of the content having ID '" + articleId + "'? Best regards";

        String messageToBeAddedToTheChat = "<br /><br />" + simpleDateFormat.format(now) + ". From " + userName +
            " to " + destinationUserName +
            "<hr>" + messageToUnlock;

        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessage(messageToBeAddedToTheChat);
        messageInfo.setFrom(userName);
        messageInfo.setTo(destinationUserName);

        eventBus.publish("/user", messageInfo);

        setAllChat(messageToBeAddedToTheChat + (getAllChat() == null ? "" : getAllChat()));
    }

    public List<ChannelType> getChannelTypeList() {
        return channelTypeList;
    }

    public void setChannelTypeList(List<ChannelType> channelTypeList) {
        this.channelTypeList = channelTypeList;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public String getUserChannel() {
        return userChannel;
    }

    public void setUserChannel(String userChannel) {
        this.userChannel = userChannel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAllChat() {
        return allChat;
    }

    public void setAllChat(String allChat) {
        this.allChat = allChat;
    }
}