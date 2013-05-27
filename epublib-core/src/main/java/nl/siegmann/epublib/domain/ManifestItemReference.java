package nl.siegmann.epublib.domain;

/**
 * a manirest item of the manitest
 *
 * @author LinQ
 * @version 2013-05-23
 */
public class ManifestItemReference extends ResourceReference {
    private ManifestItemProperties properties;

    public ManifestItemReference(Resource resource) {
        super(resource);
    }

    public ManifestItemReference(Resource resource, ManifestItemProperties properties) {
        super(resource);
        this.properties = properties;
    }

    public ManifestItemProperties getProperties() {
        return properties;
    }
}
