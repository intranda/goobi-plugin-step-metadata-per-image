package de.intranda.goobi.plugins;

import org.apache.commons.lang.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageMetadataValue {

    private String value;
    private String validationMessage;

    private boolean required;
    private String validationRegex;

    public PageMetadataValue(String value, String validationRegex, boolean required) {
        this.value = value;
        this.validationRegex = validationRegex;
        this.required = required;
    }

    public boolean isValid() {
        if (required && StringUtils.isBlank(value)) {
            return false;
        } else if (StringUtils.isNotBlank(validationMessage) && StringUtils.isNotBlank(value)) {
            if (!value.matches(validationMessage)) {
                return false;
            }
        }
        return true;
    }
}
