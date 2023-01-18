package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.SummaryQuota;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/cron/OverLimitWarnCron.class */
public class OverLimitWarnCron extends BackgroundJob {
    protected transient Map boxesByAccount;
    public static final String[] quotaLikeResourses = {"quota", "mail_quota", "winquota", "mysqldb_quota", "pgsqldb_quota", "MSSQLQuota", "summary_quota"};
    public static final String[] trafficLikeResourses = {"traffic", "reseller_traffic"};
    protected Boolean lockBoxesLoading;

    public OverLimitWarnCron(C0004CP cp) throws Exception {
        super(cp, "OVERLIMIT");
        this.lockBoxesLoading = new Boolean(true);
    }

    public OverLimitWarnCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
        this.lockBoxesLoading = new Boolean(true);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        Connection con = Session.getDb();
        try {
            Date startDate = TimeUtils.getDate();
            PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE id > ? ORDER BY id");
            ps.setLong(1, this.lastUser);
            ResultSet rs = ps.executeQuery();
            if ((this.lastUser == 0 || !isProgressInitialized()) && rs.last()) {
                setProgress(rs.getRow(), 1, 0);
            }
            if (rs.first()) {
                int countUser = 0;
                while (true) {
                    if (isInterrupted()) {
                        getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
                        break;
                    }
                    checkSuspended();
                    try {
                        User u = User.getUser(rs.getString(1));
                        getLog().debug("OverLimitWarnCron: The user is " + u.getLogin());
                        try {
                            Session.setUser(u);
                            countUser++;
                            Iterator i = u.getAccountIds().iterator();
                            while (i.hasNext()) {
                                try {
                                    Account a = u.getAccount((ResourceId) i.next());
                                    Session.setAccount(a);
                                    Session.setLanguage(new Language(null));
                                    getLog().info("OverLimitWarnCron: Account " + a.getId().getId());
                                    freshStatusMessage("Working on account #" + a.getId().getId());
                                    sendOverLimitWarnCron(a);
                                } catch (Exception ex) {
                                    getLog().error("Over Limit cron exception", ex);
                                } catch (Throwable tr) {
                                    getLog().error("Critical over Limit cron exception", tr);
                                }
                            }
                            addProgress(1, "Finished processing user '" + u.getLogin() + "'");
                            if (countUser > 10) {
                                saveLastUser(u.getId());
                                countUser = 0;
                            }
                        } catch (UnknownResellerException e) {
                            getLog().warn("Live client of removed reseller", e);
                        }
                    } catch (Exception e2) {
                        getLog().debug("Can't get user " + rs.getString(1) + " skipping", e2);
                    }
                    if (!rs.next()) {
                        break;
                    }
                }
            }
            long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
            getLog().info("OverLimit Cron FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    private Map getBoxesByAccounts() throws Exception {
        Map map;
        synchronized (this.lockBoxesLoading) {
            if (this.boxesByAccount == null) {
                List warnBoxes = new ArrayList();
                HashSet usedHosts = new HashSet();
                Iterator i = HostManager.getHostsByGroupType(3).iterator();
                while (i.hasNext()) {
                    HostEntry host = (HostEntry) i.next();
                    try {
                        if (!usedHosts.contains(host)) {
                            getLog().debug("Trying load mail warn from host:" + host.getName());
                            Collection c = host.exec("mailwarn-cat.pl", new ArrayList());
                            warnBoxes.addAll(c);
                            usedHosts.add(host);
                        }
                    } catch (Exception e) {
                        getLog().debug("Error getting mail warn from host" + host.getName(), e);
                    }
                }
                this.boxesByAccount = sortOutBoxes(warnBoxes);
            }
            map = this.boxesByAccount;
        }
        return map;
    }

    protected Map sortOutBoxes(List warnBoxes) throws Exception {
        Map map = new HashMap();
        Iterator i = warnBoxes.iterator();
        while (i.hasNext()) {
            String box = (String) i.next();
            try {
                String domainName = box.substring(box.indexOf(64) + 1);
                Hashtable hs = Domain.getDomainInfoByName(domainName);
                if (hs == null) {
                    getLog().debug("Can't find info about domain: " + domainName);
                } else {
                    Long accountId = (Long) hs.get("account_id");
                    ArrayList boxes = (List) map.get(accountId);
                    if (boxes == null) {
                        boxes = new ArrayList();
                        map.put(accountId, boxes);
                    }
                    boxes.add(box);
                }
            } catch (Exception e) {
                getLog().warn("Error dealing with mailbox " + box, e);
            }
        }
        return map;
    }

    protected List getResourceIdsByType(Collection list, String type) throws Exception {
        List found = new ArrayList();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            ResourceId rId = (ResourceId) i.next();
            if (type.equals(rId.getNamedType())) {
                found.add(rId);
            }
        }
        return found;
    }

