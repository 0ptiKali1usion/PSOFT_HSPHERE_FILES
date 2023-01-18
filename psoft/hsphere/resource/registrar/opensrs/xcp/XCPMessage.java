package psoft.hsphere.resource.registrar.opensrs.xcp;

import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/XCPMessage.class */
public class XCPMessage {
    public static long count;
    Tag envelope;
    Tag header;
    Tag body;
    public Tag data;

    public static synchronized String getId() {
        count++;
        return Long.toString(count) + "." + TimeUtils.currentTimeMillis();
    }

    public XCPMessage() {
        this(getId());
    }

    public XCPMessage(String action, String object, String cookie, Tag attributes) {
        this();
        this.data.add(new DTAssoc().add("protocol", "XCP").add("action", action).add("object", object).add("cookie", cookie).add("attributes", attributes));
    }

    public XCPMessage(String action, String object, Tag attributes) {
        this();
        this.data.add(new DTAssoc().add("protocol", "XCP").add("action", action).add("object", object).add("attributes", attributes));
    }

    public XCPMessage(String id) {
        this.envelope = new Tag("OPS_envelope");
        Tag tag = this.envelope;
        Tag tag2 = new Tag("header");
        this.header = tag2;
        tag.add(tag2);
        Tag tag3 = this.envelope;
        Tag tag4 = new Tag("body");
        this.body = tag4;
        tag3.add(tag4);
        Tag tag5 = this.body;
        Tag tag6 = new Tag("data_block");
        this.data = tag6;
        tag5.add(tag6);
        this.header.add(new Tag("version", "0.9")).add(new Tag("msg_id", id)).add(new Tag("msg_type", "standard"));
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding=\"UTF-8\" standalone=\"no\" ?>\n");
        buf.append("<!DOCTYPE OPS_envelope SYSTEM \"ops.dtd\">\n");
        this.envelope.appendTo(buf);
        return buf.toString();
    }
}
