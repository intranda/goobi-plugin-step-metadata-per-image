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
