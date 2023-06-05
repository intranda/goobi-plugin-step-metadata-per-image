package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageMetadataField {

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
    }

    public void addValue(PageMetadataValue value) {
        values.add(value);
    }

    public void removeValue(PageMetadataValue value) {
        values.remove(value);
    }
}
