package de.intranda.goobi.plugins;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessReference {

    // process id
    private String processId;
    // display this name as label
    private String processName;

    // image number, if link is set to a specific image
    private String imageNumber;

    // contains the docstruct identifier, if link is set to a specific docstruct
    private String docstructId;

    // new, deleted or changed
    private String status = "changed";

    // process id of linked process
    private String otherProcessId;
    // name of linked process
    private String otherProcessName;
    // image number (might be empty)
    private String otherImageNumber;
    // linked docstruct id (might be empty)
    private String otherDocstructId;


}
