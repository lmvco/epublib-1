package nl.siegmann.epublib.domain;

/**
 * The mediaType Element of bindings element
 *
 * @author LinQ
 * @version 2013-05-29
 */
public class MediaType {
    private MediaTypeProperty mediaTypeProperty;
    private String handler;

    public MediaTypeProperty getMediaTypeProperty() {
        return mediaTypeProperty;
    }

    public void setMediaTypeProperty(MediaTypeProperty mediaTypeProperty) {
        this.mediaTypeProperty = mediaTypeProperty;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }
}
