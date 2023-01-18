package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/EscallationCron.class */
public class EscallationCron extends BackgroundJob {
    public EscallationCron(C0004CP cp) throws Exception {
        super(cp, "ESCALLATION");
    }

    public EscallationCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        getLog().info("Starting Escalation job");
        Date startDate = TimeUtils.getDate();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT e.step, t.id, t.priority, t.escrule FROM ticket t, esc_rules e WHERE t.escrule = e.id AND t.state in (1, 2, 7)  AND age('now', t.created) > e.interval * 60");
                ResultSet rs = ps.executeQuery();
                if (rs.last()) {
                    setProgress(rs.getRow(), 1, 0);
                } else {
                    getLog().debug("JOB " + getFullMark() + " row count=" + rs.getRow());
                    if (rs.first()) {
                        while (true) {
                            if (isInterrupted()) {
                                getLog().debug("JOB " + getFullMark() + " HAS BEEN INTERRUPTED");
                                break;
                            }
                            checkSuspended();
                            try {
                                int step = rs.getInt(1);
                                int id = rs.getInt(2);
                                int prior = rs.getInt(3) + step;
                                if (prior > 100) {
                                    prior = 100;
                                }
                                PreparedStatement ps2 = con.prepareStatement("UPDATE ticket SET priority = ?, escrule = escrule + 1 WHERE id = ?");
                                ps2.setInt(1, prior);
                                ps2.setInt(2, id);
                                ps2.executeUpdate();
                                Ticket t = (Ticket) Ticket.get(id, Ticket.class);
                                if (t != null) {
                                    synchronized (SharedObject.getLock(id)) {
                                        int rule = rs.getInt(4);
                                        t.FM_setPriority(prior);
                                        t.FM_setEscrule(rule + 1);
                                    }
                                }
                            } catch (Throwable e) {
                                getLog().error("Escalation job error:", e);
                            }
                            addProgress(1);
                            if (!rs.next()) {
                                break;
                            }
                        }
                    }
                    setProgress(100, 1, 0);
                    addProgress(100);
                }
                long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
                getLog().info("Escalation job FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e2) {
                getLog().error("Escalation job error: ", e2);
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
