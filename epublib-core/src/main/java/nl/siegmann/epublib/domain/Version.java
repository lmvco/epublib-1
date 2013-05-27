package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.util.StringUtil;

/**
 * epub version
 *
 * @author LinQ
 * @version 2013-05-23
 */
public enum Version {
    V2("2.0"), V3("3.0");

    private String value;

    private Version(String value) {
        this.value = value;
    }

    public static Version findVersion(String value) {
        if (StringUtil.isBlank(value))
            return V2;
        else {
            for (Version va : Version.values()) {
                if (va.value.equals(value))
                    return va;
            }
        }

        return V2;
    }

    public String getValue() {
        return value;
    }
}
