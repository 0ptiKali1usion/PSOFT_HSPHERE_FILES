package psoft.hsphere.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/DomainChecker.class */
public class DomainChecker extends C0004CP {
    public DomainChecker() {
        super("psoft_config.hsphere");
    }

    /* renamed from: go */
    public void m15go(List domains) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT a.name, b.account_id FROM domains a, parent_child b WHERE a.name=? AND a.id+0 = b.parent_id");
        Iterator i = domains.iterator();
        while (i.hasNext()) {
            String dName = (String) i.next();
            ps.setString(1, dName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Domain " + dName + " is registered by account #" + rs.getLong("account_id"));
            } else {
                System.out.println("Domain " + dName + " is unregistered");
            }
        }
        Session.closeStatement(ps);
        con.close();
    }

    public static void main(String[] argv) throws Exception {
        ArrayList list = new ArrayList();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String tmp = br.readLine();
            if (null != tmp) {
                list.add(tmp);
            } else {
                DomainChecker checker = new DomainChecker();
                checker.m15go(list);
                System.exit(0);
                return;
            }
        }
    }
}
