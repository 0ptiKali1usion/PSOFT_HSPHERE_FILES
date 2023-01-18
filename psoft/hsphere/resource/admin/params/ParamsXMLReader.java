package psoft.hsphere.resource.admin.params;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.util.XMLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/ParamsXMLReader.class */
public abstract class ParamsXMLReader {
    protected Document document;
    protected String dataFile;

    public ParamsXMLReader(String dataFile) throws Exception {
        try {
            this.document = XMLManager.getXML(dataFile, true);
        } catch (Exception exc) {
            throw exc;
        }
    }

    public BaseParam getParam(String name, String value, String defaultValue, String type, String description, String help, String delim, String[] customParams) throws Exception {
        BaseParam param = new BaseParamImpl(name, null, defaultValue, type, description, help, customParams);
        return ParamsFactory.createControl(param, delim);
    }

    public BaseParam createParam(Node paramNode, String[] customParams) throws Exception {
        NamedNodeMap attrList = paramNode.getAttributes();
        String name = attrList.getNamedItem("name").getNodeValue();
        String type = attrList.getNamedItem("type").getNodeValue();
        String defaultValue = "";
        if (attrList.getNamedItem("defaultvalue") != null) {
            defaultValue = attrList.getNamedItem("defaultvalue").getNodeValue();
        }
        String description = "";
        if (attrList.getNamedItem("description") != null) {
            description = attrList.getNamedItem("description").getNodeValue();
        }
        String help = "";
        if (attrList.getNamedItem("help") != null) {
            help = attrList.getNamedItem("help").getNodeValue();
        }
        String delim = null;
        if (attrList.getNamedItem("delim") != null) {
            delim = attrList.getNamedItem("delim").getNodeValue();
            if (delim.length() != 1) {
                delim = null;
            }
        }
        BaseParam param = getParam(name, null, defaultValue, type, description, help, delim, customParams);
        param.setParamsForType(paramNode);
        return param;
    }

    public Document getDocument() {
        return this.document;
    }

    public static String getNodeValue(NodeList entryFields, int index) {
        String nodeValue;
        Node childNode = entryFields.item(index).getFirstChild();
        if (childNode != null) {
            nodeValue = childNode.getNodeValue();
        } else {
            nodeValue = "";
        }
        return nodeValue;
    }

    public static void setNodeValue(Document document, NodeList entryFields, int index, String nodeValue) {
        Node childNode = entryFields.item(index).getFirstChild();
        if (childNode != null) {
            childNode.setNodeValue(nodeValue);
        } else {
            entryFields.item(index).appendChild(document.createTextNode(nodeValue));
        }
    }

    public static void setAttributeValue(Node attrNode, String attrName, String attrValue) throws Exception {
        NamedNodeMap attrList = attrNode.getAttributes();
        Node node = attrList.getNamedItem(attrName);
        if (node == null) {
            throw new Exception("Not found attribute node");
        }
        node.setNodeValue(attrValue);
    }

    public static void setAttributeValue(Node attrNode, String attrName, boolean attrValue) throws Exception {
        NamedNodeMap attrList = attrNode.getAttributes();
        Node node = attrList.getNamedItem(attrName);
        if (node == null) {
            throw new Exception("Not found attribute node");
        }
        node.setNodeValue(new Boolean(attrValue).toString());
    }

    public static String getAttributeValue(Node attrNode, String attrName) throws Exception {
        NamedNodeMap attrList = attrNode.getAttributes();
        Node node = attrList.getNamedItem(attrName);
        if (node == null) {
            throw new Exception("Not found attribute node");
        }
        return node.getNodeValue();
    }

    public boolean isValidRadiogroup(Node radiogroupNode) throws Exception {
        String[] trueValues = {"On", "Yes", "Enabled", "1"};
        int typesValuesCount = 0;
        for (int i = 0; i < trueValues.length; i++) {
            NodeList paramsDefaultValues = XPathAPI.selectNodeList(radiogroupNode, "self::radiogroup/param/@defaultvalue[.=\"" + trueValues[i] + "\"]");
            if (paramsDefaultValues != null) {
                if (paramsDefaultValues.getLength() > 1) {
                    return false;
                }
                if (paramsDefaultValues.getLength() == 1) {
                    typesValuesCount++;
                }
            }
        }
        return typesValuesCount == 1;
    }

