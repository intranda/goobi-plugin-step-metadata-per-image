package de.intranda.goobi.plugins;

import lombok.Getter;
import lombok.Setter;
import ugh.dl.Metadata;

@Getter
@Setter
public class PageMetadataValue {

    private Metadata metadata;

    public PageMetadataValue(Metadata metadata) {
        this.metadata = metadata;
    }
}
