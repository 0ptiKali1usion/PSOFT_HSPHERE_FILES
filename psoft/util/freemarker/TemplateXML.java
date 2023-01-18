package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.util.List;
import org.apache.log4j.Category;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateXML.class */
public class TemplateXML implements TemplateHashModel, TemplateMethodModel, TemplateScalarModel, TemplateListModel {
    private static Category log = Category.getInstance(TemplateXML.class.getName());
    Node current;
    NodeList list;
    Node root;
    String oldKey;
    int count;
    int index;
    boolean isEmptyListFlag;

    public TemplateXML(Node node) {
        this.current = node;
    }

    public TemplateXML(Node node, String key) throws Exception {
        this.root = node;
        this.current = XPathAPI.selectSingleNode(node, key);
        this.oldKey = key;
    }

    public boolean isEmpty() {
        return this.isEmptyListFlag;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        if (l != null) {
            try {
                if (l.isEmpty()) {
                    return null;
                }
                Node n = XPathAPI.selectSingleNode(this.current, "@" + l.get(0));
                return new TemplateString(n.getNodeValue());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public TemplateModel getAttribute(String attr) throws TemplateModelException {
        try {
            Node n = XPathAPI.selectSingleNode(this.current, "@" + attr);
            return new TemplateString(n.getNodeValue());
        } catch (Exception e) {
            return null;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if ("node_name".equals(key)) {
                return new TemplateString(this.current.getNodeName());
            }
            return new TemplateXML(this.current, key);
        } catch (Exception e) {
            return null;
        }
    }

    public String getAsString() throws TemplateModelException {
        try {
            log.info("Current=" + this.current);
            Node n = XPathAPI.selectSingleNode(this.current, "text()");
            return n.getNodeValue();
        } catch (Exception te) {
            log.warn("Error getting parameter", te);
            throw new TemplateModelException(te.getMessage());
        }
    }

    public boolean hasNext() {
        return this.list != null && this.count < this.list.getLength();
    }

    public TemplateModel next() {
        NodeList nodeList = this.list;
        int i = this.count;
        this.count = i + 1;
        return new TemplateXML(nodeList.item(i));
    }

    public boolean isRewound() {
        return this.list != null && this.count == 0;
    }

    public void rewind() throws TemplateModelException {
        this.count = 0;
        try {
            this.list = XPathAPI.selectNodeList(this.root, this.oldKey);
            this.isEmptyListFlag = this.list.getLength() == 0;
        } catch (Exception te) {
            log.warn("Error getting list: " + this.oldKey, te);
            throw new TemplateModelException(te.getMessage());
        }
    }

    public TemplateModel get(int index) {
        return new TemplateXML(this.list.item(index));
    }

    public NodeList getElementsList() {
        return this.list;
    }

    public Node getCurrentNode() {
        return this.current;
    }
}
