/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi-workflow
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.production.properties.MultiSelectProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.exceptions.MetadataTypeNotAllowedException;

@Getter
@Setter
@Log4j2
public class PageMetadataField implements MultiSelectProperty<String> {

    private List<PageMetadataValue> values = new ArrayList<>();

    private String label;
    private String metadataField;
    private boolean repeatable;
    private String displayType;
    private String defaultValue;
    private boolean required;
    private String validation;
    private boolean readonly;
    private String validationErrorMessage;
    private String viafSearchFields;
    private String viafDisplayFields;

    private List<String> valueList;

    private MetadataType metadataType;

    public PageMetadataField() {

    }

    public PageMetadataField(PageMetadataField other) {
        label = other.getLabel();
        metadataField = other.getMetadataField();
        repeatable = other.isRepeatable();
        displayType = other.getDisplayType();
        defaultValue = other.getDefaultValue();
        required = other.isRequired();
        validation = other.getValidation();
        readonly = other.isReadonly();
        validationErrorMessage = other.getValidationErrorMessage();
        valueList = other.getValueList();
        viafSearchFields = other.getViafSearchFields();
        viafDisplayFields = other.getViafDisplayFields();
        metadataType = other.getMetadataType();
    }

    public void addValue(PageMetadataValue value) {
        values.add(value);
    }

    public void removeValue(PageMetadataValue value) {
        values.remove(value);
    }

    // multiselect

    @Override
    public List<String> getAllSelectedValues() {
        List<String> answer = new ArrayList<>();
        for (PageMetadataValue pmv : values) {
            answer.add(pmv.getValue());
        }
        return answer;
    }

    @Override
    public List<String> getSelectValues() {
        List<String> answer = new ArrayList<>();
        for (PageMetadataValue val : values) {
            answer.add(val.getValue());
        }
        return answer;
    }

    @Override
    public void removeSelectedValue(String value) {

        for (PageMetadataValue pmv : values) {
            if (pmv.getValue().equals(value)) {
                values.remove(pmv);
                return;
            }
        }

    }

    @Override
    public List<String> getPossibleValues() {
        List<String> answer = new ArrayList<>();
        for (String possibleValue : valueList) {
            boolean found = false;
            for (PageMetadataValue val : values) {
                if (val.getValue().equals(possibleValue)) {
                    found = true;
                }
            }
            if (!found) {
                answer.add(possibleValue);
            }
        }
        return answer;
    }

    @Override
    public String getCurrentValue() {
        return MultiSelectProperty.super.getCurrentValue();
    }

    @Override
    public void setCurrentValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                Metadata md = new Metadata(metadataType);
                md.setValue(value);
                PageMetadataValue pmv = new PageMetadataValue(md, validation, required, viafSearchFields, viafDisplayFields);
                values.add(pmv);
            } catch (MetadataTypeNotAllowedException e) {
                // ignore this, it will not occur
            }
        }

    }

    public boolean isDisplayDuplicationButton() {
        return !"multiselect".equals(displayType) && repeatable;
    }

    public boolean isDisplayDeletionButton() {
        return !"multiselect".equals(displayType) && repeatable && values.size() > 1;
    }

    public void addNewValue() {
        try {
            Metadata md = new Metadata(metadataType);
            PageMetadataValue val = new PageMetadataValue(md, validation, required, viafSearchFields, viafDisplayFields);
            values.add(val);
        } catch (MetadataTypeNotAllowedException e) {
            // ignore this, it will not occur
        }
    }
}
