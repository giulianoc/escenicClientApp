package com.client.backing;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 27/10/15
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import com.client.backing.model.SearchResult.ArticleTableData;
import com.client.backing.model.SearchResult.LazySearchResultDataModel;
import org.apache.log4j.Logger;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;


@ManagedBean
@ViewScoped
public class LazySearchResultViewBacking implements Serializable {

    private Logger mLogger = Logger.getLogger(this.getClass());

    private LazySearchResultDataModel lazySearchResultDataModel;

    private ArticleTableData selectedArticleTableData;

    @ManagedProperty("#{articleSearchBacking}")
    ArticleSearchBacking articleSearchBacking;


    public LazySearchResultViewBacking() {
        lazySearchResultDataModel = new LazySearchResultDataModel();
    }

    public void setArticleSearchBacking(ArticleSearchBacking articleSearchBacking) {
        this.articleSearchBacking = articleSearchBacking;
        lazySearchResultDataModel.setArticleSearchBacking(articleSearchBacking);
    }

    public LazyDataModel<ArticleTableData> getLazySearchResultDataModel() {
        return lazySearchResultDataModel;
    }

    public ArticleTableData getSelectedArticleTableData() {
        return selectedArticleTableData;
    }

    public void setSelectedArticleTableData(ArticleTableData selectedArticleTableData) {
        this.selectedArticleTableData = selectedArticleTableData;
    }

    public void onRowSelect(SelectEvent event)
    {
        // FacesMessage msg = new FacesMessage("Article Selected", ((Article) event.getObject()).getId().toString());
        // FacesContext.getCurrentInstance().addMessage(null, msg);

        // mLogger.info("Article selected. ID: " + ((Article) event.getObject()).getId());
    }
}