package psoft.hsphere.tools;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.email.MailForward;
import psoft.util.FakeRequest;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MailCreator.class */
public class MailCreator extends C0004CP {
    private FileWriter log;
    private Document doc;
    private String startFromDomain;
    private InputSource dataFile;

    public MailCreator(String dataFileName, String logFileName) throws Exception {
        super("psoft_config.hsphere");
        this.log = null;
        this.doc = null;
        this.startFromDomain = null;
        this.dataFile = null;
        outMessage("Engine Initialized....\n");
        outMessage("Parsing data file.....");
        this.dataFile = new InputSource(new FileInputStream(dataFileName));
        DOMParser parser = new DOMParser();
        parser.parse(this.dataFile);
        this.doc = parser.getDocument();
        this.log = new FileWriter(logFileName);
        outOK();
    }

    public void process() throws Exception {
        Element root = this.doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("domain");
        for (int i = 0; i < list.getLength(); i++) {
            processDomain((Element) list.item(i));
        }
    }

    private void processDomain(Element domainNode) throws Exception {
        double accountCredit = -1.0d;
        Account oldAcc = Session.getAccount();
        User oldUser = Session.getUser();
        String dName = domainNode.getAttributes().getNamedItem("name").getNodeValue();
        try {
            try {
                outMessage("Loking for account owner for domain " + dName);
                Hashtable t = getUserAndAccountByDomainName(dName);
                outOK();
                outMessage("Setting user with ID " + t.get("user_id"));
                User u = User.getUser(((Long) t.get("user_id")).longValue());
                Session.setUser(u);
                outOK();
                outMessage("Setting account with ID " + t.get("account_id"));
                Account a = u.getAccount(new ResourceId(((Long) t.get("account_id")).longValue(), 0));
                Session.setAccount(a);
                outOK();
                try {
                    outMessage("Preparing balance ");
                    accountCredit = Session.getAccount().getBill().getCredit();
                    a.getBill().setCredit(100000.0d);
                    outOK();
                    outMessage("Trying to get domain resource");
                    try {
                        ResourceId domainId = new ResourceId(((Long) t.get("domain_id")).longValue(), ((Long) t.get("domain_type")).intValue());
                        Resource domainRes = domainId.get();
                        outOK();
                        outMessage("Loking for mail domain resource");
                        ResourceId mDomainId = domainId.findChild("mail_domain");
                        if (mDomainId == null) {
                            try {
                                outMessage("Mail domain not found. Trying to add...");
                                domainRes.addChild("mail_service", "signup", null);
                                mDomainId = domainId.findChild("mail_domain");
                                if (mDomainId != null) {
                                    outOK();
                                }
                            } catch (Exception ex) {
                                outFail();
                                throw ex;
                            }
                        }
                        try {
                            outMessage("Getting mail domain resource ");
                            Resource mDomainRes = mDomainId.get();
                            outOK();
                            NodeList mList = domainNode.getElementsByTagName("mailbox");
                            for (int i = 0; i < mList.getLength(); i++) {
                                processMailBox(mDomainRes, (Element) mList.item(i));
                            }
                            NodeList fList = domainNode.getElementsByTagName("forward");
                            for (int i2 = 0; i2 < fList.getLength(); i2++) {
                                processMailForward(mDomainRes, (Element) fList.item(i2));
                            }
                        } catch (Exception ex2) {
                            outFail();
                            throw ex2;
                        }
                    } catch (Exception ex3) {
                        outFail();
                        throw ex3;
                    }
                } catch (Exception ex4) {
                    outFail("Failed to prepare balance", ex4);
                    throw ex4;
                }
            } catch (Exception ex5) {
                outFail();
                throw ex5;
            }
        } finally {
            if (accountCredit != -1.0d) {
                Session.getAccount().getBill().setCredit(accountCredit);
            }
            Session.setUser(oldUser);
            Session.setAccount(oldAcc);
        }
    }

    private void processMailBox(Resource mDomainRes, Element mBoxNode) throws Exception {
        Hashtable args = new Hashtable();
        String name = mBoxNode.getAttributes().getNamedItem("name").getNodeValue();
        String password = mBoxNode.getAttributes().getNamedItem("password").getNodeValue();
        args.put("email", new String[]{name});
        if (password == null || "".equals(password)) {
            args.put("password", new String[]{Session.getUser().getPassword()});
        } else {
            args.put("password", new String[]{password});
        }
        args.put("description", new String[]{"The mailbox"});
        setRequest(args);
        try {
            try {
                outMessage("Creating mailbox " + name);
                mDomainRes.addChild("mailbox", "import", null);
                outOK();
                clearRequest();
            } catch (Exception ex) {
                outFail("Failed to create mailbox " + name, ex);
                clearRequest();
            }
        } catch (Throwable th) {
            clearRequest();
            throw th;
        }
    }

