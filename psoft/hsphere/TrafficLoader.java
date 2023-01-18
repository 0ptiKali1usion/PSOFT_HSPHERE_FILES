package psoft.hsphere;

import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPElement;
import org.apache.axis.message.SOAPEnvelope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/TrafficLoader.class */
public class TrafficLoader extends C0004CP {
    private static final int DOMAINTR = 1;
    private static final int UNIXUSERTR = 2;
    private static final int VIRTUALFTPTR = 3;
    private static final int REALSERVERTR = 4;
    private static final int MAILTR = 5;
    private static final int VPSTR = 6;
    private static boolean isMicrosoftDb = false;

    public TrafficLoader() {
        super("psoft_config.hsphere");
        if ("MSSQL".equals(Session.getPropertyString("DB_VENDOR"))) {
            Session.getLog().debug("Microsoft database detected");
            isMicrosoftDb = true;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/TrafficLoader$Info.class */
    public class Info {
        private HashMap info;
        private int ttType;

        public Info(int type) {
            TrafficLoader.this = r5;
            this.info = null;
            this.info = new HashMap();
            init(type);
            this.ttType = type;
        }

        public int getSize() {
            if (this.info != null) {
                return this.info.size();
            }
            return 0;
        }

        public Hashtable get(String name) {
            Hashtable res = new Hashtable();
            Ids ids = (Ids) this.info.get(name);
            if (ids != null) {
                res.put("Name", name);
                res.put("resource_id", new Long(ids.resId));
                res.put("account_id", new Long(ids.accId));
                return res;
            } else if (this.ttType == 1 || this.ttType == 5) {
                String realDomainName = name;
                while (realDomainName.indexOf(".") > 0) {
                    realDomainName = realDomainName.substring(realDomainName.indexOf(".") + 1);
                    Ids ids2 = (Ids) this.info.get(realDomainName);
                    if (ids2 != null) {
                        res.put("Name", name);
                        res.put("resource_id", new Long(ids2.resId));
                        res.put("account_id", new Long(ids2.accId));
                        return res;
                    }
                }
                return null;
            } else {
                return null;
            }
        }

        public Hashtable get(String name, long serverId) {
            return get(name + "_" + new Long(serverId).toString());
        }

        public void init(int type) {
            PreparedStatement ps = null;
            this.info.clear();
            try {
                Connection con = Session.getDb();
                try {
                    switch (type) {
                        case 1:
                            ps = con.prepareStatement("SELECT name, id, account_id FROM domains, parent_child WHERE domains.id = parent_child.child_id AND child_type IN (2,31,32,34,35)");
                            ResultSet rs = ps.executeQuery();
                            while (rs.next()) {
                                this.info.put(rs.getString(1), new Ids(rs.getLong(2), rs.getLong(3)));
                            }
                            break;
                        case 2:
                        case 4:
                            ps = con.prepareStatement("SELECT login, unix_user.id, account_id, l_server.p_server_id FROM unix_user, parent_child, l_server WHERE unix_user.id = parent_child.child_id AND unix_user.hostid = l_server.id");
                            ResultSet rs2 = ps.executeQuery();
                            while (rs2.next()) {
                                String name = rs2.getString(1) + "_" + new Long(rs2.getLong(4)).toString();
                                this.info.put(name, new Ids(rs2.getLong(2), rs2.getLong(3)));
                            }
                            break;
                        case 3:
                            ps = con.prepareStatement("SELECT ip, r_id, account_id FROM l_server_ips, parent_child WHERE l_server_ips.r_id = child_id");
                            ResultSet rs3 = ps.executeQuery();
                            while (rs3.next()) {
                                this.info.put(rs3.getString(1), new Ids(rs3.getLong(2), rs3.getLong(3)));
                            }
                            break;
                        case 5:
                            ps = con.prepareStatement("SELECT name, id, account_id FROM domains, parent_child WHERE domains.id = parent_child.child_id AND child_type IN (2,31,32,34,35,37)");
                            ResultSet rs4 = ps.executeQuery();
                            while (rs4.next()) {
                                this.info.put(rs4.getString(1), new Ids(rs4.getLong(2), rs4.getLong(3)));
                            }
                            break;
                        case 6:
                            ps = con.prepareStatement("SELECT name, id, account_id FROM vps, parent_child  WHERE vps.id = parent_child.child_id");
                            ResultSet rs5 = ps.executeQuery();
                            while (rs5.next()) {
                                this.info.put(rs5.getString(1), new Ids(rs5.getLong(2), rs5.getLong(3)));
                            }
                            break;
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Exception ex) {
                    this.info.clear();
                    Session.getLog().error("Traffic problem. Can't get info for traffic type:  " + new Integer(type).toString(), ex);
                    Session.closeStatement(null);
                    con.close();
                }
            } catch (Exception ex2) {
                Session.getLog().error("Traffic problem. Can't open database connection" + new Integer(type).toString(), ex2);
            }
        }

        /* loaded from: hsphere.zip:psoft/hsphere/TrafficLoader$Info$Ids.class */
        public class Ids {
            public long resId;
            public long accId;

            public Ids(long resourceId, long accountId) {
                Info.this = r5;
                this.resId = resourceId;
                this.accId = accountId;
            }
        }
    }

    private Long getPserverId(HostEntry host) {
        try {
            return new Long(host.getPServer().getId());
        } catch (Exception ex) {
            Session.getLog().debug("Error getting pServer ID: ", ex);
            return new Long(0L);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v25, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r3v27, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r3v32, types: [java.lang.String[], java.lang.String[][]] */
    public void load() {
        List<HostEntry> hosts = HostManager.getHostsByGroupType(1);
        List processedServers = new ArrayList();
        for (HostEntry host : hosts) {
            Long pserver = getPserverId(host);
            if (!processedServers.contains(pserver)) {
                try {
                    load(host.exec("xfer-cat.pl", Arrays.asList("web")), pserver.longValue());
                } catch (Exception e) {
                    Session.getLog().debug("Error loading traffic from host" + host.getName(), e);
                }
                processedServers.add(pserver);
            }
        }
        if (!C0004CP.isIrisEnabled()) {
            processedServers.clear();
            List<HostEntry> hosts2 = HostManager.getHostsByGroupType(3);
            for (HostEntry host2 : hosts2) {
                Long pserver2 = getPserverId(host2);
                if (!processedServers.contains(pserver2)) {
                    try {
                        load(host2.exec("xfer-cat.pl", Arrays.asList("mail")), pserver2.longValue());
                    } catch (Exception e2) {
                        Session.getLog().debug("Error loading traffic from host" + host2.getName(), e2);
                    }
                    processedServers.add(pserver2);
                }
            }
        } else {
            List hosts3 = HostManager.getHostsByGroupType(3);
            HostEntry host3 = (HostEntry) hosts3.iterator().next();
            Connection con = null;
            PreparedStatement ps = null;
            try {
                try {
                    Connection con2 = Session.getDb();
                    PreparedStatement ps2 = con2.prepareStatement("SELECT MAX(cdate) FROM trans_log WHERE tt_type = ?");
                    ps2.setInt(1, 5);
                    ResultSet rs = ps2.executeQuery();
                    Date max_db_date = null;
                    if (rs.next()) {
                        max_db_date = rs.getDate(1);
                    }
                    ps2.close();
                    con2.close();
                    if (max_db_date == null) {
                        max_db_date = new Date(0L);
                    }
                    con = MailServices.getIrisConnection("IrisUsage");
                    ps = con.prepareStatement("SELECT day, rule_id, 1 as direct, messages, bytes/1024 as bytes FROM Incoming WHERE day >= ? UNION SELECT day, rule_id, -1 as direct, messages, bytes/1024 as bytes FROM Outgoing WHERE day >= ? ORDER BY day, rule_id");
                    ps.setDate(1, max_db_date);
                    ps.setDate(2, max_db_date);
                    ResultSet rs2 = ps.executeQuery();
                    Session.closeStatement(ps);
                    Date oday = null;
                    Hashtable ht = new Hashtable();
                    while (rs2.next()) {
                        if (rs2.getString(1) != null) {
                            if (oday != null && !oday.equals(rs2.getDate("day"))) {
                                try {
                                    loadIRIS(ht, getPserverId(host3).longValue(), oday);
                                } catch (Exception e3) {
                                    Session.getLog().debug("Error loading IRIS mail traffic from host " + host3.getName(), e3);
                                }
                                ht.clear();
                            }
                            try {
                                String domain = MailServices.getIrisDomain(rs2.getInt("rule_id"));
                                if (ht.containsKey(domain)) {
                                    ArrayList temp = (ArrayList) ht.get(domain);
                                    Float bytes = (Float) temp.get(0);
                                    Integer in_mes = (Integer) temp.get(1);
                                    Integer out_mes = (Integer) temp.get(2);
                                    temp.clear();
                                    temp.add(new Float(rs2.getFloat("bytes") + bytes.floatValue()));
                                    if (rs2.getInt("direct") > 0) {
                                        temp.add(new Integer(rs2.getInt(LangBundlesCompiler.INT_USER_BUNDLE) + in_mes.intValue()));
                                        temp.add(out_mes);
                                    } else {
                                        temp.add(in_mes);
                                        temp.add(new Integer(rs2.getInt(LangBundlesCompiler.INT_USER_BUNDLE) + out_mes.intValue()));
                                    }
                                    ht.remove(domain);
                                    ht.put(domain, temp);
                                } else {
                                    ArrayList temp2 = new ArrayList();
                                    temp2.add(new Float(rs2.getFloat("bytes")));
                                    if (rs2.getInt("direct") > 0) {
                                        temp2.add(new Integer(rs2.getInt(LangBundlesCompiler.INT_USER_BUNDLE)));
                                        temp2.add(new Integer("0"));
                                    } else {
                                        temp2.add(new Integer("0"));
                                        temp2.add(new Integer(rs2.getInt(LangBundlesCompiler.INT_USER_BUNDLE)));
                                    }
                                    ht.put(domain, temp2);
                                }
                                oday = rs2.getDate("day");
                            } catch (Exception e4) {
                                Session.getLog().debug("Error loading IRIS mail traffic ", e4);
                            }
                        }
                    }
                    if (oday != null) {
                        loadIRIS(ht, getPserverId(host3).longValue(), oday);
                    }
                    Session.closeStatement(ps);
                    try {
                        con.close();
                    } catch (Exception e5) {
                    }
                } catch (Throwable th) {
                    Session.closeStatement(ps);
                    try {
                        con.close();
                    } catch (Exception e6) {
                    }
                    throw th;
                }
            } catch (Exception e7) {
                Session.getLog().debug("Error loading IRIS mail traffic ", e7);
                Session.closeStatement(ps);
                try {
                    con.close();
                } catch (Exception e8) {
                }
            }
        }
        processedServers.clear();
        List<HostEntry> hosts4 = HostManager.getHostsByGroupType(6);
        for (HostEntry host4 : hosts4) {
            Long pserver3 = getPserverId(host4);
            if (!processedServers.contains(pserver3)) {
                try {
                    load(host4.exec("xfer-cat.pl", Arrays.asList("real")), pserver3.longValue());
                } catch (Exception e9) {
                    Session.getLog().debug("Error loading traffic from host" + host4.getName(), e9);
                }
                processedServers.add(pserver3);
            }
        }
        processedServers.clear();
        List<WinHostEntry> hosts5 = HostManager.getHostsByGroupType(5);
        for (WinHostEntry host5 : hosts5) {
            Long pserver4 = getPserverId(host5);
            if (!processedServers.contains(pserver4)) {
                try {
                    if (WinService.isSOAPSupport()) {
                        try {
                            SOAPEnvelope envelope = host5.invokeMethod("trafficservice", "gettraffic", null);
                            SOAPElement info = WinService.getChildElement(envelope, "TrafficInfo");
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder builder = factory.newDocumentBuilder();
                            Reader reader = new StringReader(info.getValue());
                            Document doc = builder.parse(new InputSource(reader));
                            load(doc, pserver4.longValue());
                        } catch (Exception e10) {
                            load(host5.exec("get-traffic.asp", (String[][]) new String[]{new String[]{"type", "web"}}), pserver4.longValue());
                        }
                    } else {
                        load(host5.exec("get-traffic.asp", (String[][]) new String[]{new String[]{"type", "web"}}), pserver4.longValue());
                    }
                } catch (Exception e11) {
                    Session.getLog().debug("Error loading traffic from " + host5.getName(), e11);
                }
                processedServers.add(pserver4);
            }
        }
        processedServers.clear();
        List<WinHostEntry> hosts6 = HostManager.getHostsByGroupType(7);
        for (WinHostEntry host6 : hosts6) {
            Long pserver5 = getPserverId(host6);
            if (!processedServers.contains(pserver5)) {
                try {
                    load(host6.exec("get-traffic.asp", (String[][]) new String[]{new String[]{"type", "real"}}), pserver5.longValue());
                } catch (Exception e12) {
                    Session.getLog().debug("Error loading traffic from host" + host6.getName(), e12);
                }
                processedServers.add(pserver5);
            }
        }
        processedServers.clear();
        List<HostEntry> hosts7 = HostManager.getHostsByGroupType(12);
        for (HostEntry host7 : hosts7) {
            Long pserver6 = getPserverId(host7);
            if (!processedServers.contains(pserver6)) {
                try {
                    load(host7.exec("xfer-cat.pl", Arrays.asList("vps")), pserver6.longValue());
                } catch (Exception e13) {
                    Session.getLog().debug("Error loading traffic from host" + host7.getName(), e13);
                }
                processedServers.add(pserver6);
            }
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(10:6|(4:19|20|22|15)|8|9|(1:11)|12|13|14|15|4) */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x0113, code lost:
        r15 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x0115, code lost:
        psoft.hsphere.Session.getLog().warn("Bad traffic line, " + r0, r15);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void load(java.util.Collection r7, long r8) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 348
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.TrafficLoader.load(java.util.Collection, long):void");
    }

    protected void loadIRIS(Hashtable trafficLog, long pServerId, Date cdate) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO trans_log(p_server_id, tt_type, cdate, name, xfer, hits, html_hits) VALUES(?, ?, ?, ?, ?, ?, ?)");
            PreparedStatement ps1 = con.prepareStatement("DELETE FROM trans_log WHERE cdate=? AND tt_type=5");
            ps1.setLong(1, pServerId);
            ps1.setDate(2, cdate);
            ps1.executeUpdate();
            ps.setLong(1, pServerId);
            ps.setString(2, "5");
            ps.setDate(3, cdate);
            Enumeration e = trafficLog.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                ps.setString(4, key);
                ArrayList ll = (ArrayList) trafficLog.get(key);
                Iterator j = ll.iterator();
                while (j.hasNext()) {
                    ps.setFloat(5, ((Float) j.next()).floatValue());
                    ps.setInt(6, ((Integer) j.next()).intValue());
                    ps.setInt(7, ((Integer) j.next()).intValue());
                    ps.executeUpdate();
                }
            }
            con.commit();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void updateLog(int ttType) throws SQLException {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        long counter = 0;
        Info trInfo = new Info(ttType);
        Connection con = Session.getTransConnection();
        try {
            ps1 = con.prepareStatement("UPDATE trans_log SET account_id = ?, resource_id = ? WHERE account_id IS NULL AND name = ? AND p_server_id = ? AND tt_type = ?");
            switch (ttType) {
                case 1:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, tt_type FROM trans_log WHERE account_id IS NULL AND tt_type=1");
                    break;
                case 2:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, tt_type FROM trans_log WHERE account_id IS NULL AND (tt_type=2 OR tt_type=4)");
                    break;
                case 3:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, tt_type FROM trans_log WHERE account_id IS NULL AND tt_type=3");
                    break;
                case 4:
                default:
                    throw new SQLException("Unknown tt_type: " + new Integer(ttType).toString());
                case 5:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, tt_type FROM trans_log WHERE account_id IS NULL AND tt_type=5");
                    break;
                case 6:
                    ps = con.prepareStatement("SELECT DISTINCT name, p_server_id, tt_type FROM trans_log WHERE account_id IS NULL AND tt_type=6");
                    break;
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Hashtable hs = null;
                String name = rs.getString(1);
                long lServerId = rs.getLong(2);
                int ttype = rs.getInt(3);
                switch (ttype) {
                    case 1:
                    case 3:
                    case 5:
                    case 6:
                        hs = trInfo.get(name);
                        break;
                    case 2:
                    case 4:
                        hs = trInfo.get(name, lServerId);
                        break;
                }
                if (hs != null) {
                    try {
                        long account_id = ((Long) hs.get("account_id")).longValue();
                        long rid = ((Long) hs.get("resource_id")).longValue();
                        Session.getLog().debug("Type:" + ttype + " Name:" + name + " account_id:" + account_id + " resource_id:" + rid);
                        ps1.setLong(1, account_id);
                        ps1.setLong(2, rid);
                        ps1.setString(3, name);
                        ps1.setLong(4, lServerId);
                        ps1.setInt(5, ttype);
                        ps1.executeUpdate();
                    } catch (Exception e) {
                        Session.getLog().error("Traffic problem on:" + name, e);
                    }
                } else {
                    Session.getLog().error("Didn`t find name:" + name);
                }
                long j = counter + 1;
                counter = j;
                if (j % 100 == 0 && !isMicrosoftDb) {
                    con.commit();
                    Session.getLog().debug("Commiting another 100 record");
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.commitTransConnection(con);
            Session.getLog().debug(counter + " records were proceeded");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    protected void parse() throws SQLException {
        updateLog(1);
        updateLog(5);
        updateLog(2);
        updateLog(3);
        updateLog(6);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM trans_log WHERE account_id IS NULL OR account_id = 0");
            int count = ps.executeUpdate();
            if (count > 0) {
                Session.getLog().info(count + " no resolved records were deleted ");
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void deleteFromDb(Date dateLogs, long pServerId, int ttType) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM trans_log WHERE p_server_id = ? AND cdate = ? AND tt_type = ?");
            ps.setLong(1, pServerId);
            ps.setDate(2, dateLogs);
            ps.setInt(3, ttType);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void load(Document log, long pServerId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO trans_log(p_server_id, tt_type, cdate, name, xfer, in_traffic, out_traffic) VALUES(?, ?, ?, ?, ?, ?, ?)");
                Element root = log.getDocumentElement();
                NodeList daily = root.getElementsByTagName("DailyEntries");
                for (int i = 0; i < daily.getLength(); i++) {
                    Node item = daily.item(i);
                    NamedNodeMap attr = item.getAttributes();
                    String date = attr.getNamedItem("Date").getNodeValue();
                    String type = attr.getNamedItem("Type").getNodeValue();
                    int intType = 0;
                    if (type.equals("web")) {
                        intType = 1;
                    } else if (type.equals("ftpg")) {
                        intType = 2;
                    } else if (type.equals("ftpa")) {
                        intType = 3;
                    }
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    Date sqlDate = new Date(df.parse(date).getTime());
                    deleteFromDb(sqlDate, pServerId, intType);
                    con.commit();
                    NodeList entries = item.getChildNodes();
                    for (int j = 0; j < entries.getLength(); j++) {
                        Node entry = entries.item(j);
                        NamedNodeMap entryAttr = entry.getAttributes();
                        String name = entryAttr.getNamedItem("Name").getNodeValue();
                        NodeList trafficData = entry.getChildNodes();
                        double in = 0.0d;
                        double out = 0.0d;
                        for (int k = 0; k < trafficData.getLength(); k++) {
                            Node traffic = trafficData.item(k);
                            String dataType = traffic.getNodeName();
                            if (dataType.equals("Incoming")) {
                                in = Long.parseLong(traffic.getFirstChild().getNodeValue()) / 1024;
                            } else if (dataType.equals("Outgoing")) {
                                out = Long.parseLong(traffic.getFirstChild().getNodeValue()) / 1024;
                            }
                        }
                        ps.setLong(1, pServerId);
                        ps.setInt(2, intType);
                        ps.setDate(3, sqlDate);
                        ps.setString(4, name);
                        ps.setDouble(5, in + out);
                        ps.setDouble(6, in);
                        ps.setDouble(7, out);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Cannot load traffic", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/TrafficLoader$DelimeterString.class */
    public class DelimeterString {
        int type;
        java.util.Date cdate;

        public DelimeterString() throws Exception {
            TrafficLoader.this = r4;
            this.type = 0;
            this.cdate = TimeUtils.getDate();
        }

        public DelimeterString(String str) throws Exception {
            TrafficLoader.this = r7;
            try {
                RE reDelimeter = new RE("^#TYPE=(\\d)#DATE=(.*)#$");
                REMatch remDelimeter = reDelimeter.getMatch(str);
                this.type = Integer.parseInt(remDelimeter.toString(1));
                this.cdate = new java.util.Date(remDelimeter.toString(2));
            } catch (REException e) {
                throw new Exception("Bad delimited string: " + str);
            }
        }

        public int getType() {
            return this.type;
        }

        public Date getDate() {
            return new Date(this.cdate.getTime());
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/TrafficLoader$TrafficLogString.class */
    public class TrafficLogString {
        String domainName;
        double xFer;
        double inTraffic;
        double outTraffic;

        public TrafficLogString(String str) throws Exception {
            TrafficLoader.this = r7;
            try {
                StringTokenizer reTrafficLog = new StringTokenizer(str, "|");
                int tokenNum = reTrafficLog.countTokens();
                if (reTrafficLog.hasMoreTokens()) {
                    this.domainName = reTrafficLog.nextToken().toString();
                    if (tokenNum == 3) {
                        try {
                            String token = reTrafficLog.nextToken();
                            this.inTraffic = Double.parseDouble(token);
                        } catch (NumberFormatException e) {
                            this.inTraffic = 0.0d;
                        }
                        try {
                            String token2 = reTrafficLog.nextToken();
                            this.outTraffic = Double.parseDouble(token2);
                        } catch (NumberFormatException e2) {
                            this.outTraffic = 0.0d;
                        }
                        this.xFer = this.inTraffic + this.outTraffic;
                    } else if (reTrafficLog.hasMoreTokens()) {
                        String token3 = reTrafficLog.nextToken();
                        this.xFer = Double.parseDouble(token3);
                        this.inTraffic = 0.0d;
                        this.outTraffic = 0.0d;
                    } else {
                        throw new Exception("Wrong parsing traffic:" + str);
                    }
                    return;
                }
                throw new Exception("Wrong parsing domainName:" + str);
            } catch (NumberFormatException e3) {
                throw new Exception("Wrong parsing");
            }
        }

        public String getName() {
            return this.domainName;
        }

        public double getXFer() {
            return this.xFer;
        }

        public double getInTraffic() {
            return this.inTraffic;
        }

        public double getOutTraffic() {
            return this.outTraffic;
        }
    }

    public static void main(String[] args) throws Exception {
        TrafficLoader trans = new TrafficLoader();
        Session.getLog().info("Starting Traffic Loader");
        java.util.Date startDate = TimeUtils.getDate();
        Connection con = Session.getTransConnection();
        try {
            try {
                trans.load();
                Session.commitTransConnection(con);
            } catch (Exception e) {
                Session.getLog().error("Problem loading", e);
                Session.commitTransConnection(con);
            }
            Session.getLog().info("Traffic loader. starting traffic parsing");
            try {
                trans.parse();
            } catch (Exception e2) {
                Session.getLog().warn("Exception during traffic parse", e2);
            }
            long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
            Session.getLog().info("Traffic Loader FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
            System.exit(0);
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            throw th;
        }
    }
}
