package psoft.hsphere.tools;

import org.apache.axis.message.MessageElement;
import org.w3c.dom.Element;
import psoft.hsphere.C0004CP;
import psoft.hsphere.resource.mpf.MPFManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/HePlanExctractor.class */
public class HePlanExctractor extends C0004CP {
    public void processPE() throws Exception {
        MPFManager manager = MPFManager.getManager();
        try {
            MessageElement data = new MessageElement("", "data");
            MessageElement tmp = new MessageElement("", "preferredDomainController");
            tmp.addTextNode(manager.getPdc());
            data.addChild(tmp);
            MessageElement tmp2 = new MessageElement("", "sendCredentials");
            tmp2.addTextNode("true");
            data.addChild(tmp2);
            MPFManager.Result result = manager.executeMPSRequest("Windows-based Hosting", "QueryPlans", data);
            if (!result.isSuccess()) {
                throw new Exception(result.getError());
            }
            MessageElement[] xmldata = result.getMessageElements();
            String resps = "<result>\n" + xmldata[0].getAsString();
            int n = xmldata[0].getAsDocument().getElementsByTagName("planName").getLength();
            for (int i = 0; i < n; i++) {
                Element el = (Element) xmldata[0].getAsDocument().getElementsByTagName("planName").item(i);
                resps = resps + "\n" + manager.getHES().getPlanDetail(el.getFirstChild().getNodeValue(), manager.getPdc(), "", true);
            }
            System.out.println(resps + "\n</result>");
        } catch (Exception ex) {
            System.out.println("Error :" + ex.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        if (0 < args.length) {
            System.out.println("\nReturns an XML output with details on all MS Exchange Plans available on the Hosted Exchange server\n\nUSAGE:\njava psoft.hsphere.tools.HePlanExctractor\njava psoft.hsphere.tools.HePlanExctractor >> FileName.xml 2>&1\n");
        } else {
            HePlanExctractor pe = new HePlanExctractor();
            pe.processPE();
        }
        System.exit(0);
    }
}
