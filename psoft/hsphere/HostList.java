package psoft.hsphere;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;

/* loaded from: hsphere.zip:psoft/hsphere/HostList.class */
public class HostList {
    private List hosts = null;
    private Hashtable hostsByTypes = null;
    private int size = 0;
    private String hostIds;

    public HostList(String commaSeparatedIds) throws Exception {
        ArrayList l = new ArrayList();
        StringTokenizer st = new StringTokenizer(commaSeparatedIds, ",");
        while (st.hasMoreTokens()) {
            l.add(new Long(st.nextToken()));
        }
        init(l);
    }

    public HostList(List serverIds) throws Exception {
        init(serverIds);
    }

    private void init(List serverIds) throws Exception {
        this.hostsByTypes = new Hashtable();
        this.hosts = new ArrayList();
        this.hostIds = "";
        Iterator i = serverIds.iterator();
        while (i.hasNext()) {
            long id = ((Long) i.next()).longValue();
            HostEntry he = HostManager.getHost(id);
            List l = (List) this.hostsByTypes.get(new Integer(he.getGroupType()));
            this.hostIds += he.getId() + (i.hasNext() ? "," : "");
            if (l == null) {
                l = new ArrayList();
            }
            l.add(he);
            this.hosts.add(he);
            this.size++;
            this.hostsByTypes.put(new Integer(he.getGroupType()), l);
        }
    }

    public String toString() {
        String result = "";
        Iterator i = this.hostsByTypes.keySet().iterator();
        while (i.hasNext()) {
            int key = ((Integer) i.next()).intValue();
            result = result + getHostIdsByHostType(key) + (i.hasNext() ? "," : "");
        }
        return result;
    }

    public String getHostIdsByHostType(int hostGroupType) {
        String result = "";
        List l = (List) this.hostsByTypes.get(new Integer(hostGroupType));
        if (l != null) {
            Iterator i = l.iterator();
            while (i.hasNext()) {
                HostEntry he = (HostEntry) i.next();
                result = result + he.getId() + (i.hasNext() ? "," : "");
            }
        }
        return result;
    }

    public List getHostsByHostType(int hostGroupType) {
        return (List) this.hostsByTypes.get(new Integer(hostGroupType));
    }

    public String getHostIds() {
        return this.hostIds;
    }

    public int size() {
        return this.size;
    }

    public boolean contains(HostEntry he) {
        for (List l : this.hostsByTypes.values()) {
            if (l.contains(he)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(long hostId) {
        for (List<HostEntry> l : this.hostsByTypes.values()) {
            for (HostEntry he : l) {
                if (he.getId() == hostId) {
                    return true;
                }
            }
        }
        return false;
    }

    public Iterator iterator() {
        return this.hosts.iterator();
    }
}
