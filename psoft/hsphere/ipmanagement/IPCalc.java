package psoft.hsphere.ipmanagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.apache.log4j.Category;
import psoft.hsphere.Session;
import psoft.hsphere.tools.ExternalCP;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPCalc.class */
public class IPCalc {
    private static Category log = Category.getInstance(IPCalc.class.getName());
    private static IPCalc ourInstance = new IPCalc();

    public static IPCalc getInstance() {
        return ourInstance;
    }

    private IPCalc() {
    }

    public IPSubnet calcSubnet(String startIP, String endIP, String netmask) throws Exception {
        return calcSubnet(startIP, endIP, string2IP(netmask));
    }

    public IPSubnet calcSubnet(String startIP, String endIP, int slash) throws Exception {
        long netmask = ((-1) << (32 - slash)) ^ (-4294967296L);
        return calcSubnet(startIP, endIP, netmask);
    }

    public IPSubnet calcSubnet(String startIP, String endIP) throws Exception {
        IPSubnet ips;
        long _startIP = string2IP(startIP);
        long _endIP = string2IP(endIP);
        int slash = 32 - Long.toBinaryString(_endIP - _startIP).length();
        do {
            int i = slash;
            slash = i - 1;
            ips = calcSubnet(startIP, endIP, i);
            if (string2IP(ips.getStartIP()) > _startIP) {
                break;
            }
        } while (string2IP(ips.getEndIP()) <= _endIP);
        return ips;
    }

    public IPSubnet calcSubnet(String startIP, String endIP, long netmask) throws Exception {
        Session.getLog().debug("Inside IPCalc::calcSubnet(String, String)");
        long _startIP = string2IP(startIP);
        long _endIP = string2IP(endIP);
        long netBase = _startIP & netmask;
        long _startIPCorrected = netBase + 1;
        long wildcard = (netmask ^ (-1)) ^ (-4294967296L);
        long _endIPCorrected = netBase + wildcard;
        IPSubnet result = new IPSubnet();
        result.setResellerId(Session.getResellerId());
        result.setStartIP(IP2String(_startIPCorrected));
        result.setEndIP(IP2String(_endIPCorrected));
        result.setNetmask(IP2String(netmask));
        if (_startIPCorrected + 1 < _startIP) {
            IPRange range = new IPRange(IP2String(_startIPCorrected + 1), IP2String(_startIP - 1));
            range.setType(2);
            result.addRange(range);
        }
        if (_endIPCorrected > _endIP) {
            IPRange range2 = new IPRange(IP2String(_endIP + 1), IP2String(_endIPCorrected - 1));
            range2.setType(2);
            result.addRange(range2);
        }
        IPRange range3 = new IPRange(startIP, endIP);
        range3.setType(1);
        result.addRange(range3);
        IPRange bc = new IPRange(IP2String(_endIPCorrected), IP2String(_endIPCorrected));
        bc.setType(5);
        result.setBroadcast(bc);
        result.addRange(bc);
        IPRange gw = new IPRange(IP2String(_startIPCorrected), IP2String(_startIPCorrected));
        gw.setType(4);
        result.setGw(gw);
        result.addRange(gw);
        return result;
    }

    public Collection generateNewRangeSet(List initialSet, IPRange newRange) {
        SortedSet result = new TreeSet();
        SortedSet<IPRange> _initialSet = new TreeSet();
        _initialSet.addAll(initialSet);
        for (IPRange _ipr : _initialSet) {
            if (_ipr.overlapsWith(newRange)) {
                if (_ipr.getStartIPasLong() < newRange.getStartIPasLong() && newRange.getStartIPasLong() < _ipr.getEndIPasLong() && _ipr.getStartIPasLong() < newRange.getEndIPasLong() && newRange.getEndIPasLong() < _ipr.getEndIPasLong()) {
                    IPRange toBeAdded = new IPRange(_ipr.getStartIPasLong(), newRange.getStartIPasLong() - 1);
                    toBeAdded.setType(_ipr.getType());
                    result.add(toBeAdded);
                    IPRange toBeAdded2 = new IPRange(newRange.getEndIPasLong() + 1, _ipr.getEndIPasLong());
                    toBeAdded2.setType(_ipr.getType());
                    result.add(toBeAdded2);
                } else if (_ipr.getStartIPasLong() <= newRange.getEndIPasLong() && newRange.getEndIPasLong() < _ipr.getEndIPasLong()) {
                    IPRange toBeAdded3 = new IPRange(newRange.getEndIPasLong() + 1, _ipr.getEndIPasLong());
                    toBeAdded3.setType(_ipr.getType());
                    result.add(toBeAdded3);
                } else if (_ipr.getStartIPasLong() < newRange.getStartIPasLong() && newRange.getStartIPasLong() <= _ipr.getEndIPasLong()) {
                    IPRange toBeAdded4 = new IPRange(_ipr.getStartIPasLong(), newRange.getStartIPasLong() - 1);
                    toBeAdded4.setType(_ipr.getType());
                    result.add(toBeAdded4);
                }
            } else if (!newRange.contains(_ipr)) {
                result.add(_ipr);
            }
        }
        result.add(newRange);
        return result;
    }

