package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import org.goobi.production.cli.helper.StringPair;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessObject {


    private String processId;
    private List<StringPair> metadataList = new ArrayList<>();

}
