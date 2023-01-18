package psoft.hsphere.resource;

import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.hsphere.cache.CacheableObject;

/* renamed from: psoft.hsphere.resource.IP */
/* loaded from: hsphere.zip:psoft/hsphere/resource/IP.class */
public class C0015IP implements CacheableObject {

    /* renamed from: ip */
    private String f146ip;
    private String mask;

    /* renamed from: gw */
    private String f147gw;

    public C0015IP(String ip, String mask) {
        this(ip, mask, "");
    }

    private C0015IP(String ip, String mask, String gw) {
        this.f146ip = ip;
        this.mask = mask;
        this.f147gw = gw;
    }

    public String getIP() {
        return this.f146ip;
    }

    public String toString() {
        return this.f146ip;
    }

    public String getMask() {
        return this.mask;
    }

    public String getGateway() {
        return this.f147gw;
    }

    public long toLong() {
        return toLong(getIP());
    }

    public static long toLong(String stIp) {
        StringTokenizer st = new StringTokenizer(stIp, ".");
        long res = 0 + (Long.parseLong(st.nextToken()) << 24);
        return res + (Long.parseLong(st.nextToken()) << 16) + (Long.parseLong(st.nextToken()) << 8) + Long.parseLong(st.nextToken());
    }

    public static String unPack(long ipNum) {
        StringBuffer res = new StringBuffer("");
        int i = 24;
        while (true) {
            int i2 = i;
            if (i2 > 0) {
                long decr = ipNum >> i2;
                res.append(decr + ".");
                ipNum -= decr << i2;
                i = i2 - 8;
            } else {
                res.append(ipNum);
                return res.toString();
            }
        }
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return toLong();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof C0015IP) {
            C0015IP ip1 = (C0015IP) o;
            if (this.f147gw != null) {
                if (!this.f147gw.equals(ip1.f147gw)) {
                    return false;
                }
            } else if (ip1.f147gw != null) {
                return false;
            }
            return this.f146ip.equals(ip1.f146ip) && this.mask.equals(ip1.mask);
        }
        return false;
    }

    public static C0015IP createIP(String ip, String mask, String gw) {
        C0015IP _ip = (C0015IP) Session.getCacheFactory().getCache(C0015IP.class).get(toLong(ip));
        if (_ip == null) {
            _ip = new C0015IP(ip, mask, gw);
            Session.getCacheFactory().getCache(C0015IP.class).put(_ip);
        }
        return _ip;
    }

    public static C0015IP createIP(String ip, String mask) {
        return createIP(ip, mask, null);
    }
}
