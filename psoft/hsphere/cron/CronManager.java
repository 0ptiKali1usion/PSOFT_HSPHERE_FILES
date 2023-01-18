package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/CronManager.class */
public class CronManager extends Thread {
    public static final int MULTIPLIER = 60000;
    protected static List crons = new ArrayList();

    /* renamed from: cp */
    protected C0004CP f83cp;
    protected long lastLaunch;
    protected long sleepingTime = 600000;
    protected String dbMark = "CRON_MANAGER";

    public CronManager(C0004CP c) throws Exception {
        this.lastLaunch = 0L;
        this.f83cp = c;
        getConfig();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT value FROM last_start WHERE name = ?");
                ps.setString(1, this.dbMark);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getTimestamp(1) != null) {
                    this.lastLaunch = rs.getTimestamp(1).getTime();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error getting LAST_LAUNCH", ex);
                Session.closeStatement(ps);
                con.close();
            }
            setDaemon(true);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected Element getConfXML() throws Exception {
        Session.getLog().warn("CRON IS DISABLED SINCE BACKGROUND SYSTEM STARTS");
        return null;
    }

    protected long getCronPeriod(Element oneNode) throws Exception {
        long cronPeriod = 0;
        try {
            cronPeriod = Long.parseLong(oneNode.getAttribute("period"));
        } catch (Exception e) {
        }
        return cronPeriod;
    }

    protected int getPriorityFromStr(String tmpStr) throws Exception {
        int newPriority = 5;
        try {
            int p = Integer.parseInt(tmpStr);
            if (p >= 1 && p <= 10) {
                newPriority = p;
            }
        } catch (Exception e) {
        }
        return newPriority;
    }

    protected long getPeriodFromStr(String tmpStr) throws Exception {
        long newPeriod = -1;
        try {
            newPeriod = Long.valueOf(tmpStr).longValue();
        } catch (Exception e) {
            Session.getLog().error("Error during cron period getting occured", e);
        }
        return newPeriod;
    }

    protected void getConfig() throws Exception {
        try {
            getConfXML();
        } catch (Exception e) {
            Session.getLog().error("CANNOT READ CRON CONFIG! ALL CRONS ARE DISABLED!", e);
        }
    }

    public boolean areCronsAvailable() throws Exception {
        return (crons == null || crons.isEmpty()) ? false : true;
    }

    private void viewReadCrons() throws Exception {
        Session.getLog().debug("Print all crons to initialize:");
        for (int i = 0; i < crons.size(); i++) {
            CronStructure cron = (CronStructure) crons.get(i);
            Session.getLog().debug("Cron from XML --> " + cron.getCronName() + " with priority = " + cron.getCronPriority() + " disabled = " + cron.getCronDisabled());
        }
    }

    protected void initCron(CronStructure oneCron) throws Exception {
        String cronName = oneCron.getCronName();
        if (oneCron.getCronDisabled() == 0) {
            try {
                if (oneCron.getCronPeriod() > 0) {
                    BackgroundJob cron = (BackgroundJob) oneCron.getCronObject(this.f83cp);
                    oneCron.setCronObject(cron);
                    Session.getLog().debug(cronName + " initialized with period: " + Long.toString(oneCron.getCronPeriod()) + " minuts");
                    cron.setPriority(oneCron.getCronPriority());
                    cron.start();
                    Session.getLog().debug(cronName + " - STARTED");
                    Session.getLog().debug("Cron priority is : " + cron.getPriority());
                } else {
                    Session.getLog().debug(cronName + " - DISABLED");
                }
                return;
            } catch (Exception e) {
                Session.getLog().debug(cronName + " initialization failed", e);
                return;
            }
        }
        Session.getLog().debug(cronName + " - DISABLED");
    }

    public void startCrons() throws Exception {
        viewReadCrons();
        Session.getLog().debug("START CRON INITIALIZING");
        for (int i = 0; i < crons.size(); i++) {
            CronStructure oneCron = (CronStructure) crons.get(i);
            initCron(oneCron);
        }
        Session.getLog().debug("CRON INITIALIZING FINISHED.");
    }

    public void stopCrons() {
        for (int i = 0; i < crons.size(); i++) {
            CronStructure oneCron = (CronStructure) crons.get(i);
            try {
                BackgroundJob cron = (BackgroundJob) oneCron.getCronObject();
                if (cron != null) {
                    Session.getLog().debug("STOPPING CRON " + oneCron.getCronName());
                    cron.interrupt();
                }
            } catch (Exception e) {
                Session.getLog().error("CANNOT CTOP CRON " + oneCron.getCronName(), e);
            }
        }
    }

