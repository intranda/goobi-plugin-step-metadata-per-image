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

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.HelperForm;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.metadaten.MetadatenHelper;
import de.sub.goobi.persistence.managers.MetadataManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import io.goobi.workflow.api.vocabulary.VocabularyAPIManager;
import org.easymock.EasyMock;
import org.goobi.beans.Process;
import org.goobi.beans.Project;
import org.goobi.beans.Ruleset;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.fileformats.mets.MetsMods;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MetadatenHelper.class, VariableReplacer.class, ConfigurationHelper.class, ProcessManager.class,
        MetadataManager.class, Helper.class, HelperForm.class, VocabularyAPIManager.class })
@PowerMockIgnore({ "javax.management.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "javax.net.ssl.*", "jdk.internal.reflect.*" })
public class MetadataPerImagePluginTest {

    private static String resourcesFolder;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File processDirectory;
    private File metadataDirectory;
    private Process process;
    private Step step;
    private Prefs prefs;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse

        PowerMock.mockStatic(VocabularyAPIManager.class);
        VocabularyAPIManager vocabularyAPIManager = EasyMock.createMock(VocabularyAPIManager.class);
        EasyMock.expect(VocabularyAPIManager.getInstance()).andReturn(vocabularyAPIManager).anyTimes();
        PowerMock.replay(VocabularyAPIManager.class);

        System.setProperty("log4j.configurationFile", log4jFile);
    }

    @Test
    public void testConstructor() throws Exception {
        MetadataPerImageStepPlugin plugin = new MetadataPerImageStepPlugin();
        assertNotNull(plugin);
    }

    @Test
    public void testInit() {
        MetadataPerImageStepPlugin plugin = new MetadataPerImageStepPlugin();
        plugin.initialize(step, "something");
        assertEquals(step.getTitel(), plugin.getStep().getTitel());
    }

    @Test
    public void testOpenRecord() {

        MetadataPerImageStepPlugin plugin = new MetadataPerImageStepPlugin();
        plugin.initialize(step, "something");

        assertTrue(plugin.execute());

        assertEquals(4, plugin.getImages().size());
        assertNotNull(plugin.getFileformat());
        assertEquals(4, plugin.getPages().size());
    }

    @Before
    public void setUp() throws Exception {
        metadataDirectory = folder.newFolder("metadata");
        processDirectory = new File(metadataDirectory + File.separator + "1");
        processDirectory.mkdirs();
        String metadataDirectoryName = metadataDirectory.getAbsolutePath() + File.separator;
        Path metaSource = Paths.get(resourcesFolder, "meta.xml");
        Path metaTarget = Paths.get(processDirectory.getAbsolutePath(), "meta.xml");
        Files.copy(metaSource, metaTarget);

        Path anchorSource = Paths.get(resourcesFolder, "meta_anchor.xml");
        Path anchorTarget = Paths.get(processDirectory.getAbsolutePath(), "meta_anchor.xml");
        Files.copy(anchorSource, anchorTarget);

        PowerMock.mockStatic(ConfigurationHelper.class);
        ConfigurationHelper configurationHelper = EasyMock.createMock(ConfigurationHelper.class);
        EasyMock.expect(ConfigurationHelper.getInstance()).andReturn(configurationHelper).anyTimes();
        EasyMock.expect(configurationHelper.getMetsEditorLockingTime()).andReturn(1800000l).anyTimes();
        EasyMock.expect(configurationHelper.isAllowWhitespacesInFolder()).andReturn(false).anyTimes();
        EasyMock.expect(configurationHelper.useS3()).andReturn(false).anyTimes();
        EasyMock.expect(configurationHelper.isUseProxy()).andReturn(false).anyTimes();
        EasyMock.expect(configurationHelper.getGoobiContentServerTimeOut()).andReturn(60000).anyTimes();
        EasyMock.expect(configurationHelper.getMetadataFolder()).andReturn(metadataDirectoryName).anyTimes();
        EasyMock.expect(configurationHelper.getRulesetFolder()).andReturn(resourcesFolder).anyTimes();
        EasyMock.expect(configurationHelper.getProcessImagesMainDirectoryName()).andReturn("00469418X_media").anyTimes();
        EasyMock.expect(configurationHelper.isUseMasterDirectory()).andReturn(true).anyTimes();
        EasyMock.expect(configurationHelper.getConfigurationFolder()).andReturn(resourcesFolder).anyTimes();
        EasyMock.expect(configurationHelper.getNumberOfMetaBackups()).andReturn(0).anyTimes();
        EasyMock.expect(configurationHelper.getImagePrefix()).andReturn("\\d{8}").anyTimes();
        EasyMock.expect(configurationHelper.getMetsEditorDefaultPagination()).andReturn("uncounted").anyTimes();
        EasyMock.replay(configurationHelper);

        PowerMock.mockStatic(Helper.class);
        //        EasyMock.expect(Helper.getTranslation(EasyMock.anyString())).andReturn("").anyTimes();
        //        EasyMock.expect(Helper.getMetadataLanguage()).andReturn("en").anyTimes();
        //        EasyMock.expect(Helper.getLoginBean()).andReturn(null).anyTimes();
        //        EasyMock.expect(Helper.getCurrentUser()).andReturn(null).anyTimes();
        //        EasyMock.expect(Helper.getTranslation(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString())).andReturn("").anyTimes();
        Helper.setFehlerMeldung(EasyMock.anyString());
        EasyMock.expect(Helper.getTranslation(EasyMock.anyString())).andReturn("fixture").anyTimes();
        PowerMock.replay(Helper.class);

        FacesContext facesContext = EasyMock.createMock(FacesContext.class);
        ExternalContext externalContext = EasyMock.createMock(ExternalContext.class);
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        FacesContextHelper.setFacesContext(facesContext);
        EasyMock.expect(facesContext.getExternalContext()).andReturn(externalContext).anyTimes();
        EasyMock.expect(externalContext.getRequest()).andReturn(request).anyTimes();
        EasyMock.expect(request.getScheme()).andReturn("http://").anyTimes();

        EasyMock.expect(request.getServerName()).andReturn("example.com").anyTimes();
        EasyMock.expect(request.getServerPort()).andReturn(80).anyTimes();
        EasyMock.expect(request.getContextPath()).andReturn("goobi").anyTimes();
        EasyMock.replay(request);
        EasyMock.replay(externalContext);
        EasyMock.replay(facesContext);

        PowerMock.mockStatic(VariableReplacer.class);
        EasyMock.expect(VariableReplacer.simpleReplace(EasyMock.anyString(), EasyMock.anyObject())).andReturn("00469418X_media").anyTimes();
        PowerMock.replay(VariableReplacer.class);
        prefs = new Prefs();
        prefs.loadPrefs(resourcesFolder + "ruleset.xml");
        Fileformat ff = new MetsMods(prefs);
        ff.read(metaTarget.toString());

        PowerMock.mockStatic(MetadatenHelper.class);
        EasyMock.expect(MetadatenHelper.getMetaFileType(EasyMock.anyString())).andReturn("mets").anyTimes();
        EasyMock.expect(MetadatenHelper.getFileformatByName(EasyMock.anyString(), EasyMock.anyObject())).andReturn(ff).anyTimes();
        EasyMock.expect(MetadatenHelper.getMetadataOfFileformat(EasyMock.anyObject(), EasyMock.anyBoolean()))
                .andReturn(Collections.emptyMap())
                .anyTimes();
        PowerMock.replay(MetadatenHelper.class);

        PowerMock.mockStatic(MetadataManager.class);
        MetadataManager.updateMetadata(1, Collections.emptyMap());
        MetadataManager.updateJSONMetadata(1, Collections.emptyMap());
        PowerMock.replay(MetadataManager.class);
        PowerMock.replay(ConfigurationHelper.class);

        process = getProcess();

        Ruleset ruleset = PowerMock.createMock(Ruleset.class);
        ruleset.setTitel("ruleset");
        ruleset.setDatei("ruleset.xml");
        EasyMock.expect(ruleset.getDatei()).andReturn("ruleset.xml").anyTimes();
        process.setRegelsatz(ruleset);
        EasyMock.expect(ruleset.getPreferences()).andReturn(prefs).anyTimes();
        PowerMock.replay(ruleset);

    }

    public Process getProcess() {
        Project project = new Project();
        project.setTitel("SampleProject");

        Process process = new Process();
        process.setTitel("00469418X");
        process.setProjekt(project);
        process.setId(1);
        List<Step> steps = new ArrayList<>();
        step = new Step();
        step.setReihenfolge(1);
        step.setProzess(process);
        step.setTitel("test step");
        step.setBearbeitungsstatusEnum(StepStatus.OPEN);
        User user = new User();
        user.setVorname("Firstname");
        user.setNachname("Lastname");
        step.setBearbeitungsbenutzer(user);
        steps.add(step);

        process.setSchritte(steps);

        try {
            createProcessDirectory(processDirectory);
        } catch (IOException e) {
        }

        return process;
    }

    private void createProcessDirectory(File processDirectory) throws IOException {

        // image folder
        File imageDirectory = new File(processDirectory.getAbsolutePath(), "images");
        imageDirectory.mkdir();
        // master folder
        File masterDirectory = new File(imageDirectory.getAbsolutePath(), "00469418X_master");
        masterDirectory.mkdir();

        // media folder
        File mediaDirectory = new File(imageDirectory.getAbsolutePath(), "00469418X_media");
        mediaDirectory.mkdir();

        // add some files
        Path image1 = Paths.get(resourcesFolder, "00000001.tif");
        Path image2 = Paths.get(resourcesFolder, "00000002.tif");
        Path image3 = Paths.get(resourcesFolder, "00000003.tif");
        Path image4 = Paths.get(resourcesFolder, "00000004.tif");

        Files.copy(image1, Paths.get(mediaDirectory.getAbsolutePath(), "00000001.tif"));
        Files.copy(image2, Paths.get(mediaDirectory.getAbsolutePath(), "00000002.tif"));
        Files.copy(image3, Paths.get(mediaDirectory.getAbsolutePath(), "00000003.tif"));
        Files.copy(image4, Paths.get(mediaDirectory.getAbsolutePath(), "00000004.tif"));

    }
}
