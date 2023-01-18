package psoft.hsphere.functions;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Session;
import psoft.util.NFUCache;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/functions/DiskUsageDetailed.class */
public class DiskUsageDetailed implements TemplateHashModel {
    private static NFUCache cache = new NFUCache(30, 43200000);

    public TemplateModel get(String key) {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    /* JADX WARN: Finally extract failed */
    private TemplateListModel getDetails(long account_id, String dateFrom, String dateTo) throws Exception {
        String key;
        String key2 = account_id + dateFrom;
        if (dateTo == null || dateTo.equals("") || dateTo.equals("now")) {
            key = key2 + DateFormat.getDateInstance(3).format(TimeUtils.getDate());
        } else {
            key = key2 + dateTo;
        }
        if (cache.get(key) != null) {
            return (TemplateListModel) cache.get(key);
        }
        Connection con = null;
        try {
            String[] sqlStatement_parts = new String[10];
            sqlStatement_parts[0] = new String("SELECT SUM(u.used)/COUNT(DISTINCT(u.cdate)) as usage, d.name as description, 3 as type FROM usage_log u, domains d, mail_domain m, parent_child p1, parent_child p2 WHERE u.resource_id = m.id AND m.id = p1.child_id AND p1.parent_id = p2.child_id AND p2.parent_id = d.id AND u.account_id = ? AND u.usage_type = 3 ");
            sqlStatement_parts[1] = new String(" GROUP BY d.name ");
            sqlStatement_parts[2] = new String("UNION SELECT SUM(u.used)/COUNT(DISTINCT(u.cdate)) as usage, users.username as description, 1 as type FROM usage_log u, users, user_account ua1, user_account ua2 WHERE u.account_id = ua1.account_id AND users.id = ua2.user_id AND ua2.account_id = ua1.account_id AND u.account_id = ? AND (u.usage_type = 1 OR u.usage_type = 5 OR u.usage_type = 12) ");
            sqlStatement_parts[3] = new String(" GROUP BY users.username ");
            sqlStatement_parts[4] = new String("UNION SELECT SUM(u.used)/COUNT(DISTINCT(u.cdate)) as usage, tn.description as description, 4 as type FROM usage_log u, parent_child p1, parent_child p2, type_name tn WHERE u.account_id=? AND u.usage_type=4 AND p1.child_id=u.resource_id AND p2.child_type=tn.id AND p2.child_id=p1.child_id ");
            sqlStatement_parts[5] = new String(" GROUP BY tn.description ");
            sqlStatement_parts[6] = new String("UNION SELECT SUM(u.used)/COUNT(DISTINCT(u.cdate)) as usage, tn.description as description, 18 as type FROM usage_log u, parent_child p1, parent_child p2, type_name tn WHERE u.account_id=? AND u.usage_type=18 AND p1.child_id=u.resource_id AND p2.child_type=tn.id AND p2.child_id=p1.child_id ");
            sqlStatement_parts[7] = new String(" GROUP BY tn.description ");
            sqlStatement_parts[8] = new String("UNION SELECT SUM(u.used)/COUNT(DISTINCT(u.cdate)) as usage, tn.description as description, 15 as type FROM usage_log u, parent_child p1, parent_child p2, type_name tn WHERE u.account_id=? AND  u.usage_type=15 AND p1.child_id=u.resource_id AND p2.child_type=tn.id AND p2.child_id=p1.child_id ");
            sqlStatement_parts[9] = new String(" GROUP BY tn.description ");
            for (int i = 0; i < 10; i += 2) {
                if (!isEmpty(dateFrom)) {
                    sqlStatement_parts[i] = sqlStatement_parts[i].concat(" AND u.cdate >= ?");
                }
                if (!isEmpty(dateTo)) {
                    sqlStatement_parts[i] = sqlStatement_parts[i].concat(" AND u.cdate < ?");
                }
            }
            con = Session.getDb();
            String sqlStatement = new String("");
            for (int i2 = 0; i2 < 10; i2++) {
                sqlStatement = sqlStatement.concat(sqlStatement_parts[i2]);
            }
            PreparedStatement ps = con.prepareStatement(sqlStatement);
            int counter = 1;
            for (int i3 = 0; i3 < 5; i3++) {
                int i4 = counter;
                counter++;
                ps.setLong(i4, account_id);
                if (!isEmpty(dateFrom)) {
                    counter++;
                    ps.setDate(counter, new Date(HsphereToolbox.parseShortDate(dateFrom).getTime()));
                }
                if (!isEmpty(dateTo)) {
                    int i5 = counter;
                    counter++;
                    ps.setDate(i5, new Date(HsphereToolbox.parseShortDate(dateTo).getTime()));
                }
            }
            ResultSet rs = ps.executeQuery();
            TemplateList listOfUsages = new TemplateList();
            while (rs.next()) {
                float used = rs.getFloat(1);
                String description = rs.getString(2);
                int type = rs.getInt(3);
                if (type == 3) {
                    listOfUsages.add((TemplateModel) new UsageInfo(new String("Mail for domain " + description), used));
                } else if (type == 1 || type == 5 || type == 12) {
                    listOfUsages.add((TemplateModel) new UsageInfo(new String("User account for user " + description), used));
                } else if (type == 4) {
                    listOfUsages.add((TemplateModel) new UsageInfo(new String("MySQL Database for user " + description), used));
                } else if (type == 18) {
                    listOfUsages.add((TemplateModel) new UsageInfo(new String("PGSQL Database for user " + description), used));
                } else if (type == 15) {
                    listOfUsages.add((TemplateModel) new UsageInfo(new String("MS SQL Database for user " + description), used));
                } else {
                    listOfUsages.add((TemplateModel) new UsageInfo(description, used));
                }
            }
            cache.put(key, listOfUsages);
            if (con != null) {
                con.close();
            }
            return listOfUsages;
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public TemplateListModel FM_getAdminDetails(String account_id, String dateFrom, String dateTo) throws Exception {
        long retrievedAccount_id = Long.parseLong(account_id);
        long currentAccount_id = Session.getAccountId();
        if (currentAccount_id != 1) {
            Session.getLog().info("Security does not allow to get disk usage info!");
            return null;
        }
        return getDetails(retrievedAccount_id, dateFrom, dateTo);
    }

    public TemplateListModel FM_getReselDetails(String account_id, String dateFrom, String dateTo) throws Exception {
        long retrievedAccount_id = Long.parseLong(account_id);
        Account account = Account.getAccount(retrievedAccount_id);
        long resellerId = account.getResellerId();
        long currentResellerId = Session.getResellerId();
        if (resellerId == currentResellerId && Session.getAccount().FM_getChild(FMACLManager.ADMIN) != null) {
            return getDetails(retrievedAccount_id, dateFrom, dateTo);
        }
        Session.getLog().info("Security does not allow to get disk usage info!");
        return null;
    }

    public TemplateListModel FM_getDetails(String dateFrom, String dateTo) throws Exception {
        long currentAccount_id = Session.getAccountId();
        return getDetails(currentAccount_id, dateFrom, dateTo);
    }

    protected boolean isEmpty(String str) {
        return str == null || str.length() == 0 || str.equals("%");
    }

    public boolean isEmpty() {
        return false;
    }
}
