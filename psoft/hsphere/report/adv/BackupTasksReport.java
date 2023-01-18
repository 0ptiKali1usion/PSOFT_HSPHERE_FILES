package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/BackupTasksReport.class */
public class BackupTasksReport extends AdvReport {
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Timestamp start = null;
        Timestamp end = null;
        long accountId = 0;
        long planId = 0;
        int taskStatus = 0;
        int taskType = 0;
        Iterator i = args.iterator();
        String startDateStr = (String) i.next();
        String endDateStr = (String) i.next();
        String userName = (String) i.next();
        String accountIdStr = (String) i.next();
        String planIdStr = (String) i.next();
        String taskStatusStr = (String) i.next();
        String taskTypeStr = (String) i.next();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            if (startDateStr != null && startDateStr.length() > 0) {
                start = new Timestamp(sdf.parse(startDateStr).getTime());
            }
            if (endDateStr != null && endDateStr.length() > 0) {
                end = new Timestamp(sdf.parse(endDateStr).getTime());
            }
            if (accountIdStr != null) {
                try {
                    if (accountIdStr.length() > 0) {
                        accountId = Long.parseLong(accountIdStr);
                    }
                } catch (Exception ex) {
                    Session.getLog().error("Unable to parse account ID " + accountIdStr, ex);
                    throw new HSUserException("backup.schedule_report.unable_to_parse_account_id");
                }
            }
            if (planIdStr != null) {
                try {
                    if (planIdStr.length() > 0) {
                        planId = Long.parseLong(planIdStr);
                    }
                } catch (Exception ex2) {
                    Session.getLog().error("Unable to parse plan ID " + planIdStr, ex2);
                    throw new HSUserException("backup.schedule_report.unable_to_parse_plan_id");
                }
            }
            if (taskStatusStr != null) {
                try {
                    if (taskStatusStr.length() > 0) {
                        taskStatus = Integer.parseInt(taskStatusStr);
                    }
                } catch (Exception ex3) {
                    Session.getLog().error("Error parsing task status " + taskStatusStr, ex3);
                }
            }
            if (taskTypeStr != null) {
                try {
                    if (taskTypeStr.length() > 0) {
                        taskType = Integer.parseInt(taskTypeStr);
                    }
                } catch (Exception ex4) {
                    Session.getLog().error("Error parsing task type " + taskTypeStr, ex4);
                }
            }
            StringBuffer buf = new StringBuffer("SELECT bl.id, bl.commenced,bl.finished,bl.state,bl.task_type,  pc.account_id, pc.parent_id, pc.parent_type, u.username, p.description,bc.balance FROM backup_log bl, parent_child pc, user_account ua, users u,accounts a,balance_credit bc,plans p, backup b WHERE bl.parent_id = b.id AND b.performer_id = ? AND b.id = pc.child_id AND pc.account_id = ua.account_id AND ua.user_id = u.id AND pc.account_id = a.id AND a.plan_id = p.id and a.id = bc.id");
            if (start != null) {
                buf.append(" AND bl.commenced >= ?");
            }
            if (end != null) {
                buf.append(" AND bl.commenced <= ?");
            }
            if (userName != null && userName.length() > 0) {
                buf.append(" AND u.username LIKE ?");
            }
            if (accountId > 0) {
                buf.append(" AND a.id = ?");
            }
            if (planId > 0) {
                buf.append(" AND a.plan_id = ?");
            }
            if (taskStatus > 0) {
                buf.append(" AND bl.state = ?");
            }
            if (taskType > 0) {
                buf.append(" AND bl.task_type = ?");
            }
            buf.append(" ORDER BY bl.commenced");
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = Session.getDb("report");
                ps = con.prepareStatement(buf.toString());
                int params = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (start != null) {
                    params++;
                    ps.setTimestamp(params, start);
                }
                if (end != null) {
                    int i2 = params;
                    params++;
                    ps.setTimestamp(i2, end);
                }
                if (userName != null && userName.length() > 0) {
                    int i3 = params;
                    params++;
                    ps.setString(i3, userName + '%');
                }
                if (accountId > 0) {
                    int i4 = params;
                    params++;
                    ps.setLong(i4, accountId);
                }
                if (planId > 0) {
                    int i5 = params;
                    params++;
                    ps.setLong(i5, planId);
                }
                if (taskStatus > 0) {
                    int i6 = params;
                    params++;
                    ps.setInt(i6, taskStatus);
                }
                if (taskType > 0) {
                    int i7 = params;
                    int i8 = params + 1;
                    ps.setInt(i7, taskType);
                }
                Session.getLog().debug("SQL statement is");
                Session.getLog().debug(ps.toString());
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("id", rs.getString("id"));
                    hashMap.put("account_id", rs.getString("account_id"));
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("task_scheduled", df.format((Date) rs.getTimestamp("commenced")));
                    hashMap.put("task_processed", rs.getTimestamp("finished") == null ? Localizer.translateMessage("backup.task.processed.not_processed") : df.format((Date) rs.getTimestamp("finished")));
                    hashMap.put("task_status", getTaskStateDescription(rs.getInt("state")));
                    hashMap.put("task_status_id", rs.getString("state"));
                    hashMap.put("task_type", getTaskTypeDescription(rs.getInt("task_type")));
                    try {
                        hashMap.put("task_object", TypeRegistry.getDescription(rs.getInt("parent_type")));
                    } catch (NoSuchTypeException e) {
                        hashMap.put("task_object", "UNKNOWN");
                    }
                    try {
                        hashMap.put("task_object_name", getTaskObjectName(rs.getInt("parent_type"), rs.getLong("parent_id")));
                    } catch (Exception ex5) {
                        Session.getLog().error("Unable to get backup object name:", ex5);
                        hashMap.put("task_object_name", "UNKNOWN");
                    }
                    hashMap.put("balance", rs.getString("balance"));
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        } catch (Exception ex6) {
            Session.getLog().error("Unable to parse start of end date. startdate=" + startDateStr + " endDate=" + endDateStr, ex6);
            throw new HSUserException("backup.schedule_report.unable_to_parse_date");
        }
    }

    private static String getTaskStateDescription(int taskStateId) {
        switch (taskStateId) {
            case 1:
                return Localizer.translateMessage("backup.task.state_planned");
            case 2:
                return Localizer.translateMessage("backup.task.state_commenced");
            case 3:
                return Localizer.translateMessage("backup.task.state_finished");
            case 4:
                return Localizer.translateMessage("backup.task.state_canceled");
            default:
                return null;
        }
    }

    private static String getTaskTypeDescription(int taskTypeId) {
        switch (taskTypeId) {
            case 1:
                return Localizer.translateMessage("backup.task.type_scheduled");
            case 2:
                return Localizer.translateMessage("backup.task.type_on_demand");
            default:
                return null;
        }
    }

    private static String getTaskObjectName(int type, long id) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb("report");
        if (type != 7100) {
            Session.closeStatement(null);
            con.close();
            return "";
        }
        try {
            ps = con.prepareStatement("SELECT name FROM dedicated_servers WHERE rid = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                Session.closeStatement(ps);
                con.close();
                return "";
            }
            String string = rs.getString("name");
            Session.closeStatement(ps);
            con.close();
            return string;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
