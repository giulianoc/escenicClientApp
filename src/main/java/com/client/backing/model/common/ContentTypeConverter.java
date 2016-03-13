package com.client.backing.model.common;

import com.client.backing.ArticleSearchBacking;
import org.apache.log4j.Logger;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 21/12/15
 * Time: 12:15
 * To change this template use File | Settings | File Templates.
 */
@FacesConverter(value = "contentTypeConverter", forClass = ContentTypeConverter.class)
public class ContentTypeConverter implements Converter {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String submittedValue)
    {
        // mLogger.error("submittedValue:  " + submittedValue);

        ValueExpression valueExpression = context.getApplication().getExpressionFactory().createValueExpression(
                context.getELContext(), "#{articleSearchBacking}", ArticleSearchBacking.class);
        ArticleSearchBacking articleSearchBacking = (ArticleSearchBacking) valueExpression.getValue(context.getELContext());

        ContentType selectedContentType = null;
        List<ContentType> contentTypes = articleSearchBacking.getContentTypes();

        for (ContentType contentType: contentTypes)
        {
            // I have to use getLabel because the submittedValue parameter receive the string displayed (label)
            if (contentType.getLabel().equalsIgnoreCase(submittedValue))
            {
                selectedContentType = contentType;

                break;
            }
        }

        return selectedContentType;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object modelValue) {

        return ((ContentType) modelValue).getLabel();
    }

}