    public boolean isValidSelect(Node selectNode) throws Exception {
        String[] trueValues = {"On", "Yes", "Enabled", "1"};
        int typesValuesCount = 0;
        for (int i = 0; i < trueValues.length; i++) {
            NodeList paramsDefaultValues = XPathAPI.selectNodeList(selectNode, "self::select/param/@defaultvalue[.=\"" + trueValues[i] + "\"]");
            if (paramsDefaultValues != null) {
                if (paramsDefaultValues.getLength() > 1) {
                    return false;
                }
                if (paramsDefaultValues.getLength() == 1) {
                    typesValuesCount++;
                }
            }
        }
        return typesValuesCount == 1;
    }

    public RadioGroup getRadioGroupParams(Node radiogroupNode) throws Exception {
        String groupName = radiogroupNode.getAttributes().getNamedItem("name").getNodeValue();
        RadioGroup params = new RadioGroup(groupName);
        NodeList paramsList = ((Element) radiogroupNode).getElementsByTagName("param");
        for (int i = 0; i < paramsList.getLength(); i++) {
            Node paramNode = paramsList.item(i);
            String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
            if (!params.isExistParam(name)) {
                params.addParam(createParam(paramNode, new String[0]));
            }
        }
        return params;
    }

    public SelectParam getSelectParams(Node selectNode) throws Exception {
        String selectName = selectNode.getAttributes().getNamedItem("name").getNodeValue();
        String selectHelp = selectNode.getAttributes().getNamedItem("help").getNodeValue();
        SelectParam selectParam = new SelectParam(selectName, selectHelp);
        NodeList paramsList = ((Element) selectNode).getElementsByTagName("param");
        for (int i = 0; i < paramsList.getLength(); i++) {
            Node paramNode = paramsList.item(i);
            String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
            if (!selectParam.isExistParam(name)) {
                selectParam.addParam(createParam(paramNode, new String[0]));
            }
        }
        return selectParam;
    }

    public RadioGroup getRadioGroupParams(Node radiogroupNode, int groupId) throws Exception {
        String groupName = radiogroupNode.getAttributes().getNamedItem("name").getNodeValue();
        RadioGroup params = new RadioGroup(groupName, new String[]{String.valueOf(groupId)});
        NodeList paramsList = ((Element) radiogroupNode).getElementsByTagName("param");
        for (int i = 0; i < paramsList.getLength(); i++) {
            Node paramNode = paramsList.item(i);
            String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
            if (!params.isExistParam(name)) {
                params.addParam(createParam(paramNode, groupId));
            }
        }
        return params;
    }

    public SelectParam getSelectParams(Node selectNode, int groupId) throws Exception {
        String selectName = selectNode.getAttributes().getNamedItem("name").getNodeValue();
        String selectHelp = selectNode.getAttributes().getNamedItem("help").getNodeValue();
        SelectParam selectParam = new SelectParam(selectName, selectHelp, new String[]{String.valueOf(groupId)});
        NodeList paramsList = ((Element) selectNode).getElementsByTagName("param");
        for (int i = 0; i < paramsList.getLength(); i++) {
            Node paramNode = paramsList.item(i);
            String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
            if (!selectParam.isExistParam(name)) {
                selectParam.addParam(createParam(paramNode, groupId));
            }
        }
        return selectParam;
    }

    public BaseParam createParam(Node paramNode, int groupID) throws Exception {
        NamedNodeMap attrList = paramNode.getAttributes();
        String depends = "";
        if (attrList.getNamedItem("depends") != null) {
            depends = attrList.getNamedItem("depends").getNodeValue();
        }
        return createParam(paramNode, new String[]{String.valueOf(groupID), depends});
    }
}
