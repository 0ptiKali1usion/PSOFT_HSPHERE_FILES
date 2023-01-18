package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.admin.TransferProcess;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.tools.ExternalCP;

/* loaded from: hsphere.zip:psoft/hsphere/cron/TransferCron.class */
public class TransferCron extends BackgroundJob {
    public TransferCron(C0004CP cp) throws Exception {
        super(cp, "TRANSFER");
    }

    public TransferCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        TransferProcess tp;
        checkSuspended();
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id FROM transfer_process WHERE stage < ? AND suspended IS NULL");
            ps.setInt(1, 8);
            ResultSet rs = ps.executeQuery();
            if (rs.last()) {
                setProgress(rs.getRow(), 1, 0);
            }
            if (rs.first()) {
                while (true) {
                    if (isInterrupted()) {
                        getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
                        break;
                    }
                    checkSuspended();
                    try {
                        getLog().debug("TransferProcess Id " + rs.getLong(1));
                        tp = TransferProcess.get(rs.getLong(1));
                    } catch (Exception ex) {
                        getLog().error("Error while executing TransferCron", ex);
                    }
                    if (tp == null) {
                        throw new Exception("TransferProcess with id " + rs.getLong(1) + " not found");
                        break;
                    }
                    getLog().debug("Got TransferProcess " + tp.getId());
                    tp.process();
                    addProgress(1);
                    if (!rs.next()) {
                        break;
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            try {
                getLog().error("Error while executing TransferCron", th);
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th2) {
                Session.closeStatement(ps);
                con.close();
                throw th2;
            }
        }
    }

    protected static void cronTest() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id FROM transfer_process WHERE stage < ? AND suspended IS NULL");
            ps.setInt(1, 8);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session.getLog().debug("TransferProcess Id " + rs.getLong(1));
                TransferProcess tp = TransferProcess.get(rs.getLong(1));
                if (tp == null) {
                    throw new Exception("TransferProcess with id " + rs.getLong(1) + " not found");
                }
                Session.getLog().debug("Got TransferProcess " + tp.getId());
                tp.process();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            try {
                Session.getLog().error("Error while executing TransferCron", th);
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th2) {
                Session.closeStatement(ps);
                con.close();
                throw th2;
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        ExternalCP.initCP();
        cronTest();
        System.exit(0);
    }
}
