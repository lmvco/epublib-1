package nl.siegmann.epublib.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DCMES element
 *
 * @author LinQ
 * @version 2013-05-27
 */
public class DcmesElement implements Serializable {
    private String id;
    private String lang;
    private String direction;
    private String value;
    // refines metas
    List<Meta> metas = new ArrayList<Meta>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void addMeta(Meta meta) {
        metas.add(meta);
    }

    public List<Meta> getMetas() {
        return metas;
    }

    @Override
    public String toString() {
        return "DcmesElement{" +
                "id='" + id + '\'' +
                ", lang='" + lang + '\'' +
                ", direction='" + direction + '\'' +
                ", value='" + value + '\'' +
                ", metas=" + metas +
                '}';
    }
}
