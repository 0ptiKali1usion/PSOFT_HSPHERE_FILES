package psoft.hsphere.resource.mpf;

import java.util.ArrayList;
import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/Tag.class */
public class Tag {
    private static final String xmlStart = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";
    List children;
    String attribute;
    String name;

    public Tag(String name) {
        this.name = name;
        this.children = new ArrayList();
    }

    public Tag(String name, Object child) {
        this(name);
        addChild(child);
    }

    public Tag(String name, String attr, Object child) {
        this(name, child);
        this.attribute = attr;
    }

    public void addChild(Object obj) {
        this.children.add(obj);
    }

    public String toXML() {
        return xmlStart + toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\n<").append(this.name);
        if (this.attribute != null) {
            buf.append(" name=\"").append(this.attribute).append("\"");
        }
        buf.append('>');
        for (Object obj : this.children) {
            buf.append(obj.toString());
        }
        buf.append("</").append(this.name).append('>');
        return buf.toString();
    }
}
