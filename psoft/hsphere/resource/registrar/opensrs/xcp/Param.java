package psoft.hsphere.resource.registrar.opensrs.xcp;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/Param.class */
public class Param {
    String name;
    String value;

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String toString() {
        return this.name + "=\"" + this.value + '\"';
    }

    public void appendTo(StringBuffer buf) {
        buf.append(' ').append(this.name).append('=').append('\"').append(this.value).append('\"');
    }
}
