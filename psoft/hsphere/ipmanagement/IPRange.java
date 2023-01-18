package psoft.hsphere.ipmanagement;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.apache.log4j.Category;
import psoft.hsphere.Localizer;
import psoft.hsphere.cache.CacheableObject;
import psoft.hsphere.resource.C0015IP;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPRange.class */
public class IPRange implements CacheableObject, TemplateHashModel, Comparable {
    public static final int TYPE_FREE = 1;
    public static final int TYPE_RESERVED = 2;
    public static final int TYPE_TAKEN = 3;
    public static final int TYPE_GW = 4;
    public static final int TYPE_BC = 5;
    private static Category log = Category.getInstance(IPRange.class.getName());

    /* renamed from: id */
    private long f92id;
    private int type;
    private IPSubnet parent;
    private String startIP;
    private String endIP;
    private String description;
    private long rid;

    public IPRange(String _startIP, String _endIP) {
        this.startIP = _startIP;
        this.endIP = _endIP;
        this.f92id = hashCode();
    }

    public IPRange(long _startIP, long _endIP) {
        this(IPCalc.getInstance().IP2String(_startIP), IPCalc.getInstance().IP2String(_endIP));
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.f92id;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public IPSubnet getParent() {
        return this.parent;
    }

    public void setParent(IPSubnet parent) {
        this.parent = parent;
    }

    public String getStartIP() {
        return this.startIP;
    }

    public void setStartIP(String startIP) {
        this.startIP = startIP;
    }

    public String getEndIP() {
        return this.endIP;
    }

    public void setEndIP(String endIP) {
        this.endIP = endIP;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getNumberOfIPs() {
        return (getEndIPasLong() - getStartIPasLong()) + 1;
    }

    public boolean isValid() {
        return C0015IP.toLong(getStartIP()) <= C0015IP.toLong(getEndIP());
    }

    public String getNetmask() {
        if (getParent() != null) {
            return getParent().getNetmask();
        }
        return null;
    }

    public String getGateway() {
        if (getParent() != null) {
            return getParent().getGw();
        }
        return null;
    }

    public String getBroadcast() {
        if (getParent() != null) {
            return getParent().getBroadcast();
        }
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IPRange) {
            IPRange ipRange = (IPRange) o;
            return this.endIP.equals(ipRange.endIP) && this.startIP.equals(ipRange.startIP);
        }
        return false;
    }

    public int hashCode() {
        int result = this.startIP.hashCode();
        return (29 * result) + this.endIP.hashCode();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("start_ip".equals(key)) {
            return new TemplateString(getStartIP());
        }
        if ("end_ip".equals(key)) {
            return new TemplateString(getEndIP());
        }
        if ("type_description".equals(key)) {
            return new TemplateString(getRangeType());
        }
        if ("type".equals(key)) {
            return new TemplateString(getType());
        }
        if ("number_of_ips".equals(key)) {
            return new TemplateString(getNumberOfIPs());
        }
        if ("descr".equals(key)) {
            return new TemplateString(toString());
        }
        if ("can_be_splited".equals(key)) {
            return new TemplateString(canBeSplitted());
        }
        if ("can_be_merged".equals(key)) {
            return new TemplateString(canBeMerged());
        }
        if ("can_be_assigned".equals(key)) {
            return new TemplateString(canBeAssigned());
        }
        if ("subnet_id".equals(key)) {
            return new TemplateString(getParent().getId());
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String getRangeType() {
        switch (getType()) {
            case 1:
                return Localizer.translateMessage("ds.ippool.range_free");
            case 2:
                return Localizer.translateMessage("ds.ippool.range_reserved");
            case 3:
                return Localizer.translateMessage("ds.ippool.range_taken");
            case 4:
                return Localizer.translateMessage("ds.ippool.range_gw");
            case 5:
                return Localizer.translateMessage("ds.ippool.range_bc");
            default:
                return "";
        }
    }

    public String getRangeDescription() {
        return null;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        IPRange _ipr = (IPRange) o;
        long sIP = IPCalc.getInstance().string2IP(getStartIP());
        long _sIP = IPCalc.getInstance().string2IP(_ipr.getStartIP());
        if (sIP == _sIP) {
            return 0;
        }
        return sIP > _sIP ? 1 : -1;
    }

    public long getStartIPasLong() {
        return IPCalc.getInstance().string2IP(getStartIP());
    }

    public long getEndIPasLong() {
        return IPCalc.getInstance().string2IP(getEndIP());
    }

    public String toString() {
        return getStartIP() + " - " + getEndIP();
    }

    public boolean overlapsWith(IPRange r) {
        return (getStartIPasLong() <= r.getStartIPasLong() && getEndIPasLong() >= r.getStartIPasLong()) || (getStartIPasLong() <= r.getEndIPasLong() && getEndIPasLong() >= r.getEndIPasLong());
    }

    public boolean contains(IPRange r) {
        return getStartIPasLong() <= r.getStartIPasLong() && getEndIPasLong() >= r.getEndIPasLong();
    }

    public boolean containsIP(long _ip) {
        return getStartIPasLong() <= _ip && _ip <= getEndIPasLong();
    }

    public boolean canBeSplitted() {
        return (getType() == 1 || getType() == 2) && getNumberOfIPs() > 1;
    }

    public boolean canBeMerged() {
        log.debug("IPRange::canBeMerged size=" + getParent().getMergeableRanges(this).size());
        return getType() == 1 && getParent().getMergeableRanges(this).size() > 0;
    }

    public boolean canBeDeleted() {
        return getType() == 1 || getType() == 2;
    }

    public boolean canBeAssigned() {
        return getType() == 1;
    }

    public long getRid() {
        return this.rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }
}
