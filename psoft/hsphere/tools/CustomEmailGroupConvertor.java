package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Set;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.util.XMLManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/CustomEmailGroupConvertor.class */
public class CustomEmailGroupConvertor {
    private static final Category log = Category.getInstance(CustomEmailGroupConvertor.class.getName());

    public CustomEmailGroupConvertor() throws Exception {
        ExternalCP.initCP();
    }

    public static void main(String[] args) throws Exception {
        StringBuffer keys = new StringBuffer("");
        for (String str : args) {
            keys.append(str);
        }
        if (keys.toString().indexOf("--help") != -1) {
            System.out.println("NAME:\n\t psoft.hsphere.tools.CustomEmailGroupConvertor - H-Sphere  tool");
            System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.CustomEmailGroupConvertor [options]");
            System.out.println("OPTIONS:");
            System.out.println("\t--help \t- shows this screen");
            System.exit(0);
        }
        CustomEmailGroupConvertor cegc = new CustomEmailGroupConvertor();
        System.out.println(keys);
        cegc.process(keys.toString());
        System.exit(0);
    }

    public void process(String keys) throws Exception {
        System.out.println("Initializing...");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        HashMap xml_data = new HashMap();
        try {
            Document custom_emails = XMLManager.getXML("USER_EMAILS");
            Element root = custom_emails.getDocumentElement();
            NodeList emails = root.getElementsByTagName("email");
            for (int i = 0; i < emails.getLength(); i++) {
                Element email = (Element) emails.item(i);
                xml_data.put(email.getAttribute("tag"), email.getAttribute("group_id"));
            }
        } catch (Exception e) {
            log.error("Unable to read custom email", e);
        }
        try {
            try {
                HashMap data = new HashMap();
                PreparedStatement ps2 = con.prepareStatement("SELECT id FROM custom_emails WHERE tag = ?");
                Set<String> xml_keys = xml_data.keySet();
                for (String tag : xml_keys) {
                    String group_id = (String) xml_data.get(tag);
                    ps2.setString(1, tag);
                    ResultSet rs = ps2.executeQuery();
                    while (rs.next()) {
                        data.put(new Long(rs.getLong(1)), group_id);
                    }
                }
                ps = con.prepareStatement("UPDATE custom_emails SET group_id = ? WHERE id= ?");
                Set<Long> data_keys = data.keySet();
                for (Long id : data_keys) {
                    int group_id2 = Integer.parseInt((String) data.get(id));
                    System.out.println("Update message with id = " + id + " and group id = " + group_id2);
                    ps.setInt(1, group_id2);
                    ps.setLong(2, id.longValue());
                    ps.executeUpdate();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e2) {
                log.error("error updating db", e2);
                e2.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
