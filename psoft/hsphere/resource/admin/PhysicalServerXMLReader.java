package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.admin.params.BaseParam;
import psoft.hsphere.resource.admin.params.Params;
import psoft.hsphere.resource.admin.params.ParamsXMLReader;
import psoft.hsphere.resource.admin.params.SelectParam;
import psoft.util.freemarker.TemplateMap;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/PhysicalServerXMLReader.class */
public class PhysicalServerXMLReader extends ParamsXMLReader {
    public PhysicalServerXMLReader() throws Exception {
        super("PHYSICAL_SERVERS_PARAMS_FILE");
    }

    public Params getPhysicalServerParams(PhysicalServer pserver) throws Exception {
        Params params = new Params();
        List boxGroupIds = pserver.getUniqueTypes();
        HashMap groupsToId = HostEntry.getGroupTypeToIdHash();
        ArrayList addedRadiogroups = new ArrayList();
        ArrayList addedSelects = new ArrayList();
        NodeList groupsList = XPathAPI.selectNodeList(this.document.getDocumentElement(), "//params/group");
        for (int i = 0; i < groupsList.getLength(); i++) {
            Node groupNode = groupsList.item(i);
            String curXMLGroupName = groupNode.getAttributes().getNamedItem("name").getNodeValue();
            String curXMLGroupId = (String) groupsToId.get(curXMLGroupName);
            if (boxGroupIds.contains(curXMLGroupId)) {
                NodeList paramsList = XPathAPI.selectNodeList((Element) groupNode, "*");
                for (int j = 0; j < paramsList.getLength(); j++) {
                    Node paramNode = paramsList.item(j);
                    String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
                    if (paramNode.getNodeName() != null) {
                        if (paramNode.getNodeName().equalsIgnoreCase("param")) {
                            if (!params.isExistParam(name)) {
                                params.add(createParam(paramNode, Integer.parseInt(curXMLGroupId)));
                            }
                        } else if (paramNode.getNodeName().equalsIgnoreCase("select")) {
                            int index = Arrays.binarySearch(addedSelects.toArray(), name);
                            if (index < 0) {
                                if (isValidSelect(paramNode)) {
                                    addedSelects.add(name);
                                    SelectParam pars = getSelectParams(paramNode, Integer.parseInt(curXMLGroupId));
                                    if (pars != null) {
                                        params.add(pars);
                                    }
                                } else {
                                    Session.getLog().error("Params group - " + name + " has more than one or none checked items\n");
                                }
                            }
                        } else if (paramNode.getNodeName().equalsIgnoreCase("radiogroup")) {
                            int index2 = Arrays.binarySearch(addedRadiogroups.toArray(), name);
                            if (index2 < 0) {
                                if (isValidRadiogroup(paramNode)) {
                                    addedRadiogroups.add(name);
                                    BaseParam pars2 = getRadioGroupParams(paramNode, Integer.parseInt(curXMLGroupId));
                                    if (pars2 != null) {
                                        params.add(pars2);
                                    }
                                } else {
                                    Session.getLog().error("Params group - " + name + " has more than one or none checked items\n");
                                }
                            }
                        } else {
                            Session.getLog().error("Check XML file. It should contains such parent node: param, select or radiogroup\n");
                        }
                    }
                }
            }
        }
        return params;
    }

    public int getGroupIDForName(Node nameNode) {
        if (nameNode == null) {
            return -1;
        }
        TemplateMap map = HostEntry.getGroupTypeToId();
        TemplateModel mod = map.get(nameNode.getNodeValue());
        return Integer.parseInt(mod.toString());
    }

    public int getGroupIDValue(Node groupNode) throws Exception {
        int groupID = -1;
        if (groupNode != null) {
            try {
                groupID = Integer.parseInt(groupNode.getNodeValue());
            } catch (Exception e) {
                throw new Exception("Incorrect groupid attribute value in xml file with physical servers params");
            }
        }
        return groupID;
    }
}
