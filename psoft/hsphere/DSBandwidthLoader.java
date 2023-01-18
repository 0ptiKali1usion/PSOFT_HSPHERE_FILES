package psoft.hsphere;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.hsphere.global.Globals;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DSNetInterface;
import psoft.hsphere.p001ds.DSNetInterfaceManager;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.p003ds.BandwidthResource;
import psoft.hsphere.resource.p003ds.ResellerBandwidth;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/DSBandwidthLoader.class */
public class DSBandwidthLoader extends C0004CP {
    public DSBandwidthLoader() {
        super("psoft_config.hsphere");
    }

    /* loaded from: hsphere.zip:psoft/hsphere/DSBandwidthLoader$LogInfo.class */
    public static class LogInfo {
        long interfaceId;
        long accountId;
        long dsId;
        Date cdate;
        double inout_average;
        double out_average;
        double inout_95percentile;
        double out_95percentile;

        private LogInfo() {
        }

        public static LogInfo parseData(String data, long netinterfaceId, long accountId, long dsId) throws Exception {
            LogInfo res = new LogInfo();
            StringTokenizer st = new StringTokenizer(data);
            if (st.countTokens() != 5) {
                throw new ParseException("Cannot parse the input data for network interface #" + netinterfaceId + ": " + data, 0);
            }
            res.cdate = new Date(TimeUtils.dropMinutes(TimeUtils.getDate()).getTime());
            st.nextToken();
            res.inout_average = Float.parseFloat(st.nextToken());
            res.out_average = Float.parseFloat(st.nextToken());
            res.inout_95percentile = Float.parseFloat(st.nextToken());
            res.out_95percentile = Float.parseFloat(st.nextToken());
            res.interfaceId = netinterfaceId;
            res.accountId = accountId;
            res.dsId = dsId;
            return res;
        }

        public long getInterfaceId() {
            return this.interfaceId;
        }

        public long getAccountId() {
            return this.accountId;
        }

        public long getDsId() {
            return this.dsId;
        }

        public java.util.Date getCdate() {
            return this.cdate;
        }

        public double getInout_average() {
            return this.inout_average;
        }

        public double getOut_average() {
            return this.out_average;
        }

        public double getInout_95percentile() {
            return this.inout_95percentile;
        }

        public double getOut_95percentile() {
            return this.out_95percentile;
        }
    }

