package psoft.hsphere.ipmanagement;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.cache.CacheableObject;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPSubnet.class */
public class IPSubnet implements CacheableObject, TemplateHashModel {
    private static Category log = Category.getInstance(IPSubnet.class.getName());
    public static final TemplateString STATUS_OK = new TemplateString("OK");

    /* renamed from: id */
    private long f93id;
    private long resellerId;
    private String startIP;
    private String endIP;
    private String netmask;

    /* renamed from: gw */
    private IPRange f94gw;
    private IPRange broadcast;
    private Hashtable ranges;

    public IPSubnet() throws Exception {
        this(TimeUtils.currentTimeMillis());
    }

    public IPSubnet(long _id) throws Exception {
        this.ranges = new Hashtable();
        this.f93id = _id;
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.f93id;
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

    public String getNetmask() {
        return this.netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGw() {
        return this.f94gw.getStartIP();
    }

    public TemplateModel FM_setBroadcast(String _bc) {
        log.debug("Inside FM_setBroadcast");
        setGw(new IPRange(_bc, _bc));
        return this;
    }

    public TemplateModel FM_setGateway(String _gw) {
        log.debug("Inside FM_setGateway");
        log.debug("Before setting GW " + toString());
        setGw(new IPRange(_gw, _gw));
        log.debug("After setting GW " + toString());
        return this;
    }

    public void setGw(IPRange _gw) {
        _gw.setType(4);
        synchronized (this.ranges) {
            if (this.f94gw != null) {
                IPRange _ipr = new IPRange(this.f94gw.getStartIP(), this.f94gw.getEndIP());
                _ipr.setType(2);
                delRange(this.f94gw);
                addRange(_ipr);
            }
            List currRanges = new ArrayList();
            currRanges.addAll(this.ranges.values());
            Collection<IPRange> newRanges = IPCalc.getInstance().generateNewRangeSet(currRanges, _gw);
            this.ranges = new Hashtable();
            for (IPRange _ipr2 : newRanges) {
                addRange(_ipr2);
            }
            this.f94gw = _gw;
        }
    }

    public String getBroadcast() {
        return this.broadcast.getStartIP();
    }

    public void setBroadcast(IPRange _broadcast) {
        _broadcast.setType(5);
        synchronized (this.ranges) {
            if (this.broadcast != null) {
                IPRange _ipr = new IPRange(this.broadcast.getStartIP(), this.broadcast.getEndIP());
                _ipr.setType(2);
                delRange(this.broadcast);
                addRange(_ipr);
            }
            List currRanges = new ArrayList();
            currRanges.addAll(this.ranges.values());
            Collection<IPRange> newRanges = IPCalc.getInstance().generateNewRangeSet(currRanges, _broadcast);
            this.ranges = new Hashtable();
            for (IPRange _ipr2 : newRanges) {
                addRange(_ipr2);
            }
            this.broadcast = _broadcast;
        }
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public void setResellerId(long resellerId) {
        this.resellerId = resellerId;
    }

    public List getFreeRanges() {
        return null;
    }

    public boolean isValid() {
        return false;
    }

    public Collection getRanges() {
        return this.ranges.values();
    }

    public Collection getRangesSorted() {
        List c = new ArrayList(this.ranges.values());
        Collections.sort(c);
        return c;
    }

    public void setId(long id) {
        this.f93id = id;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return "id".equals(key) ? new TemplateString(getId()) : "start_ip".equals(key) ? new TemplateString(getStartIP()) : "end_ip".equals(key) ? new TemplateString(getEndIP()) : "netmask".equals(key) ? new TemplateString(getNetmask()) : "broadcast".equals(key) ? new TemplateString(getBroadcast()) : "gateway".equals(key) ? new TemplateString(getGw()) : "ranges".equals(key) ? new TemplateList(getRangesSorted()) : "stats".equals(key) ? new TemplateString(getSubnetStats()) : "free_ips".equals(key) ? new TemplateString(getNumberOfIPs(1)) : "taken_ips".equals(key) ? new TemplateString(getNumberOfIPs(3)) : "unusable_ips".equals(key) ? new TemplateString(getNumberOfIPs(2)) : "status".equals(key) ? STATUS_OK : "can_be_deleted".equals(key) ? new TemplateString(canBeDeleted()) : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public void addRange(IPRange r) {
        synchronized (this.ranges) {
            this.ranges.put(new Long(r.getId()), r);
            r.setParent(this);
        }
    }

    public void delRange(IPRange _ipr) {
        delRange(_ipr.getId());
    }

    public void delRange(long id) {
        synchronized (this.ranges) {
            IPRange ipr = (IPRange) this.ranges.get(new Long(id));
            if (ipr != null) {
                ipr.setParent(null);
                this.ranges.remove(new Long(id));
            }
        }
    }

    public IPRange getClosestRange(IPRange _ipr) {
        List r = (List) getRangesSorted();
        int ind = r.indexOf(_ipr);
        if (ind > 0) {
            return ind == 0 ? (IPRange) r.get(1) : ind == r.size() - 1 ? (IPRange) r.get(r.size() - 2) : (IPRange) r.get(ind - 1);
        }
        return null;
    }

    public List getOverlapingRanges(IPRange _ipr) {
        ArrayList res = new ArrayList();
        synchronized (this.ranges) {
            for (IPRange ipr : this.ranges.values()) {
                if ((ipr.getStartIPasLong() <= _ipr.getStartIPasLong() && ipr.getEndIPasLong() >= _ipr.getStartIPasLong()) || (ipr.getStartIPasLong() <= _ipr.getEndIPasLong() && ipr.getEndIPasLong() >= _ipr.getEndIPasLong())) {
                    res.add(ipr);
                }
            }
        }
        return res;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nStart: " + getStartIP()).append('\n');
        sb.append("End: " + getEndIP()).append('\n');
        sb.append("Netmask: " + getNetmask()).append('\n');
        sb.append("Gateway: " + getGw()).append('\n');
        sb.append("Broadcast: " + getBroadcast()).append('\n');
        sb.append("Defined ranges: ").append('\n');
        TreeSet ts = new TreeSet();
        ts.addAll(this.ranges.values());
        Iterator i = ts.iterator();
        while (i.hasNext()) {
            IPRange _ipr = (IPRange) i.next();
            sb.append(_ipr.toString()).append(" " + _ipr.getRangeType()).append('\n');
        }
        return sb.toString();
    }

    public long getStartIPasLong() {
        return IPCalc.getInstance().string2IP(getStartIP());
    }

    public long getEndIPasLong() {
        return IPCalc.getInstance().string2IP(getEndIP());
    }

    public String getSubnetStats() {
        return getNumberOfIPs(2) + "/" + getNumberOfIPs(3) + "/" + getNumberOfIPs(1);
    }

    public int getNumberOfIPs(int ipType) {
        int res = 0;
        synchronized (this.ranges) {
            for (IPRange _ipr : this.ranges.values()) {
                int rType = _ipr.getType();
                if (rType == ipType || (ipType == 2 && (rType == 5 || rType == 4))) {
                    res = (int) (res + _ipr.getNumberOfIPs());
                }
            }
        }
        return res;
    }

    public String getShortDescription() {
        return getStartIP() + " - " + getEndIP() + "/" + getNetmask();
    }

    public List getMergeableRanges(IPRange _ipr) {
        List result = new ArrayList();
        List ranges = Arrays.asList(getRangesSorted().toArray());
        int pos = ranges.indexOf(_ipr);
        if (ranges.size() > pos - 1) {
            IPRange ipr1 = (IPRange) ranges.get(pos + 1);
            if (ipr1.getType() == _ipr.getType()) {
                result.add(ipr1);
            }
        }
        if (pos != 0) {
            IPRange ipr2 = (IPRange) ranges.get(pos - 1);
            if (ipr2.getType() == _ipr.getType()) {
                result.add(ipr2);
            }
        }
        return result;
    }

    public TemplateModel FM_getMergeableRanges(long rangeId) {
        IPRange ipr = getRange(rangeId);
        List result = new ArrayList();
        if (ipr != null) {
            result = getMergeableRanges(ipr);
        }
        return new TemplateList(result);
    }

    public IPRange getRange(long id) {
        return (IPRange) this.ranges.get(new Long(id));
    }

    public TemplateModel FM_getRange(long id) {
        return getRange(id);
    }

    public boolean canBeDeleted() {
        return getNumberOfIPs(3) == 0;
    }
}
