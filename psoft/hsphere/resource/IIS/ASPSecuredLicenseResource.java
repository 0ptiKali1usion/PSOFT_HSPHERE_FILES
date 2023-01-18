package psoft.hsphere.resource.IIS;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HTTPAuth;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/ASPSecuredLicenseResource.class */
public class ASPSecuredLicenseResource extends Resource {
    public static String hostname = "www.asp-programmers.com";
    public static String aspcommand = "/psoft/getorder.asp";
    protected String orderid;

    public ASPSecuredLicenseResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.orderid = (String) i.next();
    }

    public ASPSecuredLicenseResource(ResourceId rid) throws Exception {
        super(rid);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v44, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        List args = Arrays.asList(new String[]{"orderid", this.orderid}, new String[]{"domainname", recursiveGet("real_name").toString()}, new String[]{"uniqueid", "1111111111"});
        Session.getLog().debug("WinPhysicalServer.exec  at " + hostname + " command: " + aspcommand);
        StringBuffer buf = new StringBuffer();
        Iterator i = args.iterator();
        if (i.hasNext()) {
            String[] pair = (String[]) i.next();
            buf.append(pair[0]).append("=").append(URLEncoder.encode(pair[1]));
            while (i.hasNext()) {
                String[] pair2 = (String[]) i.next();
                buf.append("&").append(pair2[0]).append("=").append(pair2[1] == null ? "" : URLEncoder.encode(pair2[1]));
            }
        }
        String data = buf.toString();
        Session.getLog().debug("WinPhysicalServer.exec, params " + data.substring(0, Math.min(data.length(), 200)));
        Authenticator.setDefault(new HTTPAuth("", ""));
        URL url = new URL("http://" + hostname + aspcommand);
        Session.getLog().info("----------> winbox URL:" + url.toString());
        StringBuffer message = new StringBuffer();
        Collection col = new LinkedList();
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            PrintWriter out = new PrintWriter(con.getOutputStream());
            out.print(data);
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String result = in.readLine();
            while (true) {
                String inputline = in.readLine();
                if (inputline == null) {
                    break;
                }
                message.append(inputline);
                col.add(inputline);
            }
            in.close();
            if (!"0".equals(result)) {
                Session.getLog().debug("WinPhysicalServer some errors: " + result + ":" + ((Object) message));
                throw new HSUserException(message.toString());
            } else if (col.size() != 3) {
                throw new HSUserException("asp_secured.licinfo_incorrect");
            } else {
                Iterator it = col.iterator();
                String license = "";
                String licensekey = "";
                String licusername = "";
                if (it.hasNext()) {
                    license = (String) it.next();
                    if (it.hasNext()) {
                        licensekey = (String) it.next();
                        if (it.hasNext()) {
                            licusername = (String) it.next();
                        }
                    }
                }
                int lictype = getLicType(license);
                if (lictype == 0) {
                    throw new HSUserException("asp_secured.licinfo_incorrect_lic_param");
                }
                String login = recursiveGet("login").toString();
                String hostnum = recursiveGet("hostnum").toString();
                String name = recursiveGet("real_name").toString();
                WinHostEntry he = (WinHostEntry) recursiveGet("host");
                he.exec("set-aspsecured.asp", (String[][]) new String[]{new String[]{"user-name", login}, new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"lictype", String.valueOf(lictype)}, new String[]{"licensekey", licensekey}, new String[]{"licusername", licusername}});
                Connection con2 = Session.getDb();
                PreparedStatement ps = null;
                try {
                    ps = con2.prepareStatement("INSERT INTO asp_secured_lic (id, type, license, username) VALUES (?, ?, ?, ?)");
                    ps.setLong(1, getId().getId());
                    ps.setInt(2, lictype);
                    ps.setString(3, licensekey);
                    ps.setString(4, licusername);
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                    con2.close();
                } catch (Throwable th) {
                    Session.closeStatement(ps);
                    con2.close();
                    throw th;
                }
            }
        } catch (FileNotFoundException e) {
            Session.getLog().error("Winbox error:", e);
            throw new HSUserException("Script " + aspcommand + " not found on server " + hostname);
        } catch (NoRouteToHostException e2) {
            Session.getLog().error("Winbox error:", e2);
            throw new HSUserException("No route to host " + hostname + ". Server is probably down");
        } catch (ProtocolException e3) {
            Session.getLog().error("Winbox error:", e3);
            throw new HSUserException("Password authentification failed. Server: " + hostname);
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM asp_secured_lic WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int getLicType(String lic_name) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM asp_secured_lic_type WHERE name = ?");
            ps.setString(1, lic_name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int i = rs.getInt(1);
                Session.closeStatement(ps);
                con.close();
                return i;
            }
            Session.closeStatement(ps);
            con.close();
            return 0;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