    public void load() throws Exception {
        boolean isNewReseller;
        long resAccountId = 0;
        java.util.Date resBStartDate = null;
        DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT a.reseller_id, a.id, ni.id, ds.id FROM ds_netinterfaces ni, dedicated_servers ds, parent_child pc, accounts a WHERE ni.deleted is null AND ni.ds_id = ds.id AND ds.rid = pc.child_id AND pc.account_id = a.id AND a.deleted IS null ORDER BY a.reseller_id, a.id, ni.ds_id, ni.switch_id");
            ResultSet rs = ps.executeQuery();
            boolean firstIter = true;
            while (rs.next()) {
                long curResellerId = rs.getLong(1);
                long userAccountId = rs.getLong(2);
                long netInterfaceId = rs.getLong(3);
                long dsId = rs.getLong(4);
                if (firstIter) {
                    isNewReseller = true;
                    firstIter = false;
                } else {
                    try {
                        isNewReseller = curResellerId != Session.getResellerId();
                    } catch (UnknownResellerException e) {
                        isNewReseller = true;
                    }
                }
                if (isNewReseller) {
                    resBStartDate = null;
                    resAccountId = 0;
                    if (curResellerId != 1) {
                        try {
                            Session.setResellerId(1L);
                            Reseller reseller = Reseller.getReseller(curResellerId);
                            try {
                                Account resAccount = reseller.getAccount();
                                if (resAccount != null) {
                                    Session.setUser(resAccount.getUser());
                                    Session.setAccount(resAccount);
                                    resAccountId = resAccount.getId().getId();
                                    ResourceId rBandwidthResId = resAccount.FM_findChild("r_ds_bandwidth");
                                    if (rBandwidthResId != null) {
                                        resBStartDate = ((ResellerBandwidth) rBandwidthResId.get()).getStartDate();
                                    }
                                }
                            } catch (Exception ex) {
                                Session.getLog().error("Unable to determine the dedicated server bandwidth resource for the reseller billing account. Reseller id : " + curResellerId, ex);
                            }
                        } catch (Exception ex2) {
                            Session.getLog().error("Unable to process bandwidth for the DS network interface #" + netInterfaceId, ex2);
                            if (0 != 0) {
                                Session.getLog().debug("The complete response message (from the script) was the following:\n" + ((Object) null));
                            }
                        }
                    }
                    Session.setResellerId(curResellerId);
                }
                Account userAccount = Account.getAccount(userAccountId);
                Session.setUser(userAccount.getUser());
                Session.setAccount(userAccount);
                DSNetInterface netInterface = netInterfaceMan.getInterface(netInterfaceId);
                ResourceId dsBandwidthResId = userAccount.FM_findChild("ds_bandwidth");
                java.util.Date userBStartDate = dsBandwidthResId != null ? ((BandwidthResource) dsBandwidthResId.get()).getStartDate() : null;
                if (userBStartDate != null) {
                    java.util.Date niCreated = netInterface.getCreated();
                    if (niCreated != null && userBStartDate.before(niCreated)) {
                        userBStartDate = niCreated;
                    }
                    HostEntry host = HostManager.getHost(netInterface.getMrtgHostId());
                    Collection scriptResp = host.exec("mrtg-rrd-getstatistics", Arrays.asList(String.valueOf(netInterfaceId), String.valueOf(TimeUtils.getDateTimeInSeconds(userBStartDate))));
                    Iterator ri = scriptResp.iterator();
                    writeDb(LogInfo.parseData((String) ri.next(), netInterfaceId, userAccountId, dsId));
                    if (resAccountId > 0 && !isResellerManageable(dsId) && resBStartDate != null) {
                        java.util.Date tmpStartDate = (niCreated == null || !resBStartDate.before(niCreated)) ? resBStartDate : niCreated;
                        Collection scriptResp2 = host.exec("mrtg-rrd-getstatistics", Arrays.asList(String.valueOf(netInterfaceId), String.valueOf(TimeUtils.getDateTimeInSeconds(tmpStartDate))));
                        Iterator ri2 = scriptResp2.iterator();
                        writeDb(LogInfo.parseData((String) ri2.next(), netInterfaceId, resAccountId, dsId));
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

    public boolean isResellerManageable(long dsId) {
        try {
            DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(dsId);
            if (ds != null) {
                if (ds.getResellerId() != 1) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            Session.getLog().error("Unable to determine whether the dedicated server #" + dsId + " is managed by a reseller or by the master hosting provider. ", ex);
            return false;
        }
    }

    protected void writeDb(LogInfo info) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM ds_bandwidth_log WHERE interface_id = ? AND account_id = ? AND is_closed is null");
            ps2.setLong(1, info.interfaceId);
            ps2.setLong(2, info.accountId);
            ps2.executeUpdate();
            ps = con.prepareStatement("INSERT INTO ds_bandwidth_log(interface_id, account_id, ds_id, cdate, inout_95percentile, inout_average, out_95percentile, out_average) VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, info.interfaceId);
            ps.setLong(2, info.accountId);
            ps.setLong(3, info.dsId);
            ps.setDate(4, info.cdate);
            ps.setDouble(5, info.inout_95percentile);
            ps.setDouble(6, info.inout_average);
            ps.setDouble(7, info.out_95percentile);
            ps.setDouble(8, info.out_average);
            ps.executeUpdate();
            con.commit();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void main(String[] args) throws Exception {
        DSBandwidthLoader instance = new DSBandwidthLoader();
        Session.setResellerId(1L);
        if (Globals.isObjectDisabled("ds_enable") != 0) {
            Session.getLog().debug("The Dedicated Server facilities disabled for the entire system. Process is finished.");
        } else {
            Session.getLog().info("Dedicated Server bandwidth loader is stared");
            java.util.Date startDate = TimeUtils.getDate();
            Connection con = Session.getTransConnection();
            try {
                try {
                    instance.load();
                    Session.commitTransConnection(con);
                } catch (Exception e) {
                    Session.getLog().error("Detected error during loading data of dedicated server bandwidth", e);
                    Session.commitTransConnection(con);
                }
                long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
                Session.getLog().info("Dedicated Server bandwidth loader is finished. Process took " + (timeDiff / 60000) + " minutes, " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " seconds.");
            } catch (Throwable th) {
                Session.commitTransConnection(con);
                throw th;
            }
        }
        System.exit(0);
    }
}
