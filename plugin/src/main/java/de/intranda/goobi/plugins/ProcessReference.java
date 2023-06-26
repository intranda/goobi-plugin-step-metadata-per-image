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

import lombok.Getter;
import lombok.Setter;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;

@Getter
@Setter
public class ProcessReference {

    // process id
    private Metadata processId;
    // display this name as label
    private Metadata processName;

    // image number, if link is set to a specific image
    private Metadata imageNumber;

    // contains the docstruct identifier, if link is set to a specific docstruct
    private Metadata docstructId;

    // new, deleted or changed
    private Metadata status;// = "changed";

    // process id of linked process
    private Metadata otherProcessId;
    // name of linked process
    private Metadata otherProcessName;
    // image number (might be empty)
    private Metadata otherImageNumber;
    // linked docstruct id (might be empty)
    private Metadata otherDocstructId;

    private MetadataGroup group;

    public ProcessReference(MetadataGroup group) {
        this.group = group;
    }
}
