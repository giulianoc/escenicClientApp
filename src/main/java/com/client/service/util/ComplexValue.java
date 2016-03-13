package com.client.service.util;

import com.client.service.EscenicField;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 06/12/15
 * Time: 18:50
 * To change this template use File | Settings | File Templates.
 */
public class ComplexValue {

    // initial escenic field, the one to be locked in case any field of the complex is modified
    private EscenicField initialEscenicField;
    private HashMap<String,EscenicField> metadataFields = new HashMap<>();


    public EscenicField getInitialEscenicField() {
        return initialEscenicField;
    }

    public void setInitialEscenicField(EscenicField initialEscenicField) {
        this.initialEscenicField = initialEscenicField;
    }

    public HashMap<String, EscenicField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(HashMap<String, EscenicField> metadataFields) {
        this.metadataFields = metadataFields;
    }
}
