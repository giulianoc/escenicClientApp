package com.components.myCkEditor;

import org.apache.log4j.Logger;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 02/01/16
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
@FacesComponent("MyCkEditorComponent")
public class MyCkEditorComponent extends UINamingContainer {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private static final String ELEMENT_ID = "my-ck-editor";

    private static final String ATTRIBUTE_READONLY = "readonly";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String ATTRIBUTE_CUSTOMCONFIG = "customConfig";
    private static final String ATTRIBUTE_DISABLECHANGEEVENTCONDITION = "disableChangeEventCondition";
    private static final String ATTRIBUTE_UPDATECHANGEEVENTCONDITION = "updateChangeEventCondition";

    public String getElementId()
    {
        return ELEMENT_ID;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        super.encodeBegin(context);

        UIComponent element = findElement();

        addBooleanAttribute(element, ATTRIBUTE_READONLY, false);
        addBooleanAttribute(element, ATTRIBUTE_DISABLECHANGEEVENTCONDITION, false);

        addAttributeIfNotNull(element, ATTRIBUTE_VALUE);
        addAttributeIfNotNull(element, ATTRIBUTE_CUSTOMCONFIG);
        addAttributeIfNotNull(element, ATTRIBUTE_UPDATECHANGEEVENTCONDITION);
    }

    private void addAttributeIfNotNull(UIComponent component, String attributeName)
    {
        Object attributeValue = getAttributeValue(attributeName);
        if (attributeValue != null)
        {
            component.getPassThroughAttributes().put(attributeName, attributeValue);
        }
    }

    private void addBooleanAttribute(UIComponent component, String attributeName, boolean ifTrue)
    {
        if (ifTrue)
        {
            if (isAttributeTrue(attributeName))
            {
                component.getPassThroughAttributes().put(attributeName, "true");
            }
        }
        else
        {
            component.getPassThroughAttributes().put(attributeName, Boolean.toString(isAttributeTrue(attributeName)));
        }
    }

    private UIComponent findElement() throws IOException
    {
        UIComponent element = findComponent(getElementId());
        if (element == null)
        {
            throw new IOException("My element with ID " + getElementId() + " could not be found");
        }

        return element;
    }

    private Object getAttributeValue(String name)
    {
        ValueExpression ve = getValueExpression(name);

        if (ve != null)
        {
            // Attribute is a value expression
            return ve.getValue(getFacesContext().getELContext());
        }
        else if (getAttributes().containsKey(name))
        {
            // Attribute is a fixed value
            return getAttributes().get(name);
        }
        else
        {
            // Attribute does not exist
            return null;
        }
    }

    private boolean isAttributeTrue(String attributeName)
    {
        boolean isBoolean = getAttributeValue(attributeName) instanceof Boolean;
        if (!isBoolean)
            return false;

        return ((Boolean) getAttributeValue(attributeName)) == Boolean.TRUE;
    }

    /*
    public void saveEventListener(AjaxBehaviorEvent event)
    {
        mLogger.error("ajaxEventListener. event: " + event);
        mLogger.error("ajaxEventListener. getAttributeValue(ATTRIBUTE_VALUE): " + getAttributeValue(ATTRIBUTE_VALUE));
        FacesContext context = FacesContext.getCurrentInstance();
        MethodExpression ajaxEventListener = (MethodExpression) getAttributes().get("saveEventListener");
        // ajaxEventListener.invoke(context.getELContext(), new Object[] { event });
        ajaxEventListener.invoke(context.getELContext(), new Object[] { getAttributeValue(ATTRIBUTE_VALUE) });
    }
    */

    public void changeEventListener()
    {
        // mLogger.error("changeEventListener.");
        FacesContext context = FacesContext.getCurrentInstance();
        MethodExpression ajaxEventListener = (MethodExpression) getAttributes().get("changeEventListener");
        // ajaxEventListener.invoke(context.getELContext(), new Object[] { event });
        ajaxEventListener.invoke(context.getELContext(),
                new Object[]{
                        getAttributeValue(ATTRIBUTE_VALUE)
                });
    }
}
