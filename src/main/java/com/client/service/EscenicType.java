package com.client.service;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 16/11/15
 * Time: 21:48
 * To change this template use File | Settings | File Templates.
 */
public class EscenicType implements Serializable {

    private static final Logger mLogger = Logger.getLogger(EscenicType.class);

    public enum Type
    {
        ESCENIC_SIMPLE,
        ESCENIC_NESTEDSTRINGTYPE, // nested having just one field that it is a string
        ESCENIC_NESTEDCOMPLEXTYPE // nested having one or more fields of any type
    };

    private EscenicTypeGroup escenicTypeGroup;
    private Type type;

    private String keyField;
    private String label;
    private Boolean visibility;
    private String xsdType;
    private String valueIfUnset;
    private HashMap<String,EscenicType> nestedType;

    private Map<String, String> alternativesList;

    public String toString()
    {
        String toString = "";

        toString = "EscenicType. Type: " + type + ", keyField: " + keyField + ", Label: " + label + ", visibility: " + visibility + ", xsdType: " + xsdType + ", valueIfUnSet: " + valueIfUnset + "; ";

        if (nestedType != null)
        {
            for(String keyField: nestedType.keySet())
                toString += nestedType.get(keyField).toString();
        }

        return toString;
    }

    public String getKeyField() {
        return keyField;
    }

    public void setKeyField(String keyField) {
        this.keyField = keyField;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(Boolean visibility) {
        this.visibility = visibility;
    }

    public String getXsdType() {
        return xsdType;
    }

    public void setXsdType(String xsdType) {
        this.xsdType = xsdType;
    }

    public String getValueIfUnset() {
        return valueIfUnset;
    }

    public void setValueIfUnset(String valueIfUnset) {
        this.valueIfUnset = valueIfUnset;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public HashMap<String, EscenicType> getNestedType() {
        return nestedType;
    }

    public void setNestedType(HashMap<String, EscenicType> nestedType) {
        this.nestedType = nestedType;
    }

    public Map<String, String> getAlternativesList() {
        return alternativesList;
    }

    public void setAlternativesList(Map<String, String> alternativesList) {
        this.alternativesList = alternativesList;
    }

    public EscenicTypeGroup getEscenicTypeGroup() {
        return escenicTypeGroup;
    }

    public void setEscenicTypeGroup(EscenicTypeGroup escenicTypeGroup) {
        this.escenicTypeGroup = escenicTypeGroup;
    }
}
