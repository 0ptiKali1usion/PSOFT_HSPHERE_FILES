package psoft.hsphere.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.resource.HostEntry;
import psoft.util.xml.DOMLoader;

/* loaded from: hsphere.zip:psoft/hsphere/tools/CronConfigUpdater.class */
public class CronConfigUpdater {
    private static final String configFile = "psoft_config.hsphere";
    private static ResourceBundle config = PropertyResourceBundle.getBundle(configFile);

    public void update(File standartXML, File mergeWithXML, boolean needToCreateXML) {
        Document standartDoc = null;
        Document mergeWithDoc = null;
        try {
            if (!needToCreateXML) {
                DOMParser parser = new DOMParser();
                FileInputStream stream = new FileInputStream(standartXML);
                parser.parse(new InputSource(stream));
                standartDoc = parser.getDocument();
            } else {
                standartDoc = getStandartDoc();
            }
        } catch (IOException e) {
            System.out.println("An IO exception occured during the XML file access " + mergeWithXML.getPath());
            System.exit(-1);
        } catch (SAXException saxex) {
            System.out.println("An exception occured during the XML file analysis " + mergeWithXML.getPath());
            saxex.printStackTrace();
            System.exit(-1);
        }
        try {
            mergeWithDoc = DOMLoader.getXML(mergeWithXML.getPath());
        } catch (IOException e2) {
            System.out.println("An IO exception occured during the XML file access " + mergeWithXML.getPath());
            System.exit(-1);
        } catch (SAXException e3) {
            System.out.println("An exception occured during the  XML file analysis " + mergeWithXML.getPath());
            System.exit(-1);
        }
        Document docToSave = (Document) merge(standartDoc, mergeWithDoc);
        if (needToCreateXML) {
            try {
                standartXML.createNewFile();
            } catch (IOException e4) {
                System.out.println("cron_config.xml creation failed");
                System.exit(-1);
            }
        }
        OutputFormat format = new OutputFormat(docToSave);
        format.setIndenting(true);
        format.setIndent(2);
        format.setLineWidth((int) SignupGuard.CVV_VALIDATION);
        try {
            FileOutputStream out = new FileOutputStream(standartXML);
            XMLSerializer s = new XMLSerializer(out, format);
            s.serialize(docToSave);
            out.close();
        } catch (IOException e5) {
            System.out.println("cron.config.xml creation failed");
            System.exit(-1);
        }
    }

