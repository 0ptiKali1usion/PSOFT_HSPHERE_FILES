package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/ConfigImport.class */
public class ConfigImport {
    protected static Connection conn;

    /* renamed from: db */
    protected static Database f33db;
    protected static Map typeMap = new HashMap();

    public static void main(String[] args) {
        try {
            DOMParser parser = new DOMParser();
            System.out.println("Start import");
            parser.parse(new InputSource(System.in));
            Document doc = parser.getDocument();
            Element root = doc.getDocumentElement();
            NodeList list = root.getElementsByTagName("type");
            ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            f33db = Toolbox.getDB(config);
            conn = f33db.getConnection();
            PreparedStatement ps9 = conn.prepareStatement("DELETE FROM plan_access");
            ps9.executeUpdate();
            ps9.close();
            PreparedStatement ps92 = conn.prepareStatement("DELETE FROM plan_ivalue");
            ps92.executeUpdate();
            ps92.close();
            PreparedStatement ps93 = conn.prepareStatement("DELETE FROM plan_imod");
            ps93.executeUpdate();
            ps93.close();
            PreparedStatement ps94 = conn.prepareStatement("DELETE FROM plan_resource");
            ps94.executeUpdate();
            ps94.close();
            PreparedStatement ps95 = conn.prepareStatement("DELETE FROM plan_iresource");
            ps95.executeUpdate();
            ps95.close();
            PreparedStatement ps96 = conn.prepareStatement("DELETE FROM plan_value");
            ps96.executeUpdate();
            Session.closeStatement(ps96);
            conn.close();
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                String typeId = node.getAttributes().getNamedItem("id").getNodeValue();
                String typeName = node.getAttributes().getNamedItem("name").getNodeValue();
                String typePrice = node.getAttributes().getNamedItem("price").getNodeValue();
                String resellerPriceType = node.getAttributes().getNamedItem("rprice").getNodeValue();
                String required = getValue(node.getAttributes().getNamedItem("required"), "0");
                String priority = getValue(node.getAttributes().getNamedItem("priority"), "0");
                String typeDesc = node.getFirstChild().getNodeValue();
                typeMap.put(typeName, typeId);
                conn = f33db.getConnection();
                PreparedStatement ps = conn.prepareStatement("UPDATE type_name SET name = ?, description= ?,  price = ?, rprice = ?, required = ?, priority = ? WHERE id = ?");
                ps.setString(1, typeName);
                ps.setString(2, typeDesc);
                ps.setString(3, typePrice);
                ps.setString(4, resellerPriceType);
                ps.setInt(5, Integer.parseInt(required));
                ps.setInt(6, Integer.parseInt(priority));
                ps.setInt(7, Integer.parseInt(typeId));
                if (ps.executeUpdate() == 0) {
                    ps.close();
                    ps = conn.prepareStatement("INSERT INTO type_name (id, name, description, price, rprice, required, priority) VALUES(?, ?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, Integer.parseInt(typeId));
                    ps.setString(2, typeName);
                    ps.setString(3, typeDesc);
                    ps.setString(4, typePrice);
                    ps.setString(5, resellerPriceType);
                    ps.setInt(6, Integer.parseInt(required));
                    ps.setInt(7, Integer.parseInt(priority));
                    try {
                        ps.executeUpdate();
                    } catch (SQLException se) {
                        System.out.println("Error at: " + typeId + ":" + typeName + ":" + se);
                        throw se;
                    }
                }
                Session.closeStatement(ps);
                conn.close();
            }
            conn = f33db.getConnection();
            PreparedStatement ps97 = conn.prepareStatement("SELECT id FROM type_name ORDER BY id");
            PreparedStatement ps8 = conn.prepareStatement("DELETE FROM type_name WHERE id = ?");
            ResultSet rs9 = ps97.executeQuery();
            while (rs9.next()) {
                String typeId2 = rs9.getString(1);
                if (!typeMap.containsValue(typeId2)) {
                    ps8.setInt(1, Integer.parseInt(typeId2));
                    try {
                        ps8.executeUpdate();
                    } catch (SQLException se2) {
                        System.out.println("Warning: can't remove unused type:" + typeId2 + ":" + se2);
                    }
                }
            }
            Session.closeStatement(ps8);
            Session.closeStatement(ps97);
            conn.close();
            NodeList list2 = root.getElementsByTagName("plan");
            ArrayList usedPlans = new ArrayList();
            for (int i2 = 0; i2 < list2.getLength(); i2++) {
                Node node2 = list2.item(i2);
                String planId = node2.getAttributes().getNamedItem("id").getNodeValue();
                usedPlans.add(planId);
                String disabled = node2.getAttributes().getNamedItem("disabled").getNodeValue();
                String billable = getValue(node2.getAttributes().getNamedItem("billable"), "1");
                String cinfo = getValue(node2.getAttributes().getNamedItem("cinfo"), "1");
                String planDesc = getValue(node2.getAttributes().getNamedItem("description"), "No description");
                String resellerId = getValue(node2.getAttributes().getNamedItem("reseller_id"), "1");
                String deleted = getValue(node2.getAttributes().getNamedItem("deleted"), "0");
                conn = f33db.getConnection();
                System.out.println("Plan -->" + planId);
                PreparedStatement ps2 = conn.prepareStatement("UPDATE plans SET description = ?, disabled = ?, billing = ?,  cinfo = ?, reseller_id = ?, deleted = ? WHERE id = ?");
                ps2.setString(1, planDesc);
                ps2.setInt(2, Integer.parseInt(disabled));
                ps2.setInt(3, Integer.parseInt(billable));
                ps2.setInt(4, Integer.parseInt(cinfo));
                ps2.setFloat(5, Float.parseFloat(resellerId));
                ps2.setInt(6, Integer.parseInt(deleted));
                ps2.setInt(7, Integer.parseInt(planId));
                if (ps2.executeUpdate() == 0) {
                    ps2.close();
                    ps2 = conn.prepareStatement("INSERT INTO plans (id, description, disabled, billing, cinfo, reseller_id, deleted) VALUES(?, ?, ?, ?, ?, ?, ?)");
                    ps2.setInt(1, Integer.parseInt(planId));
                    ps2.setString(2, planDesc);
                    ps2.setInt(3, Integer.parseInt(disabled));
                    ps2.setInt(4, Integer.parseInt(billable));
                    ps2.setInt(5, Integer.parseInt(cinfo));
                    ps2.setLong(6, Long.parseLong(resellerId));
                    ps2.setInt(7, Integer.parseInt(deleted));
                    ps2.executeUpdate();
                }
                parsePlan(planId, (Element) node2);
                Session.closeStatement(ps2);
                conn.close();
            }
            conn = f33db.getConnection();
            PreparedStatement ps98 = conn.prepareStatement("SELECT id FROM plans ORDER BY id");
            PreparedStatement ps7 = conn.prepareStatement("DELETE FROM plans WHERE id = ?");
            PreparedStatement ps7a = conn.prepareStatement("DELETE FROM cmp_plan_group WHERE plan_id = ?");
            ResultSet rs92 = ps98.executeQuery();
            while (rs92.next()) {
                String planId2 = rs92.getString(1);
                if (!usedPlans.contains(planId2)) {
                    ps7a.setInt(1, Integer.parseInt(planId2));
                    ps7.setInt(1, Integer.parseInt(planId2));
                    try {
                        ps7a.executeUpdate();
                        ps7.executeUpdate();
                    } catch (SQLException se3) {
                        System.out.println("Warning: can't remove unused plan:" + planId2 + ":" + se3);
                    }
                }
            }
            Session.closeStatement(ps7a);
            Session.closeStatement(ps7);
            Session.closeStatement(ps98);
            conn.close();
            NodeList list3 = root.getElementsByTagName("plan");
            for (int i3 = 0; i3 < list3.getLength(); i3++) {
                Node node3 = list3.item(i3);
                String planId3 = node3.getAttributes().getNamedItem("id").getNodeValue();
                NodeList alist = ((Element) node3).getElementsByTagName("access");
                conn = f33db.getConnection();
                PreparedStatement ps3 = conn.prepareStatement("INSERT INTO plan_access(id, a_id) values(?, ?)");
                ps3.setInt(1, Integer.parseInt(planId3));
                for (int j = 0; j < alist.getLength(); j++) {
                    Node anode = alist.item(j);
                    ps3.setInt(2, Integer.parseInt(getValue(anode.getFirstChild())));
                    ps3.executeUpdate();
                }
                Session.closeStatement(ps3);
                conn.close();
            }
            System.out.println("Done");
        } catch (Exception e) {
            System.out.println("Plan = " + ((String) null));
            e.printStackTrace();
        }
    }

