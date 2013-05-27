package nl.siegmann.epublib.domain;

import nl.siegmann.epublib.util.StringUtil;

public enum ManifestItemProperties implements ManifestProperties {
	COVER_IMAGE("cover-image"),
	MATHML("mathml"),
	NAV("nav"),
	REMOTE_RESOURCES("remote-resources"),
	SCRIPTED("scripted"),
	SVG("svg"),
	SWITCH("switch");
	
	private String name;
	
	private ManifestItemProperties(String name) {
		this.name = name;
	}

    public static ManifestItemProperties findProperties(String name) {
        if (StringUtil.isBlank(name)) {
            return null;
        } else {
            for (ManifestItemProperties value : ManifestItemProperties.values()) {
                if (value.name.equals(name))
                    return value;
            }
        }

        return null;
    }

    public String getName() {
		return name;
	}
}