    public IPSubnet reorganizeSubnet(IPSubnet ips) {
        IPRange[] ranges = (IPRange[]) ips.getRangesSorted().toArray();
        new ArrayList();
        for (int i = 0; i < ranges.length; i++) {
            IPRange ipr1 = ranges[i];
            int j = i;
            while (j < ranges.length) {
                IPRange ipr2 = ranges[j];
                j = (ipr1.getStartIPasLong() < ipr2.getStartIPasLong() || ipr2.getEndIPasLong() <= ipr1.getEndIPasLong()) ? j + 1 : j + 1;
            }
        }
        return null;
    }

    public boolean overlaps(IPRange r1, IPRange r2) {
        boolean result = (r1.getStartIPasLong() <= r2.getStartIPasLong() && r1.getEndIPasLong() >= r2.getStartIPasLong()) || (r1.getStartIPasLong() <= r2.getEndIPasLong() && r1.getEndIPasLong() >= r2.getEndIPasLong());
        return result;
    }

    public boolean contains(IPRange container, IPRange r) {
        return container.getStartIPasLong() >= r.getStartIPasLong() && container.getEndIPasLong() >= r.getEndIPasLong();
    }

    public String IP2String(long ip) {
        StringBuffer res = new StringBuffer("");
        int i = 24;
        while (true) {
            int i2 = i;
            if (i2 > 0) {
                long decr = ip >> i2;
                res.append(decr + ".");
                ip -= decr << i2;
                i = i2 - 8;
            } else {
                res.append(ip);
                return res.toString();
            }
        }
    }

    public long string2IP(String ip) {
        StringTokenizer st = new StringTokenizer(ip, ".");
        long res = 0 + (Long.parseLong(st.nextToken()) << 24);
        return res + (Long.parseLong(st.nextToken()) << 16) + (Long.parseLong(st.nextToken()) << 8) + Long.parseLong(st.nextToken());
    }

    public static void testIntersect() {
        IPRange r0 = new IPRange("192.168.112.1", "192.168.112.1");
        r0.setType(4);
        IPRange r1 = new IPRange("192.168.112.2", "192.168.112.20");
        r1.setType(1);
        IPRange r2 = new IPRange("192.168.112.21", "192.168.112.25");
        r2.setType(1);
        IPRange r3 = new IPRange("192.168.112.26", "192.168.112.30");
        r3.setType(1);
        IPRange r4 = new IPRange("192.168.112.31", "192.168.112.40");
        r4.setType(1);
        IPRange over = new IPRange("192.168.112.1", "192.168.112.1");
        over.setType(5);
        List<IPRange> l = new ArrayList();
        l.add(r0);
        l.add(r1);
        l.add(r2);
        l.add(r3);
        l.add(r4);
        int k = 1;
        System.out.println("Intersecting");
        for (IPRange _r : l) {
            int i = k;
            k++;
            System.out.println(i + ". " + _r.toString() + " " + _r.getRangeType());
        }
        System.out.println("with");
        System.out.println(over.toString());
        System.out.println("Result");
        Collection<IPRange> res = getInstance().generateNewRangeSet(l, over);
        int k2 = 1;
        for (IPRange _r2 : res) {
            int i2 = k2;
            k2++;
            System.out.println(i2 + ". " + _r2.toString() + " " + _r2.getRangeType());
        }
    }

    public static void main(String[] argv) throws Exception {
        ExternalCP.initCP();
        testIntersect();
    }
}
