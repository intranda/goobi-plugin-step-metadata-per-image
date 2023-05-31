package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

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

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;

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
    private List<Path> images = null;
    @Getter
    private transient Fileformat fileformat;
    private transient DigitalDocument digitalDocument;

    private Prefs prefs;

    @Getter
    private List<PageElement> pages = new ArrayList<>();

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        process = step.getProzess();
        prefs = process.getRegelsatz().getPreferences();

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);

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
        for (DocStruct pageStruct : physical.getAllChildren()) {
            PageElement pe = null;
            for (Reference ref : pageStruct.getAllFromReferences()) {
                // ignore reference is to logical topstruct
                if (!ref.getSource().getType().isTopmost()) {
                    pe = new PageElement(ref.getSource(), pageStruct);
                }
            }
            // if not, create doscstruct

            if (pe == null) {
                try {
                    DocStruct ds = digitalDocument.createDocStruct(prefs.getDocStrctTypeByName("Chapter")); //TODO get type from config?
                    ds.addReferenceTo(pageStruct, "logical_physical");
                    pe = new PageElement(ds, pageStruct);
                } catch (TypeNotAllowedForParentException e) {
                    log.error(e);
                }
            }
            pages.add(pe);
        }

        return true;
    }

    @Override
    public PluginReturnValue run() {
        return PluginReturnValue.FINISH;
    }
}
