package psoft.hsphere.resource.opensrs;

import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import psoft.hsphere.Session;
import psoft.util.Config;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/opensrs/Control.class */
public class Control {
    protected static boolean silentMode;
    protected static final TemplateModel domainLookup = new DomainLookup();
    protected static String scriptLocation = Config.getProperty("CLIENT", "OPENSRS_SCRIPT");

    static {
        try {
            silentMode = Config.getProperty("CLIENT", "OPENSRS_SILENT_MODE").equals("TRUE");
        } catch (Exception e) {
            silentMode = false;
        }
    }

    public static TemplateModel getDomainLookup() {
        return domainLookup;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/opensrs/Control$DomainLookup.class */
    static class DomainLookup implements TemplateMethodModel {
        DomainLookup() {
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) {
            List list2 = HTMLEncoder.decode(list);
            if (list2.size() == 0) {
                return new TemplateString("4");
            }
            return new TemplateString(Control.lookup((String) list2.get(0)));
        }
    }

    public static String getCookie(String username, String password, String domain) {
        String[] cmd = {scriptLocation + "cookie.pl"};
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            PrintWriter writer = new PrintWriter(p.getOutputStream());
            writer.println("domain\n" + domain + "\nreg_username\n" + username + "\nreg_password\n" + password);
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            if (p.waitFor() == 0) {
                return reader.readLine();
            }
            System.err.println(reader.readLine());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static TemplateModel updateContactInfo(String domain, String username, String password, TemplateModelRoot root, Template t) {
        String[] cmd = {scriptLocation + "update_cinfo.pl"};
        TemplateHash result = new TemplateHash();
        if (silentMode) {
            result.put("status", "OK");
            return result;
        }
        try {
            String cookie = getCookie(username, password, domain);
            root.put("cookie", new TemplateString(cookie));
            Process p = Runtime.getRuntime().exec(cmd);
            PrintWriter writer = new PrintWriter(p.getOutputStream());
            t.process(root, writer);
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            if (p.waitFor() == 0) {
                result.put("status", "OK");
            } else {
                StringBuffer buf = new StringBuffer();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    buf.append(line);
                }
                result.put("status", "ERROR");
                result.put("msg", buf.toString());
            }
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("msg", e.getMessage());
        }
        return result;
    }

    public static TemplateHashModel register(TemplateModelRoot root, Template t) {
        Session.getLog().debug("Start register");
        String[] cmd = {scriptLocation + "register.pl"};
        TemplateHash result = new TemplateHash();
        if (silentMode) {
            result.put("status", "OK");
            return result;
        }
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            PrintWriter writer = new PrintWriter(p.getOutputStream());
            t.process(root, writer);
            writer.close();
            FileOutputStream out1 = new FileOutputStream("/tmp/opensrs.out");
            PrintWriter out2 = new PrintWriter(new OutputStreamWriter(out1));
            t.process(root, out2);
            out2.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            if (p.waitFor() == 0) {
                result.put("status", "OK");
                result.put("id", line1);
            } else {
                result.put("status", "ERROR");
                result.put("msg", line2);
                result.put("error", line1);
            }
            Session.getLog().debug("Finish register, line1=" + line1 + " line2=" + line2);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        return result;
    }

    public static int lookup(String name) {
        String[] cmd = {scriptLocation + "lookup.pl", name};
        if (silentMode) {
            return 0;
        }
        try {
            int exitVal = Runtime.getRuntime().exec(cmd).waitFor();
            return exitVal > 255 ? exitVal >> 8 : exitVal;
        } catch (Exception e) {
            e.printStackTrace();
            Session.getLog().debug("Cannot execute" + cmd);
            return 3;
        }
    }

    public static void main(String[] args) {
        lookup(args[0]);
    }
}
