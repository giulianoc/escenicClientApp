package com.client.backing.model.SearchResult;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 27/10/15
 * Time: 20:44
 * To change this template use File | Settings | File Templates.
 */
import java.io.Serializable;
import java.util.*;

import com.client.backing.ArticleSearchBacking;
import com.client.service.EscenicService;
import com.client.backing.model.common.ContentType;
import com.client.backing.model.common.State;
import org.apache.log4j.Logger;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

public class LazySearchResultDataModel extends LazyDataModel<ArticleTableData> implements Serializable {

    private Logger mLogger = Logger.getLogger(this.getClass());

    ArticleSearchBacking articleSearchBacking;

    private List<ArticleTableData> currentArticlesTableData = new ArrayList<>();
    private Long totalSearchItems;

    public LazySearchResultDataModel()
    {
    }

    public void setArticleSearchBacking(ArticleSearchBacking articleSearchBacking) {
        this.articleSearchBacking = articleSearchBacking;
    }

    public List<ArticleTableData> getCurrentArticlesTableData() {
        return currentArticlesTableData;
    }

    public void setCurrentArticlesTableData(List<ArticleTableData> currentArticlesTableData) {
        this.currentArticlesTableData = currentArticlesTableData;
    }

    @Override
    public ArticleTableData getRowData(String id)
    {
        for(ArticleTableData article : currentArticlesTableData)
        {
            if(article.getObjectId().equals(Long.parseLong(id)))
                return article;
        }

        return null;
    }

    @Override
    public Object getRowKey(ArticleTableData article)
    {
        return article.getObjectId();
    }

