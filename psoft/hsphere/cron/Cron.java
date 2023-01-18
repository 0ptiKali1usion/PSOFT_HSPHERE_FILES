package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Category;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/Cron.class */
public abstract class Cron extends BackgroundJob {
    private static final Category log = Category.getInstance(Cron.class.getName());

    public Cron(C0004CP cp) throws Exception {
        super(cp);
        try {
            if (!getLastStartInfo()) {
                this.lastStart = TimeUtils.currentTimeMillis();
            }
        } catch (Exception e) {
            getLog().error("Job " + getFullMark() + " starting error ", e);
        }
    }

    protected boolean getLastStartInfo() throws Exception {
        boolean res = false;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT value, last_user FROM last_start WHERE name = ?");
                ps.setString(1, getDBMark());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getTimestamp(1) != null) {
                        this.lastStart = rs.getTimestamp(1).getTime();
                    }
                    this.lastUser = rs.getLong(2);
                    res = true;
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                getLog().error("Error getting JOB_LAST_START info", e);
                Session.closeStatement(ps);
                con.close();
            }
            return res;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void setLastUser(long userId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE last_start SET last_user = ? WHERE name = ?");
                ps.setLong(1, userId);
                ps.setString(2, getDBMark());
                int count = ps.executeUpdate();
                if (count < 1) {
                    ps.close();
                    ps = con.prepareStatement("INSERT INTO last_start(name,last_user)  VALUES(?, ?)");
                    ps.setString(1, getDBMark());
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                }
                this.lastUser = userId;
                getLog().debug("SET LAST USER TO " + userId);
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                getLog().error("Error setting LAST_USER", e);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
