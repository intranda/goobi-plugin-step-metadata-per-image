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

import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.goobi.production.properties.GeonamesSearchProperty;
import org.goobi.production.properties.GndSearchProperty;
import org.goobi.production.properties.ViafSearchProperty;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.metadaten.search.ViafSearch;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ugh.dl.Metadata;

@Getter
@Setter
@Log4j2
public class PageMetadataValue implements GndSearchProperty, GeonamesSearchProperty, ViafSearchProperty {

    private String validationMessage;
    private Metadata metadata;
    private boolean required;
    private String validationRegex;

    // gnd search fields
    private String searchValue;
    private String searchOption;
    private List<List<NormData>> dataList;
    private List<NormData> currentData;
    private boolean showNoHits;

    // geonames search fields
    private Toponym currentToponym;
    private List<Toponym> resultList;
    private int totalResults;

    // viaf search fields
    private ViafSearch viafSearch = new ViafSearch();
    private String viafSearchFields;
    private String viafDisplayFields;

    public PageMetadataValue(Metadata metadata, String validationRegex, boolean required, String viafSearchFields, String viafDisplayFields) {
        this.metadata = metadata;
        this.validationRegex = validationRegex;
        this.required = required;
        this.viafSearchFields = viafSearchFields;
        this.viafDisplayFields = viafDisplayFields;
    }

    public boolean isValid() {
        if (required && StringUtils.isBlank(getValue())) {
            // required field is empty
            return false;
        } else if (StringUtils.isNotBlank(validationRegex) && StringUtils.isNotBlank(getValue()) && !getValue().matches(validationRegex)) {
            // configured regular expression does not match with value
            return false;
        }
        return true;
    }

    @Override
    public String getValue() {
        return metadata.getValue();
    }

    @Override
    public void setValue(String value) {
        metadata.setValue(value);
    }

    @Override
    public String getGeonamesNumber() {
        return metadata.getAuthorityValue();
    }

    @Override
    public void setGeonamesNumber(String number) {
        metadata.setAuthorityFile("geonames", "http://www.geonames.org/", number);
    }

    @Override
    public String getViafNumber() {
        return metadata.getAuthorityValue();
    }

    @Override
    public void setViafNumber(String number) {
        // do nothing
    }

    @Override
    public void importGeonamesData() {
        metadata.setValue(currentToponym.getName());
        metadata.setAuthorityFile("geonames", "http://www.geonames.org/", "" + currentToponym.getGeoNameId());

        currentToponym = null;
        resultList = null;
        totalResults = 0;
    }

    @Override
    public String getGndNumber() {
        return metadata.getAuthorityValue();
    }

    @Override
    public void setGndNumber(String arg0) {
        // do nothing
    }

    @Override
    public void importGndData() {
        for (NormData normdata : currentData) {
            if ("NORM_IDENTIFIER".equals(normdata.getKey())) {
                String gndNumber = normdata.getValues().get(0).getText();
                metadata.setAuthorityFile("gnd", "http://d-nb.info/gnd/", gndNumber);
            } else if ("NORM_NAME".equals(normdata.getKey())) {
                String value = normdata.getValues().get(0).getText();
                metadata.setValue(value);
            }
        }
    }

    @Override
    public void importViafData() {
        viafSearch.getMetadata(metadata);
    }

    @Override
    public void searchGnd() {
        String val = "";
        if (StringUtils.isBlank(getSearchOption()) && StringUtils.isBlank(getSearchValue())) {
            setShowNoHits(true);
            return;
        }
        if (StringUtils.isBlank(getSearchOption())) {
            val = "dnb.nid=" + searchValue;
        } else {
            val = searchValue + " and BBG=" + searchOption;
        }
        URL url = convertToURLEscapingIllegalCharacters("http://normdata.intranda.com/normdata/gnd/woe/" + val);
        String string = url.toString()
                .replace("Ä", "%C3%84")
                .replace("Ö", "%C3%96")
                .replace("Ü", "%C3%9C")
                .replace("ä", "%C3%A4")
                .replace("ö", "%C3%B6")
                .replace("ü", "%C3%BC")
                .replace("ß", "%C3%9F");
        if (ConfigurationHelper.getInstance().isUseProxy()) {
            setDataList(NormDataImporter.importNormDataList(string, 3, ConfigurationHelper.getInstance().getProxyUrl(),
                    ConfigurationHelper.getInstance().getProxyPort()));
        } else {
            setDataList(NormDataImporter.importNormDataList(string, 3, null, 0));
        }
        setShowNoHits(getDataList() == null || getDataList().isEmpty());
    }

    @Override
    public void searchGeonames() {
        String credentials = ConfigurationHelper.getInstance().getGeonamesCredentials();
        if (credentials != null) {
            WebService.setUserName(credentials);
            ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
            searchCriteria.setNameEquals(searchValue);
            searchCriteria.setStyle(Style.FULL);

            try {
                ToponymSearchResult searchResult = WebService.search(searchCriteria);
                resultList = searchResult.getToponyms();
                totalResults = searchResult.getTotalResultsCount();
            } catch (Exception e) {
                log.error(e);
            }
            setShowNoHits(getResultList() == null || getResultList().isEmpty());
        } else {
            Helper.setFehlerMeldung("geonamesList", "Missing data", "mets_geoname_account_inactive");
        }
    }

    @Override
    public void searchViaf() {
        viafSearch.setSource(viafSearchFields);
        viafSearch.setField(viafDisplayFields);
        viafSearch.performSearchRequest();
    }

}
