package psoft.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/mail/MailServer.class */
public class MailServer extends Thread {
    protected static Salt boudarySalt;
    protected boolean cont;
    protected String mailer;
    protected LinkedList messagePool;
    File mailFile;

    static {
        try {
            boudarySalt = new Salt();
        } catch (Exception e) {
        }
    }

    public MailServer(String mailer) {
        this(mailer, null);
    }

    public MailServer(String mailer, String pathToMailSwp) {
        super(mailer);
        this.mailFile = null;
        this.mailer = mailer;
        this.messagePool = new LinkedList();
        this.cont = true;
        if (pathToMailSwp != null && !"".equals(pathToMailSwp)) {
            this.mailFile = new File(pathToMailSwp);
        }
        setDaemon(true);
    }

    public void sendMessage(QueuedMessage m) {
        synchronized (this.messagePool) {
            this.messagePool.addLast(m);
        }
    }

    public void stopSending() {
        this.cont = false;
    }

    @Override // java.lang.Thread
    public void start() {
        this.cont = true;
        super.start();
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        if (this.mailFile != null && this.mailFile.exists()) {
            ObjectInputStream ois = null;
            try {
                try {
                    FileInputStream fis = new FileInputStream(this.mailFile);
                    ois = new ObjectInputStream(fis);
                    this.messagePool = (LinkedList) ois.readObject();
                    Session.getLog().info("messagePool size " + this.messagePool.size() + " has been loaded from " + this.mailFile.getAbsolutePath());
                    try {
                        ois.close();
                    } catch (IOException e) {
                        Session.getLog().info("Unable to init message pool: ", e);
                    }
                } catch (Exception e2) {
                    Session.getLog().info("Unable to init message pool: ", e2);
                    try {
                        ois.close();
                    } catch (IOException e3) {
                        Session.getLog().info("Unable to init message pool: ", e3);
                    }
                }
            } catch (Throwable th) {
                try {
                    ois.close();
                } catch (IOException e4) {
                    Session.getLog().info("Unable to init message pool: ", e4);
                }
                throw th;
            }
        }
        if (this.messagePool == null) {
            this.messagePool = new LinkedList();
        }
        while (this.cont && !isInterrupted()) {
            if (!isPoolEmpty()) {
                sendMessage();
            }
            try {
                sleep(100L);
            } catch (InterruptedException e5) {
                Session.getLog().info("Interrupting, Tring to save messagePool: ");
            }
        }
        if (this.mailFile == null || isPoolEmpty()) {
            return;
        }
        ObjectOutputStream oos = null;
        try {
            try {
                FileOutputStream fos = new FileOutputStream(this.mailFile);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(this.messagePool);
                Session.getLog().info("messagePool size " + this.messagePool.size() + " has been saved to " + this.mailFile.getAbsolutePath());
                try {
                    oos.close();
                } catch (IOException e6) {
                    Session.getLog().info("Unable to save message pool: ", e6);
                }
            } catch (FileNotFoundException e7) {
                Session.getLog().info("Unable to save message pool: ", e7);
                try {
                    oos.close();
                } catch (IOException e8) {
                    Session.getLog().info("Unable to save message pool: ", e8);
                }
            } catch (IOException e9) {
                Session.getLog().info("Unable to save message pool: ", e9);
                try {
                    oos.close();
                } catch (IOException e10) {
                    Session.getLog().info("Unable to save message pool: ", e10);
                }
            }
        } catch (Throwable th2) {
            try {
                oos.close();
            } catch (IOException e11) {
                Session.getLog().info("Unable to save message pool: ", e11);
            }
            throw th2;
        }
    }

