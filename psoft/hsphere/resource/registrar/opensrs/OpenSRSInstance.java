package psoft.hsphere.resource.registrar.opensrs;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import psoft.hsphere.Session;
import psoft.hsphere.resource.registrar.DomainTransferStatus;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.hsphere.resource.registrar.RegistrarTransactionData;
import psoft.hsphere.resource.registrar.opensrs.xcp.XCPCommands;
import psoft.hsphere.resource.registrar.opensrs.xcp.XCPMessage;
import psoft.hsphere.resource.registrar.opensrs.xcp.XCPParser;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/OpenSRSInstance.class */
public class OpenSRSInstance {
    OpenSRSConnection con;
    String cookie;

    /* renamed from: ip */
    String f211ip;
    XCPParser parser;

    /* renamed from: sf */
    private static final SimpleDateFormat f212sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    protected XCPMessage logRequest(RegistrarTransactionData dat, XCPMessage message) {
        if (dat != null) {
            dat.setRequest(message.toString());
        }
        return message;
    }

    protected void logResponse(RegistrarTransactionData dat, String message) {
        if (dat != null) {
            dat.setResponse(message);
        }
    }

    public OpenSRSInstance(OpenSRS openSRS) throws Exception {
        try {
            this.con = openSRS.getConnection();
        } catch (RegistrarException ex) {
            Session.getLog().error("Error getting OpenSRS connection:", ex);
        }
        this.f211ip = openSRS.get("ip");
    }

    public void close() throws Exception {
        this.con.close();
    }

    public boolean lookup(String domain, String tld) throws Exception {
        send(XCPCommands.domainLookup(domain + "." + tld));
        return this.parser.getResponseCode() == 210;
    }

    public Date belongsToRSP(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        String expDate;
        send(logRequest(dat, XCPCommands.belongsToRSP(domain + "." + tld)));
        logResponse(dat, this.parser.toString());
        if (this.parser.getResponseCode() == 200 && "1".equals(this.parser.getAttribute("belongs_to_rsp")) && (expDate = this.parser.getAttribute("domain_expdate")) != null) {
            return f212sf.parse(expDate);
        }
        return null;
    }

    public boolean isTransferable(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        send(logRequest(dat, XCPCommands.isTransferable(domain + "." + tld)));
        logResponse(dat, this.parser.toString());
        return "1".equals(this.parser.getAttribute("transferrable"));
    }

    public DomainTransferStatus checkTransferStatus(String domain, String tld) throws Exception {
        int status;
        send(XCPCommands.isTransferable(domain + "." + tld));
        if (this.parser.getResponseCode() == 200) {
            String _status = this.parser.getAttribute("status");
            if ("pending_owner".equals(_status)) {
                status = 0;
            } else if ("pending_admin".equals(_status)) {
                status = 1;
            } else if ("pending_registry".equals(_status)) {
                status = 2;
            } else if ("completed".equals(_status)) {
                status = 3;
            } else if ("cancelled".equals(_status)) {
                status = 4;
            } else if ("".equals(_status)) {
                status = 5;
            } else {
                status = 6;
            }
            return new DomainTransferStatus(status, this.parser.getAttribute("reason"));
        }
        throwException(this.parser);
        return null;
    }

