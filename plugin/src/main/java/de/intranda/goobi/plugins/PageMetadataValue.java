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

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ugh.dl.Metadata;

@Getter
@Setter
@Log4j2
public class PageMetadataValue implements GndSearchProperty, GeonamesSearchProperty {

    private String validationMessage;
    private Metadata metadata;

    private boolean required;
    private String validationRegex;

    public PageMetadataValue(Metadata metadata, String validationRegex, boolean required) {
        this.metadata = metadata;
        this.validationRegex = validationRegex;
        this.required = required;
    }

    public boolean isValid() {
        if (required && StringUtils.isBlank(getValue())) {
            return false;
        } else if (StringUtils.isNotBlank(validationMessage) && StringUtils.isNotBlank(getValue()) && !getValue().matches(validationMessage)) {
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
        metadata.setAutorityFile("geonames","http://www.geonames.org/", number);
    }

    private String searchValue;
    private String searchOption;
    private List<List<NormData>> dataList;
    private List<NormData> currentData;
    private boolean showNoHits;
    private String gndNumber;
    private Toponym currentToponym;
    private List<Toponym> resultList;
    private int totalResults;

    @Override
    public void importGeonamesData() {
        metadata.setValue( currentToponym.getName());
        metadata.setAutorityFile("geonames", "http://www.geonames.org/", "" + currentToponym.getGeoNameId());

        currentToponym = null;
        resultList=null;
        totalResults=0;
    }


    @Override
    public void importGndData() {
        for (NormData normdata : currentData) {
            if ("NORM_IDENTIFIER".equals(normdata.getKey())) {
                gndNumber = normdata.getValues().get(0).getText();
                metadata.setAutorityFile("gnd", "http://d-nb.info/gnd/", gndNumber);
            } else if ("NORM_NAME".equals(normdata.getKey())) {
                String value = normdata.getValues().get(0).getText();
                metadata.setValue(value);
            }
        }

    }

    @Override
    public void searchGnd() {
        String val = "";
        if (StringUtils.isBlank(getSearchOption()) && StringUtils.isBlank(getSearchValue())) {
            setShowNoHits(true);
            return;
        }
        if (StringUtils.isBlank(getSearchOption())) {
            val = "dnb.nid=" + getSearchValue();
        } else {
            val = getSearchOption() + " and BBG=" + getSearchValue();
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

}
