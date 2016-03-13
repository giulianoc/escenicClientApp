package com.client.backing.model.ArticleTabs;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 25/10/15
 * Time: 08:44
 * To change this template use File | Settings | File Templates.
 */
public class EscenicTab {

    private final Logger mLogger = Logger.getLogger(this.getClass());

    private Article article;


    public String getId() {
        return article.getMetadataFields().get("com.escenic.displayId").getStringValue();
    }

    /*
    public void setId(long id) {
        this.id = id;
    }
    */

    public String getTabHeaderTitle() {
        int maxTabHeaderTitleLength = 20;

        String tabHeaderTitle = article.getMetadataFields().get("com.escenic.displayId").getStringValue();
        if (article.getMetadataFields().get("title") != null)
            tabHeaderTitle += (" - " + article.getMetadataFields().get("title").getStringValue());
        else if (article.getMetadataFields().get("name") != null)
            tabHeaderTitle += (" - " + article.getMetadataFields().get("name").getStringValue());

        if (tabHeaderTitle.length() > maxTabHeaderTitleLength)
            tabHeaderTitle = tabHeaderTitle.substring(0, maxTabHeaderTitleLength - 3) + "...";

        return tabHeaderTitle;
    }

    public Article getArticle() {
        /*
        for (String fieldKey: article.getEntryFields().keySet())
        {
            mLogger.info("Field (entryFields). Name: " + fieldKey + ", ChangeType: " + article.getEntryFields().get(fieldKey).getChangeType());
        }

        // if (!isArticleModified)
        {
            for (String fieldKey: article.getPayloadFields().keySet())
            {
                mLogger.info("Field (payloadFields). Name: " + fieldKey + ", ChangeType: " + article.getPayloadFields().get(fieldKey).getChangeType());
            }
        }
        */

        return article;
    }

    public void setArticle(Article article) {

        /*
        mLogger.error("article: " + article);

        for (String fieldKey: article.getEntryFields().keySet())
        {
            mLogger.info("Field (entryFields). Name: " + fieldKey + ", ChangeType: " + article.getEntryFields().get(fieldKey).getChangeType());
        }

        // if (!isArticleModified)
        {
            for (String fieldKey: article.getPayloadFields().keySet())
            {
                mLogger.info("Field (payloadFields). Name: " + fieldKey + ", ChangeType: " + article.getPayloadFields().get(fieldKey).getChangeType());
            }
        }
        */

        this.article = article;
    }
}
