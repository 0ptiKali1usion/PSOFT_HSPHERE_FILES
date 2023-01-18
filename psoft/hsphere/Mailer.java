package psoft.hsphere;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.MailMan;
import psoft.mail.MailServer;

/* loaded from: hsphere.zip:psoft/hsphere/Mailer.class */
public class Mailer {
    private static MailServer server;
    protected String host;
    protected String from;
    protected Properties props = System.getProperties();

    static {
        server = null;
        String mailerName = null;
        String mailSwp = null;
        try {
            mailerName = Session.getPropertyString("MAILER");
            mailSwp = Session.getPropertyString("MAILER_SWP");
        } catch (MissingResourceException e) {
        }
        server = new MailServer(mailerName == null ? "H-Sphere" : mailerName, mailSwp);
        server.start();
    }

    public static void stop() {
        Session.getLog().debug("Tring to stop mail server");
        server.interrupt();
    }

    public Mailer(String host, String from) {
        this.host = host;
        this.props.put("mail.smtp.host", host);
        this.from = from;
    }

    protected Properties getProperties() {
        return this.props;
    }

    public void sendMessage(String address, int type, String subject, String text, String charset) throws Exception {
        sendMessage(address, type, subject, text, (String) null, charset);
    }

    public void sendMessage(String address, int type, String subject, String text, String replyTo, String charset) throws Exception {
        sendMessage(address, null, type, subject, text, replyTo, charset, "");
    }

    public void sendMessage(String address, List cc, int type, String subject, String text, String replyTo, String charset) throws Exception {
        sendMessage(address, cc, type, subject, text, replyTo, charset, "");
    }

    public void sendMessage(String address, int type, String subject, String text, String replyTo, String charset, String contentType) throws Exception {
        sendMessage(address, null, type, subject, text, replyTo, charset, contentType);
    }

    public void sendMessage(String address, List cc, int type, String subject, String text, String replyTo, String charset, String contentType) throws Exception {
        String from = Settings.get().getValue("email");
        if (from == null) {
            from = this.from;
        }
        Session.getLog().info("Sending email for type: " + type);
        Session.getLog().info("Sending email to: " + address + " host: " + this.host + " from: " + from);
        if (replyTo == null) {
            replyTo = from;
        }
        long resellerId = Session.getResellerId();
        if (type == 3) {
            try {
                Session.setResellerId(1L);
            } catch (Throwable th) {
                Session.setResellerId(resellerId);
                throw th;
            }
        }
        server.sendMessage(getProperties(), replyTo, address, server.safeAddress(cc), MailMan.getMailMan().getEmails(type), subject, text, charset, contentType);
        Session.setResellerId(resellerId);
    }

    public void sendMessage(String address, List cc, int type, String subject, String text, String replyTo, List atts, String charset, String contentType) throws Exception {
        String from = Settings.get().getValue("email");
        if (from == null) {
            from = this.from;
        }
        Session.getLog().info("Sending email for type: " + type);
        Session.getLog().info("Sending email to: " + address + " host: " + this.host + " from: " + from + " attachments:" + (atts != null) + " contentType:" + contentType);
        if (replyTo == null) {
            replyTo = from;
        }
        long resellerId = Session.getResellerId();
        if (type == 3) {
            try {
                Session.setResellerId(1L);
            } catch (Throwable th) {
                Session.setResellerId(resellerId);
                throw th;
            }
        }
        server.sendMessage(getProperties(), replyTo, server.safeAddress(address), server.safeAddress(cc), MailMan.getMailMan().getEmails(type), subject, text, atts, charset, contentType);
        Session.setResellerId(resellerId);
    }

    public void sendMessage(String replyTo, String address, String subject, String text, String charset, String contentType) throws Exception {
        String from = Settings.get().getValue("email");
        if (from == null) {
            from = this.from;
        }
        if (replyTo == null) {
            replyTo = from;
        }
        Session.getLog().info("Sending -" + replyTo + "-" + address + "-" + subject + "-" + text + "<");
        server.sendMessage(getProperties(), replyTo, address, subject, text, charset, contentType);
    }

    public void sendMessage(String address, String subject, String text, String charset) throws Exception {
        sendMessage(address, subject, text, charset, "");
    }

    public void sendMessage(String address, String subject, String text, String charset, String contentType) throws Exception {
        sendMessage(address, new ArrayList(), new ArrayList(), subject, text, charset, contentType);
    }

    public void sendMessage(String address, List cc, List bcc, String subject, String text, String charset, String contentType) throws Exception {
        String from = Settings.get().getValue("email");
        if (from == null) {
            from = this.from;
        }
        Session.getLog().info("Sending email to: " + address + " host: " + this.host + " from: " + from + "--->" + text);
        if ((cc == null || cc.isEmpty()) && (bcc == null || bcc.isEmpty())) {
            server.sendMessage(getProperties(), from, address, subject, text, charset, contentType);
        } else {
            server.sendMessage(getProperties(), from, address, server.safeAddress(cc), server.safeAddress(bcc), subject, text, charset, contentType);
        }
    }

    public void sendMessage(String address, List cc, List bcc, String subject, String text, String replyTo, List atts, String charset, String contentType) throws Exception {
        String from = Settings.get().getValue("email");
        if (from == null) {
            from = this.from;
        }
        Session.getLog().info("Sending email to: " + address + " host: " + this.host + " from: " + from + " attachments:" + (atts != null) + " contentType:" + contentType);
        if (replyTo == null) {
            replyTo = from;
        }
        long resellerId = Session.getResellerId();
        try {
            server.sendMessage(getProperties(), replyTo, server.safeAddress(address), server.safeAddress(cc), server.safeAddress(bcc), subject, text, atts, charset, contentType);
            Session.setResellerId(resellerId);
        } catch (Throwable th) {
            Session.setResellerId(resellerId);
            throw th;
        }
    }
}
