package psoft.hsphere.resource.adm3W;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.reminder.Reminder;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/PRAproval.class */
public class PRAproval implements TemplateHashModel {
    protected long prId;
    protected long owner;
    protected Approval root;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("root")) {
            return this.root;
        }
        if (key.equals("list")) {
            TemplateList list = new TemplateList();
            Approval approval = this.root;
            while (true) {
                Approval ap = approval;
                if (ap != null) {
                    list.add((TemplateModel) ap);
                    approval = ap.next();
                } else {
                    return list;
                }
            }
        } else {
            return null;
        }
    }

    public static PRAproval create(long prId, long owner, long manager) throws Exception {
        PRAproval pr = new PRAproval(prId, owner);
        pr.init(manager);
        return pr;
    }

    public static PRAproval load(long prId, long owner) throws Exception {
        PRAproval pr = new PRAproval(prId, owner);
        pr.load();
        return pr;
    }

    public void load() throws Exception {
        long owner = this.owner;
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT manager, created, status, note, moderated FROM pr_approval WHERE pr_id = ? AND owner = ?");
        ps.setLong(1, this.prId);
        ps.setLong(2, owner);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            throw new Exception("Not Submited");
        }
        this.root = new Approval(rs.getLong(1), rs.getTimestamp(2), rs.getInt(3), rs.getString(4), rs.getTimestamp(5));
        Approval ap = this.root;
        long j = ap.owner;
        while (true) {
            long owner2 = j;
            if (owner2 != 0) {
                ps.setLong(2, owner2);
                ResultSet rs2 = ps.executeQuery();
                if (rs2.next()) {
                    ap.f165ap = new Approval(rs2.getLong(1), rs2.getTimestamp(2), rs2.getInt(3), rs2.getString(4), rs2.getTimestamp(5));
                    ap.f165ap.prev = ap;
                    ap = ap.f165ap;
                    j = ap.owner;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    protected String getManagerText(long manager) throws Exception {
        return manager == 0 ? "Accounting" : Employee.get(manager).toString();
    }

    protected String getPRLink(long manager, long prId) throws Exception {
        String postfix;
        int level;
        if (manager == 0) {
            postfix = Session.getProperty("ACCOUNTING_LOGIN");
            level = 2;
        } else {
            Employee e = Employee.get(manager);
            long accountId = e.getAccountId();
            String username = User.getUsername(accountId);
            postfix = username + "/" + accountId;
            level = 1;
        }
        return Session.getProperty("CP_URL") + "/" + postfix + "_0/CP?manager=" + level + "&template_name=adm3W/prequest_edit.html&pr_id=" + prId;
    }

    protected void init(long manager) throws Exception {
        this.root = new Approval(manager, TimeUtils.getSQLTimestamp());
        try {
            Reminder.notify("PR#" + this.prId + "|" + manager, "Please Review PR #" + this.prId, getManagerText(manager) + "\n<a href=\"" + getPRLink(manager, this.prId) + "\">Please Review PR #" + this.prId + "</a>\n", getManagerEmail(manager), 24);
        } catch (Exception e) {
            Session.getLog().info("Unable to set reminder", e);
        }
        save();
    }

    public PRAproval(long prId, long owner) {
        this.prId = prId;
        this.owner = owner;
    }

    protected void save() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("DELETE FROM pr_approval WHERE pr_id = ? ");
        ps.setLong(1, this.prId);
        ps.executeUpdate();
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO pr_approval (pr_id, owner, created, status, note, moderated, manager) VALUES (?, ?, ?, ?, ?, ?, ?)");
        ps2.setLong(1, this.prId);
        long owner = this.owner;
        Approval approval = this.root;
        while (true) {
            Approval ap = approval;
            if (ap != null) {
                ps2.setLong(2, owner);
                ps2.setTimestamp(3, ap.created);
                ps2.setInt(4, ap.status);
                ps2.setString(5, ap.note);
                if (ap.moderated == null) {
                    ps2.setNull(6, 93);
                } else {
                    ps2.setTimestamp(6, ap.moderated);
                }
                ps2.setLong(7, ap.owner);
                ps2.executeUpdate();
                owner = ap.owner;
                approval = ap.next();
            } else {
                return;
            }
        }
    }

    protected String getManagerEmail(long manager) throws Exception {
        return manager == 0 ? Session.getProperty("ACCOUNTING_EMAIL") : Employee.get(manager).getEmail();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/adm3W/PRAproval$Approval.class */
    public class Approval implements TemplateHashModel {

        /* renamed from: ap */
        protected Approval f165ap;
        protected Approval prev;
        protected long owner;
        protected int status;
        protected String note;
        protected Timestamp created;
        protected Timestamp moderated;

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel get(String key) {
            if (key.equals("ap")) {
                return this.f165ap;
            }
            if (key.equals("owner")) {
                return new TemplateString(this.owner);
            }
            if (key.equals("created")) {
                return new TemplateString(this.created);
            }
            if (key.equals("status")) {
                return new TemplateString(this.status);
            }
            if (key.equals("note")) {
                return new TemplateString(this.note);
            }
            if (key.equals("moderated")) {
                return new TemplateString(this.moderated);
            }
            return null;
        }

        public Approval next() {
            return this.f165ap;
        }

        public Approval(long owner, Timestamp created, int status, String note, Timestamp moderated) {
            PRAproval.this = r5;
            this.owner = owner;
            this.created = created;
            this.status = status;
            this.note = note;
            this.moderated = moderated;
        }

        public Approval(long owner, Timestamp created) {
            PRAproval.this = r5;
            this.owner = owner;
            this.created = created;
        }

        public void set(int status, String note, long manager) throws Exception {
            this.status = status;
            this.note = note;
            this.moderated = TimeUtils.getSQLTimestamp();
            try {
                Reminder.remove("PR#" + PRAproval.this.prId + "|" + this.owner);
            } catch (Exception e) {
                Session.getLog().info("Error removing notification", e);
            }
            try {
            } catch (Exception e2) {
                Session.getLog().info("Error sending notification email", e2);
            }
            if (status == 1) {
                Session.getMailer().sendMessage(PRAproval.this.getManagerEmail(PRAproval.this.owner), "PR#" + PRAproval.this.prId + " was approved by " + PRAproval.this.getManagerText(this.owner), "PR#" + PRAproval.this.prId + " was approved by " + PRAproval.this.getManagerText(this.owner) + "\n" + note, Session.getCurrentCharset());
            } else {
                if (status == 2) {
                    Session.getMailer().sendMessage(PRAproval.this.getManagerEmail(PRAproval.this.owner), "PR#" + PRAproval.this.prId + " was rejected by " + PRAproval.this.getManagerText(this.owner), "Sorry, " + PRAproval.this.getManagerText(PRAproval.this.owner) + "\n<a href=\"" + PRAproval.this.getPRLink(PRAproval.this.owner, PRAproval.this.prId) + "\">PR#" + PRAproval.this.prId + "</a> was rejected by " + PRAproval.this.getManagerText(this.owner) + "\n" + note, Session.getCurrentCharset());
                    if (this.prev != null) {
                        Session.getMailer().sendMessage(PRAproval.this.getManagerEmail(this.prev.owner), "PR#" + PRAproval.this.prId + " was rejected by " + PRAproval.this.getManagerText(this.owner), "Sorry, " + PRAproval.this.getManagerText(this.prev.owner) + "\n<a href=\"" + PRAproval.this.getPRLink(this.prev.owner, PRAproval.this.prId) + "\">PR#" + PRAproval.this.prId + " was rejected by " + PRAproval.this.getManagerText(this.owner) + "\n" + note, Session.getCurrentCharset());
                    }
                }
                if (status != 1 && manager != -1) {
                    this.f165ap = new Approval(manager, TimeUtils.getSQLTimestamp());
                    this.f165ap.prev = this;
                    try {
                        Reminder.notify("PR#" + PRAproval.this.prId + "|" + manager, "Please Review PR #" + PRAproval.this.prId, PRAproval.this.getManagerText(manager) + "\n<a href=\"" + PRAproval.this.getPRLink(manager, PRAproval.this.prId) + "\">Please Review PR #" + PRAproval.this.prId + "</a>\n\n" + note, PRAproval.this.getManagerEmail(manager), 24);
                        return;
                    } catch (Exception e3) {
                        Session.getLog().info("Error setting notification", e3);
                        return;
                    }
                }
            }
            if (status != 1) {
            }
        }
    }

    public void moderate(long owner, long manager, int status, String note) throws Exception {
        Approval ap = this.root;
        Session.getLog().info("root-->" + this.root.owner);
        while (ap.owner != 0 && ap.owner != -1 && ap.owner != owner) {
            ap = ap.next();
            Session.getLog().info("ap->" + ap.owner);
        }
        ap.set(status, note, manager);
        save();
    }

    public void resubmit(long owner, long manager, int status, String note) throws Exception {
        Approval approval = this.root;
        while (true) {
            Approval ap = approval;
            if (ap.owner != owner) {
                approval = ap.next();
            } else {
                ap.set(status, note, manager);
                save();
                return;
            }
        }
    }
}
