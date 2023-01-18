package psoft.hsphere.converter.alabanza;

import java.io.IOException;
import java.util.Hashtable;
import org.apache.log4j.Category;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/AlabanzaConfig.class */
public class AlabanzaConfig {
    public static String systemLogin;
    public static String systemPassword;
    public static String systemClientsListUrl;
    public static String systemPrefix;
    public static String systemUnknownUserPrefix;
    public static String systemUnknownUserPassword;
    public static String systemFileName;
    public static String systemBillFileName;
    public static String systemCPSuffix;
    public static String systemHttpPrefix;
    public static String systemFtpPrefix;
    public static String systemLogFile;
    public static String systemOneClientUrl;
    public static String systemCustomUsersFileName;
    public static String systemBillsUrl;
    public static Boolean systemUseCC;
    public static Boolean systemUseBill;
    public static Boolean systemGetNoDomain;
    public static Boolean systemGetIP;
    public static Boolean systemGetOwnerForward;
    public static Boolean systemGetCustomUsers;
    public static Boolean systemUseSeparateAccs;
    public static Integer systemStartPage;
    protected static Category log;
    protected static Hashtable billPeriods = new Hashtable();

    public static boolean setClassVariables() {
        boolean result;
        System.out.println("Getting system variables from xml file...");
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource("migrate_config.xml"));
            Document init_doc = parser.getDocument();
            Element root = init_doc.getDocumentElement();
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node oneNode = list.item(i);
                short nodeType = oneNode.getNodeType();
                if (nodeType != 3) {
                    String nodeName = oneNode.getNodeName();
                    String nodeValue = oneNode.getAttributes().getNamedItem("name").getNodeValue();
                    if (nodeName.equals("login")) {
                        systemLogin = nodeValue;
                    } else if (nodeName.equals("password")) {
                        systemPassword = nodeValue;
                    } else if (nodeName.equals("clientslisturl")) {
                        systemClientsListUrl = nodeValue;
                    } else if (nodeName.equals("prefix")) {
                        systemPrefix = nodeValue;
                    } else if (nodeName.equals("unknownuserprefix")) {
                        systemUnknownUserPrefix = nodeValue;
                    } else if (nodeName.equals("filename")) {
                        systemFileName = nodeValue;
                    } else if (nodeName.equals("cpsuffix")) {
                        systemCPSuffix = nodeValue;
                    } else if (nodeName.equals("httpprefix")) {
                        systemHttpPrefix = nodeValue;
                    } else if (nodeName.equals("ftpprefix")) {
                        systemFtpPrefix = nodeValue;
                    } else if (nodeName.equals("usecc")) {
                        systemUseCC = new Boolean(nodeValue);
                    } else if (nodeName.equals("billfile")) {
                        systemBillFileName = nodeValue;
                    } else if (nodeName.equals("usebill")) {
                        systemUseBill = new Boolean(nodeValue);
                    } else if (nodeName.equals("getnodomain")) {
                        systemGetNoDomain = new Boolean(nodeValue);
                    } else if (nodeName.equals("getip")) {
                        systemGetIP = new Boolean(nodeValue);
                    } else if (nodeName.equals("logfile")) {
                        systemLogFile = nodeValue;
                    } else if (nodeName.equals("getownerforward")) {
                        systemGetOwnerForward = new Boolean(nodeValue);
                    } else if (nodeName.equals("getcustomclients")) {
                        systemGetCustomUsers = new Boolean(nodeValue);
                    } else if (nodeName.equals("oneclienturl")) {
                        systemOneClientUrl = nodeValue;
                    } else if (nodeName.equals("customclientsfile")) {
                        systemCustomUsersFileName = nodeValue;
                    } else if (nodeName.equals("startpage")) {
                        systemStartPage = new Integer(Integer.parseInt(nodeValue));
                    } else if (nodeName.equals("billperiods")) {
                        systemBillsUrl = nodeValue;
                    } else if (nodeName.equals("separateaccs")) {
                        systemUseSeparateAccs = new Boolean(nodeValue);
                    }
                }
            }
            System.out.println("Getting system variables finished.\n\n");
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public static boolean setLog() {
        try {
            log = Category.getRoot();
            log.addAppender(new FileAppender(new SimpleLayout(), systemLogFile));
            log.info("Log initialized.");
            return true;
        } catch (IOException e) {
            System.out.println("Failed log settings.");
            return false;
        }
    }

    public static Category getLog() {
        return log;
    }

    public static void setBillPeriods(Hashtable bills) {
        billPeriods = bills;
    }

    public static Hashtable getBillPeriods() {
        return billPeriods;
    }

    public static String getBillPeriodValue(String key) {
        if (billPeriods.containsKey(key)) {
            return (String) billPeriods.get(key);
        }
        return null;
    }
}
