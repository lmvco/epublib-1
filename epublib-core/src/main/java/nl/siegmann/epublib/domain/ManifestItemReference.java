package nl.siegmann.epublib.domain;

/**
 * a manirest item of the manitest
 *
 * @author LinQ
 * @version 2013-05-23
 */
public class ManifestItemReference extends ResourceReference {
    private String fallback;
    private String mediaOverlay;
    private ManifestItemProperties properties;

    public String getId() {
        return resource.getId();
    }

    public String getHref() {
        return resource.getHref();
    }

    public MediaTypeProperty getMediaTypeProperty() {
        return resource.getMediaTypeProperty();
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public String getMediaOverlay() {
        return mediaOverlay;
    }

    public void setMediaOverlay(String mediaOverlay) {
        this.mediaOverlay = mediaOverlay;
    }

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

    public void setProperties(ManifestItemProperties prop) {
        this.properties = prop;
    }

}
