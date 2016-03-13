package com.client.service;

import com.client.service.util.ComplexColumnModel;
import com.client.service.util.ComplexValue;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 14/11/15
 * Time: 15:24
 * To change this template use File | Settings | File Templates.
 */
public class EscenicField implements Serializable {

    private static final Logger mLogger = Logger.getLogger(EscenicField.class);

    public enum ValueType {
        ESCENIC_STRINGVALUE,
        ESCENIC_BOOLEANVALUE,
        ESCENIC_LONGVALUE,
        ESCENIC_DECIMALVALUE,
        ESCENIC_DATEVALUE,
        ESCENIC_LINKVALUE,   // like the binary field in a picture
        ESCENIC_LISTLINKVALUE,
        ESCENIC_LISTCOMPLEXVALUE
    };

    public enum ChangeType {
        ESCENIC_NOCHANGE,
        ESCENIC_MODIFIED,
        ESCENIC_ADDED
    }

    // lock by some other person
    private EscenicLock externalLock;
    // lock by yourself
    private String myPrivateLockURL;

    private ValueType valueType;
    private EscenicType escenicType;

    private String fieldName;
    private Boolean readOnly;

    private String stringValue;
    private Boolean booleanValue;
    private Long longValue;
    private Float decimalValue;
    private Date dateValue;
    private EscenicLink linkValue;
    private List<EscenicLink> listLinkValues;

    private List<ComplexValue> listComplexValue;
    private List<ComplexColumnModel> listComplexColumns;

    private ChangeType changeType = ChangeType.ESCENIC_NOCHANGE;


    static public EscenicField createEmptyEscenicField(String modelKeyField, EscenicType escenicType)
    {
        EscenicField.ValueType escenicFieldValueType;

        if (escenicType.getType() == EscenicType.Type.ESCENIC_SIMPLE)
        {
            if (escenicType.getXsdType() != null && !escenicType.getXsdType().equalsIgnoreCase(""))
            {
                if (escenicType.getXsdType().equalsIgnoreCase("string"))
                {
                    escenicFieldValueType = ValueType.ESCENIC_STRINGVALUE;
                }
                else if (escenicType.getXsdType().equalsIgnoreCase("boolean"))
                {
                    escenicFieldValueType = ValueType.ESCENIC_BOOLEANVALUE;
                }
                else if (escenicType.getXsdType().equalsIgnoreCase("datetime"))
                {
                    escenicFieldValueType = ValueType.ESCENIC_DATEVALUE;
                }
                else if (escenicType.getXsdType().equalsIgnoreCase("link"))
                {
                    escenicFieldValueType = ValueType.ESCENIC_LINKVALUE;
                }
                else if (escenicType.getXsdType().equalsIgnoreCase("long"))
                {
                    escenicFieldValueType = ValueType.ESCENIC_LONGVALUE;
                }
                else if (escenicType.getXsdType().equalsIgnoreCase("decimal"))
                {
                    // this check using the 'value' to find out if it is a decimal or long
                    // it is not true (confirmed by the documentation as well).
                    // if (escenicType.getValue() != null && !escenicType.getValue().equalsIgnoreCase("") &&
                    //        Integer.parseInt(escenicType.getValue()) > 0)
                    {
                        escenicFieldValueType = ValueType.ESCENIC_DECIMALVALUE;
                    }
                    /*
                    else
                    {
                        escenicFieldValueType = ValueType.ESCENIC_LONGVALUE;
                    }
                    */
                }
                else
                {
                    Logger.getLogger(EscenicField.class).error("xsdtype unknown: " + escenicType.getXsdType());

                    return null;
                }
            }
            else
            {
                // in case of reserved Escenic field the error is not logged
                if (modelKeyField.equalsIgnoreCase("tags"))
                    return null;

                Logger.getLogger(EscenicField.class).error("modelKeyField: " + modelKeyField + ", xsdtype unknown: " + escenicType.getXsdType());

                return null;
            }
        }
        else if (escenicType.getType() == EscenicType.Type.ESCENIC_NESTEDSTRINGTYPE)
        {
            escenicFieldValueType = EscenicField.ValueType.ESCENIC_LISTCOMPLEXVALUE;
        }
        else
        {
            escenicFieldValueType = EscenicField.ValueType.ESCENIC_LISTCOMPLEXVALUE;
        }


        EscenicField escenicField = new EscenicField();

        escenicField.setFieldName(modelKeyField);
        escenicField.setValueType(escenicFieldValueType);
        if (escenicFieldValueType == ValueType.ESCENIC_LINKVALUE)
            escenicField.setReadOnly(true);
        else
            escenicField.setReadOnly(false);
        escenicField.setEscenicType(escenicType);
        escenicField.setChangeType(EscenicField.ChangeType.ESCENIC_NOCHANGE);

        return escenicField;
    }

