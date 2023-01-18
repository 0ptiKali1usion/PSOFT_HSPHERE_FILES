package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/DNSComparator.class */
public class DNSComparator implements Comparator {
    private static Random rnd = new Random();
    private List resList;

    public DNSComparator() {
        this.resList = null;
    }

    public DNSComparator(List serversList) {
        this.resList = null;
        this.resList = new ArrayList();
        List tmpList = new ArrayList(serversList);
        while (tmpList.size() > 0) {
            int rndId = rnd.nextInt(tmpList.size());
            HostEntry he = (HostEntry) tmpList.get(rndId);
            this.resList.add(he);
            tmpList.remove(rndId);
        }
    }

    @Override // java.util.Comparator
    public int compare(Object o1, Object o2) {
        HostEntry ns1 = (HostEntry) o1;
        HostEntry ns2 = (HostEntry) o2;
        String role1 = ns1.getOption("named_role");
        String role2 = ns2.getOption("named_role");
        int pos1 = 5;
        int pos2 = 5;
        Session.getLog().debug("Role1 for:" + ns1.getName() + " pose1 :" + role1);
        Session.getLog().debug("Role2 for:" + ns2.getName() + " pose2 :" + role2);
        if (role1 != null && !"".equals(role1)) {
            if (role1.equals("slave2")) {
                pos1 = 4;
            } else if (role1.equals("slave1")) {
                pos1 = 3;
            } else if (role1.equals("master")) {
                pos1 = 2;
            }
        }
        Session.getLog().debug("Role1 for:" + ns1.getName() + " pose1 :" + pos1);
        if (role2 != null && !"".equals(role2)) {
            if (role2.equals("slave2")) {
                pos2 = 4;
            } else if (role2.equals("slave1")) {
                pos2 = 3;
            } else if (role2.equals("master")) {
                pos2 = 2;
            }
        }
        Session.getLog().debug("Role2 for:" + ns2.getName() + " pose2 :" + pos2);
        if ((pos1 == 5 && pos2 == 5) || pos1 == pos2) {
            return this.resList.indexOf(ns1) - this.resList.indexOf(ns2);
        }
        return pos1 - pos2;
    }

    @Override // java.util.Comparator
    public boolean equals(Object obj) {
        return equals(obj);
    }
}
