package nl.siegmann.epublib.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * represents the manifest in the package document
 *
 * @author LinQ
 * @version 2013-05-23
 */
public class Manifest implements Serializable {
    private List<ManifestItemReference> references = new ArrayList<ManifestItemReference>();
    private Resources resources = new Resources();

    public List<ManifestItemReference> getReferences() {
        return references;
    }

    public ManifestItemReference addReference(ManifestItemReference reference) {
        references.add(reference);
        resources.add(reference.getResource());
        return reference;
    }

    public Resources getResources() {
        return resources;
    }
}
