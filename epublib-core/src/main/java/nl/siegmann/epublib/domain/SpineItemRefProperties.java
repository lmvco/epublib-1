package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.util.StringUtil;

public enum SpineItemRefProperties implements ManifestProperties {
	PAGE_SPREAD_LEFT("page-spread-left"),
	PAGE_SPREAD_RIGHT("page-spread-right");
	
	private String name;
	
	private SpineItemRefProperties(String name) {
		this.name = name;
	}

    public static SpineItemRefProperties findProperties(String name) {
        if (StringUtil.isBlank(name))
            return null;
        for (SpineItemRefProperties value : SpineItemRefProperties.values()) {
            if (value.name.equals(name))
                return value;
        }

        return null;
    }

    public String getName() {
		return name;
	}
}
