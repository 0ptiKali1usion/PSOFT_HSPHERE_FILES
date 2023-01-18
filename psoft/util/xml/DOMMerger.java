package psoft.util.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/* loaded from: hsphere.zip:psoft/util/xml/DOMMerger.class */
public class DOMMerger {
    private static final int MERGE = 0;
    private static final int REMOVE = 1;
    private static final int CHANGE = 2;
    private static final int CHNAGE_ATTR = 3;
    private static Category log = Category.getInstance(DOMMerger.class.getName());
    private static final int TRESHOLD = 10000;

    private static Map parseAttributes(String attr) {
        Map map = new HashMap();
        int i = 0;
        while (i < attr.length()) {
            while (i < attr.length() && Character.isWhitespace(attr.charAt(i))) {
                i++;
            }
            StringBuffer token = new StringBuffer();
            while (true) {
                if (i < attr.length()) {
                    char ch = attr.charAt(i);
                    if (ch != '=') {
                        token.append(ch);
                        i++;
                    } else {
                        i++;
                        break;
                    }
                } else {
                    break;
                }
            }
            int i2 = i + 1;
            String name = token.toString();
            StringBuffer token2 = new StringBuffer();
            while (i2 < attr.length()) {
                char ch2 = attr.charAt(i2);
                if (ch2 == '\\') {
                    i2++;
                    token2.append(attr.charAt(i2));
                } else if (ch2 == '\"') {
                    break;
                } else {
                    token2.append(ch2);
                }
                i2++;
            }
            i = i2 + 1;
            if (name.length() > 0) {
                map.put(name, token2.toString());
            }
        }
        return map;
    }

    private static void merge(Document doc, Node main, Node n) {
        NodeList list1 = main.getChildNodes();
        NodeList list2 = n.getChildNodes();
        int l1 = list1.getLength();
        int l2 = list2.getLength();
        if (l1 == 0) {
            for (int i = 0; i < l2; i++) {
                main.appendChild(doc.importNode(list2.item(i), true));
            }
        }
        if (l2 == 0) {
            return;
        }
        ArrayList al1 = null;
        ArrayList al2 = null;
        boolean optimized = false;
        if (l1 > 1 && l2 > 1 && l1 * l2 > TRESHOLD) {
            optimized = true;
            al1 = new ArrayList(l1);
            for (int i2 = 0; i2 < l1; i2++) {
                al1.add(new Long(getHashCode(list1.item(i2))));
            }
            al2 = new ArrayList(l2);
            for (int i3 = 0; i3 < l2; i3++) {
                al2.add(new Long(getHashCode(list2.item(i3))));
            }
        }
        int action = 0;
        long hash = -1;
        Map changeAttrMap = null;
        for (int j = 0; j < l2; j++) {
            Node n2 = list2.item(j);
            boolean found = false;
            if (n2.getNodeType() == 7) {
                String instruction = n2.getNodeName();
                if ("remove".equals(instruction)) {
                    action = 1;
                } else if ("change".equals(instruction)) {
                    action = 2;
                } else if ("merge".equals(instruction)) {
                    action = 0;
                } else if ("changeattr".equals(instruction)) {
                    action = 3;
                    String changeAttrData = ((ProcessingInstruction) n2).getData();
                    changeAttrMap = parseAttributes(changeAttrData);
                }
            }
            if (optimized) {
                hash = ((Long) al2.get(j)).longValue();
            }
            int i4 = 0;
            while (true) {
                if (i4 < l1) {
                    if (!optimized || ((Long) al1.get(i4)).longValue() != hash) {
                        Node n1 = list1.item(i4);
                        if (equals(n1, n2)) {
                            if (n1.getNodeType() == 1) {
                                switch (action) {
                                    case 0:
                                        merge(doc, n1, n2);
                                        break;
                                    case 1:
                                        n1.getParentNode().removeChild(n1);
                                        l1--;
                                        if (optimized) {
                                            al1.remove(i4);
                                            break;
                                        }
                                        break;
                                    case 2:
                                        n1.getParentNode().replaceChild(doc.importNode(n2, true), n1);
                                        break;
                                    case 3:
                                        Element e = (Element) n1;
                                        for (String name : changeAttrMap.keySet()) {
                                            String value = (String) changeAttrMap.get(name);
                                            e.setAttribute(name, value);
                                        }
                                        break;
                                }
                            }
                            found = true;
                        }
                    }
                    i4++;
                }
            }
            if (!found) {
                switch (action) {
                    case 0:
                    case 2:
                        main.appendChild(doc.importNode(n2, true));
                        break;
                }
            }
            if (n2.getNodeType() == 1) {
                action = 0;
            }
        }
    }

    private static long getHashCode(Node n) {
        switch (n.getNodeType()) {
            case 1:
                return n.getNodeName().hashCode() + getHashCode(n.getAttributes());
            case 2:
            case 5:
            case 6:
            default:
                return -1L;
            case 3:
            case 4:
            case 8:
                return n.getNodeValue().hashCode();
            case 7:
                return n.getNodeName().hashCode() + n.getNodeValue().hashCode();
        }
    }

    private static long getHashCode(NamedNodeMap m) {
        if (m == null) {
            return 0L;
        }
        long code = 0;
        for (int i = 0; i < m.getLength(); i++) {
            Node n = m.item(i);
            code = code + n.getNodeName().hashCode() + n.getNodeValue().hashCode();
        }
        return code;
    }

    private static boolean equals(Node n1, Node n2) {
        if (n1.getNodeType() != n2.getNodeType()) {
            return false;
        }
        switch (n1.getNodeType()) {
            case 1:
                if (n1.getNodeName().equals(n2.getNodeName())) {
                    return compareAttributes(n1.getAttributes(), n2.getAttributes());
                }
                return false;
            case 2:
            case 5:
            case 6:
            default:
                return false;
            case 3:
            case 4:
            case 8:
                return n1.getNodeValue().equals(n2.getNodeValue());
            case 7:
                if (n1.getNodeName().equals(n2.getNodeName())) {
                    return n1.getNodeValue().equals(n2.getNodeValue());
                }
                return false;
        }
    }

    private static boolean compareAttributes(NamedNodeMap m1, NamedNodeMap m2) {
        if (m1 == null) {
            return m2 == null;
        } else if (m1.getLength() != m2.getLength()) {
            return false;
        } else {
            for (int i = 0; i < m1.getLength(); i++) {
                Node n = m1.item(i);
                String name = n.getNodeName();
                String value = n.getNodeValue();
                if (!value.equals(m2.getNamedItem(name).getNodeValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    public static void merge(Document doc1, Document doc2) {
        merge(doc1, doc1.getDocumentElement(), doc2.getDocumentElement());
    }
}
