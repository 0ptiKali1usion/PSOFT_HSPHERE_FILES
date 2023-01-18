package psoft.hsphere.resource.registrar.opensrs.xcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/Tag.class */
public class Tag {
    String name;
    List tags;
    List params;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
        this.tags = new ArrayList();
        this.params = new ArrayList();
    }

    public Tag(String name, String value) {
        this(name);
        add(value);
    }

    public Tag add(Param p) {
        this.params.add(p);
        return this;
    }

    public Tag add(Tag t) {
        this.tags.add(t);
        return this;
    }

    public Tag add(String name, Map values) {
        DTAssoc dta = new DTAssoc();
        for (String key : values.keySet()) {
            dta.add(key, (String) values.get(key));
        }
        add(name, dta);
        return this;
    }

    public Tag add(String name, String value) {
        add(new ItemTag(name, value));
        return this;
    }

    public Tag add(String name, Tag tag) {
        add(new ItemTag(name, tag));
        return this;
    }

    public Tag add(String name, Collection l) {
        return add(new ItemTag(name, DTArray.newInstance().add(l)));
    }

    public Tag add(String text) {
        this.tags.add(text);
        return this;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        appendTo(buf);
        return buf.toString();
    }

    public void appendTo(StringBuffer buf) {
        buf.append('<').append(this.name);
        for (Param param : this.params) {
            param.appendTo(buf);
        }
        if (this.tags.isEmpty()) {
            buf.append("/>");
        } else {
            buf.append(">");
            for (Object tmp : this.tags) {
                if (tmp instanceof String) {
                    buf.append(tmp);
                } else if (tmp != null) {
                    ((Tag) tmp).appendTo(buf);
                }
            }
            buf.append("</").append(this.name).append(">");
        }
        buf.append('\n');
    }
}