    protected void sendOverLimitWarnCron(Account a) {
        Resource res;
        boolean dis_twarn = false;
        boolean enable_tsusp = false;
        boolean dis_qwarn = false;
        boolean dis_disk_usage_warn = false;
        boolean enable_disk_usage_susp = false;
        try {
            try {
                dis_twarn = new Boolean(Settings.get().getValue("dis_twarn")).booleanValue();
            } catch (Exception e) {
            }
            try {
                enable_tsusp = new Boolean(Settings.get().getValue("enable_tsusp")).booleanValue();
            } catch (Exception e2) {
            }
            try {
                dis_qwarn = new Boolean(Settings.get().getValue("dis_qwarn")).booleanValue();
            } catch (Exception e3) {
            }
            try {
                dis_disk_usage_warn = new Boolean(Settings.get().getValue("dis_disk_usage_warn")).booleanValue();
            } catch (Exception e4) {
            }
            try {
                enable_disk_usage_susp = new Boolean(Settings.get().getValue("enable_disk_usage_susp")).booleanValue();
            } catch (Exception e5) {
            }
            boolean sendMessage = false;
            boolean suspendAccount = false;
            StringBuffer suspendReason = new StringBuffer();
            List overLimitRes = new ArrayList();
            Collection resIds = a.getChildManager().getAllResources();
            for (int j = 0; j < quotaLikeResourses.length; j++) {
                String type = quotaLikeResourses[j];
                boolean isSummaryQuota = "summary_quota".equals(type);
                if ((!isSummaryQuota || !dis_disk_usage_warn || enable_disk_usage_susp) && (isSummaryQuota || !dis_qwarn)) {
                    if (!a.getPlan().areResourcesAvailable(type)) {
                        getLog().debug("Skiped " + type + " for account " + a.getId().getId());
                    } else {
                        for (ResourceId resId : getResourceIdsByType(resIds, type)) {
                            getLog().debug("Processing " + type + " " + resId.getId() + " for account " + a.getId().getId());
                            try {
                                res = resId.get();
                            } catch (Exception e6) {
                                getLog().warn("Error during overlimit", e6);
                            }
                            if (isSummaryQuota) {
                                if (res instanceof SummaryQuota) {
                                    if (!dis_disk_usage_warn && ((SummaryQuota) res).warnLimit()) {
                                        sendMessage = true;
                                        overLimitRes.add(res);
                                    }
                                    if (enable_disk_usage_susp && ((SummaryQuota) res).suspendLimit()) {
                                        if (overLimitRes.indexOf(res) == -1) {
                                            overLimitRes.add(res);
                                        }
                                        suspendAccount = true;
                                        suspendReason.append(res.get("info").getAsString()).append("| ");
                                    }
                                }
                            } else {
                                if ("mail_quota".equals(type)) {
                                    List boxes = (List) getBoxesByAccounts().get(new Long(a.getId().getId()));
                                    if (boxes != null) {
                                        String box = res.recursiveGet("fullemail").toString();
                                        if (!boxes.contains(box)) {
                                        }
                                    }
                                }
                                if ((res instanceof Quota) && ((Quota) res).warnLimit()) {
                                    sendMessage = true;
                                    overLimitRes.add(res);
                                }
                            }
                        }
                    }
                }
            }
            for (int j2 = 0; j2 < trafficLikeResourses.length && (!dis_twarn || enable_tsusp); j2++) {
                String type2 = trafficLikeResourses[j2];
                if (!a.getPlan().areResourcesAvailable(type2)) {
                    getLog().debug("Skiped " + type2 + " for account " + a.getId().getId());
                } else {
                    for (ResourceId resId2 : getResourceIdsByType(resIds, type2)) {
                        getLog().debug("Processing " + type2 + " " + resId2.getId() + " for account " + a.getId().getId());
                        try {
                            Resource res2 = resId2.get();
                            if (res2 instanceof Traffic) {
                                if (!dis_twarn && ((Traffic) res2).warnLimit()) {
                                    sendMessage = true;
                                    overLimitRes.add(res2);
                                }
                                if (enable_tsusp && ((Traffic) res2).suspendLimit()) {
                                    if (overLimitRes.indexOf(res2) == -1) {
                                        overLimitRes.add(res2);
                                    }
                                    suspendAccount = true;
                                    suspendReason.append(res2.get("info").getAsString()).append("| ");
                                }
                            }
                        } catch (Exception e7) {
                            getLog().warn("Error during overlimit", e7);
                        }
                    }
                }
            }
            if (!sendMessage) {
                return;
            }
            String email = a.getContactInfo().getEmail();
            SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
            if (suspendAccount) {
                root.put("suspend", "1");
            }
            root.put("over_limit_res", new TemplateList(overLimitRes));
            CustomEmailMessage.send("OVERLIMIT", email, (TemplateModelRoot) root);
            if (suspendAccount) {
                try {
                    synchronized (a) {
                        a.suspend(suspendReason.toString());
                    }
                } catch (Exception e8) {
                    getLog().error("Error suspending account", e8);
                }
            }
        } catch (Throwable t) {
            getLog().warn("Error", t);
        }
    }
}
