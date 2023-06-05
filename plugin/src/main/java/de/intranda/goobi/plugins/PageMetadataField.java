package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import org.goobi.production.properties.MultiSelectProperty;

import lombok.Getter;
import lombok.Setter;
import ugh.dl.MetadataType;

@Getter
@Setter
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
        PageMetadataValue pmv = new PageMetadataValue(value, validation, required);
        values.add(pmv);

    }
}
