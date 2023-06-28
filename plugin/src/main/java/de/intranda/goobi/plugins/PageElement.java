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

    private List<PageMetadataField> metadata = new ArrayList<>();

    private List<ProcessReference> processReferences = new ArrayList<>();

    private List<ProcessReference> deletedProcessReferences = new ArrayList<>();

    @Setter
    private int rating = 0;

    private int order = 0;
    @Setter
    private ProcessReference selectedReference;

    @Setter
    private Metadata identifier;

    @Setter
    private boolean selected = false; // indicates if this page was selected for mass manipulation

    public PageElement(DocStruct docstruct, DocStruct page, Image image, int order) {
        this.docstruct = docstruct;
        this.page = page;
        this.image = image;
        this.order = order;
    }

    public void deleteProcessReference() {
        processReferences.remove(selectedReference);
        deletedProcessReferences.add(selectedReference);

    }
}
