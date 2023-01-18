package psoft.hsphere.report.adv;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.hsphere.resource.p004tt.Ticket;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/AllUserTTReport.class */
public class AllUserTTReport extends AdvReport {
    protected StringBuffer where = new StringBuffer();

    protected void daterange(String varname, Object before, Object after) {
        if (before != null) {
            addAND(varname + " < to_date('" + before + "', 'MM/DD/YYYY')");
        }
        if (after != null) {
            addAND(varname + " >= to_date('" + after + "', 'MM/DD/YYYY')");
        }
    }

    protected void addAND(String str) {
        if (this.where.length() == 0) {
            this.where.append(str);
        } else {
            this.where.append(" AND ").append(str);
        }
    }

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        String resellerId = (String) i.next();
        Object createdBefore = i.next();
        Object createdAfter = i.next();
        Object closedBefore = i.next();
        Object closedAfter = i.next();
        Object notClose = i.next();
        Object assigned = i.next();
        Object title = i.next();
        Object type = i.next();
        Object answeredBefore = i.next();
        Object answeredAnswered = i.next();
        Object notAnswered = i.next();
        Object modifiedBefore = i.next();
        Object modifiedAnswered = i.next();
        Object contains = i.next();
        Object matchCase = i.next();
        Object tt_id = i.next();
        Object email = i.next();
        Object quid = i.next();
        Object state = i.next();
        StringBuffer buf = new StringBuffer("SELECT id, title, assigned, type, created, closed, answered, modified, email, state FROM ticket ");
        if (!isEmpty(resellerId)) {
            addAND(" reseller_id LIKE " + resellerId);
        }
        daterange("created", createdBefore, createdAfter);
        daterange("closed", closedBefore, closedAfter);
        daterange("answered", answeredBefore, answeredAnswered);
        daterange("modified", modifiedBefore, modifiedAnswered);
        if (tt_id != null) {
            addAND(" id = " + tt_id.toString());
        }
        if (state != null) {
            addAND(" state = " + state.toString());
        }
        if (quid != null) {
            addAND(" queue_id = " + quid.toString());
        }
        if (title != null) {
            addAND(" UPPER(title) like '%" + title.toString().toUpperCase() + "%'");
        }
        if (notClose != null) {
            addAND(" closed IS NULL");
        }
        if (notAnswered != null) {
            addAND(" (answered < modified OR answered IS NULL)");
        }
        if (assigned != null) {
            if ("-1".equals(assigned)) {
                addAND(" assigned IS NULL");
            } else {
                addAND(" assigned = " + assigned);
            }
        }
        if (type != null) {
            this.where.append(" AND type = ").append(type);
        }
        if (email != null) {
            addAND(" UPPER(email) LIKE '%" + email.toString().toUpperCase() + "%'");
        }
        if (this.where.length() == 0) {
            buf.append(" ORDER BY created");
        } else {
            buf.append(" WHERE ").append(this.where);
            buf.append(" ORDER BY created");
        }
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            try {
                con = Session.getDb("report");
                ps = con.prepareStatement(buf.toString());
                Session.getLog().info("Query ---> " + ((Object) buf));
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                Map admins = Ticket.getAdmins();
                ps2 = con.prepareStatement("SELECT message FROM ttmessage WHERE id = ?");
                boolean bodySearch = contains != null;
                String searchStr = null;
                if (bodySearch) {
                    searchStr = matchCase == null ? contains.toString().toLowerCase() : contains.toString();
                }
                while (rs.next()) {
                    if (bodySearch) {
                        ps2.setLong(1, rs.getLong(1));
                        ResultSet rs2 = ps2.executeQuery();
                        boolean wasFound = false;
                        while (rs2.next()) {
                            String message = Session.getClobValue(rs2, 1);
                            if (message != null && !"".equals(message)) {
                                if (matchCase != null) {
                                    wasFound = message.indexOf(searchStr) >= 0;
                                } else {
                                    wasFound = message.toLowerCase().indexOf(searchStr) >= 0;
                                }
                                if (wasFound) {
                                    break;
                                }
                            }
                        }
                        if (!wasFound) {
                        }
                    }
                    HashMap hashMap = new HashMap();
                    hashMap.put("id", new Long(rs.getLong(1)));
                    hashMap.put("title", rs.getString(2));
                    hashMap.put("state", new Long(rs.getLong(10)));
                    String admId = rs.getString(3);
                    if (admId != null) {
                        try {
                            TemplateHashModel th = (TemplateHashModel) admins.get(admId);
                            if (th != null) {
                                hashMap.put("assigned", th.get("name").toString());
                                hashMap.put("assigned_id", th.get("id"));
                            }
                        } catch (TemplateModelException tme) {
                            Session.getLog().warn("Error: " + tme);
                        }
                    }
                    hashMap.put("type", new Integer(rs.getInt(4)));
                    hashMap.put("created", rs.getTimestamp(5));
                    hashMap.put("closed", rs.getTimestamp(6));
                    hashMap.put("answered", rs.getTimestamp(7));
                    hashMap.put("modified", rs.getTimestamp(8));
                    hashMap.put("email", rs.getString(9));
                    data.add(hashMap);
                }
                Session.getLog().info("Field in data");
                init(new DataContainer(data));
                setOrderParams("created", true);
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
            } catch (Throwable th2) {
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se12) {
                        se12.printStackTrace();
                        throw th2;
                    }
                }
                throw th2;
            }
        } catch (SQLException se) {
            Session.getLog().error("error getting the report", se);
            throw se;
        }
    }
}
