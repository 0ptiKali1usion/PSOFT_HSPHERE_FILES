package psoft.hsphere.background;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/background/BackgroundManager.class */
public class BackgroundManager {
    private List processors = null;
    static Map managers = new Hashtable();
    static Class[] argT = {C0004CP.class, Long.class, String.class, Date.class};
    static Class[] initArgT = {C0004CP.class, Long.class, String.class, Date.class, Map.class};

    static void initBackgroundManagers() {
        managers.clear();
        for (Reseller resel : Reseller.getResellerList()) {
            if (resel == null || !resel.isSuspended()) {
                long oldReseller = 0;
                try {
                    oldReseller = Session.getResellerId();
                    Session.setResellerId(resel.getId());
                    managers.put(Long.toString(resel.getId()), new BackgroundManager());
                    if (oldReseller > 0) {
                        try {
                            Session.setResellerId(oldReseller);
                        } catch (UnknownResellerException e) {
                        }
                    }
                } catch (Exception e2) {
                    if (oldReseller > 0) {
                        try {
                            Session.setResellerId(oldReseller);
                        } catch (UnknownResellerException e3) {
                        }
                    }
                } catch (Throwable th) {
                    if (oldReseller > 0) {
                        try {
                            Session.setResellerId(oldReseller);
                        } catch (UnknownResellerException e4) {
                            throw th;
                        }
                    }
                    throw th;
                }
            }
        }
    }

    public static BackgroundManager getBackgroundManager(long resellerId) {
        BackgroundManager bgm = (BackgroundManager) managers.get(Long.toString(resellerId));
        if (bgm != null) {
            return bgm;
        }
        long oldReseller = 0;
        try {
            oldReseller = Session.getResellerId();
            Session.setResellerId(resellerId);
            BackgroundManager bgm2 = new BackgroundManager();
            managers.put(Long.toString(resellerId), bgm2);
            if (oldReseller > 0) {
                try {
                    Session.setResellerId(oldReseller);
                } catch (UnknownResellerException e) {
                }
            }
            return bgm2;
        } catch (Exception e2) {
            if (oldReseller > 0) {
                try {
                    Session.setResellerId(oldReseller);
                } catch (UnknownResellerException e3) {
                    return null;
                }
            }
            return null;
        } catch (Throwable th) {
            if (oldReseller > 0) {
                try {
                    Session.setResellerId(oldReseller);
                } catch (UnknownResellerException e4) {
                    throw th;
                }
            }
            throw th;
        }
    }

    public BackgroundManager() throws Exception {
        initProcessors();
    }

    protected static long getNewBackgroundId() throws Exception {
        return Session.getNewId("background_seq");
    }

    private Processor initProcessor(long id, String name, String pClass, Date created) throws Exception {
        return initProcessor(id, name, pClass, created, null);
    }

    private Processor initProcessor(long id, String name, String pClass, Date created, Map values) throws Exception {
        Processor pr;
        Class procClass = Class.forName(pClass);
        Collection classInterfaces = Arrays.asList(procClass.getInterfaces());
        if (classInterfaces.contains(AdminOnlyProcessor.class) && Session.getResellerId() != 1) {
            throw new AdminProcessorOnlyException();
        }
        if (values == null) {
            Object[] argV = {C0004CP.getCP(), new Long(id), name, created};
            pr = (Processor) procClass.getConstructor(argT).newInstance(argV);
        } else {
            Object[] argV2 = {C0004CP.getCP(), new Long(id), name, created, values};
            pr = (Processor) procClass.getConstructor(initArgT).newInstance(argV2);
        }
        this.processors.add(pr);
        pr.run();
        return pr;
    }

    private synchronized void initProcessors() throws Exception {
        if (this.processors != null) {
            return;
        }
        this.processors = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, name, class, created FROM background_manager AND reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    initProcessor(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4));
                } catch (Exception e) {
                    Session.getLog().error("Unable to init processer:" + rs.getString(2) + " for reseller #" + Session.getResellerId());
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

    public Processor addProcessor(long id, String name, String pClass, Map values) throws Exception {
        long newProcId = getNewBackgroundId();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Date now = TimeUtils.getDate();
        try {
            ps = con.prepareStatement("INSERT INTO background_manager(id, name, class, created, reseller_id) VALUES (?, ?, ?, ?)");
            ps.setLong(1, newProcId);
            ps.setString(2, name);
            ps.setString(3, pClass);
            ps.setTimestamp(4, new Timestamp(now.getTime()));
            ps.setLong(4, Session.getResellerId());
            ps.executeUpdate();
            Processor pr = initProcessor(newProcId, name, pClass, now, values);
            Session.closeStatement(ps);
            con.close();
            return pr;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
