package nl.siegmann.epublib.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The bindings Element
 *
 * @author LinQ
 * @version 2013-05-29
 */
public class Bindings {
    private List<MediaType> mediaTypes = new ArrayList<MediaType>();

    public void addMediaType(MediaType mediaType) {
        mediaTypes.add(mediaType);
    }

    public List<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public void setMediaTypes(List<MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }
}
