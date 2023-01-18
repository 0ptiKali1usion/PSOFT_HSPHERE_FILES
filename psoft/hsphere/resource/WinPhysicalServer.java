package psoft.hsphere.resource;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.axis.AxisFault;
import org.apache.axis.message.SOAPEnvelope;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.axis.tcp.TCPSender;
import psoft.hsphere.resource.soappool.WinServicePool;
import psoft.hsphere.servmon.ServerInfo;
import psoft.util.Base64;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinPhysicalServer.class */
public class WinPhysicalServer extends PhysicalServer {
    protected static boolean stopServerInfo;
    protected final String[] WIN_SERVICES;
    public static final String WIN_PREFIX = "/cfg/";

    static {
        stopServerInfo = false;
        try {
            stopServerInfo = "STOP".equals(Session.getProperty("SERVER_INFO_MONITOR"));
        } catch (Exception e) {
        }
    }

    public WinPhysicalServer(long id, String fqdn, String ip1, String ip2, String mask1, String mask2, String login, String password, int status) {
        super(id, fqdn, ip1, ip2, mask1, mask2, status);
        this.WIN_SERVICES = new String[]{"windows_hosting", "windows_real", "mssql"};
        this.login = login;
        this.password = password;
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public int getOsType() {
        return 2;
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public boolean isAuthorized() {
        return true;
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public boolean isInstalled() {
        return true;
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public synchronized Collection getServices() {
        setStatus(0);
        setServices(Arrays.asList(this.WIN_SERVICES));
        return Arrays.asList(this.WIN_SERVICES);
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public boolean checkIfAuthorized() {
        return true;
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    protected synchronized void checkInstalledServices() {
        setStatus(0);
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public boolean checkIfAuthorized(boolean closeSession) {
        return true;
    }

    @Override // psoft.hsphere.resource.PhysicalServer
    public void monitor(ServerInfo serverInfo) {
        if (stopServerInfo || HostEntry.getEmulationMode()) {
            serverInfo.update(ServerInfo.getStaticInfo("psoft_config/WinServerInfo.xml"));
            return;
        }
        try {
            boolean isSOAP = WinService.isSOAPSupport();
            if (isSOAP) {
                SOAPEnvelope envelope = invokeMethod("serverinfoservice", "getinfo", null);
                if (envelope != null) {
                    serverInfo.update(WinService.getChildElement(envelope, "ServerInfo").toString());
                } else {
                    isSOAP = false;
                }
            }
            if (!isSOAP) {
                Collection<String> col = exec("getserverinfo.asp", new ArrayList());
                StringBuffer buf = new StringBuffer();
                for (String str : col) {
                    buf.append(str).append("\n");
                }
                serverInfo.update(buf.toString());
            }
        } catch (Exception e) {
            Session.getLog().warn("Error while querying the p_server " + getId(), e);
        }
    }

    public Collection exec(String command, Collection args) throws Exception {
        Session.getLog().debug("WinPhysicalServer.exec  at " + this.ip1 + " command: " + command);
        StringBuffer buf = new StringBuffer();
        Iterator i = args.iterator();
        if (i.hasNext()) {
            String[] pair = (String[]) i.next();
            buf.append(pair[0]).append("=").append(URLEncoder.encode(pair[1], LanguageManager.STANDARD_CHARSET));
            while (i.hasNext()) {
                String[] pair2 = (String[]) i.next();
                buf.append("&").append(pair2[0]).append("=").append(pair2[1] == null ? "" : URLEncoder.encode(pair2[1], LanguageManager.STANDARD_CHARSET));
            }
        }
        String data = buf.toString();
        Session.getLog().debug("WinPhysicalServer.exec, params " + data.substring(0, Math.min(data.length(), 200)));
        System.getProperties().setProperty("sun.net.client.defaultConnectTimeout", "300000");
        System.getProperties().setProperty("sun.net.client.defaultReadTimeout", "300000");
        Authenticator.setDefault(new HTTPAuth(this.login, this.password));
        URL url = new URL("http://" + this.ip1 + WIN_PREFIX + command);
        Session.getLog().info("----------> winbox URL:" + url.toString() + " >>" + this.login + "]");
        StringBuffer message = new StringBuffer();
        Collection col = new LinkedList();
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Basic " + Base64.encode(this.login + ":" + this.password).trim());
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
                throw new Exception(((Object) message) + "\nCommand: " + command);
            }
            return col;
        } catch (FileNotFoundException e) {
            Session.getLog().error("Winbox error:", e);
            throw new Exception("Script " + command + " not found on server " + this.ip1 + ". Check the server configuration");
        } catch (NoRouteToHostException e2) {
            Session.getLog().error("Winbox error:", e2);
            throw new Exception("No route to host " + this.ip1 + ". Server is probably down");
        } catch (ProtocolException e3) {
            Session.getLog().error("Winbox error:", e3);
            throw new Exception("Password authentification failed. Server: " + this.ip1);
        }
    }

    public SOAPEnvelope invokeMethod(String method, String[][] params) throws Exception {
        WinService service = WinServicePool.get(this.ip1.toString(), WinService.getServicePort(), getLogin(), getPassword());
        SOAPEnvelope result = null;
        AxisFault fault = null;
        try {
            try {
                result = service.invokeMethod(method, params);
                WinServicePool.release(service);
                if (0 != 0) {
                    throw AxisFault.makeFault((Exception) null);
                }
            } catch (AxisFault ex1) {
                if (ex1.getMessage().equals(TCPSender.CONNECTION_RESET)) {
                    try {
                        result = service.invokeMethod(method, params);
                    } catch (AxisFault ex2) {
                        fault = ex2;
                    }
                } else {
                    fault = ex1;
                }
                WinServicePool.release(service);
                if (fault != null) {
                    throw AxisFault.makeFault(fault);
                }
            }
            return result;
        } catch (Throwable th) {
            WinServicePool.release(service);
            if (0 != 0) {
                throw AxisFault.makeFault((Exception) null);
            }
            throw th;
        }
    }

    public SOAPEnvelope invokeMethod(String serviceName, String method, String[][] params) throws Exception {
        WinService service = WinServicePool.get(this.ip1.toString(), WinService.getServicePort(), getLogin(), getPassword());
        SOAPEnvelope result = null;
        AxisFault fault = null;
        try {
            try {
                result = service.invokeMethod(serviceName, method, params);
                WinServicePool.release(service);
                if (0 != 0) {
                    throw AxisFault.makeFault((Exception) null);
                }
            } catch (AxisFault ex1) {
                if (ex1.getMessage().equals(TCPSender.CONNECTION_RESET)) {
                    try {
                        result = service.invokeMethod(serviceName, method, params);
                    } catch (AxisFault ex2) {
                        fault = ex2;
                    }
                } else {
                    fault = ex1;
                }
                WinServicePool.release(service);
                if (fault != null) {
                    throw AxisFault.makeFault(fault);
                }
            }
            return result;
        } catch (Throwable th) {
            WinServicePool.release(service);
            if (0 != 0) {
                throw AxisFault.makeFault((Exception) null);
            }
            throw th;
        }
    }
}
