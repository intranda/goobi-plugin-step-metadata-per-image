package de.intranda.goobi.plugins;

import org.apache.commons.lang.StringUtils;

import lombok.Getter;
import lombok.Setter;
import ugh.dl.Metadata;

@Getter
@Setter
public class PageMetadataValue {

    private String validationMessage;
    private Metadata metadata;

    private boolean required;
    private String validationRegex;

    public PageMetadataValue(Metadata metadata, String validationRegex, boolean required) {
        this.metadata = metadata;
        this.validationRegex = validationRegex;
        this.required = required;
    }

    public boolean isValid() {
        if (required && StringUtils.isBlank(getValue())) {
            return false;
        } else if (StringUtils.isNotBlank(validationMessage) && StringUtils.isNotBlank(getValue()) && !getValue().matches(validationMessage)) {
            return false;
        }
        return true;
    }

    public String getValue() {
        return metadata.getValue();
    }

    public void setValue(String value) {
        metadata.setValue(value);
    }
}