    protected void checkDisabled(Element oneNode, CronStructure oneCron) throws Exception {
        BackgroundJob cron;
        int newDisabled = 0;
        if (oneNode.hasAttribute("disabled")) {
            try {
                newDisabled = Integer.parseInt(oneNode.getAttribute("disabled"));
            } catch (Exception e) {
            }
        }
        int oldDisabled = oneCron.getCronDisabled();
        if (newDisabled == 0 && oldDisabled == 1) {
            if (((BackgroundJob) oneCron.getCronObject()) != null) {
                try {
                    Session.getLog().debug("Start disabled cron " + oneCron.getCronName() + "...");
                    oneCron.setCronDisabled(newDisabled);
                    Session.getLog().debug("Disabled cron " + oneCron.getCronName() + " started.");
                    return;
                } catch (Exception e2) {
                    oneCron.setCronDisabled(oldDisabled);
                    Session.getLog().error("Cannot start cron " + oneCron.getCronName(), e2);
                    return;
                }
            }
            try {
                Session.getLog().debug("Init disabled cron " + oneCron.getCronName() + "...");
                oneCron.setCronDisabled(newDisabled);
                initCron(oneCron);
                Session.getLog().debug("disabled cron " + oneCron.getCronName() + " initialized.");
            } catch (Exception e3) {
                oneCron.setCronDisabled(oldDisabled);
                Session.getLog().error("Cannot init cron " + oneCron.getCronName(), e3);
            }
        } else if (newDisabled == 1 && oldDisabled == 0 && (cron = (BackgroundJob) oneCron.getCronObject()) != null && cron.isAlive()) {
            try {
                Session.getLog().debug("Interrupt enabled cron " + oneCron.getCronName() + "...");
                oneCron.setCronDisabled(newDisabled);
                Session.getLog().debug("Enabled cron " + oneCron.getCronName() + " interrupted.");
            } catch (Exception e4) {
                oneCron.setCronDisabled(oldDisabled);
                Session.getLog().error("Cannot interrupt cron " + oneCron.getCronName(), e4);
            }
        }
    }

    protected void checkPriority(Element oneNode, CronStructure oneCron) throws Exception {
        String tmpStr;
        int oldPriority = oneCron.getCronPriority();
        int newPriority = 5;
        if (oneNode.hasAttribute("priority") && (tmpStr = oneNode.getAttribute("priority")) != null && !"".equals(tmpStr)) {
            newPriority = getPriorityFromStr(tmpStr);
        }
        if (oldPriority != newPriority) {
            Session.getLog().debug("Cron name = " + oneCron.getCronName() + " Old priority = " + oldPriority);
            try {
                BackgroundJob cron = (BackgroundJob) oneCron.getCronObject();
                cron.setPriority(newPriority);
                oneCron.setCronPriority(newPriority);
                Session.getLog().debug("New priority for cron " + oneCron.getCronName() + " is " + oneCron.getCronPriority());
            } catch (Exception e) {
                Session.getLog().error("Cannot change priority for cron " + oneCron.getCronName(), e);
            }
        }
    }

    public void checkPeriod(Element oneNode, CronStructure oneCron) throws Exception {
        long oldPeriod = oneCron.getCronPeriod();
        long newPeriod = oldPeriod;
        if (oneNode.hasAttribute("period")) {
            String tmpStr = oneNode.getAttribute("period");
            if (tmpStr != null && !"".equals(tmpStr)) {
                newPeriod = getPeriodFromStr(tmpStr);
            }
            if (newPeriod <= 0) {
                newPeriod = oldPeriod;
            }
        }
        if (oldPeriod != newPeriod) {
            Session.getLog().debug("Cron name = " + oneCron.getCronName() + " Old period = " + oldPeriod);
            try {
                BackgroundJob backgroundJob = (BackgroundJob) oneCron.getCronObject();
                oneCron.setCronPeriod(newPeriod);
                Session.getLog().debug("New period for cron " + oneCron.getCronName() + " is " + oneCron.getCronPeriod());
            } catch (Exception e) {
                Session.getLog().error("Cannot change period for cron " + oneCron.getCronName(), e);
            }
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        long j = this.sleepingTime;
        setName(this.dbMark);
        while (!isInterrupted()) {
            long begin = TimeUtils.currentTimeMillis();
            if (this.lastLaunch == 0 || this.lastLaunch < begin - this.sleepingTime) {
                long lastLaunchOld = 0;
                try {
                    lastLaunchOld = this.lastLaunch;
                    if (this.lastLaunch == 0) {
                        this.lastLaunch = TimeUtils.currentTimeMillis();
                    } else {
                        long multiplier = 1;
                        if (this.lastLaunch + this.sleepingTime < TimeUtils.currentTimeMillis()) {
                            long tVal = TimeUtils.currentTimeMillis() - this.lastLaunch;
                            multiplier = (long) (tVal / this.sleepingTime);
                        }
                        this.lastLaunch += this.sleepingTime * multiplier;
                    }
                    try {
                        getConfXML();
                    } catch (Exception e) {
                        Session.getLog().error("Error during CronManager.run()", e);
                    }
                    Connection con = Session.getDb();
                    PreparedStatement ps = con.prepareStatement("UPDATE last_start SET value = ?  WHERE name = ?");
                    ps.setTimestamp(1, new Timestamp(this.lastLaunch));
                    ps.setString(2, this.dbMark);
                    int count = ps.executeUpdate();
                    if (count < 1) {
                        ps.close();
                        ps = con.prepareStatement("INSERT INTO last_start(name,value)  VALUES(?, ?)");
                        ps.setString(1, this.dbMark);
                        ps.setTimestamp(2, new Timestamp(this.lastLaunch));
                        ps.executeUpdate();
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Throwable e2) {
                    this.lastLaunch = lastLaunchOld;
                    Session.getLog().error("Exception during CronManager: ", e2);
                }
            }
            long finish = TimeUtils.currentTimeMillis();
            long nextLaunch = (this.sleepingTime - finish) + this.lastLaunch;
            if (nextLaunch > 0) {
                try {
                    TimeUtils.sleep(this.sleepingTime);
                } catch (InterruptedException e3) {
                }
            }
        }
        Session.getLog().debug("CronManager has been interrupted");
        Session.getLog().debug("CronManager has been stopped");
    }
}
