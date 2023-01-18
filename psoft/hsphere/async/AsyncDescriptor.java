package psoft.hsphere.async;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/async/AsyncDescriptor.class */
public class AsyncDescriptor implements TemplateHashModel {
    private static final Category log = Category.getInstance(AsyncDescriptor.class.getName());
    public static final int COMPLETE = 0;
    public static final int WAIT = 1;
    public static final int DECLINED = 2;
    public static final int ERROR = 3;
    public static final int TIMEOUT = 4;
    public static final int DELETED = 5;
    long userId;
    ResourceId rid;
    String description;
    Date startDate;
    Date lastCheck;
    int state;
    int maxDelay;
    int interval;
    String error;
    int errorCode;

    private void save() throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO async_desc (user_id, rid, description, start_date, state, max_delay, interval) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, this.userId);
            ps.setString(2, this.rid.toString());
            ps.setString(3, this.description);
            ps.setTimestamp(4, new Timestamp(this.startDate.getTime()));
            ps.setInt(5, this.state);
            ps.setInt(6, this.maxDelay);
            ps.setInt(7, this.interval);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void remove() throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM async_desc WHERE rid = ?");
            ps.setString(1, this.rid.toString());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public AsyncDescriptor(AsyncResource ar) throws SQLException {
        this.userId = Session.getUser().getId();
        this.rid = ((Resource) ar).getId();
        this.description = ar.getAsyncDescription();
        this.startDate = TimeUtils.getDate();
        this.state = 1;
        this.maxDelay = ar.getAsyncTimeout();
        this.interval = ar.getAsyncInterval();
        save();
    }

    private AsyncResource getResource() throws Exception {
        return (AsyncResource) Resource.get(this.rid);
    }

    public static Collection load() throws SQLException {
        List l = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT user_id, rid, description, start_date, last_check, state, max_delay, interval, error, error_code FROM async_desc");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                l.add(new AsyncDescriptor(rs.getLong("user_id"), new ResourceId(rs.getString("rid")), rs.getString("description"), rs.getTimestamp("start_date"), rs.getTimestamp("last_check"), rs.getInt("state"), rs.getInt("max_delay"), rs.getInt("interval"), rs.getString("error"), rs.getInt("error_code")));
            }
            Session.closeStatement(ps);
            con.close();
            return l;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void update() throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE async_desc SET last_check = ?, state = ?, error = ?, error_code = ? WHERE rid = ?");
            if (this.lastCheck != null) {
                ps.setTimestamp(1, new Timestamp(this.lastCheck.getTime()));
            } else {
                ps.setNull(1, 93);
            }
            ps.setInt(2, this.state);
            if (this.error != null) {
                ps.setString(3, this.error);
            } else {
                ps.setNull(3, 12);
            }
            ps.setInt(4, this.errorCode);
            ps.setString(5, this.rid.toString());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void reset() throws SQLException {
        this.state = 1;
        this.errorCode = 0;
        this.lastCheck = null;
        this.error = null;
    }

    public void cancel() throws Exception {
        if (this.state != 5) {
            Session.save();
            try {
                Session.setUser(User.getUser(this.userId));
                Session.setAccount(this.rid.getAccount());
                getResource().asyncDelete();
                this.state = 5;
                Session.restore();
                update();
            } catch (Throwable th) {
                this.state = 5;
                Session.restore();
                throw th;
            }
        }
    }

    public AsyncDescriptor(long userId, ResourceId rid, String description, Date startDate, Date lastCheck, int state, int maxDelay, int interval, String error, int errorCode) {
        this.userId = userId;
        this.rid = rid;
        this.description = description;
        this.startDate = startDate;
        this.lastCheck = lastCheck;
        this.state = state;
        this.maxDelay = maxDelay;
        this.interval = interval;
        this.error = error;
        this.errorCode = errorCode;
    }

    public void check() throws Exception {
        if (this.state != 1) {
            return;
        }
        Date now = TimeUtils.getDate();
        Calendar c = Calendar.getInstance();
        if (this.lastCheck != null) {
            c.setTime(this.lastCheck);
            c.add(10, this.interval);
            if (now.before(c.getTime())) {
                return;
            }
        }
        Session.save();
        try {
            Session.setUser(User.getUser(this.userId));
            Session.setAccount(this.rid.getAccount());
            AsyncResource r = getResource();
            this.lastCheck = now;
            try {
                boolean isComplete = r.isAsyncComplete();
                if (isComplete) {
                    this.state = 0;
                } else {
                    c.setTime(this.startDate);
                    c.add(10, this.maxDelay);
                    if (this.lastCheck.after(c.getTime())) {
                        this.state = 4;
                    } else {
                        this.state = 1;
                    }
                }
            } catch (AsyncDeclinedException ade) {
                processError(3, r, ade);
            } catch (AsyncResourceException are) {
                processError(2, r, are);
            }
        } catch (Exception e) {
            this.state = 3;
            this.errorCode = 100;
            if (e instanceof InvocationTargetException) {
                this.error = ((InvocationTargetException) e).getTargetException().getMessage();
            } else {
                this.error = e.getMessage();
            }
        } finally {
            Session.restore();
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("user_id".equals(key)) {
            return new TemplateString(this.userId);
        }
        if (!FMACLManager.USER.equals(key)) {
            return "rid".equals(key) ? this.rid : "description".equals(key) ? new TemplateString(this.description) : "start_date".equals(key) ? new TemplateString(this.startDate) : "last_check".equals(key) ? new TemplateString(this.lastCheck) : "state".equals(key) ? new TemplateString(this.state) : "max_delay".equals(key) ? new TemplateString(this.maxDelay) : "interval".equals(key) ? new TemplateString(this.interval) : "error".equals(key) ? new TemplateString(this.error) : "error_code".equals(key) ? new TemplateString(this.errorCode) : new TemplateString("<!-- UNKNOWN KEY FOR AsyncDscriptor: " + key + " -->");
        }
        try {
            return User.getUser(this.userId);
        } catch (Exception e) {
            log.warn(e);
            return new TemplateString(Localizer.translateMessage("label.not_available_short"));
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public long getUserId() {
        return this.userId;
    }

    public ResourceId getRid() {
        return this.rid;
    }

    public String getDescription() {
        return this.description;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public Date getLastCheck() {
        return this.lastCheck;
    }

    public int getState() {
        return this.state;
    }

    public int getMaxDelay() {
        return this.maxDelay;
    }

    public int getInterval() {
        return this.interval;
    }

    public String getError() {
        return this.error;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    private void processError(int state, AsyncResource r, AsyncResourceException are) throws Exception {
        this.error = are.getMessage();
        this.errorCode = are.getCode();
        if (r.isAsyncAutoRemove()) {
            r.asyncDelete();
            this.state = 5;
            return;
        }
        this.state = state;
    }

    public void setError(int errorCode, String error) {
        this.errorCode = errorCode;
        this.error = error;
    }
}
