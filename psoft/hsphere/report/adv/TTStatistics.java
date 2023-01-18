package psoft.hsphere.report.adv;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/TTStatistics.class */
public class TTStatistics extends AdvReport implements TemplateHashModel, TemplateMethodModel {
    protected int count_all;
    protected int count_today;
    protected int count_yesterday;
    protected int count_week;
    protected int count_month;
    protected int count_unassigned;

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Connection con = null;
        try {
            try {
                con = Session.getDb("report");
                long resel_id = Session.getResellerId();
                PreparedStatement ps3 = con.prepareStatement("SELECT count(*) FROM ticket WHERE reseller_id = ?");
                PreparedStatement ps4 = con.prepareStatement("SELECT count(*) FROM ticket where created > ? AND reseller_id = ?");
                PreparedStatement ps5 = con.prepareStatement("SELECT count(*) FROM ticket where created >= ? AND created < ? AND reseller_id = ?");
                PreparedStatement ps6 = con.prepareStatement("SELECT count(*) FROM ticket where created >= ? AND reseller_id = ?");
                PreparedStatement ps7 = con.prepareStatement("SELECT count(*) FROM ticket where created >= ? AND reseller_id = ?");
                PreparedStatement ps8 = con.prepareStatement("SELECT count(*) FROM ticket where assigned is null AND reseller_id = ?");
                ps3.setLong(1, resel_id);
                Calendar cal = TimeUtils.getCalendar();
                cal.getTime();
                ps4.setDate(1, new Date(cal.getTime().getTime()));
                ps4.setLong(2, resel_id);
                ps5.setDate(2, new Date(cal.getTime().getTime()));
                cal.add(5, -1);
                ps5.setDate(1, new Date(cal.getTime().getTime()));
                ps5.setLong(3, resel_id);
                cal.add(5, -5);
                ps6.setDate(1, new Date(cal.getTime().getTime()));
                ps6.setLong(2, resel_id);
                cal.add(5, -24);
                ps7.setDate(1, new Date(cal.getTime().getTime()));
                ps7.setLong(2, resel_id);
                ps8.setLong(1, resel_id);
                ResultSet rs3 = ps3.executeQuery();
                if (rs3.next()) {
                    this.count_all = rs3.getInt(1);
                }
                Session.closeStatement(ps3);
                ResultSet rs4 = ps4.executeQuery();
                if (rs4.next()) {
                    this.count_today = rs4.getInt(1);
                } else {
                    this.count_today = 0;
                }
                Session.closeStatement(ps4);
                ResultSet rs5 = ps5.executeQuery();
                if (rs5.next()) {
                    this.count_yesterday = rs5.getInt(1);
                } else {
                    this.count_yesterday = 0;
                }
                Session.closeStatement(ps5);
                ResultSet rs6 = ps6.executeQuery();
                if (rs6.next()) {
                    this.count_week = rs6.getInt(1);
                } else {
                    this.count_week = 0;
                }
                Session.closeStatement(ps6);
                ResultSet rs7 = ps7.executeQuery();
                if (rs7.next()) {
                    this.count_month = rs7.getInt(1);
                } else {
                    this.count_month = 0;
                }
                Session.closeStatement(ps7);
                ResultSet rs8 = ps8.executeQuery();
                if (rs8.next()) {
                    this.count_unassigned = rs8.getInt(1);
                } else {
                    this.count_unassigned = 0;
                }
                Session.closeStatement(ps8);
                Session.closeStatement(null);
                Session.closeStatement(null);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
            } catch (SQLException se) {
                Session.getLog().error("error getting the report", se);
                throw se;
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            Session.closeStatement(null);
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException se12) {
                    se12.printStackTrace();
                    throw th;
                }
            }
            throw th;
        }
    }

    @Override // psoft.hsphere.report.AdvReport
    public TemplateModel get(String key) {
        if ("count_all".equals(key)) {
            return new TemplateString(this.count_all);
        }
        if ("count_today".equals(key)) {
            return new TemplateString(this.count_today);
        }
        if ("count_yesterday".equals(key)) {
            return new TemplateString(this.count_yesterday);
        }
        if ("count_week".equals(key)) {
            return new TemplateString(this.count_week);
        }
        if ("count_month".equals(key)) {
            return new TemplateString(this.count_month);
        }
        if ("count_unassigned".equals(key)) {
            return new TemplateString(this.count_unassigned);
        }
        try {
            return TemplateMethodWrapper.getMethod(this, key);
        } catch (IllegalArgumentException iae) {
            Session.getLog().info("GET ERROR", iae);
            return null;
        }
    }
}
