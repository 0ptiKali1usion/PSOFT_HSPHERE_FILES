package psoft.hsphere.resource.epayment;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/MerchantSetting.class */
public class MerchantSetting implements TemplateHashModel {

    /* renamed from: id */
    protected int f194id;
    protected String type;
    protected Date created;
    protected int period;
    protected int status;
    protected double maxAmount;
    protected int maxTransactions;
    protected int priority;
    protected double amount;
    protected int transactions;
    protected static Map map = new HashMap();
    protected Date pStart = null;
    protected Date pEnd = null;

    public double getMaxAmount() {
        return this.maxAmount;
    }

    public int getMaxTransactions() {
        return this.maxTransactions;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("id".equals(key)) {
            return new TemplateString(this.f194id);
        }
        if ("type".equals(key)) {
            return new TemplateString(this.type);
        }
        if ("pstart".equals(key)) {
            return new TemplateString(getPeriodStart());
        }
        if ("pend".equals(key)) {
            return new TemplateString(getPeriodEnd());
        }
        if ("period".equals(key)) {
            return new TemplateString(this.period);
        }
        if ("created".equals(key)) {
            return new TemplateString(this.created);
        }
        if ("status".equals(key)) {
            return new TemplateString(this.status);
        }
        if ("maxAmount".equals(key)) {
            return new TemplateString(this.maxAmount);
        }
        if ("maxTransactions".equals(key)) {
            return new TemplateString(this.maxTransactions);
        }
        if ("priority".equals(key)) {
            return new TemplateString(this.priority);
        }
        if ("amount".equals(key)) {
            return new TemplateString(getAmount());
        }
        if ("transactions".equals(key)) {
            return new TemplateString(getTransactions());
        }
        return null;
    }

    public double getAmount() {
        checkPeriod();
        return this.amount;
    }

    public int getTransactions() {
        checkPeriod();
        return this.transactions;
    }

    protected synchronized void checkPeriod() {
        if (this.pEnd == null || this.pEnd.before(TimeUtils.getDate())) {
            initPeriod();
            this.amount = 0.0d;
            this.transactions = 0;
        }
    }

    public void add(double amount) throws Exception {
        checkPeriod();
        this.amount += amount;
        this.transactions++;
    }

    public static MerchantSetting get(int id) throws Exception {
        MerchantSetting ms = (MerchantSetting) map.get(new Integer(id));
        if (ms == null) {
            ms = new MerchantSetting(id);
        }
        return ms;
    }

    protected void init() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT COUNT(amount), SUM(amount) FROM merchant_log WHERE (id = ?) AND (created > ? AND created < ?) AND ((type = 0) OR (type = 4))");
                ps.setInt(1, this.f194id);
                ps.setTimestamp(2, new Timestamp(getPeriodStart().getTime()));
                ps.setTimestamp(3, new Timestamp(getPeriodEnd().getTime()));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.amount = rs.getDouble(2);
                    this.transactions = rs.getInt(1);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException se) {
                Session.getLog().warn("problems", se);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void create(int id, String type) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO merch_gateway_settings (id, type, period_start, period, status, max_amount, max_transactions, priority) VALUES (?, ?, ?, 1, 1, 0, 0, 1)");
            ps.setInt(1, id);
            if (type == null) {
                ps.setString(2, "");
            } else {
                ps.setString(2, type);
            }
            ps.setTimestamp(3, TimeUtils.getSQLTimestamp());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public MerchantSetting(int id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT type, period_start, period, status, max_amount, max_transactions, priority FROM merch_gateway_settings WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new Exception("No Merchant Settings For ID#" + id);
            }
            init(id, rs.getString(1), rs.getDate(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5), rs.getInt(6), rs.getInt(7));
            init();
            map.put(new Integer(id), this);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void init(int id, String type, Date created, int period, int status, double maxAmount, int maxTransactions, int priority) {
        this.f194id = id;
        this.type = type;
        this.created = created;
        this.period = period;
        this.status = status;
        this.maxAmount = maxAmount;
        this.maxTransactions = maxTransactions;
        this.priority = priority;
    }

    protected void initPeriod() {
        Date now = TimeUtils.getDate();
        Date s = this.created;
        Date e = s;
        Calendar cal = TimeUtils.getCalendar(s);
        while (e.before(now)) {
            cal.add(2, this.period);
            s = e;
            e = cal.getTime();
        }
        this.pStart = s;
        this.pEnd = e;
    }

    public Date getPeriodStart() {
        checkPeriod();
        return this.pStart;
    }

    public Date getPeriodEnd() {
        checkPeriod();
        return this.pEnd;
    }
}
