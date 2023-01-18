package psoft.util.xml;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/* loaded from: hsphere.zip:psoft/util/xml/XMLElementTracker.class */
public class XMLElementTracker {
    private Element xmlElement;

    public XMLElementTracker() {
        this.xmlElement = null;
    }

    public XMLElementTracker(Element xmlElement) {
        this.xmlElement = xmlElement;
    }

    public void setElement(Element curElement) {
        this.xmlElement = curElement;
    }

    public String getTagSequence() {
        if (this.xmlElement != null) {
            String res = getElementDescription();
            String indent = "";
            Node parentNode = this.xmlElement.getParentNode();
            while (true) {
                Node curNode = parentNode;
                if (curNode == null || curNode.getNodeType() == 9) {
                    break;
                }
                if (curNode.getNodeName() != null && curNode.getNodeType() == 1) {
                    res = getElementDescription(curNode) + indent + "\n" + res;
                    indent = indent + "  ";
                }
                parentNode = curNode.getParentNode();
            }
            return res;
        }
        return null;
    }

    public String getElementDescription() {
        return getElementDescription(this.xmlElement);
    }

    public static String getElementDescription(Node node) {
        if (node != null && node.getNodeType() == 1) {
            StringBuffer attrStr = new StringBuffer();
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                for (int i = attributes.getLength() - 1; i >= 0; i--) {
                    attrStr.append(" ").append(attributes.item(i));
                }
            }
            return "<" + node.getNodeName() + attrStr.toString() + ">";
        }
        return "";
    }
}