    static public ComplexValue createEmptyComplexValue(EscenicField initialEscenicField, HashMap<String,EscenicType> complexModel)
    {
        ComplexValue complexValue = new ComplexValue();

        complexValue.setInitialEscenicField(initialEscenicField);

        for (String modelKeyField: complexModel.keySet())
        {
            EscenicType escenicType = complexModel.get(modelKeyField);

            EscenicField escenicField = EscenicField.createEmptyEscenicField (modelKeyField, escenicType);

            if (escenicField == null)
            {
                Logger.getLogger(EscenicField.class).error("escenicField was not created");

                continue;
            }

            complexValue.getMetadataFields().put(modelKeyField, escenicField);

            // mLogger.error("Add the modelKeyField '" + modelKeyField + "' to the complex field");
        }

        return complexValue;
    }

    public String toString()
    {
        String toString = "";

        if (getValueType() == ValueType.ESCENIC_STRINGVALUE)
            toString = "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", StringValue: " + getStringValue() + " ";
        else if (getValueType() == ValueType.ESCENIC_BOOLEANVALUE)
            toString = "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", BooleanValue: " + getBooleanValue() + " ";
        else if (getValueType() == ValueType.ESCENIC_LONGVALUE)
            toString = "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", longValue: " + getLongValue() + " ";
        else if (getValueType() == ValueType.ESCENIC_DECIMALVALUE)
            toString = "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", decimalValue: " + getDecimalValue() + " ";
        else if (getValueType() == ValueType.ESCENIC_LINKVALUE)
        {
            if (linkValue != null)
                toString += "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", linkValue. Rel: " + linkValue.getRel() + ", Href: " + linkValue.getHref() + " ";
            else
                toString += "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", linkValue: " + linkValue + " ";
        }
        else if (getValueType() == ValueType.ESCENIC_DATEVALUE)
        {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String fieldValue = null;

            if (getDateValue() != null)
                fieldValue = dateFormat.format(getDateValue());

            toString = "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", dateValue: " + fieldValue + " ";
        }
        else if (getValueType() == ValueType.ESCENIC_LISTLINKVALUE)
        {
            for (EscenicLink escenicLink: getListLinkValues())
            {
                toString += "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", linkValue. Rel: " + escenicLink.getRel() + ", Href: " + escenicLink.getHref() + " ";
            }
        }
        else if (getValueType() == ValueType.ESCENIC_LISTCOMPLEXVALUE)
        {
            if (getListComplexValue() != null)
            {
                for (ComplexValue complexValue: getListComplexValue())
                {
                    for (String key: complexValue.getMetadataFields().keySet())
                    {
                        toString += "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", ComplexValue: " + complexValue.getMetadataFields().get(key).toString() + " ";
                    }
                }
            }
            else
            {
                toString += "EscenicField. ChangeType: " + getChangeType() + ", Name: " + getFieldName() + ", ComplexValue: null ";
            }
        }
        else
        {
            toString = "Unknown type: " + getValueType() + " ";
        }

        return toString;
    }

    public EscenicLock getExternalLock() {
        return externalLock;
    }

    public void setExternalLock(EscenicLock externalLock) {
        this.externalLock = externalLock;
    }

    public String getMyPrivateLockURL() {
        return myPrivateLockURL;
    }

    public void setMyPrivateLockURL(String myPrivateLockURL) {
        this.myPrivateLockURL = myPrivateLockURL;
    }

    public EscenicType getEscenicType() {
        return escenicType;
    }