    private Node merge(Document stand, Document updater) {
        Document result = null;
        try {
            result = (Document) stand.cloneNode(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NodeList updaterList = updater.getElementsByTagName("cron");
        NodeList standartList = result.getElementsByTagName("cron");
        for (int i = 0; i < updaterList.getLength(); i++) {
            Node updaterNode = updaterList.item(i).cloneNode(true);
            if (updaterNode.getAttributes().getLength() != 0) {
                boolean addNode = true;
                int j = 0;
                while (true) {
                    if (j >= standartList.getLength()) {
                        break;
                    }
                    Node currentNode = standartList.item(j);
                    if (currentNode.getAttributes().getLength() != 0) {
                        try {
                            if (currentNode.getAttributes().getNamedItem("name").getNodeValue().equals(updaterNode.getAttributes().getNamedItem("name").getNodeValue())) {
                                result.getElementsByTagName("crons").item(0).removeChild(currentNode);
                                Element nodeToAdd = result.createElement("cron");
                                NamedNodeMap newAttributes = updaterNode.getAttributes();
                                for (int k = 0; k < newAttributes.getLength(); k++) {
                                    Attr attribute = result.createAttribute(newAttributes.item(k).getNodeName());
                                    attribute.setValue(newAttributes.item(k).getNodeValue());
                                    nodeToAdd.setAttributeNode(attribute);
                                }
                                result.getElementsByTagName("crons").item(0).appendChild(nodeToAdd);
                                addNode = false;
                            }
                        } catch (Exception e2) {
                        }
                    }
                    j++;
                }
                if (addNode) {
                    Element nodeToAdd2 = result.createElement("cron");
                    NamedNodeMap newAttributes2 = updaterNode.getAttributes();
                    for (int k2 = 0; k2 < newAttributes2.getLength(); k2++) {
                        Attr attribute2 = result.createAttribute(newAttributes2.item(k2).getNodeName());
                        attribute2.setValue(newAttributes2.item(k2).getNodeValue());
                        nodeToAdd2.setAttributeNode(attribute2);
                    }
                    result.getElementsByTagName("crons").item(0).appendChild(nodeToAdd2);
                }
            }
        }
        return result;
    }

    private Document getStandartDoc() throws SAXException, IOException {
        DOMParser parser = new DOMParser();
        new String("");
        String accounting = new String("");
        String reseller = new String("");
        String trial = new String("");
        String overlimit = new String("");
        new String("");
        String externalcharge = new String("");
        String failedsignup = new String("");
        String contentmoving = new String("");
        String vps = new String("");
        new String("");
        try {
            String tmp = config.getString("LAUNCH_PERIOD");
            accounting = new String("<cron name=\"accounting\" period=\"" + tmp + "\" class=\"psoft.hsphere.cron.Accounting\" disabled=\"0\"/>");
        } catch (MissingResourceException e) {
        }
        try {
            String tmp2 = config.getString("RESELLER_PERIOD");
            reseller = new String("<cron name=\"resellerCron\" period=\"" + tmp2 + "\" class=\"psoft.hsphere.cron.ResellerCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e2) {
        }
        try {
            String tmp3 = config.getString("TRIAL_PERIOD");
            trial = new String("<cron name=\"trialCron\" period=\"" + tmp3 + "\" class=\"psoft.hsphere.cron.TrialCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e3) {
        }
        try {
            String tmp4 = config.getString("OVERLIMIT_PERIOD");
            overlimit = new String("<cron name=\"overlimitCron\" period=\"" + tmp4 + "\" class=\"psoft.hsphere.cron.OverLimitWarnCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e4) {
        }
        try {
            String tmp5 = config.getString("EC_CRON_PERIOD");
            externalcharge = new String("<cron name=\"ecCron\" period=\"" + tmp5 + "\" class=\"psoft.hsphere.cron.ExternalChargeCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e5) {
        }
        try {
            String tmp6 = config.getString("FSIGNUPS_PERIOD");
            failedsignup = new String("<cron name=\"fSignupsCron\" period=\"" + tmp6 + "\" class=\"psoft.hsphere.cron.FailedSignupsCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e6) {
        }
        try {
            String tmp7 = config.getString("MOVING_CRON_PERIOD");
            contentmoving = new String("<cron name=\"contentMovingCron\" period=\"" + tmp7 + "\" class=\"psoft.hsphere.cron.ContentMovingCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e7) {
        }
        try {
            String tmp8 = config.getString("VPS_CRON_PERIOD");
            vps = new String("<cron name=\"vpsCron\" period=\"" + tmp8 + "\" class=\"psoft.hsphere.cron.VPSCron\" disabled=\"0\"/>");
        } catch (MissingResourceException e8) {
        }
        String revenue = new String("<cron name=\"revenueCron\" period=\"1440\" class=\"psoft.hsphere.cron.RevenueCron\" disabled=\"0\"/>");
        String ttautoclose = new String("<cron name=\"ttAutoCloseCron\" period=\"1440\" class=\"psoft.hsphere.cron.TTAutoCloseCron\" disabled=\"0\"/>");
        String newXMLDoc = new String("<crons>" + accounting + reseller + trial + overlimit + revenue + externalcharge + failedsignup + contentmoving + vps + ttautoclose + "</crons>");
        ByteArrayInputStream stream = new ByteArrayInputStream(newXMLDoc.getBytes());
        parser.parse(new InputSource(stream));
        Document result = parser.getDocument();
        return result;
    }

    public static void main(String[] args) {
        String filePath;
        File xmlFile;
        boolean needToCreateXML = false;
        CronConfigUpdater updater = new CronConfigUpdater();
        if (args.length < 2) {
            printHelp();
            System.exit(-1);
        }
        if (!args[0].equals("-f") && !args[0].equals("--file")) {
            printHelp();
            System.exit(-1);
        }
        String pathToMergeWith = new String(args[1]);
        File xmlToMergeWith = new File(pathToMergeWith);
        if (!xmlToMergeWith.exists()) {
            System.out.println("XML file " + pathToMergeWith + " does not exist");
            System.exit(-1);
        }
        try {
            filePath = config.getString("CRON_CONFIG");
        } catch (MissingResourceException e) {
            filePath = null;
        }
        if (filePath == null) {
            String separator = new String(File.separator);
            File tempPath = new File(separator + "hsphere" + separator + "local" + separator + HostEntry.UnixUserDefaultHomeDir + separator + "cpanel" + separator + "shiva" + separator + "psoft_config");
            if (!tempPath.exists()) {
                System.out.println("Directory '" + tempPath.getPath() + "' does not exist");
                System.exit(-1);
            }
            String filePath2 = tempPath.getPath();
            xmlFile = new File(filePath2 + separator + "cron_config.xml");
        } else {
            xmlFile = new File(filePath);
        }
        if (!xmlFile.exists()) {
            needToCreateXML = true;
        }
        updater.update(xmlFile, xmlToMergeWith, needToCreateXML);
    }

    public static void printHelp() {
        System.out.println("H-Sphere cron config updater.");
        System.out.println("Usage java psoft.hsphere.tools.CronConfigUpdater");
        System.out.println("   -f|--file <XML file to merge with>");
    }
}