    protected synchronized void sendMessage() {
        QueuedMessage m;
        synchronized (this.messagePool) {
            m = (QueuedMessage) this.messagePool.getFirst();
            this.messagePool.remove(m);
        }
        MimeMessage msg = null;
        try {
        } catch (Exception ex) {
            Session.getLog().error("Failed to create message for sending from:" + m.getFrom() + " to:" + m.getTo()[0], ex);
        }
        if (m.getNextTime().getTime() > System.currentTimeMillis()) {
            if (m.isNeedsToBeRemoved()) {
                return;
            }
            sendMessage(m);
            return;
        }
        m.setNextTime();
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(m.getProperties(), (Authenticator) null);
        msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(m.getFrom()));
        msg.addRecipients(Message.RecipientType.TO, m.getTo());
        msg.addRecipients(Message.RecipientType.CC, m.getCC());
        msg.addRecipients(Message.RecipientType.BCC, m.getBCC());
        String charset = m.getCharset();
        String contentType = m.getContentType();
        msg.setSentDate(m.getDate());
        msg.setHeader("X-Mailer", this.mailer);
        if (m.getAttachments() == null) {
            Session.getLog().debug("Sending message without attachment");
            msg.setContentLanguage(new String[]{LanguageManager.STANDARD_CHARSET});
            msg.setSubject(m.getSubject().trim(), LanguageManager.STANDARD_CHARSET);
            String str = "; charset=" + charset;
            msg.setText(m.getText(), LanguageManager.STANDARD_CHARSET);
            contentType = (contentType == null || "".equals(contentType)) ? "text/plain" : "text/plain";
            msg.setHeader("Content-Type", contentType + "; charset=UTF-8");
        } else {
            Session.getLog().debug("Sending message with!!! attachment");
            MimeMultipart mimeMultipart = new MimeMultipart();
            MimeBodyPart mbp1 = new MimeBodyPart();
            if (m.getText() != null) {
                mbp1.setText(m.getText(), LanguageManager.STANDARD_CHARSET);
                msg.setContentLanguage(new String[]{LanguageManager.STANDARD_CHARSET});
                msg.setSubject(m.getSubject().trim(), LanguageManager.STANDARD_CHARSET);
                mimeMultipart.addBodyPart(mbp1);
            }
            setAttachments(mimeMultipart, m.getAttachments());
            msg.setContent(mimeMultipart);
            msg.setHeader("Content-Transfer-Encoding", "8bit");
        }
        try {
            try {
                try {
                    System.setProperty("sun.net.inetaddr.ttl", "0");
                    Transport.send(msg);
                    InternetAddress[] allRecipients = msg.getAllRecipients();
                    String subj = msg.getSubject();
                    for (int i = 0; i < allRecipients.length; i++) {
                        if (allRecipients[i] != null) {
                            Session.getLog().debug("Message from: " + m.from + " Subject" + subj + " has been succesfully sent to" + allRecipients[i].getAddress());
                        }
                    }
                    System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
                } catch (SendFailedException sfe) {
                    try {
                        msg.setRecipients(Message.RecipientType.TO, sfe.getValidUnsentAddresses());
                        msg.setRecipients(Message.RecipientType.CC, "");
                        msg.setRecipients(Message.RecipientType.BCC, "");
                        Transport.send(msg);
                    } catch (MessagingException e) {
                        sendMessage(m);
                    } catch (Throwable tr) {
                        Session.getLog().info("Error sending message: ", tr);
                    }
                    System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
                }
            } catch (MessagingException e2) {
                sendMessage(m);
                System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            }
        } catch (Throwable th) {
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            throw th;
        }
    }

    protected void setAttachments(Multipart mp, Collection col) {
        if (col == null) {
            return;
        }
        for (Object at : col) {
            try {
                MimeBodyPart mbp = null;
                if (at instanceof Attachment) {
                    mbp = new MimeBodyPart();
                    mbp.setContent(((Attachment) at).getBody(), ((Attachment) at).getType());
                } else if (at instanceof psoft.hsphere.resource.p004tt.Attachment) {
                    mbp = (MimeBodyPart) ((psoft.hsphere.resource.p004tt.Attachment) at).getAttachmentAsPart();
                }
                mp.addBodyPart(mbp);
            } catch (Exception me) {
                me.printStackTrace();
            }
        }
    }

    protected void setAddr(Message msg, Message.RecipientType type, Collection address) {
        if (address == null) {
            return;
        }
        Iterator i = address.iterator();
        while (i.hasNext()) {
            try {
                msg.addRecipients(type, InternetAddress.parse((String) i.next(), false));
            } catch (AddressException ae) {
                ae.printStackTrace();
            } catch (MessagingException me) {
                me.printStackTrace();
            }
        }
    }

    public void sendMessage(Properties props, String from, Address[] rcpt, Address[] cc, Address[] bcc, String subject, String message, List attachments) {
        if (subject.indexOf("\n") > 0) {
            subject = subject.substring(0, subject.indexOf("\n"));
        }
        sendMessage(new QueuedMessage(props, from, cc, bcc, rcpt, subject, message, attachments, ""));
    }

    public void sendMessage(Properties props, String from, String to, String subject, String message) throws Exception {
        sendMessage(props, from, to, new Address[0], subject, message);
    }

    public void sendMessage(Properties props, String from, String to, List cc, Address[] bcc, String subject, String message) throws Exception {
        sendMessage(props, from, to, safeAddress(cc), bcc, subject, message);
    }

    public void sendMessage(Properties props, String from, String to, Address[] bcc, String subject, String message) throws Exception {
        sendMessage(props, from, to, new Address[0], bcc, subject, message);
    }

    public void sendMessage(Properties props, String from, String to, Address[] cc, Address[] bcc, String subject, String message) throws Exception {
        Address[] l;
        if (to == null) {
            if (cc != null) {
                l = cc;
                cc = null;
            } else {
                l = bcc;
                bcc = null;
            }
        } else {
            l = safeAddress(to);
        }
        sendMessage(props, from, l, cc, bcc, subject, message, null);
    }

    public void sendMessage(Properties props, String from, Address[] rcpt, Address[] cc, Address[] bcc, String subject, String message, List attachments, String charset, String contentType) {
        if (subject.indexOf("\n") > 0) {
            subject = subject.substring(0, subject.indexOf("\n"));
        }
        sendMessage(new QueuedMessage(props, from, cc, bcc, rcpt, subject, message, attachments, charset, contentType));
    }

    public void sendMessage(Properties props, String from, String to, String subject, String message, String charset, String contentType) throws Exception {
        sendMessage(props, from, to, new Address[0], new Address[0], subject, message, charset, contentType);
    }

    public void sendMessage(Properties props, String from, String to, Address[] cc, Address[] bcc, String subject, String message, String charset, String contentType) throws Exception {
        Address[] l;
        if ((to == null || to.length() == 0) && ((cc == null || cc.length == 0) && (bcc == null || bcc.length == 0))) {
            Session.getLog().error("Failed to send message: no recipients specified for message with subject: " + subject);
        }
        if (to == null) {
            if (cc != null && cc.length > 0) {
                l = cc;
                cc = null;
            } else {
                l = bcc;
                bcc = null;
            }
        } else {
            l = safeAddress(to);
        }
        sendMessage(props, from, l, cc, bcc, subject, message, null, charset, contentType);
    }

    public Address[] safeAddress(List list) {
        if (list == null) {
            return new Address[0];
        }
        List<Address> addresses = new ArrayList();
        Iterator i = list.iterator();
        while (i.hasNext()) {
            try {
                addresses.add(new InternetAddress((String) i.next()));
            } catch (Exception e) {
            }
        }
        Address[] l = new Address[addresses.size()];
        int count = 0;
        for (Address address : addresses) {
            int i2 = count;
            count++;
            l[i2] = address;
        }
        return l;
    }

    public Address[] safeAddress(String email) {
        Address[] l;
        try {
            l = new Address[]{new InternetAddress(email)};
        } catch (Exception e) {
            l = new Address[0];
        }
        return l;
    }

    public boolean isPoolEmpty() {
        boolean isEmpty;
        synchronized (this.messagePool) {
            isEmpty = this.messagePool.isEmpty();
        }
        return isEmpty;
    }
}
