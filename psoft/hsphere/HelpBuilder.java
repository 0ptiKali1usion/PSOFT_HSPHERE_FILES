package psoft.hsphere;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/HelpBuilder.class */
public class HelpBuilder {
    public static TemplateHash getHelp(String cfgStr) {
        TemplateHash help = new TemplateHash();
        try {
            Document doc = XMLManager.getXML(cfgStr);
            Element root = doc.getDocumentElement();
            NodeList topics = root.getElementsByTagName("topic");
            for (int i = 0; i < topics.getLength(); i++) {
                Element currTopic = (Element) topics.item(i);
                TemplateHash topic = new TemplateHash();
                topic.put("title", currTopic.getAttributes().getNamedItem("name").getNodeValue());
                topic.put("tip", currTopic.getAttributes().getNamedItem("tip").getNodeValue());
                try {
                    topic.put("full", currTopic.getAttributes().getNamedItem("fulltxt").getNodeValue());
                } catch (Exception e) {
                    topic.put("full", new TemplateString(""));
                }
                NodeList seeAlso = currTopic.getElementsByTagName("seealso");
                TemplateList also = new TemplateList();
                for (int j = 0; j < seeAlso.getLength(); j++) {
                    also.add(((Element) seeAlso.item(j)).getAttributes().getNamedItem("id").getNodeValue());
                }
                topic.put("seealso", also);
                help.put(currTopic.getAttributes().getNamedItem("id").getNodeValue(), topic);
            }
        } catch (Exception ex) {
            Session.getLog().error("Error during help building ", ex);
        }
        return help;
    }

    public static TemplateHash getOnlineHelp(String cfgStr) {
        TemplateHash online_help = new TemplateHash();
        try {
            Document doc = XMLManager.getXML(cfgStr);
            Element root = doc.getDocumentElement();
            String base_dir = root.getAttribute("base_dir");
            String file_ext = root.getAttribute("file_ext");
            online_help.put("file_ext", file_ext);
            NodeList references = root.getElementsByTagName("reference");
            for (int i = 0; i < references.getLength(); i++) {
                Element curReference = (Element) references.item(i);
                online_help.put(curReference.getAttributes().getNamedItem("id").getNodeValue(), base_dir + curReference.getAttributes().getNamedItem("file").getNodeValue());
            }
        } catch (Exception ex) {
            Session.getLog().error("Error getting online help. ", ex);
        }
        return online_help;
    }
}
