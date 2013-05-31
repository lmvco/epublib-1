package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.util.StringUtil;

/**
 * page-progression-direction of spine
 *
 * @author LinQ
 * @version 2013-05-29
 */
public enum PageProgressionDirection {
    LTR("ltr"), RTL("rtl");

    private String value;

    private PageProgressionDirection(String value) {
        this.value = value;
    }

    public static PageProgressionDirection findDirection(String value) {
        if (StringUtil.isBlank(value))
            return null;

        for (PageProgressionDirection direction : PageProgressionDirection.values()) {
            if (direction.value.equals(value))
                return direction;
        }

        return null;
    }

    public String getValue() {
        return value;
    }
}