    private void processMailForward(Resource mDomainRes, Element forwardNode) throws Exception {
        Hashtable args = new Hashtable();
        ResourceId fId = null;
        String name = forwardNode.getAttributes().getNamedItem("name").getNodeValue();
        NodeList subscribers = forwardNode.getElementsByTagName("subscriber");
        for (int i = 0; i < subscribers.getLength(); i++) {
            if (i == 0) {
                String foreign = subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue();
                args.put("local", new String[]{name});
                args.put("subscriber", new String[]{foreign});
                args.put("description", new String[]{"The mail forward"});
                setRequest(args);
                try {
                    try {
                        outMessage("Creating mail forward " + name + "-->" + foreign);
                        fId = mDomainRes.addChild("mail_forward", "import", null);
                        outOK();
                        clearRequest();
                    } catch (Exception ex) {
                        outFail("Failed to add forward ", ex);
                        Session.getLog().error("Failed to add forward ", ex);
                        clearRequest();
                        return;
                    }
                } catch (Throwable th) {
                    clearRequest();
                    throw th;
                }
            } else {
                try {
                    outMessage("Adding forward subscriber");
                    ((MailForward) fId.get()).addSubscriber(subscribers.item(i).getAttributes().getNamedItem("email").getNodeValue());
                    outOK();
                } catch (Exception ex2) {
                    outFail("Failed to add forward subscriber", ex2);
                    Session.getLog().error("Failed to add forward subscriber", ex2);
                    return;
                }
            }
        }
    }

    private Hashtable getUserAndAccountByDomainName(String name) throws Exception {
        Hashtable result = new Hashtable();
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT ua.user_id, p.account_id, p.child_id, p.child_type FROM parent_child p, user_account ua, domains d WHERE d.name = ? AND d.id = p.child_id AND p.account_id = ua.account_id");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.put("user_id", new Long(rs.getLong(1)));
                result.put("account_id", new Long(rs.getLong(2)));
                result.put("domain_id", new Long(rs.getLong(3)));
                result.put("domain_type", new Long(rs.getLong(4)));
                Session.closeStatement(ps);
                con.close();
                return result;
            }
            throw new Exception("Account and User ID not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    protected void outOK() {
        System.out.println(" [  OK  ]");
        if (getLog() != null) {
            try {
                getLog().write(" [  OK  ]\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void outFail(String errMessage) {
        System.out.println(" [  FAILED  ]");
        System.out.println(errMessage);
        Session.getLog().error(errMessage);
        if (getLog() != null) {
            try {
                getLog().write(" [  FAILED  ]\n");
                getLog().write(errMessage + "\n");
            } catch (IOException e) {
            }
        }
    }

    protected void outMessage(String message) {
        System.out.print(message);
        Session.getLog().debug(message);
        if (getLog() != null) {
            try {
                getLog().write(message);
                getLog().flush();
            } catch (IOException e) {
            }
        }
    }

    protected void outFail() {
        System.out.println(" [  FAILED  ]");
        if (getLog() != null) {
            try {
                getLog().write(" [  FAILED  ]\n");
            } catch (IOException e) {
            }
        }
    }

    protected FileWriter getLog() {
        return this.log;
    }

    protected void setRequest(Hashtable args) {
        Session.setRequest(new FakeRequest(args));
    }

    protected void clearRequest() {
        Session.setRequest(new FakeRequest(new Hashtable()));
    }

    protected void outFail(String message, Exception ex) throws Exception {
        System.out.println(message);
        Session.getLog().error(ex);
        if (getLog() != null) {
            try {
                getLog().write(" [  FAILED  ]\n");
                getLog().write(message + "\n");
                ex.printStackTrace(new PrintWriter((Writer) getLog(), true));
                getLog().flush();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        String fileName = null;
        String logFileName = null;
        int i = 0;
        while (i < argv.length) {
            if ("-d".equals(argv[i]) || "--data".equals(argv[i])) {
                fileName = argv[i + 1];
                i++;
            }
            if ("-l".equals(argv[i]) || "--log".equals(argv[i])) {
                logFileName = argv[i + 1];
                i++;
            }
            i++;
        }
        if (fileName != null && logFileName != null) {
            MailCreator mc = new MailCreator(fileName, logFileName);
            mc.process();
        } else {
            System.out.println("Missconfiguration");
        }
        System.exit(0);
    }
}
