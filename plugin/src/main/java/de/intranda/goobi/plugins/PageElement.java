package de.intranda.goobi.plugins;

import java.util.List;

import de.sub.goobi.metadaten.Image;
import lombok.Getter;
import lombok.Setter;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;

@Getter
public class PageElement {

    private Image image;

    private DocStruct docstruct;

    private DocStruct page;

    private List<Metadata> metadata;

    @Setter
    private int rating = 0;

    public PageElement(DocStruct docstruct, DocStruct page, Image image) {
        this.docstruct = docstruct;
        this.page = page;
        this.image = image;
    }
}
