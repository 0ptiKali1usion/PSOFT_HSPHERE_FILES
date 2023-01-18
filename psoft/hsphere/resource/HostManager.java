package psoft.hsphere.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.resource.allocation.AllocatedPServer;
import psoft.hsphere.resource.allocation.AllocatedServerManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/HostManager.class */
public class HostManager {
    protected static Map groupMap = new HashMap();
    protected static Map map = new HashMap();
    protected static Random rnd = new Random();

    static {
        loadHosts();
    }

    public static HostEntry getHost(Object id) throws Exception {
        HostEntry host;
        synchronized (map) {
            host = getHost(Long.parseLong(id.toString()));
        }
        return host;
    }

    public static HostEntry getHost(long id) throws Exception {
        HostEntry he;
        synchronized (map) {
            he = (HostEntry) map.get(new Long(id));
            if (he == null) {
                throw new HSUserException("hostmanager.entry", new Object[]{String.valueOf(id)});
            }
        }
        return he;
    }

    public static LinkedList getHosts(int group) {
        LinkedList ll;
        synchronized (map) {
            ll = new LinkedList();
            for (HostEntry he : map.values()) {
                if (he.getGroup() == group) {
                    ll.add(he);
                }
            }
        }
        return ll;
    }

    public static LinkedList getHostsByGroupType(int groupType) {
        LinkedList ll;
        synchronized (map) {
            ll = new LinkedList();
            for (HostEntry he : map.values()) {
                if (he.getGroupType() == groupType) {
                    ll.add(he);
                }
            }
        }
        return ll;
    }

    public static LinkedList getSignupHosts(int group) throws Exception {
        synchronized (map) {
            if (AllocatedServerManager.supportedServices.contains(new Integer(group))) {
                HashMap allocatedMap = new HashMap();
                AllocatedServerManager asm = AllocatedServerManager.getInstance();
                AllocatedPServer aps = Session.getReseller().getAllocatedPServer();
                Session.getLog().debug("Got allocated pserver aps=" + (aps == null ? "null" : "" + aps.getId()));
                for (HostEntry he : map.values()) {
                    if (he.getGroup() == group) {
                        if (asm.isAllocated(he.getPServer().getId())) {
                            if (aps != null && aps.getId() == he.getPServer().getId()) {
                                Session.getLog().debug("Adding " + he.getBaseName() + " to the allocated server map");
                                allocatedMap.put(new Long(he.getId()), he);
                            }
                        } else if (aps == null) {
                            allocatedMap.put(new Long(he.getId()), he);
                        }
                    }
                }
                Session.getLog().debug("The allocatedMap has been filled in: " + allocatedMap);
                return getSignupHosts(group, allocatedMap);
            }
            return getSignupHosts(group, map);
        }
    }

    private static LinkedList getSignupHosts(int group, Map allowedServers) {
        Session.getLog().debug("Insode getSignupHosts(int Map). group=" + group + " allowebServers=" + allowedServers);
        try {
            if (getTypeByGroup(group) == 3 && Session.getPropertyString("IRIS_USER").equals("")) {
                LinkedList ll = new LinkedList();
                for (HostEntry he : allowedServers.values()) {
                    String role = he.getOption("mail_role");
                    if (role == null) {
                        role = "master+relay";
                    }
                    if (he.getGroup() == group && he.availableForSignup() && role.equals("master+relay")) {
                        ll.add(he);
                    }
                }
                return ll;
            }
        } catch (Exception e) {
        }
        Session.getLog().debug("Scanning allowed servers:");
        LinkedList ll2 = new LinkedList();
        for (HostEntry he2 : allowedServers.values()) {
            Session.getLog().debug("Got and checking " + he2.getBaseName() + " group=" + he2.getGroup());
            if (he2.getGroup() == group && he2.availableForSignup()) {
                ll2.add(he2);
            }
        }
        return ll2;
    }

    public static LinkedList getMailRelayHosts(int group) throws Exception {
        synchronized (map) {
            if (AllocatedServerManager.supportedServices.contains(new Integer(group))) {
                HashMap allocatedMap = new HashMap();
                AllocatedServerManager asm = AllocatedServerManager.getInstance();
                AllocatedPServer aps = Session.getReseller().getAllocatedPServer();
                Session.getLog().debug("Got allocated pserver aps=" + (aps == null ? "null" : "" + aps.getId()));
                for (HostEntry he : map.values()) {
                    if (he.getGroup() == group) {
                        if (asm.isAllocated(he.getPServer().getId())) {
                            if (aps != null && aps.getId() == he.getPServer().getId()) {
                                Session.getLog().debug("Adding " + he.getBaseName() + " to the allocated server map");
                                allocatedMap.put(new Long(he.getId()), he);
                            }
                        } else if (aps == null) {
                            allocatedMap.put(new Long(he.getId()), he);
                        }
                    }
                }
                Session.getLog().debug("The allocatedMap has been filled in: " + allocatedMap);
                return getMailRelayHosts(group, allocatedMap);
            }
            return getMailRelayHosts(group, map);
        }
    }