    public void transfer(String domain, String tld, String username, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        XCPMessage message = XCPCommands.transfer(domain + "." + tld, username, password, registrant, tech, admin, billing, dns);
        logRequest(dat, message);
        send(message);
        try {
            logResponse(dat, this.parser.toString());
            int code = this.parser.getResponseCode();
            if ((code == 200 || code == 250) && this.parser.isSuccess()) {
                return;
            }
            throwException(this.parser);
        } catch (NumberFormatException nfe) {
            Session.getLog().debug("Error getting response code:", nfe);
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, "Error getting registrar response code");
        } catch (TransformerException te) {
            RegistrarException re = new RegistrarException(RegistrarException.OTHER_ERROR, te.getMessage());
            re.fillInStackTrace();
            throw re;
        }
    }

    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        register(domain, tld, username, password, period, registrant, tech, admin, billing, dns, null, dat);
    }

    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, Map extraInfo, RegistrarTransactionData dat) throws Exception {
        XCPMessage message = XCPCommands.register(domain + "." + tld, username, password, Integer.toString(period), registrant, tech, admin, billing, dns, extraInfo);
        logRequest(dat, message);
        send(message);
        try {
            logResponse(dat, this.parser.toString());
            int code = this.parser.getResponseCode();
            if ((code == 200 || code == 250) && this.parser.isSuccess()) {
                return;
            }
            throwException(this.parser);
        } catch (NumberFormatException nfe) {
            Session.getLog().debug("Error getting response code:", nfe);
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, "Error getting registrar response code");
        } catch (TransformerException te) {
            RegistrarException re = new RegistrarException(RegistrarException.OTHER_ERROR, te.getMessage());
            re.fillInStackTrace();
            throw re;
        }
    }

    public void setCookie(String domain, String tld, String username, String password) throws Exception {
        send(XCPCommands.cookie(this.f211ip, domain + "." + tld, username, password));
        if (this.parser.getResponseCode() == 200) {
            this.cookie = getAttribute("cookie");
        } else {
            throwException(this.parser);
        }
    }

    public void changeNameServers(List names, RegistrarTransactionData dat) throws Exception {
        execute(XCPCommands.changeNameServer(this.cookie, names), dat);
    }

    public void changeContactInfo(String type, Map info, RegistrarTransactionData dat) throws Exception {
        execute(XCPCommands.changeContactInfo(this.cookie, type, info), dat);
    }

    public void changePassword(String newPassword, RegistrarTransactionData dat) throws Exception {
        execute(XCPCommands.changePassword(this.cookie, newPassword), dat);
    }

    public void renew(String domain, String tld, String currentExpYear, int period, RegistrarTransactionData dat) throws Exception {
        execute(XCPCommands.renew(domain + "." + tld, currentExpYear, period), dat);
    }

    protected void processResult(RegistrarTransactionData dat) throws RegistrarException {
        try {
            dat.setResponse(this.parser.toString());
            if (this.parser.getResponseCode() != 200) {
                throwException(this.parser);
            }
        } catch (NumberFormatException nfe) {
            Session.getLog().debug("Error getting response code:", nfe);
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, "Error getting registrar response code");
        } catch (TransformerException te) {
            RegistrarException re = new RegistrarException(RegistrarException.OTHER_ERROR, te.getMessage());
            re.fillInStackTrace();
            throw re;
        }
    }

    static void throwException(XCPParser parser) throws RegistrarException {
        String attribute = null;
        try {
            attribute = parser.getAttribute("error");
        } catch (Exception e) {
            Session.getLog().debug("Inside OpenSRSInstance::throwException Unable to get 'error' item in attributes section");
        }
        try {
            String responseText = parser.getResponseText();
            Session.getLog().debug("responseText=" + responseText);
            throw new RegistrarException(parser.getResponseCode(), attribute != null ? responseText + "\n" + attribute : responseText);
        } catch (TransformerException te) {
            RegistrarException re = new RegistrarException(RegistrarException.OTHER_ERROR, te.getMessage());
            re.fillInStackTrace();
            throw re;
        }
    }

    protected String getAttribute(String key) throws Exception {
        return this.parser.getAttribute(key);
    }

    protected void send(XCPMessage message) throws Exception {
        this.con.send(message);
        this.parser = this.con.receiveXCP();
    }

    protected void getDomainInfo(String domain, String tld) {
    }

    protected void execute(XCPMessage message) throws Exception {
        execute(message, new RegistrarTransactionData());
    }

    protected void execute(XCPMessage message, RegistrarTransactionData dat) throws Exception {
        dat.setRequest(message.toString());
        send(message);
        processResult(dat);
    }

    protected void finalize() {
        try {
            close();
        } catch (Exception e) {
        }
    }
}
