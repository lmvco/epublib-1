package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * metadata meta
 *
 * @author LinQ
 * @version 2013-05-27
 */
public class Meta extends DcmesElement {
    private String property;
    private String refines;
    private String scheme;
    private Map<String, String> customProperties = new HashMap<String, String>();

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getRefines() {
        return refines;
    }

    public void setRefines(String refines) {
        this.refines = refines;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public void addCustomProperties(String property, String value) {
        if (StringUtil.isNotBlank(property)) {
            customProperties.put(property, value);
        }
    }

    public Map<String, String> getCustomProperties() {
        return customProperties;
    }

    @Override
    public String toString() {
        return "Meta{" +
                "value='" + getValue() + '\'' +
                ", id='" + getId() + '\'' +
                ", property='" + property + '\'' +
                ", refines='" + refines + '\'' +
                ", scheme='" + scheme + '\'' +
                '}';
    }
}
