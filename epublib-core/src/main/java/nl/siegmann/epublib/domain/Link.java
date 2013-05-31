package nl.siegmann.epublib.domain;

/**
 * link Element
 *
 * @author LinQ
 * @version 2013-05-28
 */
public class Link extends DcmesElement {
    private String href;
    private String rel;
    private String refines;
    private String mediaType;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public String getRefines() {
        return refines;
    }

    public void setRefines(String refines) {
        this.refines = refines;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String toString() {
        return "Link{" +
                "href='" + href + '\'' +
                ", rel='" + rel + '\'' +
                ", refines='" + refines + '\'' +
                ", mediaType='" + mediaType + '\'' +
                '}';
    }
}
