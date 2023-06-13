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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.goobi.production.properties.GeonamesSearchProperty;
import org.goobi.production.properties.GndSearchProperty;
import org.goobi.production.properties.MultiSelectProperty;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.exceptions.MetadataTypeNotAllowedException;

@Getter
@Setter
@Log4j2
public class PageMetadataField implements MultiSelectProperty<String>, GndSearchProperty, GeonamesSearchProperty {

    private List<PageMetadataValue> values = new ArrayList<>();

    private String label;
    private String metadataField;
    private boolean repeatable;
    private String displayType;
    private String defaultValue;
    private boolean required;
    private String validation;
    private boolean readonly;
    private String validationErrorMessage;

    private List<String> valueList;

    private MetadataType metadataType;

    public PageMetadataField() {

    }

    public PageMetadataField(PageMetadataField other) {
        label = other.getLabel();
        metadataField = other.getMetadataField();
        repeatable = other.isRepeatable();
        displayType = other.getDisplayType();
        defaultValue = other.getDefaultValue();
        required = other.isRequired();
        validation = other.getValidation();
        readonly = other.isReadonly();
        validationErrorMessage = other.getValidationErrorMessage();
        valueList = other.getValueList();
        metadataType = other.getMetadataType();
    }

    public void addValue(PageMetadataValue value) {
        values.add(value);
    }

    public void removeValue(PageMetadataValue value) {
        values.remove(value);
    }

    // multiselect

    @Override
    public List<String> getAllSelectedValues() {
        List<String> answer = new ArrayList<>();
        for (PageMetadataValue pmv : values) {
            answer.add(pmv.getValue());
        }
        return answer;
    }

    @Override
    public List<String> getSelectValues() {
        List<String> answer = new ArrayList<>();
        for (PageMetadataValue val : values) {
            answer.add(val.getValue());
        }
        return answer;
    }

    @Override
    public void removeSelectedValue(String value) {

        for (PageMetadataValue pmv : values) {
            if (pmv.getValue().equals(value)) {
                values.remove(pmv);
                return;
            }
        }

    }

    @Override
    public List<String> getPossibleValues() {
        List<String> answer = new ArrayList<>();
        for (String possibleValue : valueList) {
            boolean found = false;
            for (PageMetadataValue val : values) {
                if (val.getValue().equals(possibleValue)) {
                    found = true;
                }
            }
            if (!found) {
                answer.add(possibleValue);
            }
        }
        return answer;
    }

    @Override
    public String getCurrentValue() {
        return MultiSelectProperty.super.getCurrentValue();
    }

    @Override
    public void setCurrentValue(String value) {
        try {
            Metadata md = new Metadata(metadataType);
            md.setValue(value);
            PageMetadataValue pmv = new PageMetadataValue(md, validation, required);
            values.add(pmv);
        } catch (MetadataTypeNotAllowedException e) {
            // ignore this, it will not occur
        }

    }

    private String searchValue;
    private String searchOption;
    private List<List<NormData>> dataList;
    private List<NormData> currentData;
    private boolean showNoHits;
    private String value;
    private String gndNumber;
    private String geonamesNumber;
    private Toponym currentToponym;
    private List<Toponym> resultList;
    private int totalResults;

    @Override
    public void importGeonamesData() {
        value = currentToponym.getName();
        geonamesNumber= "" + currentToponym.getGeoNameId();

        currentToponym = null;
        resultList=null;
        totalResults=0;
    }


    @Override
    public void importGndData() {
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
