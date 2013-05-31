package nl.siegmann.epublib.domain;

import java.io.Serializable;
import java.util.*;

/**
 * represents the manifest in the package document
 *
 * @author LinQ
 * @version 2013-05-23
 */
public class Manifest implements Serializable {
    private String id;
    private Map<String , ManifestItemReference> references = new HashMap<String, ManifestItemReference>();
    private Resources resources = new Resources();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Collection<ManifestItemReference> getReferences() {
        return references.values();
    }

    public ManifestItemReference addReference(ManifestItemReference reference) {
        references.put(reference.getResourceId(), reference);
        resources.add(reference.getResource());
        return reference;
    }

    public ManifestItemReference removeManifestItem(String href) {
        return references.remove(href);
    }

    public Resources getResources() {
        return resources;
    }
}