    private static LinkedList getMailRelayHosts(int group, Map allowedServers) {
        LinkedList ll = new LinkedList();
        for (HostEntry he : allowedServers.values()) {
            if (he.getGroup() == group && he.availableForSignup()) {
                ll.add(he);
            }
        }
        return ll;
    }

    public static LinkedList getDNSSignupHosts(int group) {
        LinkedList ll;
        if (!Session.getPropertyString("MYDNS_USER").equals("")) {
            group = 21;
        }
        synchronized (map) {
            ll = new LinkedList();
            for (HostEntry he : map.values()) {
                if (he.getGroup() == group && he.availableForSignup()) {
                    String access = he.getOption("named_access");
                    if (access != null && !"".equals(access)) {
                        try {
                            if (Session.getResellerId() != 1 || access.equals("main_only")) {
                                if (Session.getResellerId() == 1 || access.equals("reseller_only")) {
                                }
                            }
                        } catch (UnknownResellerException ex) {
                            Session.getLog().error("Unable to get Reseller", ex);
                        }
                    }
                    ll.add(he);
                }
            }
        }
        return ll;
    }

    public static boolean areThereSignupHosts(int group) {
        synchronized (map) {
            for (HostEntry he : map.values()) {
                if (he.getGroup() == group && he.availableForSignup()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static HostEntry getCPHost() {
        synchronized (map) {
            for (HostEntry he : map.values()) {
                if (he.getGroup() == 10) {
                    return he;
                }
            }
            return null;
        }
    }

    public static LinkedList getHosts() {
        LinkedList ll;
        synchronized (map) {
            ll = new LinkedList();
            for (HostEntry he : map.values()) {
                ll.add(he);
            }
        }
        return ll;
    }

    public static HostEntry getRandomHost(int group) throws Exception {
        HostEntry hostEntry;
        synchronized (map) {
            LinkedList list = getSignupHosts(group);
            Session.getLog().debug("Inside getRandomHost. Got signupHosts:");
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Session.getLog().debug(((HostEntry) it.next()).getBaseName());
            }
            Session.getLog().debug("Inside getRandomHost. End of list");
            if (list.size() > 0) {
                int i = (int) Math.rint((list.size() - 1) * Math.random());
                Session.getLog().debug("list size " + list.size() + " i " + i);
                hostEntry = (HostEntry) list.get(i);
            } else {
                throw new HSUserException("hostmanager.signup", new Object[]{String.valueOf(group)});
            }
        }
        return hostEntry;
    }

    public static List getRandomHostsList(int group) throws Exception {
        ArrayList arrayList;
        synchronized (map) {
            LinkedList list = getDNSSignupHosts(group);
            TreeSet resList = new TreeSet(new DNSComparator(list));
            resList.addAll(list);
            if (resList.size() > 0) {
                arrayList = new ArrayList(resList);
            } else {
                throw new HSUserException("hostmanager.signup", new Object[]{String.valueOf(group)});
            }
        }
        return arrayList;
    }

    public static int getTypeByGroup(int groupId) throws Exception {
        int intValue;
        synchronized (groupMap) {
            Integer typeId = (Integer) groupMap.get(new Integer(groupId));
            if (typeId == null) {
                throw new Exception("Cant resolve group type for group #" + groupId);
            }
            intValue = typeId.intValue();
        }
        return intValue;
    }

    public static synchronized void loadHosts() {
        loadHosts(false);
    }

    public static synchronized void loadHosts(boolean force) {
        if ((groupMap.size() == 0 && map.size() == 0) || force) {
            try {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT id, type_id FROM l_server_groups");
                ResultSet rs = ps.executeQuery();
                synchronized (groupMap) {
                    groupMap.clear();
                    while (rs.next()) {
                        groupMap.put(new Integer(rs.getInt(1)), new Integer(rs.getInt(2)));
                    }
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("SELECT id, name, group_id, file_server, file_server_path, status, type_id, signup FROM l_server");
                ResultSet rs2 = ps2.executeQuery();
                synchronized (map) {
                    map.clear();
                    while (rs2.next()) {
                        try {
                            int groupType = getTypeByGroup(rs2.getInt(3));
                            switch (rs2.getInt(7)) {
                                case 1:
                                    HostEntry he = new HostEntry(rs2.getLong(1), rs2.getString(2), rs2.getInt(3), groupType, rs2.getString(4), rs2.getString(5), rs2.getInt(6), rs2.getInt(8));
                                    map.put(new Long(rs2.getLong(1)), he);
                                    break;
                                case 2:
                                    WinHostEntry he1 = new WinHostEntry(rs2.getLong(1), rs2.getString(2), rs2.getInt(3), groupType, rs2.getInt(6), rs2.getInt(8));
                                    map.put(new Long(rs2.getLong(1)), he1);
                                    break;
                            }
                        } catch (Exception e) {
                            Session.getLog().error("Skip logical server #" + rs2.getLong(1), e);
                        }
                    }
                }
                Session.closeStatement(ps2);
                con.close();
            } catch (Exception e2) {
                throw new ExceptionInInitializerError(e2);
            }
        }
    }
}
