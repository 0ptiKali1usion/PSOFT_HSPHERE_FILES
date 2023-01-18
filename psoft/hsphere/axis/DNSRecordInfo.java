package psoft.hsphere.axis;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/axis/DNSRecordInfo.class */
public class DNSRecordInfo implements Serializable {

    /* renamed from: id */
    protected String f68id;
    protected String name;
    protected String ttl;
    protected String clas;
    protected String type;

    /* renamed from: ip */
    protected String f69ip;
    protected String priority;

    public DNSRecordInfo() {
    }

    public DNSRecordInfo(String id, String name, String ttl, String clas, String type, String ip, String priority) {
        this.f68id = id;
        this.name = name;
        this.ttl = ttl;
        this.clas = clas;
        this.type = type;
        this.f69ip = ip;
        this.priority = priority;
    }

    public String getId() {
        return this.f68id;
    }

    public void setId(String id) {
        this.f68id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTTL() {
        return this.ttl;
    }

    public void setTTL(String ttl) {
        this.ttl = ttl;
    }

    public String getClas() {
        return this.clas;
    }

    public void setClas(String clas) {
        this.clas = clas;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIP() {
        return this.f69ip;
    }

    public void setIP(String ip) {
        this.f69ip = ip;
    }

    public String getPriority() {
        return this.priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
