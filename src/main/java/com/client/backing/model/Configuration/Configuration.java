package com.client.backing.model.Configuration;

import com.client.backing.model.common.ContentType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 22/12/15
 * Time: 05:32
 * To change this template use File | Settings | File Templates.
 */
public class Configuration implements Serializable {

    private String groupName;

    private List<String> groupUsers = new ArrayList<>();

    private boolean readOnly = true;
    private List<String> sectionsUniqueNames = new ArrayList<>();

    private List<ContentType> selectedContentTypes = new ArrayList<>();

    private HashMap<String,List<FieldConfiguration>> fieldsConfigurations = new HashMap<>();


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getGroupUsers() {
        return groupUsers;
    }

    public void setGroupUsers(List<String> groupUsers) {
        this.groupUsers = groupUsers;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<String> getSectionsUniqueNames() {
        return sectionsUniqueNames;
    }

    public void setSectionsUniqueNames(List<String> sectionsUniqueNames) {
        this.sectionsUniqueNames = sectionsUniqueNames;
    }

    public boolean isAllowedSection (String sectionUniqueName)
    {
        // Logger.getLogger(Configuration.class).error("isAllowedSection: " + sectionUniqueName);

        for (String localSectionUniqueName: getSectionsUniqueNames())
        {
            if (localSectionUniqueName.equalsIgnoreCase(sectionUniqueName))
                return true;
        }

        return false;
    }

    public List<ContentType> getSelectedContentTypes() {
        return selectedContentTypes;
    }

    public void setSelectedContentTypes(List<ContentType> selectedContentTypes) {
        this.selectedContentTypes = selectedContentTypes;
    }

    public HashMap<String, List<FieldConfiguration>> getFieldsConfigurations() {
        return fieldsConfigurations;
    }

    public void setFieldsConfigurations(HashMap<String, List<FieldConfiguration>> fieldsConfigurations) {
        this.fieldsConfigurations = fieldsConfigurations;
    }
}
