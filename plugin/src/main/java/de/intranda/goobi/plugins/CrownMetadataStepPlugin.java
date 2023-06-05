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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
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
import de.sub.goobi.persistence.managers.VocabularyManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.UGHException;

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
    private List<PageElement> pages = new ArrayList<>();

    private List<PageMetadataField> configuredFields = new ArrayList<>();

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

            if ("select".equals(field.getDisplayType())) {
                // get allowed values
                List<String> values = Arrays.asList(hc.getStringArray("/field"));
                field.setValueList(values);
            } else if ("vocabulary".equals(field.getDisplayType())) {
                List<String> values = new ArrayList<>();
                String vocabularyName = hc.getString("/vocabulary");
                Vocabulary currentVocabulary = VocabularyManager.getVocabularyByTitle(vocabularyName);
                VocabularyManager.getAllRecords(currentVocabulary);
                List<VocabRecord> recordList = currentVocabulary.getRecords();

                for (VocabRecord vr : recordList) {
                    values.add(vr.getTitle());
                }
                Collections.sort(values);
                field.setValueList(values);
            }

            configuredFields.add(field);
        }

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

                    if (field.getValues().isEmpty()) {
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

                // create field, find metadata value from docstruct or create new metadata

                pages.add(pe);
            } catch (IOException | SwapException | DAOException e) {
                log.error(e);
            }
        }

        return true;
    }

    @Override
    public PluginReturnValue run() {
        return PluginReturnValue.FINISH;
    }
}
