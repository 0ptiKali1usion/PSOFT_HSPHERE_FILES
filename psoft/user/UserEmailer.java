package psoft.user;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateCache;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import psoft.util.Mail;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/user/UserEmailer.class */
public class UserEmailer {
    protected String from;
    protected static String DEF_TEMPLATE = "email.txt";
    protected TemplateCache tCache;

    public UserEmailer(String from, TemplateCache tCache) {
        this.from = from;
        this.tCache = tCache;
    }

    public void send(User u) {
        send(u, DEF_TEMPLATE);
    }

    public void send(User u, String template) {
        System.err.println("Sending " + template + " to " + u.getEmail());
        SimpleHash root = new SimpleHash();
        root.put("returnAddress", this.from);
        root.put(FMACLManager.USER, u);
        try {
            OutputStreamWriter out = new OutputStreamWriter(Mail.send(u.getEmail()));
            this.tCache.getTemplate(template).process(root, new PrintWriter(out));
            out.close();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }
}
