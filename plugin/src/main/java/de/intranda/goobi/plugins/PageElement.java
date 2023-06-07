package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import de.sub.goobi.metadaten.Image;
import lombok.Getter;
import lombok.Setter;
import ugh.dl.DocStruct;

@Getter
public class PageElement {

    private Image image;

    private DocStruct docstruct;

    private DocStruct page;

    private List<PageMetadataField> metadata = new ArrayList<>();

    private List<ProcessReference> processReferences = new ArrayList<>();

    @Setter
    private int rating = 0;

    private int order = 0;

    public PageElement(DocStruct docstruct, DocStruct page, Image image, int order) {
        this.docstruct = docstruct;
        this.page = page;
        this.image = image;
        this.order = order;
    }
}
