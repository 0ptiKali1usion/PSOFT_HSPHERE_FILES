package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpUtils;
import org.apache.log4j.Category;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/ClientDump.class */
public class ClientDump {
    public static Category LOG;
    public static ResourceBundle config;
    public static Hashtable conf;

    public static void main(String[] argv) throws Exception {
        config = PropertyResourceBundle.getBundle("psoft_config.hsphere_3w");
        conf = new Hashtable();
        conf.put("config", config);
        Session.initMailer();
        System.out.println("1");
        Session.setDb(Toolbox.getDB(config));
        System.out.println("1");
        ClientDump main = new ClientDump();
        try {
            main.exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public String getFirst(Object param) {
        try {
            String[] x = (String[]) param;
            return x[0];
        } catch (Throwable th) {
            return "";
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/ClientDump$AccInfo.class */
    public class AccInfo {

        /* renamed from: id */
        String f31id;
        String pName;
        Date created;

        AccInfo(String id, String pName, Date created) {
            ClientDump.this = r4;
            this.f31id = id;
            this.pName = pName;
            this.created = created;
        }
    }

    String displayList(Object l) {
        if (l == null) {
            return "";
        }
        Iterator i = ((List) l).iterator();
        String str = "";
        while (true) {
            String result = str;
            if (i.hasNext()) {
                AccInfo info = (AccInfo) i.next();
                str = result + "[" + info.f31id + "#" + info.pName + "-" + info.created + "]";
            } else {
                return result;
            }
        }
    }

    public void referals() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT accounts.id, plans.description, billing_info.name, accounts.created FROM accounts, billing_info, plans WHERE accounts.plan_id = plans.id AND accounts.bi_id = billing_info.id");
            ResultSet rs = ps2.executeQuery();
            Map names = new HashMap();
            while (rs.next()) {
                ArrayList info = (List) names.get(rs.getString(3));
                if (info == null) {
                    info = new ArrayList();
                    names.put(rs.getString(3), info);
                }
                info.add(new AccInfo(rs.getString(1), rs.getString(2), rs.getDate(4)));
            }
            ps2.close();
            ps = con.prepareStatement("SELECT data, created FROM first_page_log");
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                Hashtable hash = HttpUtils.parseQueryString(rs2.getString(1));
                Set<String> set = new HashSet();
                try {
                    String name = getFirst(hash.get("fname")) + " " + getFirst(hash.get("lname"));
                    if (!name.equals(" ")) {
                        String result = rs2.getDate(2) + ";" + name + ";" + getFirst(hash.get("list3")) + ";" + displayList(names.get(name));
                        set.add(result);
                    }
                } catch (NullPointerException e) {
                }
                for (String str : set) {
                    System.out.println(str);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/ClientDump$AccBill.class */
    public class AccBill {
        Date opened;
        double setup;
        double rec;
        double onetime;
        double paid;
        double refund;
        List list = new ArrayList();
        Date closed = TimeUtils.getDate();

        public AccBill() {
            ClientDump.this = r5;
        }

        public void add(Object obj) {
            this.list.add(obj);
        }

        public void init() {
            for (DumpBill tmp : this.list) {
                tmp.init();
                this.setup += tmp.setup;
                this.rec += tmp.rec;
                this.onetime += tmp.onetime;
                this.paid += tmp.paid;
                this.refund += this.refund;
                if (this.opened == null || this.opened.after(tmp.opened)) {
                    this.opened = tmp.opened;
                }
                if (tmp.closed == null) {
                    this.closed = null;
                }
                if (this.closed != null && this.closed.before(tmp.closed)) {
                    this.closed = tmp.closed;
                }
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/ClientDump$DumpBill.class */
    public class DumpBill {
        Date opened;
        Date closed;
        List entries = new ArrayList();
        double setup;
        double rec;
        double onetime;
        double paid;
        double refund;

        public void init() {
            for (Entry tmp : this.entries) {
                if (tmp.rtype == 14) {
                    this.onetime += tmp.amount;
                } else if (tmp.type == 3 || tmp.type == 2) {
                    this.rec += tmp.amount;
                } else if (tmp.type == 1) {
                    this.setup += tmp.amount;
                } else if (tmp.type == 5 || tmp.type == 8) {
                    this.paid += tmp.amount;
                } else if (tmp.type == 4) {
                    this.refund += tmp.amount;
                } else {
                    System.err.println("Type: " + tmp.type);
                }
            }
        }

        public DumpBill(Date opened, Date closed) {
            ClientDump.this = r5;
            this.opened = opened;
            this.closed = closed;
        }

        public void add(double amount, int rtype, int type) {
            this.entries.add(new Entry(amount, rtype, type));
        }

        /* loaded from: hsphere.zip:psoft/hsphere/ClientDump$DumpBill$Entry.class */
        public class Entry {
            int type;
            int rtype;
            double amount;

            public Entry(double amount, int rtype, int type) {
                DumpBill.this = r5;
                this.amount = amount;
                this.type = type;
                this.rtype = rtype;
            }
        }
    }

    public void exec() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT DISTINCT signup_log.sell_aid, users.username FROM signup_log, users, user_account WHERE signup_log.sell_aid = user_account.account_id AND user_account.user_id = users.id");
            ResultSet rs = ps2.executeQuery();
            Map sales = new HashMap();
            while (rs.next()) {
                sales.put(rs.getString(1), rs.getString(2));
            }
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT bill.account_id, bill.id, bill_entry.amount, bill_entry.rtype, bill_entry.type, bill.opened, bill.closed FROM bill, bill_entry WHERE bill.id = bill_entry.bill_id AND bill_entry.canceled IS NULL AND bill_entry.amount != 0");
            ResultSet rs2 = ps3.executeQuery();
            Map bills = new HashMap();
            Map acc_bills = new HashMap();
            while (rs2.next()) {
                DumpBill tmpBill = (DumpBill) bills.get(rs2.getString(2));
                if (tmpBill == null) {
                    tmpBill = new DumpBill(rs2.getDate(6), rs2.getDate(7));
                    AccBill accBill = (AccBill) acc_bills.get(rs2.getString(1));
                    if (accBill == null) {
                        accBill = new AccBill();
                        acc_bills.put(rs2.getString(1), accBill);
                    }
                    accBill.add(tmpBill);
                }
                tmpBill.add(rs2.getDouble(3), rs2.getInt(4), rs2.getInt(5));
            }
            ps3.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT domains.name, parent_child.account_id FROM domains, parent_child WHERE domains.id = parent_child.child_id");
            ResultSet rs3 = ps4.executeQuery();
            Map domains = new HashMap();
            while (rs3.next()) {
                String domain = (String) domains.get(rs3.getString(2));
                domains.put(rs3.getString(2), domain == null ? rs3.getString(1) : domain + " " + rs3.getString(1));
            }
            ps4.close();
            ps = con.prepareStatement("SELECT accounts.id, billing_info.name, billing_info.type, plans.description, signup_log.sell_aid, accounts.deleted, accounts.created, credit_card.type FROM accounts, billing_info, plans, signup_log, credit_card WHERE accounts.plan_id = plans.id AND accounts.bi_id = billing_info.id AND signup_log.client_aid(+) = accounts.id AND credit_card.id(+) = billing_info.id ORDER BY accounts.deleted DESC");
            ResultSet rs4 = ps.executeQuery();
            System.out.println("ID;name;domain;card type;plan_desc;sales;setup;subscription;one time;paid;refund;opened;closed;deleted");
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            DateFormat df = new SimpleDateFormat();
            while (rs4.next()) {
                AccBill ac = (AccBill) acc_bills.get(rs4.getString(1));
                if (ac == null) {
                    ac = new AccBill();
                }
                ac.init();
                System.out.println(rs4.getString(1) + ";" + checkNull(rs4.getString(2)) + ";" + checkNull(domains.get(rs4.getString(1))) + ";" + (rs4.getString(8) == null ? rs4.getString(3) : rs4.getString(8)) + ";" + rs4.getString(4) + ";" + checkNull(sales.get(rs4.getString(5))) + ";" + nf.format(ac.setup) + ";" + nf.format(ac.rec) + ";" + nf.format(ac.onetime) + ";" + nf.format(ac.paid) + ";" + nf.format(ac.refund) + ";" + (ac.opened != null ? df.format(ac.opened) : "") + ";" + (ac.closed != null ? df.format(ac.closed) : "") + ";" + (rs4.getDate(6) != null ? df.format(rs4.getDate(6)) : ""));
            }
            System.err.println("DONE");
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected Object checkNull(Object str) {
        return str == null ? "" : str;
    }
}
