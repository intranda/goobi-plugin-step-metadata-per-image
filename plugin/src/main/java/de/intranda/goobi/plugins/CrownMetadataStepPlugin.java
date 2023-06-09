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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang.StringEscapeUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.Image;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.VocabularyManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class CrownMetadataStepPlugin implements IStepPluginVersion2 {

    private static final long serialVersionUID = -4190101017743019816L;

    @Getter
    private String title = "intranda_step_crownMetadata";
    @Getter
    private Step step;

    private Process process;

    private String returnPath;

    @Getter
    private transient List<Path> images = null;
    @Getter
    private transient Fileformat fileformat;

    private Prefs prefs;

    @Getter
    @Setter
    private String imageUrl;

    @Getter
    @Setter
    private String imageName;

    @Getter
    private transient List<PageElement> pages = new ArrayList<>();

    private transient List<PageMetadataField> configuredFields = new ArrayList<>();

    @Getter
    private transient List<ProcessObject> processDataList = new ArrayList<>();

    @Getter
    @Setter
    private transient ProcessObject currentProcess;

    @Getter
    @Setter
    private transient PageElement currentPage;

    @Getter
    @Setter
    private String searchValue;

    @Getter
    private transient PublicationElement publicationElement;

    // metadata names
    private MetadataType identifierField;
    private String referenceMetadataGroupName;
    private String metadataNameProcessID;
    private String metadataNameDocstructID;
    private String metadataNamePageNumber;
    private String metadataNameLabel;
    private List<String> searchFields = new ArrayList<>();
    private List<String> processDisplayFields = new ArrayList<>();

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        process = step.getProzess();
        prefs = process.getRegelsatz().getPreferences();

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);

        for (HierarchicalConfiguration hc : myconfig.configurationsAt("/field")) {
            PageMetadataField field = new PageMetadataField();
            field.setLabel(hc.getString("@label"));
            field.setMetadataField(hc.getString("@metadataField"));
            field.setRepeatable(hc.getBoolean("@repeatable", false));
            field.setDisplayType(hc.getString("@displayType", "input"));
            field.setDefaultValue(hc.getString("@defaultValue", ""));
            field.setRequired(hc.getBoolean("@required", false));
            field.setValidation(hc.getString("@validation", ""));
            field.setReadonly(hc.getBoolean("@readonly", false));
            field.setValidationErrorMessage(hc.getString("@validationErrorMessage", ""));

            field.setMetadataType(prefs.getMetadataTypeByName(hc.getString("@metadataField")));

            switch (field.getDisplayType()) {
                case "select":
                case "multiselect":
                    // get allowed values
                    List<String> values = Arrays.asList(hc.getStringArray("/field"));

                    if (values.isEmpty()) {
                        values = new ArrayList<>();
                        String vocabularyName = hc.getString("/vocabulary");
                        Vocabulary currentVocabulary = VocabularyManager.getVocabularyByTitle(vocabularyName);
                        VocabularyManager.getAllRecords(currentVocabulary);
                        List<VocabRecord> recordList = currentVocabulary.getRecords();

                        for (VocabRecord vr : recordList) {
                            values.add(vr.getTitle());
                        }
                        Collections.sort(values);
                    }
                    field.setValueList(values);
                    break;
                default:
                    break;
            }

            configuredFields.add(field);
        }

        searchFields = Arrays.asList(myconfig.getStringArray("/searchfield"));
        processDisplayFields = Arrays.asList(myconfig.getStringArray("/display/field"));

        identifierField = prefs.getMetadataTypeByName(myconfig.getString("/identifierField"));

        referenceMetadataGroupName = myconfig.getString("/reference/group");
        metadataNameProcessID = myconfig.getString("/reference/process");
        metadataNameDocstructID = myconfig.getString("/reference/docstruct");
        metadataNamePageNumber = myconfig.getString("/reference/image");
        metadataNameLabel = myconfig.getString("/reference/label");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.FULL;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_crownMetadata.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null; // NOSONAR
    }

    public void processSearch() {
        processDataList.clear();
        String query = generateSearchQuery();
        String currentProcessId = null;
        List<StringPair> metadataList = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> list = ProcessManager.runSQL(query);
        for (Object[] obj : list) {
            String processid = (String) obj[0];
            String metadataName = (String) obj[1];
            String metadataValue = (String) obj[2];

            if (currentProcessId == null) {
                currentProcessId = processid;
            }
            StringPair sp = new StringPair(metadataName, metadataValue);
            if (currentProcessId.equals(processid)) {
                // another metadata field for the current process
                metadataList.add(sp);
            } else if (!metadataList.isEmpty()) {
                // new process started
                processDataList.add(createProcessObject(currentProcessId, metadataList));
                // reset data
                currentProcessId = processid;
                metadataList = new ArrayList<>();
            }
        }
        // finally add entry for last process
        if (!metadataList.isEmpty()) {
            processDataList.add(createProcessObject(currentProcessId, metadataList));
        }
    }

    private ProcessObject createProcessObject(String currentProcessId, List<StringPair> metadataList) {
        ProcessObject po = new ProcessObject();
        po.setProcessId(currentProcessId);
        for (StringPair pair : metadataList) {
            if ("TitleDocMain".equals(pair.getOne())) {
                po.setLabel(pair.getTwo());
            }
        }
        // add fields in the configured order
        for (String displayName : processDisplayFields) {
            for (StringPair pair : metadataList) {
                if (pair.getOne().equals(displayName)) {
                    po.getMetadataList().add(pair);
                    break;
                }
            }
        }
        return po;
    }

    private String generateSearchQuery() {
        String escapedValue = StringEscapeUtils.escapeSql(searchValue);

        StringBuilder processIDBuilder = new StringBuilder();
        processIDBuilder.append("SELECT processid, name, value FROM metadata WHERE processid IN ( ");
        processIDBuilder.append("SELECT DISTINCT processid FROM metadata ");
        processIDBuilder.append("WHERE ");

        // search for the exact value in the identifier field
        processIDBuilder.append("(name = 'CatalogIDDIgital' AND value = '");
        processIDBuilder.append(escapedValue);

        // or use a contains search in the configured fields
        processIDBuilder.append("') ");
        if (!searchFields.isEmpty()) {
            StringBuilder sub = new StringBuilder();
            processIDBuilder.append("OR name IN (");
            for (String searchfield : searchFields) {
                if (sub.length() > 0) {
                    sub.append(", ");
                }
                sub.append("'");
                sub.append(searchfield);
                sub.append("'");
            }
            processIDBuilder.append(sub.toString());
            processIDBuilder.append(") AND value LIKE '%");
            processIDBuilder.append(escapedValue);
            processIDBuilder.append("%') ");
        }
        return processIDBuilder.toString();
    }

    @Override
    public boolean execute() {

        // get all images from media folder
        String folderName;
        try {
            folderName = process.getImagesTifDirectory(false);
            images = StorageProvider.getInstance().listFiles(folderName);
        } catch (IOException | SwapException e) {
            log.error(e);
            return false;
        }
        if (images == null || images.isEmpty()) {
            // no images found, abort
            Helper.setFehlerMeldung(""); // TODO
            return false;
        }
        DigitalDocument digitalDocument;
        // open metadata file
        try {
            fileformat = process.readMetadataFile();
            digitalDocument = fileformat.getDigitalDocument();
        } catch (ReadException | IOException | SwapException | PreferencesException e) {
            log.error(e);
            Helper.setFehlerMeldung(""); // TODO
            return false;
        }

        // check if images are linked in metadata file

        // if not, create pagination

        MetadatenImagesHelper mih = new MetadatenImagesHelper(prefs, digitalDocument);
        try {
            mih.createPagination(process, folderName);
        } catch (TypeNotAllowedForParentException | IOException | SwapException | DAOException e) {
            log.error(e);
            Helper.setFehlerMeldung(""); // TODO
            return false;
        }

        DocStruct logical = digitalDocument.getLogicalDocStruct();
        if (logical.getType().isAnchor()) {
            logical = logical.getAllChildren().get(0);
        }

        publicationElement = new PublicationElement();
        publicationElement.setType(logical.getType().getName());
        List<StringPair> processMetadataList = new ArrayList<>();
        for (Metadata md : logical.getAllMetadata()) {
            if ("TitleDocMain".equals(md.getType().getName())) {
                publicationElement.setTitle(md.getValue());
            } else {
                StringPair sp = new StringPair(md.getType().getName(), md.getValue());
                processMetadataList.add(sp);
            }
        }

        // add fields in the configured order
        for (String displayName : processDisplayFields) {
            for (StringPair pair : processMetadataList) {
                if (pair.getOne().equals(displayName)) {
                    publicationElement.getMetadataList().add(pair);
                    break;
                }
            }
        }

        // check if a logical docstruct exists for each image
        DocStruct physical = digitalDocument.getPhysicalDocStruct();
        int order = 0;
        for (DocStruct pageStruct : physical.getAllChildren()) {
            Path imagePath = Paths.get(pageStruct.getImageName());
            try {
                Image image = new Image(process, folderName, imagePath.getFileName().toString(), 1, 400);
                PageElement pe = null;
                for (Reference ref : pageStruct.getAllFromReferences()) {
                    // ignore reference is to logical topstruct
                    if (!ref.getSource().getType().isTopmost()) {
                        pe = new PageElement(ref.getSource(), pageStruct, image, order);
                    }
                }
                // if not, create doscstruct
                if (pe == null) {
                    try {
                        //TODO get type from config
                        DocStruct ds = digitalDocument.createDocStruct(prefs.getDocStrctTypeByName("Chapter"));
                        ds.addReferenceTo(pageStruct, "logical_physical");
                        pe = new PageElement(ds, pageStruct, image, order);
                        logical.addChild(ds);
                    } catch (UGHException e) {
                        log.error(e);
                        return false;
                    }
                }
                order++;

                // for each configured metadata field
                for (PageMetadataField configuredField : configuredFields) {
                    PageMetadataField field = new PageMetadataField(configuredField);
                    pe.getMetadata().add(field);
                    if (pe.getDocstruct().getAllMetadata() != null) {
                        for (Metadata md : pe.getDocstruct().getAllMetadata()) {
                            if (md.getType().getName().equals(field.getMetadataField())) {
                                field.addValue(new PageMetadataValue(md, field.getValidation(), field.isRequired()));
                            }
                        }
                    }

                    if (field.getValues().isEmpty() && !"multiselect".equals(field.getDisplayType())) {
                        // new metadata
                        // default value
                        try {
                            Metadata md = new Metadata(prefs.getMetadataTypeByName(field.getMetadataField()));
                            md.setValue(field.getDefaultValue());
                            pe.getDocstruct().addMetadata(md);
                            field.addValue(new PageMetadataValue(md, field.getValidation(), field.isRequired()));
                        } catch (MetadataTypeNotAllowedException e) {
                            log.error(e);
                        }

                    }
                }

                // if needed, create new docstruct identifier
                Metadata identifier = null;
                List<? extends Metadata> mdl = pageStruct.getAllMetadataByType(identifierField);
                if (!mdl.isEmpty()) {
                    identifier = mdl.get(0);
                } else {
                    identifier = new Metadata(identifierField);
                    identifier.setValue(UUID.randomUUID().toString());
                    pageStruct.addMetadata(identifier);
                }
                pe.setIdentifier(identifier);

                // TODO metadata for rating

                pages.add(pe);
            } catch (IOException | SwapException | DAOException | UGHException e) {
                log.error(e);
            }
        }

        return true;
    }

    public void saveMetadata() {
        // make sure all new metadata is assigned to the docstruct
        for (PageElement pe : pages) {
            for (PageMetadataField field : pe.getMetadata()) {
                for (PageMetadataValue val : field.getValues()) {
                    if (val.getMetadata().getParent() == null) {
                        try {
                            pe.getDocstruct().addMetadata(val.getMetadata());
                        } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                            log.error(e);
                        }
                    }
                }
            }
            // create metadata group for each reference
            for (ProcessReference ref : pe.getProcessReferences()) {
                // if reference is marked as new, open other process, add reference

                // if reference is marked as delete, open other process, remove reference

                // otherwise just store the metadata

            }

        }

        try {
            process.writeMetadataFile(fileformat);
        } catch (WriteException | PreferencesException | IOException | SwapException e) {
            log.error(e);
        }

    }

    @Override
    public PluginReturnValue run() {
        return PluginReturnValue.FINISH;
    }

    public void addReference() {
        String otherProcessId = currentProcess.getProcessId();
        String otherProcessTitle = currentProcess.getLabel();

        String processid = String.valueOf(process.getId());
        String identifier = currentPage.getIdentifier().getValue();

        // get current Page

        // add reference to other process in page object

        ProcessReference reference = new ProcessReference();
        reference.setStatus("new");
        reference.setProcessId(processid);
        reference.setDocstructId(identifier); // identifier of selected docstruct
        reference.setProcessName(publicationElement.getTitle()); //main title
        reference.setImageNumber(String.valueOf(currentPage.getOrder()));

        reference.setOtherProcessId(otherProcessId);
        reference.setOtherProcessName(otherProcessTitle);
        reference.setOtherDocstructId(null); // keep it empty, we link to the process itself
        reference.setOtherImageNumber(null);// keep it empty, we link to the process itself

        currentPage.getProcessReferences().add(reference);

        // reset search results, searchvalue
        searchValue = "";
        processDataList.clear();
    }

}
