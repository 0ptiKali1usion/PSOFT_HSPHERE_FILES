package psoft.hsphere.resource.registrar.custom_registrar;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModelRoot;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Account;
import psoft.hsphere.Session;
import psoft.hsphere.manager.Entity;
import psoft.hsphere.resource.PhysicalServer;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/custom_registrar/CustomRegistrar.class */
public class CustomRegistrar extends Entity implements Registrar {
    protected String username;
    protected String key;

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String[] getSupportedTLDs() throws Exception {
        return (String[]) this.data.get("tlds");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String getSignature() {
        return new String("Custom");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void transfer(String domain, String tld, String login, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns) throws Exception {
        throw new Exception("NOT IMPLEMENTED");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public Date isTransfered(String name, String tld) throws Exception {
        throw new Exception("NOT IMPLEMENTED");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public boolean isTransferable(String name, String tld) throws Exception {
        return exec(get("tr_lookup_script"), name, tld);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public boolean lookup(String domain, String tld) throws Exception {
        return exec(get("lookup_script"), domain, tld);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void renew(String domain, String tld, String currentExpYear, int period, Map registrant) throws Exception {
        Account a = Session.getAccount();
        SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
        TemplateMap rgs = new TemplateMap(registrant);
        root.put("registrant", rgs);
        root.put("domain_name", new TemplateString(domain + "." + tld));
        if (this.data.get("renew_days") != null && !get("renew_days").equals("")) {
            root.put("renew", new TemplateString(get("renew_days")));
        }
        if (this.data.get("email_days") != null && !get("email_days").equals("")) {
            root.put("expiration", new TemplateString(get("email_days")));
        }
        root.put("period", new TemplateString(new Integer(period).toString()));
        send("CUSTOM_REGISTRAR_RENEW", root);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void register(String domain, String tld, String login, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns) throws Exception {
        register(domain, tld, login, password, period, registrant, tech, admin, billing, dns, null);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void register(String domain, String tld, String login, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, Map extraInfo) throws Exception {
        Account a = Session.getAccount();
        SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
        TemplateMap rgs = new TemplateMap(registrant);
        TemplateMap tch = new TemplateMap(tech);
        TemplateMap adm = new TemplateMap(admin);
        TemplateMap bll = new TemplateMap(billing);
        TemplateMap extra = new TemplateMap(extraInfo);
        TemplateList ds = new TemplateList(dns);
        root.put("registrant", rgs);
        root.put("tech", tch);
        root.put(FMACLManager.ADMIN, adm);
        root.put("billing", bll);
        root.put("extra", extra);
        root.put("dns", ds);
        root.put("domain_name", new TemplateString(domain + "." + tld));
        if (this.data.get("renew_days") != null && get("renew_days") != null && !get("renew_days").equals("")) {
            root.put("renew", new TemplateString(get("renew_days")));
        }
        if (this.data.get("email_days") != null && get("email_days") != null && !get("email_days").equals("")) {
            root.put("expiration", new TemplateString(get("email_days")));
        }
        root.put("period", new TemplateString(new Integer(period).toString()));
        send("CUSTOM_REGISTRAR_REGISTRATION", root);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void changeContacts(String domain, String tld, String login, String password, Map registrant, Map tech, Map admin, Map billing) throws Exception {
        Account a = Session.getAccount();
        SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
        TemplateMap rgs = new TemplateMap(registrant);
        TemplateMap tch = new TemplateMap(tech);
        TemplateMap adm = new TemplateMap(admin);
        TemplateMap bll = new TemplateMap(billing);
        root.put("registrant", rgs);
        root.put("tech", tch);
        root.put(FMACLManager.ADMIN, adm);
        root.put("billing", bll);
        root.put("domain_name", new TemplateString(domain + "." + tld));
        if (this.data.get("renew_days") != null && get("renew_days") != null && !get("renew_days").equals("")) {
            root.put("renew", new TemplateString(get("renew_days")));
        }
        if (this.data.get("email_days") != null && get("email_days") != null && !get("email_days").equals("")) {
            root.put("expiration", new TemplateString(get("email_days")));
        }
        send("CUSTOM_REGISTRAR_CONTACT_CHANGED", root);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void setPassword(String domain, String tld, String login, String password, String newPassword) throws Exception {
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void checkLogin() throws RegistrarException {
    }

    public CustomRegistrar(Integer id, String description) throws Exception {
        super(id, description);
        this.username = get("username");
        this.key = get(MerchantGatewayManager.MG_KEY_PREFIX);
    }

    protected boolean exec(String script, String domain, String tld) throws Exception, InterruptedException {
        if (script == null || script.length() == 0) {
            return false;
        }
        String errors = "STDERR: ";
        Session.getLog().debug("CustomRegistrar.lookup " + get("lookup_script") + " " + domain + "." + tld);
        Process p = Runtime.getRuntime().exec(new String[]{PhysicalServer.PREFIX + script, domain + "." + tld});
        Collection col = new LinkedList();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while (true) {
            String tmp = br.readLine();
            if (null == tmp) {
                break;
            }
            col.add(tmp);
        }
        int exit = p.waitFor();
        Session.getLog().debug("EXIT VALUE:" + p.exitValue());
        if (0 != p.exitValue()) {
            Session.getLog().debug("Toolbox.exec some errors: " + exit);
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String tmp2 = er.readLine();
                if (null == tmp2) {
                    break;
                }
                errors = errors + tmp2 + "\n";
            }
            throw new Exception("command /hsphere/shared/scripts/" + script + " failed: \n" + errors + "\n STDIN: " + domain + "." + tld);
        }
        String strState = (String) col.iterator().next();
        if (strState.indexOf("0") != -1) {
            return true;
        }
        if (strState.indexOf("1") != -1) {
            return false;
        }
        Ticket.create(new Exception("command /hsphere/shared/scripts/" + get("lookup_script") + " failed: \n" + errors + "\n STDIN: " + domain + "." + tld), this);
        return false;
    }

    protected void send(String tag, TemplateModelRoot root) throws Exception {
        String[] emls = getValues("mailto");
        String[] ccs = getValues("cc");
        List ccList = null;
        if (ccs != null) {
            ccList = new LinkedList();
            for (String str : ccs) {
                ccList.add(str);
            }
        }
        boolean createTT = false;
        for (int i = 0; i < emls.length; i++) {
            if (emls[i].equals("tts")) {
                createTT = true;
            } else {
                CustomEmailMessage.send(tag, emls[i], root, ccList);
            }
        }
        if (createTT) {
            CustomEmailMessage customMessage = CustomEmailMessage.getMessage(tag);
            String body = customMessage.getBody(root);
            String subject = customMessage.getSubject(root);
            Ticket.create(subject, 75, body, null, 1, 1, 0, 1, 1, 0);
        }
    }
}
