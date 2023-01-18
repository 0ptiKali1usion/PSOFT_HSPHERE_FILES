package psoft.hsphere.report.adv;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
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
import psoft.util.freemarker.TemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/TTReport.class */
public class TTReport extends AdvReport implements TemplateHashModel, TemplateMethodModel {
    protected StringBuffer where = new StringBuffer();

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    protected void daterange(String varname, Object before, Object after) {
        if (before != null) {
            this.where.append(" AND ").append(varname).append(" < to_date('").append(before).append("', 'MM/DD/YYYY')");
        }
        if (after != null) {
            this.where.append(" AND ").append(varname).append(" >= to_date('").append(after).append("', 'MM/DD/YYYY')");
        }
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        int c = 0;
        int count = 2;
        Iterator i = args.iterator();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        String closedBefore = (String) i.next();
        String closedAfter = (String) i.next();
        String notClose = (String) i.next();
        String assigned = (String) i.next();
        String title = (String) i.next();
        String type = (String) i.next();
        String answeredBefore = (String) i.next();
        String answeredAnswered = (String) i.next();
        String notAnswered = (String) i.next();
        String modifiedBefore = (String) i.next();
        String modifiedAnswered = (String) i.next();
        String contains = (String) i.next();
        String matchCase = (String) i.next();
        String tt_id = (String) i.next();
        String email = (String) i.next();
        String state = (String) i.next();
        String quid = (String) i.next();
        String show = (String) i.next();
        StringBuffer buf = new StringBuffer("SELECT id, title, assigned, type, created, closed, answered, modified, email, cc, state, priority, queue_id FROM ticket WHERE reseller_id = ?");
        daterange("created", createdBefore, createdAfter);
        daterange("closed", closedBefore, closedAfter);
        daterange("answered", answeredBefore, answeredAnswered);
        daterange("modified", modifiedBefore, modifiedAnswered);
        if (!isEmpty(tt_id)) {
            this.where.append(" AND id = ?");
        }
        if (!isEmpty(state)) {
            this.where.append(" AND state = ?");
        }
        if (!isEmpty(quid)) {
            this.where.append(" AND queue_id = ?");
        }
        if (!isEmpty(title)) {
            this.where.append(" AND UPPER(title) LIKE ?");
        }
        if (notClose != null) {
            this.where.append(" AND closed IS NULL");
        }
        if (notAnswered != null) {
            this.where.append(" AND (answered < modified OR answered IS NULL) AND state <> 9");
            if ("unanswered".equals(notAnswered)) {
                this.where.append(" ORDER by priority desc");
            }
        }
        if (assigned != null) {
            if ("-1".equals(assigned) || "unassigned".equals(assigned)) {
                this.where.append(" AND assigned IS NULL");
            } else {
                this.where.append(" AND assigned = ").append(assigned);
            }
        }
        if (type != null) {
            this.where.append(" AND type = ").append(type);
        }
        if (email != null) {
            this.where.append(" AND UPPER(email) LIKE '%").append(email.toString().toUpperCase()).append("%'");
        }
        buf.append(this.where);
        if (!"unanswered".equals(notAnswered)) {
            buf.append(" ORDER BY created");
        }
        if ("unassigned".equals(assigned)) {
            buf.append(" desc");
        }
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            try {
                con = Session.getDb("report");
                ps = con.prepareStatement(buf.toString());
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(tt_id)) {
                    count = 2 + 1;
                    ps.setLong(2, Long.parseLong(tt_id));
                }
                if (!isEmpty(state)) {
                    int i2 = count;
                    count++;
                    ps.setInt(i2, Integer.parseInt(state));
                }
                if (!isEmpty(quid)) {
                    int i3 = count;
                    count++;
                    ps.setInt(i3, Integer.parseInt(quid));
                }
                if (!isEmpty(title)) {
                    int i4 = count;
                    int i5 = count + 1;
                    ps.setString(i4, "%" + title.toUpperCase() + "%");
                }
                Session.getLog().info("-->Query: " + ((Object) buf));
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                Map admins = Ticket.getAdmins();
                ps2 = con.prepareStatement("SELECT message FROM ttmessage WHERE id = ?");
                boolean bodySearch = contains != null;
                String searchStr = null;
                if (bodySearch) {
                    searchStr = matchCase == null ? contains.toString().toLowerCase() : contains.toString();
                }
                int cnt = Integer.valueOf(show.toString()).intValue();
                while (rs.next() && c < cnt) {
                    if ("unassigned".equals(assigned) || "unanswered".equals(notAnswered)) {
                        c++;
                    }
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
                    hashMap.put("state", new Long(rs.getLong(11)));
                    hashMap.put("priority", new Long(rs.getLong(12)));
                    hashMap.put("quid", new Long(rs.getLong(13)));
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
                    String ttEmail = rs.getString(9);
                    String ttCc = rs.getString(10);
                    hashMap.put("email", ttEmail);
                    hashMap.put("highlight", Ticket.isHigh(ttEmail, ttCc) ? "1" : "");
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

    @Override // psoft.hsphere.report.AdvReport
    public TemplateModel get(String key) {
        if ("status".equals(key)) {
            return STATUS_OK;
        }
        if ("id".equals(key)) {
            return new TemplateString(this.f116id);
        }
        if ("size".equals(key)) {
            return new TemplateString(this.data.size());
        }
        if ("isNext".equals(key)) {
            return new TemplateString(this.endPos < this.data.size());
        } else if ("isPrev".equals(key)) {
            return new TemplateString(this.startPos > 0);
        } else if ("pages".equals(key)) {
            return this.pages;
        } else {
            if ("currentPage".equals(key)) {
                return new TemplateString(this.currentPage);
            }
            if ("field".equals(key)) {
                return this.field;
            }
            if ("direction".equals(key)) {
                return this.direction;
            }
            try {
                return TemplateMethodWrapper.getMethod(this, key);
            } catch (IllegalArgumentException iae) {
                Session.getLog().info("GET ERROR", iae);
                return null;
            }
        }
    }
}
