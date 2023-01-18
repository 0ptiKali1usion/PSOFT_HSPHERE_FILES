package psoft.hsphere.resource.registrar.opensrs.xcp;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/TextTag.class */
public class TextTag extends Tag {
    String value;

    public TextTag(String value) {
        this.value = value;
    }

    @Override // psoft.hsphere.resource.registrar.opensrs.xcp.Tag
    public String toString() {
        return this.value;
    }

    @Override // psoft.hsphere.resource.registrar.opensrs.xcp.Tag
    public void appendTo(StringBuffer buf) {
        buf.append(this.value);
    }
}
