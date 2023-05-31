package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Path;

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
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Fileformat;
import ugh.exceptions.ReadException;

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
    private Fileformat fileformat;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;
        process = step.getProzess();
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

        try {
            String folderName = process.getImagesTifDirectory(false);
            images = StorageProvider.getInstance().listFiles(folderName);
        } catch (IOException | SwapException e) {
            log.error(e);
        }
        if (images == null || images.isEmpty()) {
            // no images found, abort
            Helper.setFehlerMeldung(""); // TODO
            return false;
        }

        // open metadata file
        try {
            fileformat = process.readMetadataFile();
        } catch (ReadException | IOException | SwapException e) {
            log.error(e);
            Helper.setFehlerMeldung(""); // TODO
            return false;
        }

        // check if images are linked in metadata file

        // if not, create pagination

        // create ics urls for each image

        return true;
    }

    @Override
    public PluginReturnValue run() {
        return PluginReturnValue.FINISH;
    }
}