    @Override
    public List<ArticleTableData> load(int startIndex, int pageSize, String sortField,
        SortOrder sortOrder, Map<String,Object> filters)
    {

        mLogger.info("loading. startIndex: " + startIndex +
            ", sortField: " + sortField + ", sortOrder: " + sortOrder + ", filters: " + filters);

        if (articleSearchBacking == null)
        {
            mLogger.info("widget not completely load yet. Data not loaded. articleSearchBacking: " + articleSearchBacking);

            return currentArticlesTableData;
        }

        EscenicService escenicService;

        {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

            Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
            escenicService = (EscenicService) session.getAttribute("escenicService");

            if (!isAuthenticated || escenicService == null)
            {
                mLogger.error("It should never happen this error. isAuthenticated: " + isAuthenticated + ", escenicService: " + escenicService);


                return currentArticlesTableData;
            }
        }

        {
            currentArticlesTableData.clear();

            String [] selectedContentTypes = null;
            if (articleSearchBacking.getSelectedContentTypes() != null)
            {
                int contentTypesLength = articleSearchBacking.getSelectedContentTypes().length;

                selectedContentTypes = new String [contentTypesLength];

                List<ContentType> contentTypes = articleSearchBacking.getContentTypes();
                boolean contentTypeFound;
                for (int index = 0; index < contentTypesLength; index++)
                {
                    String contentTypeLabel = (articleSearchBacking.getSelectedContentTypes())[index];

                    contentTypeFound = false;
                    for(ContentType contentType: contentTypes)
                    {
                        if (contentType.getLabel().equalsIgnoreCase(contentTypeLabel))
                        {
                            selectedContentTypes[index] = contentType.getType();
                            contentTypeFound = true;

                            break;
                        }
                    }

                    if (!contentTypeFound)
                        mLogger.error("content type was not found. Label: " + contentTypeLabel);
                }
            }

            String [] selectedStates = null;
            if (articleSearchBacking.getSelectedState() != null)
            {
                int statesLength = articleSearchBacking.getSelectedState().length;

                selectedStates = new String [statesLength];

                List<State> states = articleSearchBacking.getStates();
                boolean stateFound;
                for (int index = 0; index < statesLength; index++)
                {
                    String stateLabel = (articleSearchBacking.getSelectedState())[index];

                    stateFound = false;
                    for (State state: states)
                    {
                        if (state.getLabel().equalsIgnoreCase(stateLabel))
                        {
                            selectedStates[index] = state.getState();
                            stateFound = true;

                            break;
                        }
                    }

                    if (!stateFound)
                        mLogger.error("state was not found. Label: " + stateLabel);
                }
            }

            String selectedPublications;
            {
                if (articleSearchBacking.getSelectedPublications() == null ||
                    articleSearchBacking.getSelectedPublications().equalsIgnoreCase("<all>"))
                    selectedPublications = "";
                else
                    selectedPublications = articleSearchBacking.getSelectedPublications();
            }

            totalSearchItems = escenicService.getContentItems (
                startIndex, pageSize,
                articleSearchBacking.getTextToSearch(), articleSearchBacking.getSelectedTextToSearchType(),
                selectedPublications,
                articleSearchBacking.getSelectedSectionsIds(), articleSearchBacking.isIncludeSubSections(),
                articleSearchBacking.getPublishStartTime(), articleSearchBacking.getPublishEndTime(),
                articleSearchBacking.getExpireStartTime(), articleSearchBacking.getExpireEndTime(),
                articleSearchBacking.getLastModifiedStartTime(), articleSearchBacking.getLastModifiedEndTime(),
                articleSearchBacking.getActivateStartTime(), articleSearchBacking.getActivateEndTime(),
                articleSearchBacking.getStartTime(), articleSearchBacking.getEndTime(),
                articleSearchBacking.getSelectedOrderBy(), articleSearchBacking.getSelectedOrderType(),
                selectedContentTypes, selectedStates,
                articleSearchBacking.getTagsURI(), articleSearchBacking.getSelectedTagsSearchType(),
                    currentArticlesTableData);

            this.setRowCount(totalSearchItems.intValue());

            /*
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Load Contents",
                    "Search. Text to search: " + (articleSearchBacking.getTextToSearch() == null ? "" : articleSearchBacking.getTextToSearch())
                        + ", Publications: " + (articleSearchBacking.getSelectedPublications() == null ? "" : articleSearchBacking.getSelectedPublications())
                        + ", SectionsIds: " + (articleSearchBacking.getSelectedSectionsIds() == null ? "" : articleSearchBacking.getSelectedSectionsIds())
                        + ", PublishStartTime: " + (articleSearchBacking.getPublishStartTime() == null ? "" : articleSearchBacking.getPublishStartTime())
                        + ", PublishEndTime: " + (articleSearchBacking.getPublishEndTime() == null ? "" : articleSearchBacking.getPublishEndTime())
                        + ", ExpireStartTime: " + (articleSearchBacking.getExpireStartTime() == null ? "" : articleSearchBacking.getExpireStartTime())
                        + ", ExpireEndTime: " + (articleSearchBacking.getExpireEndTime() == null ? "" : articleSearchBacking.getExpireEndTime())
                        + ", ActivateStartTime: " + (articleSearchBacking.getActivateStartTime() == null ? "" : articleSearchBacking.getActivateStartTime())
                        + ", ActivateEndTime: " + (articleSearchBacking.getActivateEndTime() == null ? "" : articleSearchBacking.getActivateEndTime())
                        + ", StartTime: " + (articleSearchBacking.getStartTime() == null ? "" : articleSearchBacking.getStartTime())
                        + ", EndTime: " + (articleSearchBacking.getEndTime() == null ? "" : articleSearchBacking.getEndTime())
                        + ", SelectedContentTypes: " + (selectedContentTypes == null ? "" : selectedContentTypes));
            // RequestContext.getCurrentInstance().showMessageInDialog(message);
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, message);
            */
        }


        /*
        {
            //filter
            for(Article article : currentArticles)
            {
                boolean match = true;

                if (filters != null)
                {
                    for (Iterator<String> it = filters.keySet().iterator(); it.hasNext();)
                    {
                        try {
                            String filterProperty = it.next();
                            String filterValue = filters.get(filterProperty);
                            String fieldValue = String.valueOf(article.getClass().getField(filterProperty).get(article));

                            if(filterValue == null || fieldValue.startsWith(filterValue))
                            {
                                match = true;
                            }
                            else
                            {
                                match = false;

                                break;
                            }
                        }
                        catch(Exception e)
                        {
                            match = false;
                        }
                    }
                }

                if(match)
                {
                    datasource.add(article);
                }
            }

            //sort
            if(sortField != null)
            {
                Collections.sort(currentArticles, new LazySorter(sortField, sortOrder));
            }
        }
        */

        mLogger.info("content-items. totalSearchItems.intValue(): " + totalSearchItems.intValue() + ", currentArticles.size(): " + currentArticlesTableData.size());

        return currentArticlesTableData;
    }
}