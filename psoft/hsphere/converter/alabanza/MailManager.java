package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.resource.HTTPAuth;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/MailManager.class */
public class MailManager {
    public static boolean parseMailManager(URL url, Document doc, Element domain, String domainName, String address, String login, String password, List traffic) {
        boolean result;
        url.toString();
        boolean resMailbox = false;
        boolean resMailingList = false;
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            Hashtable lists = MigrationProvider.getMailURLList(in);
            if (!lists.isEmpty()) {
                Element mailservice = doc.createElement("mailservice");
                if (lists.containsKey("mailBoxes")) {
                    List mailboxes = (List) lists.get("mailBoxes");
                    Authenticator.setDefault(new HTTPAuth(login, password));
                    int size = mailboxes.size();
                    for (int i = 0; i < size; i++) {
                        String oneMailbox = (String) mailboxes.get(i);
                        URL urlOneMailbox = new URL(AlabanzaConfig.systemHttpPrefix + address + oneMailbox);
                        System.out.println("Mail manager link: " + AlabanzaConfig.systemHttpPrefix + address + oneMailbox);
                        resMailbox = parseOneMailbox(urlOneMailbox, doc, mailservice, domainName, login, password, traffic);
                    }
                }
                if (lists.containsKey("mailLists")) {
                    List maillists = (List) lists.get("mailLists");
                    int size2 = maillists.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        String oneMaillist = (String) maillists.get(i2);
                        int dotIndex = domainName.indexOf(".");
                        String domainCutName = domainName.substring(0, dotIndex);
                        URL urlOneMailingList = new URL(AlabanzaConfig.systemFtpPrefix + login + ":" + password + "@" + domainName + "/" + domainCutName + "-mail/" + oneMaillist + "/dist");
                        System.out.println("Mailing list manager link: " + AlabanzaConfig.systemFtpPrefix + login + ":" + password + "@" + domainName + "/" + domainCutName + "-mail/" + oneMaillist + "/dist");
                        resMailingList = parseOneMailingList(urlOneMailingList, doc, mailservice, oneMaillist, login, traffic);
                    }
                } else {
                    resMailingList = true;
                }
                domain.appendChild(mailservice);
            }
            in.close();
            System.out.println("Finished reading mail manager panel info.");
            if (resMailbox && resMailingList) {
                result = true;
            } else {
                result = false;
            }
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MailManager.parseMailManeger() for domain with name = " + domainName, e);
            System.out.println("Reading mail manager panel info failed.");
            result = false;
        }
        return result;
    }

    private static boolean parseOneMailbox(URL url, Document doc, Element mailservice, String domainName, String login, String password, List traffic) {
        boolean result;
        String tmp;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            Hashtable mailboxInfo = MigrationProvider.getMailBoxInfo(in);
            if (!mailboxInfo.isEmpty()) {
                Element mailbox = doc.createElement("mailbox");
                Element forward = doc.createElement("forward");
                String ownerName = HTMLEncoder.encode((String) mailboxInfo.get("ownerName"));
                if (!"/dev/null".equals(ownerName) && !"postmaster".equals(ownerName) && !"webmaster".equals(ownerName)) {
                    mailbox.setAttribute("name", ownerName);
                    mailbox.setAttribute("password", ownerName);
                    if (AlabanzaConfig.systemGetOwnerForward.booleanValue() && mailboxInfo.containsKey("isForwardToOwner")) {
                        Boolean isForwardToOwner = (Boolean) mailboxInfo.get("isForwardToOwner");
                        if (isForwardToOwner.booleanValue()) {
                            Element subs = doc.createElement("subscriber");
                            subs.setAttribute("email", ownerName + "@" + domainName);
                            forward.appendChild(subs);
                        }
                    }
                    if (mailboxInfo.containsKey("isForwardToOthers")) {
                        Boolean isForwardToOthers = (Boolean) mailboxInfo.get("isForwardToOthers");
                        if (isForwardToOthers.booleanValue()) {
                            List forwards = (List) mailboxInfo.get("forwardToOthers");
                            if (!forwards.isEmpty()) {
                                int size = forwards.size();
                                for (int i = 0; i < size; i++) {
                                    String oneForward = (String) forwards.get(i);
                                    Element subs2 = doc.createElement("subscriber");
                                    subs2.setAttribute("email", oneForward + "@" + domainName);
                                    forward.appendChild(subs2);
                                }
                            }
                        }
                    }
                    if (mailboxInfo.containsKey("isForwardToOutside")) {
                        Boolean isForwardToOutside = (Boolean) mailboxInfo.get("isForwardToOutside");
                        if (isForwardToOutside.booleanValue()) {
                            List forwards2 = (List) mailboxInfo.get("forwardToOutside");
                            if (!forwards2.isEmpty()) {
                                int size2 = forwards2.size();
                                for (int i2 = 0; i2 < size2; i2++) {
                                    String oneForward2 = (String) forwards2.get(i2);
                                    Element subs3 = doc.createElement("subscriber");
                                    subs3.setAttribute("email", oneForward2);
                                    forward.appendChild(subs3);
                                }
                            }
                        }
                    }
                    if (mailboxInfo.containsKey("isAutoresponse")) {
                        Boolean isAutoresponse = (Boolean) mailboxInfo.get("isAutoresponse");
                        if (isAutoresponse.booleanValue() && (tmp = (String) mailboxInfo.get("autoresponse")) != null) {
                            Element resp = doc.createElement("autoresponder");
                            resp.appendChild(doc.createTextNode(tmp));
                            mailbox.appendChild(resp);
                        }
                    }
                    mailservice.appendChild(mailbox);
                    if (forward.hasChildNodes()) {
                        forward.setAttribute("name", ownerName);
                        mailservice.appendChild(forward);
                    }
                }
            }
            in.close();
            System.out.println("Finished reading one mailbox info.");
            result = true;
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MailManager.parseOneMailbox() for domain with name = " + domainName, e);
            System.out.println("Reading one mailbox info failed.");
            result = false;
        }
        return result;
    }

    private static boolean parseOneMailingList(URL url, Document doc, Element mailservice, String listName, String login, List traffic) {
        boolean result;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            URLConnection con = url.openConnection();
            con.setDoInput(true);
            InputStream tmpStream = con.getInputStream();
            traffic.add(new Integer(tmpStream.available()));
            BufferedReader in = new BufferedReader(new InputStreamReader(tmpStream));
            List addresses = new LinkedList();
            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                inputLine.trim();
                if (inputLine.indexOf(listName) == -1 && inputLine.indexOf(" ") == -1 && !inputLine.startsWith("(Only addresses below this line can be automatically removed)")) {
                    addresses.add(inputLine);
                }
            }
            Element maillist = doc.createElement("maillist");
            maillist.setAttribute("name", listName);
            int size = addresses.size();
            for (int i = 0; i < size; i++) {
                String address = (String) addresses.get(i);
                if (!"".equals(address)) {
                    System.out.println("Mailing list subscriber's email: " + address);
                    Element subsc = doc.createElement("subscriber");
                    subsc.setAttribute("email", address);
                    maillist.appendChild(subsc);
                }
            }
            mailservice.appendChild(maillist);
            in.close();
            System.out.println("Finished reading one mailing list info.");
            result = true;
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MailManager.parseOneMailingList() for mailing list with name = " + listName, e);
            System.out.println("Reading one mailing list failed.");
            result = false;
        }
        return result;
    }
}
