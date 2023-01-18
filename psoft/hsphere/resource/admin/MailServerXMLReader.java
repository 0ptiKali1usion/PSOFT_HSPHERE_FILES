package psoft.hsphere.resource.admin;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.params.BaseParam;
import psoft.hsphere.resource.admin.params.Params;
import psoft.hsphere.resource.admin.params.ParamsXMLReader;
import psoft.hsphere.resource.admin.params.SelectParam;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MailServerXMLReader.class */
public class MailServerXMLReader extends ParamsXMLReader {
    public MailServerXMLReader() throws Exception {
        super("MAIL_SERVERS_PARAMS_FILE");
    }

    public Params getMailServerParams() throws Exception {
        Params params = new Params();
        ArrayList addedRadiogroups = new ArrayList();
        ArrayList addedSelects = new ArrayList();
        NodeList paramsList = XPathAPI.selectNodeList(this.document.getDocumentElement(), "//params/*");
        for (int i = 0; i < paramsList.getLength(); i++) {
            Node paramNode = paramsList.item(i);
            String name = paramNode.getAttributes().getNamedItem("name").getNodeValue();
            if (paramNode.getLocalName().equalsIgnoreCase("param")) {
                if (!params.isExistParam(name)) {
                    params.add(createParam(paramNode));
                }
            } else if (paramNode.getLocalName().equalsIgnoreCase("select")) {
                int index = Arrays.binarySearch(addedSelects.toArray(), name);
                if (index < 0) {
                    if (isValidSelect(paramNode)) {
                        addedSelects.add(name);
                        SelectParam pars = getSelectParams(paramNode);
                        if (pars != null) {
                            params.add(pars);
                        }
                    } else {
                        Session.getLog().error("Qmail params : Params group - " + name + " has more than one or none checked items\n");
                    }
                }
            } else if (paramNode.getLocalName().equalsIgnoreCase("radiogroup")) {
                int index2 = Arrays.binarySearch(addedRadiogroups.toArray(), name);
                if (index2 < 0) {
                    if (isValidRadiogroup(paramNode)) {
                        addedRadiogroups.add(name);
                        BaseParam pars2 = getRadioGroupParams(paramNode);
                        if (pars2 != null) {
                            params.add(pars2);
                        }
                    } else {
                        Session.getLog().error("Qmail params : Params group - " + name + " has more than one or none checked items\n");
                    }
                }
            } else {
                Session.getLog().error("Check XML file. It should contains such parent node: param, select or radiogroup\n");
            }
        }
        return params;
    }

    protected BaseParam createParam(Node paramNode) throws Exception {
        return super.createParam(paramNode, new String[0]);
    }
}
