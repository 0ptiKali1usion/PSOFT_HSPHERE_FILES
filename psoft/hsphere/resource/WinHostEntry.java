package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.apache.axis.message.SOAPEnvelope;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinHostEntry.class */
public class WinHostEntry extends HostEntry {
    @Override // psoft.hsphere.resource.HostEntry
    public TemplateModel get(String key) {
        return key.equals("platform") ? new TemplateString("Windows 2000 Server") : key.equals("ftp_type") ? new TemplateString(getFTPType()) : super.get(key);
    }

    public WinHostEntry(long id, String fqdn, int group, int groupType, int status, int signup) throws Exception {
        super(id, fqdn, group, groupType, "", "", status, signup);
    }

    public Collection exec(String command, String[][] args) throws Exception {
        return exec(command, Arrays.asList(args));
    }

    @Override // psoft.hsphere.resource.HostEntry
    public Collection exec(String command, Collection args) throws Exception {
        WinPhysicalServer ps = (WinPhysicalServer) getPServer();
        if (getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, command to execute: " + command + " arguments " + args);
            return Arrays.asList("0");
        }
        return ps.exec(command, args);
    }

    @Override // psoft.hsphere.resource.HostEntry
    public PhysicalServer getPServer() throws Exception {
        for (int i = 5; i > 0; i--) {
            PhysicalServer ps = PhysicalServer.getPServerForLServer(this.f145id);
            if (null == ps) {
                Session.getLog().debug("WinHostEntry.getPServer sleeping ....");
                TimeUtils.sleep(10000L);
            } else {
                return ps;
            }
        }
        throw new HSUserException("winhostentry.server", new Object[]{String.valueOf(this.f145id)});
    }

    public String getFTPType() {
        String ftpType = null;
        if (getGroupType() == 5) {
            try {
                ftpType = getOption("ftp-type");
                if (ftpType == null) {
                    try {
                        Collection col = exec("ftp-gettype.asp", new ArrayList());
                        ftpType = (String) col.iterator().next();
                        Session.getLog().debug("Got FTP type = " + ftpType + " for server " + this.f145id);
                        if ("ServU".equals(ftpType) || "IIS".equals(ftpType)) {
                            setOption("ftp-type", ftpType);
                        }
                    } catch (Exception ex) {
                        Session.getLog().error("Error while quering FTP type for server " + this.f145id, ex);
                        return null;
                    }
                }
            } catch (Exception e) {
                Session.getLog().error("Error while getting ftp-type option for server " + this.f145id);
                return null;
            }
        }
        return ftpType;
    }

    @Override // psoft.hsphere.resource.HostEntry
    public String platformType() {
        return "win2k";
    }

    public SOAPEnvelope invokeMethod(String method, String[][] params) throws Exception {
        if (getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, method to execute: " + method);
            return null;
        }
        return ((WinPhysicalServer) getPServer()).invokeMethod(method, params);
    }

    public SOAPEnvelope invokeMethod(String serviceName, String method, String[][] params) throws Exception {
        if (getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, method to execute: " + method);
            return null;
        }
        return ((WinPhysicalServer) getPServer()).invokeMethod(serviceName, method, params);
    }
}
