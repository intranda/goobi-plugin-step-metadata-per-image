package de.intranda.goobi.plugins;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessReference {

    // process id
    private Integer processId;
    // display this name as label
    private String processName;

    // image number, if link is set to a specific image
    private Integer imageNumber;

}
