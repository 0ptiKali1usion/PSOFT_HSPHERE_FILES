package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.admin.ContentMoveItem;
import psoft.hsphere.resource.p004tt.Ticket;

/* loaded from: hsphere.zip:psoft/hsphere/cron/ContentMovingCron.class */
public class ContentMovingCron extends BackgroundJob {
    public ContentMovingCron(C0004CP cp) throws Exception {
        super(cp, "MOVING_CRON");
    }

    public ContentMovingCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        getLog().debug("MOVING_CRON - BEGIN");
        List items = ContentMoveItem.getAllMoveItems();
        if (items.size() > 0) {
            launchRsync();
        }
        setProgress(items.size(), 1, 0);
        int i = 0;
        while (true) {
            if (i >= items.size()) {
                break;
            } else if (isInterrupted()) {
                getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
                break;
            } else {
                checkSuspended();
                ContentMoveItem cmi = (ContentMoveItem) items.get(i);
                try {
                    if (!cmi.isSuspended()) {
                        cmi.processRsyncModule();
                    }
                } catch (Exception e) {
                    getLog().error("An error occured during processing ContentMoveItem " + cmi.getId());
                }
                try {
                    if (!cmi.isSuspended()) {
                        cmi.deleteExpiredResource();
                    }
                } catch (Exception e2) {
                    getLog().error("An error occured during processing ContentMoveItem " + cmi.getId());
                }
                addProgress(1);
                i++;
            }
        }
        getLog().debug("MOVING_CRON - FINISHED");
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v13, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v24, types: [java.lang.String[], java.lang.String[][]] */
    protected void launchRsync() throws Exception {
        Iterator output;
        String result;
        Iterator output2;
        String result2;
        PreparedStatement ps = null;
        ArrayList rsyncStart = new ArrayList();
        Connection con = Session.getDb();
        try {
            LinkedList servers = HostManager.getHostsByGroupType(1);
            ps = con.prepareStatement("SELECT target FROM content_move WHERE started IS NULL OR finished IS NULL;");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HostEntry he = HostManager.getHost(rs.getLong(1));
                if (!rsyncStart.contains(Long.toString(he.getPServer().getId()))) {
                    try {
                        if (he instanceof WinHostEntry) {
                            output2 = ((WinHostEntry) he).exec("rsync.asp", (String[][]) new String[]{new String[]{"command", "start"}}).iterator();
                        } else {
                            output2 = he.exec("rsync.sh", Arrays.asList("start")).iterator();
                            rsyncStart.add(Long.toString(he.getPServer().getId()));
                        }
                        if (output2.hasNext()) {
                            result2 = (String) output2.next();
                        } else {
                            result2 = "";
                        }
                        if (!"OK".equals(result2)) {
                            throw new Exception("Unable to launch rsync on " + he.getName() + " logical server");
                            break;
                        }
                    } catch (Exception ex) {
                        Ticket.create(ex, null, null);
                    }
                }
            }
            getLog().debug("PSERVERS=" + rsyncStart);
            ListIterator si = servers.listIterator();
            while (si.hasNext()) {
                HostEntry he2 = (HostEntry) si.next();
                if (!rsyncStart.contains(Long.toString(he2.getPServer().getId()))) {
                    try {
                        if (he2 instanceof WinHostEntry) {
                            output = ((WinHostEntry) he2).exec("rsync.asp", (String[][]) new String[]{new String[]{"command", "stop"}}).iterator();
                        } else {
                            output = he2.exec("rsync.sh", Arrays.asList("stop")).iterator();
                        }
                        if (output.hasNext()) {
                            result = (String) output.next();
                        } else {
                            result = "";
                        }
                        if (!"OK".equals(result)) {
                            throw new Exception("Unable to shut down rsync on " + he2.getName() + " logical server");
                            break;
                        }
                    } catch (Exception ex2) {
                        Ticket.create(ex2, null, null);
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