    static String getType(Node node) throws Exception {
        String type = node.getAttributes().getNamedItem("name").getNodeValue();
        String typeId = (String) typeMap.get(type);
        if (typeId == null) {
            throw new Exception("Couldn't resolve type " + type);
        }
        return typeId;
    }

    public static void parsePlan(String id, Element root) throws Exception {
        NodeList list = root.getElementsByTagName("value");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            conn = f33db.getConnection();
            PreparedStatement ps1 = null;
            try {
                ps1 = conn.prepareStatement("INSERT INTO plan_value (plan_id, type_id, name, value)  VALUES (?,?,?,?)");
                ps1.setString(1, id);
                ps1.setString(2, node.getParentNode() == root ? null : getType(node.getParentNode()));
                ps1.setString(3, node.getAttributes().getNamedItem("name").getNodeValue());
                ps1.setString(4, getValue(node.getFirstChild()));
                ps1.executeUpdate();
                Session.closeStatement(ps1);
                conn.close();
            } catch (Throwable th) {
                Session.closeStatement(ps1);
                conn.close();
                throw th;
            }
        }
        NodeList list2 = root.getElementsByTagName("resource");
        for (int i2 = 0; i2 < list2.getLength(); i2++) {
            Node node2 = list2.item(i2);
            String rId = getType(node2);
            conn = f33db.getConnection();
            PreparedStatement ps2 = null;
            try {
                ps2 = conn.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES( ?, ?, ?, ?, ? )");
                ps2.setLong(1, Long.parseLong(id));
                ps2.setLong(2, Long.parseLong(rId));
                ps2.setString(3, node2.getAttributes().getNamedItem("class").getNodeValue());
                ps2.setInt(4, Integer.parseInt(node2.getAttributes().getNamedItem("disabled").getNodeValue()));
                ps2.setInt(5, 1);
                try {
                    ps2.executeUpdate();
                    Session.closeStatement(ps2);
                    conn.close();
                    parseResource(id, rId, (Element) node2);
                } catch (SQLException se) {
                    System.err.println("resource -->" + node2.getAttributes().getNamedItem("class").getNodeValue() + "|" + rId + "|" + id);
                    throw se;
                }
            } catch (Throwable th2) {
                Session.closeStatement(ps2);
                conn.close();
                throw th2;
            }
        }
    }

    static void parseResource(String id, String rId, Element root) throws Exception {
        NodeList list = root.getElementsByTagName("mod");
        for (int i = 0; i < list.getLength(); i++) {
            parseResourceMod(id, rId, (Element) list.item(i));
        }
    }

    static void parseResourceMod(String id, String rId, Element root) throws Exception {
        String type;
        String mod = root.getAttributes().getNamedItem("name").getNodeValue();
        String disabled = root.getAttributes().getNamedItem("disabled").getNodeValue();
        conn = f33db.getConnection();
        PreparedStatement ps1 = null;
        try {
            ps1 = conn.prepareStatement("INSERT INTO plan_imod (plan_id, type_id, mod_id, disabled) VALUES(?, ?, ?, ?)");
            ps1.setString(1, id);
            ps1.setString(2, rId);
            if (mod.length() == 0) {
                ps1.setNull(3, 12);
            } else {
                ps1.setString(3, mod);
            }
            ps1.setString(4, disabled);
            ps1.executeUpdate();
            Session.closeStatement(ps1);
            conn.close();
            NodeList list = root.getElementsByTagName("initvalue");
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                String label = "";
                try {
                    String type2 = e.getAttributes().getNamedItem("type").getNodeValue();
                    if (type2.equals("field")) {
                        type = "1";
                        label = e.getAttributes().getNamedItem("label").getNodeValue();
                    } else if (type2.equals("relative")) {
                        type = "2";
                    } else if (type2.equals("absolute")) {
                        type = "3";
                    } else if (type2.equals("relative_rec")) {
                        type = "4";
                    } else if (type2.equals("absolute_rec")) {
                        type = "5";
                    } else {
                        type = type2.equals("hostgroup") ? "6" : "0";
                    }
                } catch (NullPointerException e2) {
                    type = "0";
                }
                conn = f33db.getConnection();
                PreparedStatement ps12 = null;
                try {
                    ps12 = conn.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, mod_id, value, order_id, disabled, type, label) VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
                    ps12.setString(1, id);
                    ps12.setString(2, rId);
                    if (mod.length() == 0) {
                        ps12.setNull(3, 12);
                    } else {
                        ps12.setString(3, mod);
                    }
                    ps12.setString(4, getValue(e.getFirstChild()));
                    ps12.setInt(5, i);
                    ps12.setString(6, disabled);
                    ps12.setString(7, type);
                    ps12.setString(8, label);
                    ps12.executeUpdate();
                    Session.closeStatement(ps12);
                    conn.close();
                } finally {
                }
            }
            NodeList list2 = root.getElementsByTagName("initresource");
            for (int i2 = 0; i2 < list2.getLength(); i2++) {
                Node node = list2.item(i2);
                String disabled2 = node.getAttributes().getNamedItem("disabled").getNodeValue();
                conn = f33db.getConnection();
                PreparedStatement ps13 = null;
                try {
                    ps13 = conn.prepareStatement("INSERT INTO plan_iresource (plan_id, type_id, new_type_id, mod_id, new_mod_id, order_id, disabled) VALUES(?, ?, ?, ?, ?, ?, ?)");
                    ps13.setString(1, id);
                    ps13.setString(2, rId);
                    ps13.setString(3, getType(node));
                    if (mod.length() == 0) {
                        ps13.setNull(4, 12);
                    } else {
                        ps13.setString(4, mod);
                    }
                    ps13.setString(5, getValue(node.getAttributes().getNamedItem("mod")));
                    ps13.setInt(6, i2);
                    ps13.setString(7, disabled2);
                    ps13.executeUpdate();
                    Session.closeStatement(ps13);
                    conn.close();
                } finally {
                    Session.closeStatement(ps13);
                    conn.close();
                }
            }
        } finally {
        }
    }

    static String getValue(Node n) {
        return getValue(n, null);
    }

    static String getValue(Node n, String def) {
        return n == null ? def : n.getNodeValue();
    }
}