    public void setEscenicType(EscenicType escenicType) {
        this.escenicType = escenicType;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        if ((stringValue == null && this.stringValue == null) ||
            (stringValue != null && this.stringValue != null && stringValue.compareTo(this.stringValue) == 0) ||
            (stringValue == null && this.stringValue != null && this.stringValue.equalsIgnoreCase("")) ||
            (stringValue != null && stringValue.equalsIgnoreCase("") && this.stringValue == null)
                )
            return;

        // mLogger.error("fieldName: " + fieldName + ", String field modified. New: " + stringValue + ", Old: " + this.stringValue);

        setChangeType(ChangeType.ESCENIC_MODIFIED);

        this.stringValue = stringValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        if ((booleanValue == null && this.booleanValue == null) ||
            (booleanValue != null && this.booleanValue != null && booleanValue.booleanValue() == this.booleanValue.booleanValue()))
            return;

        // mLogger.error("fieldName: " + fieldName + ", Boolean field modified. New: " + booleanValue + ", Old: " + this.booleanValue);

        setChangeType(ChangeType.ESCENIC_MODIFIED);

        if (booleanValue == null)
            this.booleanValue = false;
        else
            this.booleanValue = booleanValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        if ((longValue == null && this.longValue == null) ||
            (longValue != null && this.longValue != null && longValue.longValue() == this.longValue.longValue()))
            return;

        // mLogger.error("fieldName: " + fieldName + ", Long field modified. New: " + longValue + ", Old: " + this.longValue);

        setChangeType(ChangeType.ESCENIC_MODIFIED);

        this.longValue = longValue;
    }

    public Float getDecimalValue() {
        return decimalValue;
    }

    public void setDecimalValue(Float decimalValue) {
        if (decimalValue == this.decimalValue)
            return;

        // mLogger.error("fieldName: " + fieldName + ", Decimal field modified. New: " + decimalValue + ", Old: " + this.decimalValue);

        setChangeType(ChangeType.ESCENIC_MODIFIED);

        this.decimalValue = decimalValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        if ((dateValue == null && this.dateValue == null) ||
            (dateValue != null && this.dateValue != null && dateValue.getTime() == this.dateValue.getTime()))
            return;

        // mLogger.error("fieldName: " + fieldName + ", Date field modified. New: " + dateValue + ", Old: " + this.dateValue);

        setChangeType(ChangeType.ESCENIC_MODIFIED);

        this.dateValue = dateValue;
    }

    public EscenicLink getLinkValue() {
        return linkValue;
    }

    public void setLinkValue(EscenicLink linkValue) {
        this.linkValue = linkValue;
    }

    public List<EscenicLink> getListLinkValues() {
        return listLinkValues;
    }

    public void setListLinkValues(List<EscenicLink> listLinkValues) {
        setChangeType(ChangeType.ESCENIC_MODIFIED);

        this.listLinkValues = listLinkValues;
    }

    public List<ComplexValue> getListComplexValue() {
        return listComplexValue;
    }

    public void setListComplexValue(List<ComplexValue> listComplexValue) {
        setChangeType(ChangeType.ESCENIC_MODIFIED);

        this.listComplexValue = listComplexValue;
    }

    public List<ComplexColumnModel> getListComplexColumns()
    {
        List<ComplexColumnModel> listComplexColumns = new ArrayList<>();


        for (String keyField: escenicType.getNestedType().keySet())
        {
            ComplexColumnModel complexColumnModel = new ComplexColumnModel();

            complexColumnModel.setHeader(escenicType.getNestedType().get(keyField).getLabel());
            complexColumnModel.setProperty(keyField);

            listComplexColumns.add(complexColumnModel);

            // mLogger.info("Header: " + escenicType.getNestedType().get(keyField).getLabel());
            // mLogger.info("Property/keyField: " + keyField);
        }

        // mLogger.info("fieldName: " + getFieldName() + ", listComplexColumns.size: " + listComplexColumns.size());

        return listComplexColumns;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {

        // mLogger.error("fieldName: " + fieldName + ", setChangeType from: " + this.changeType + " to " + changeType);

        this.changeType = changeType;
    }
}
