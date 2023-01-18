package psoft.hsphere.converter.alabanza;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/CustomUsers.class */
public class CustomUsers {
    private static List users = new LinkedList();

    public static List getCustomUsers() {
        System.out.println("Getting custom users from xml file...");
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(AlabanzaConfig.systemCustomUsersFileName));
            Document init_doc = parser.getDocument();
            Element root = init_doc.getDocumentElement();
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node oneNode = list.item(i);
                short nodeType = oneNode.getNodeType();
                if (nodeType != 3) {
                    String nodeName = oneNode.getNodeName();
                    if (nodeName.equals(FMACLManager.USER)) {
                        Hashtable oneUser = new Hashtable();
                        String userId = oneNode.getAttributes().getNamedItem("id").getNodeValue();
                        oneUser.put("id", userId);
                        users.add(oneUser);
                    }
                }
            }
            System.out.println("Getting custom users finished.\n\n");
        } catch (Exception e) {
            System.out.println("Reading custom users failed. View log file for details.\n");
            AlabanzaConfig.getLog().error("Some errors occured in CustomUsers.getCustomUsers()", e);
        }
        return users;
    }
}
