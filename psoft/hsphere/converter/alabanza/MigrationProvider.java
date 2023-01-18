package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/MigrationProvider.class */
public class MigrationProvider {
    private static NodeList table_tags;
    private static NodeList tr_tags;
    private static NodeList td_tags;
    private static NodeList p_tags;
    private static NodeList a_tags;
    private static NodeList font_tags;
    private static NodeList input_tags;
    private static NodeList textarea_tags;
    private static Node table_tag;
    private static Node tr_tag;
    private static Node td_tag;
    private static Node p_tag;
    private static Node a_tag;
    private static Node font_tag;
    private static Node b_tag;
    private static Node a_href;
    private static Node child;
    private static Element root;
    private static Element body;
    private static Element center;
    private static Element table;

    /* renamed from: tr */
    private static Element f81tr;

    /* renamed from: td */
    private static Element f82td;
    private static Document doc;
    private static DOMParser parser;
    private static Tidy tidy;

    public static List getClientURLList(InputStream input) {
        List clientURLList = new LinkedList();
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            table_tags = body.getElementsByTagName("table");
            for (int table_i = 0; table_i < table_tags.getLength(); table_i++) {
                tr_tags = ((Element) table_tags.item(table_i)).getElementsByTagName("tr");
                for (int tr_i = 0; tr_i < tr_tags.getLength(); tr_i++) {
                    td_tags = ((Element) tr_tags.item(tr_i)).getElementsByTagName("td");
                    for (int td_i = 0; td_i < td_tags.getLength(); td_i++) {
                        a_tags = ((Element) td_tags.item(td_i)).getElementsByTagName("a");
                        for (int a_i = 0; a_i < a_tags.getLength(); a_i++) {
                            a_href = a_tags.item(a_i).getAttributes().getNamedItem("href");
                            if (a_href != null) {
                                String href = a_href.getNodeValue().toString();
                                System.out.println("href = " + href);
                                if (href.indexOf("/apps/viewClient.html") != -1) {
                                    clientURLList.add(nbspClear(href.substring(2)));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error getting a list of clients !");
            ex.printStackTrace();
        }
        return clientURLList;
    }

    public static Hashtable getClientDomains(InputStream input) {
        String nodeValue;
        String nodeValue2;
        Hashtable clientDomains = new Hashtable();
        LinkedList linkedList = new LinkedList();
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            table_tags = body.getElementsByTagName("table");
            tr_tags = ((Element) table_tags.item(0)).getElementsByTagName("tr");
            tr_tag = (Element) tr_tags.item(1);
            if (tr_tag != null) {
                td_tag = ((Element) tr_tag).getElementsByTagName("td").item(0);
                if (td_tag != null) {
                    font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                    if (font_tag != null) {
                        NodeList children = font_tag.getChildNodes();
                        int childLen = children.getLength();
                        for (int i = 0; i < childLen; i++) {
                            child = children.item(i);
                            if (child != null) {
                                String nodeValue3 = child.getNodeValue().trim();
                                if ("Status:".equals(nodeValue3) && i < childLen - 1) {
                                    b_tag = ((Element) children.item(i + 1)).getElementsByTagName("b").item(0);
                                    if (b_tag != null) {
                                        font_tag = ((Element) b_tag).getElementsByTagName("font").item(0);
                                        if (font_tag != null && "Suspended".equals(font_tag.getFirstChild().getNodeValue().trim())) {
                                            clientDomains.put("suspended", "1");
                                        }
                                    }
                                } else if ("Balance:".equals(nodeValue3) && i < childLen - 1) {
                                    b_tag = ((Element) children.item(i + 1)).getElementsByTagName("b").item(0);
                                    if (b_tag != null && (nodeValue2 = b_tag.getFirstChild().getNodeValue().trim()) != null) {
                                        clientDomains.put("balance", nodeValue2);
                                    }
                                } else if ("Payment Method:".equals(nodeValue3) && i < childLen - 1) {
                                    b_tag = ((Element) children.item(i + 1)).getElementsByTagName("b").item(0);
                                    if (b_tag != null && (nodeValue = b_tag.getFirstChild().getNodeValue().trim()) != null) {
                                        clientDomains.put("payment_method", nodeValue);
                                    }
                                }
                            }
                        }
                    }
                }
                td_tag = ((Element) tr_tag).getElementsByTagName("td").item(1);
                if (td_tag != null) {
                    p_tag = ((Element) td_tag).getElementsByTagName("p").item(0);
                    if (p_tag != null) {
                        font_tag = ((Element) p_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            a_tag = ((Element) font_tag).getElementsByTagName("a").item(0);
                            if (a_tag != null) {
                                a_href = a_tag.getAttributes().getNamedItem("href");
                                if (a_href != null) {
                                    String href = a_href.getNodeValue().toString();
                                    if (href.indexOf("/apps/editClient.html") != -1) {
                                        clientDomains.put("clientURL", nbspClear(href.substring(3)));
                                    }
                                }
                            }
                        }
                    }
                    p_tag = ((Element) td_tag).getElementsByTagName("p").item(1);
                    if (p_tag != null) {
                        font_tag = ((Element) p_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            a_tag = ((Element) font_tag).getElementsByTagName("a").item(0);
                            if (a_tag != null) {
                                a_href = a_tag.getAttributes().getNamedItem("href");
                                if (a_href != null) {
                                    String href2 = a_href.getNodeValue().toString();
                                    if (href2.indexOf("/apps/viewAccountStatus.html") != -1) {
                                        clientDomains.put("billingHistoryURL", nbspClear(href2.substring(2)));
                                    }
                                }
                            }
                        }
                    }
                    p_tag = ((Element) td_tag).getElementsByTagName("p").item(3);
                    if (p_tag != null) {
                        font_tag = ((Element) p_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            a_tag = ((Element) font_tag).getElementsByTagName("a").item(0);
                            if (a_tag != null) {
                                a_href = a_tag.getAttributes().getNamedItem("href");
                                if (a_href != null) {
                                    String href3 = a_href.getNodeValue().toString();
                                    if (href3.indexOf("/apps/viewCCNumber.html") != -1) {
                                        clientDomains.put("viewCCURL", nbspClear(href3.substring(2)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Hashtable domainInfo = new Hashtable();
            table_tag = table_tags.item(1);
            if (table_tag != null) {
                tr_tags = ((Element) table_tag).getElementsByTagName("tr");
                tr_tag = tr_tags.item(1);
                if (tr_tag != null) {
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(0);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        a_tags = ((Element) font_tag).getElementsByTagName("a");
                        for (int tag_i = 0; tag_i < a_tags.getLength(); tag_i++) {
                            a_tag = (Element) a_tags.item(tag_i);
                            if (a_tag != null) {
                                a_href = a_tag.getAttributes().getNamedItem("href");
                                String href4 = a_href.getNodeValue().toString();
                                switch (tag_i) {
                                    case 0:
                                        continue;
                                    case 1:
                                        domainInfo.put("controlPanel", nbspClear(href4));
                                        continue;
                                    case 2:
                                        if (href4.indexOf("/apps/addDomainPackage.html?exDomainName=") == -1) {
                                            break;
                                        } else {
                                            int from = href4.indexOf("=");
                                            String domain = nbspClear(href4.substring(from + 1));
                                            domainInfo.put("domainName", nbspClear(href4.substring(from + 1)));
                                            System.out.println("Domain name = " + domain);
                                            continue;
                                        }
                                }
                            }
                        }
                    }
                }
                tr_tag = tr_tags.item(2);
                if (tr_tag != null) {
                    td_tags = ((Element) tr_tag).getElementsByTagName("td");
                    td_tag = td_tags.item(0);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            a_tag = ((Element) tr_tag).getElementsByTagName("a").item(0);
                            if (a_tag != null) {
                                String nodeValue4 = a_tag.getFirstChild().getNodeValue().trim();
                                if (!"".equals(nodeValue4)) {
                                    domainInfo.put("IP", nbspClear(nodeValue4));
                                }
                            }
                        }
                    }
                    td_tag = td_tags.item(2);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue5 = font_tag.getFirstChild().getNodeValue().trim();
                            if (!"".equals(nodeValue5)) {
                                domainInfo.put("login", nbspClear(nodeValue5));
                            }
                        }
                    }
                    td_tag = td_tags.item(3);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue6 = font_tag.getFirstChild().getNodeValue().trim();
                            if (!"".equals(nodeValue6)) {
                                domainInfo.put("password", nbspClear(nodeValue6));
                            }
                        }
                    }
                }
                tr_tags = ((Element) table_tags.item(2)).getElementsByTagName("tr");
                tr_tag = tr_tags.item(2);
                if (tr_tag != null) {
                    td_tags = ((Element) tr_tag).getElementsByTagName("td");
                    td_tag = td_tags.item(0);
                    if (td_tag != null) {
                        font_tags = ((Element) td_tag).getElementsByTagName("font");
                        a_tags = ((Element) font_tags.item(0)).getElementsByTagName("a");
                        a_href = a_tags.item(0).getAttributes().getNamedItem("href");
                        if (a_href != null) {
                            String href5 = a_href.getNodeValue().toString();
                            if (href5.indexOf("/apps/editPackage.html") != -1) {
                                domainInfo.put("editPackage", nbspClear(href5).substring(3));
                                String nodeValue7 = ((Element) a_tags.item(0)).getFirstChild().getNodeValue().trim();
                                if (!"".equals(nodeValue7)) {
                                    if (0 == 0) {
                                        clientDomains.put("planName", nbspClear(nodeValue7));
                                    }
                                    domainInfo.put("planName", nbspClear(nodeValue7));
                                    td_tag = td_tags.item(1);
                                    if (td_tag != null) {
                                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                                        if (font_tag != null) {
                                            String nodeValue8 = nbspClear(((Element) font_tag).getFirstChild().getNodeValue().trim());
                                            if (0 == 0) {
                                                clientDomains.put("billingPeriod", nodeValue8);
                                            }
                                            domainInfo.put("billingPeriod", nbspClear(nodeValue8));
                                        }
                                    }
                                    td_tag = td_tags.item(3);
                                    if (td_tag != null) {
                                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                                        if (font_tag != null) {
                                            String nodeValue9 = nbspClear(((Element) font_tag).getFirstChild().getNodeValue().trim());
                                            if (!"".equals(nodeValue9)) {
                                                if (0 == 0) {
                                                    clientDomains.put("startDate", nodeValue9);
                                                }
                                                domainInfo.put("startDate", nodeValue9);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!domainInfo.isEmpty()) {
                linkedList.add(domainInfo);
            }
            clientDomains.put("domainList", linkedList);
        } catch (Exception ex) {
            System.err.println("Error getting the client domain info !");
            ex.printStackTrace();
        }
        return clientDomains;
    }

    public static List getBillingHistory(InputStream input) {
        List billingHistory = new LinkedList();
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            table_tag = body.getElementsByTagName("table").item(3);
            tr_tags = ((Element) table_tag).getElementsByTagName("tr");
            for (int tr_i = 1; tr_i < tr_tags.getLength(); tr_i++) {
                tr_tag = (Element) tr_tags.item(tr_i);
                if (tr_tag != null) {
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(0);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            a_tag = ((Element) font_tag).getElementsByTagName("a").item(0);
                            if (a_tag != null) {
                                a_href = a_tag.getAttributes().getNamedItem("href");
                                if (a_href != null) {
                                    String href = a_href.getNodeValue().toString();
                                    if (!href.startsWith("../apps/viewTransactionDetails.html")) {
                                    }
                                }
                            }
                        }
                    }
                    Hashtable domainInfo = new Hashtable();
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(1);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Domain", nbspClear(nodeValue));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(2);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue2 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Package", nbspClear(nodeValue2));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(3);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue3 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Type", nbspClear(nodeValue3));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(4);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue4 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Date", nbspClear(nodeValue4));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(5);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue5 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Notes", nbspClear(nodeValue5));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(6);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue6 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("StartDate", nbspClear(nodeValue6));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(7);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue7 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("UnitPrice", nbspClear(nodeValue7));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(8);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue8 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Length", nbspClear(nodeValue8));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(9);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            String nodeValue9 = font_tag.getFirstChild().getNodeValue().trim();
                            domainInfo.put("Discount", nbspClear(nodeValue9));
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(10);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            NodeList tmpList = font_tag.getChildNodes();
                            int size = tmpList.getLength();
                            for (int i = 0; i < size; i++) {
                                Node tmpEl = tmpList.item(i);
                                if (tmpEl.getNodeType() == 3) {
                                    String nodeValue10 = tmpEl.getNodeValue().trim();
                                    String w = nbspClear(nodeValue10);
                                    if (w != null && !"".equals(w)) {
                                        domainInfo.put("Credit", w);
                                    }
                                }
                            }
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(11);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            NodeList tmpList2 = font_tag.getChildNodes();
                            int size2 = tmpList2.getLength();
                            for (int i2 = 0; i2 < size2; i2++) {
                                Node tmpEl2 = tmpList2.item(i2);
                                if (tmpEl2.getNodeType() == 3) {
                                    String nodeValue11 = tmpEl2.getNodeValue().trim();
                                    String w2 = nbspClear(nodeValue11);
                                    if (w2 != null && !"".equals(w2)) {
                                        if (!"0.00".equals(w2) && !"-".equals(w2)) {
                                            w2 = "-" + w2;
                                        }
                                        domainInfo.put("Debit", w2);
                                    }
                                }
                            }
                        }
                    }
                    td_tag = ((Element) tr_tag).getElementsByTagName("td").item(12);
                    if (td_tag != null) {
                        font_tag = ((Element) td_tag).getElementsByTagName("font").item(0);
                        if (font_tag != null) {
                            Element cr_font = (Element) ((Element) font_tag).getElementsByTagName("font").item(1);
                            boolean soom_sign = true;
                            if (cr_font != null) {
                                String cr_color = cr_font.getAttribute("color");
                                String cr_value = cr_font.getFirstChild().getNodeValue().trim();
                                if ("Green".equals(cr_color) && "cr".equals(cr_value)) {
                                    soom_sign = false;
                                } else {
                                    soom_sign = true;
                                }
                            }
                            NodeList tmpList3 = font_tag.getChildNodes();
                            int size3 = tmpList3.getLength();
                            for (int i3 = 0; i3 < size3; i3++) {
                                Node tmpEl3 = tmpList3.item(i3);
                                if (tmpEl3.getNodeType() == 3) {
                                    String nodeValue12 = tmpEl3.getNodeValue().trim();
                                    String w3 = nbspClear(nodeValue12);
                                    if (w3 != null && !"".equals(w3)) {
                                        if (soom_sign && !"0.00".equals(w3)) {
                                            w3 = "-" + w3;
                                        }
                                        domainInfo.put("Balance", w3);
                                    }
                                }
                            }
                        }
                    }
                    if (!domainInfo.isEmpty()) {
                        billingHistory.add(domainInfo);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error getting Billing History info !");
            ex.printStackTrace();
        }
        return billingHistory;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static Hashtable getClientInfo(InputStream input) {
        Hashtable clientInfo = new Hashtable();
        String[] strArr = {new String[]{"AF", "Afghanistan"}, new String[]{"AL", "Albania"}, new String[]{"DZ", "Algeria"}, new String[]{"DS", "American Samoa"}, new String[]{"AD", "Andorra"}, new String[]{"AO", "Angola"}, new String[]{"AI", "Anguilla"}, new String[]{"AQ", "Antarctica"}, new String[]{"AG", "Antigua and Barbuda"}, new String[]{"AR", "Argentina"}, new String[]{"AM", "Armenia"}, new String[]{"AW", "Aruba"}, new String[]{"AU", "Australia"}, new String[]{"AT", "Austria"}, new String[]{"AZ", "Azerbaijan"}, new String[]{"BS", "Bahamas"}, new String[]{"BH", "Bahrain"}, new String[]{"BD", "Banglades"}, new String[]{"BB", "Barbados"}, new String[]{"BY", "Belarus"}, new String[]{"BE", "Belgium"}, new String[]{"BZ", "Belize"}, new String[]{"BJ", "Benin"}, new String[]{"BM", "Bermuda"}, new String[]{"BT", "Buthan"}, new String[]{"BO", "Bolivia"}, new String[]{"BA", "Bosnia-Herzegovina"}, new String[]{"BW", "Botswana"}, new String[]{"BV", "Bouvet Island"}, new String[]{"BR", "Brazil"}, new String[]{"IO", "British lndian O. Terr."}, new String[]{"BN", "Brunei Darussalam"}, new String[]{"BG", "Bulgaria"}, new String[]{"BF", "Burkina Faso"}, new String[]{"BI", "Burundi"}, new String[]{"CA", "Canada"}, new String[]{"KH", "Cambodia"}, new String[]{"CM", "Cameroon"}, new String[]{"CV", "Cape Verde"}, new String[]{"KY", "Cayman Islands"}, new String[]{"CF", "Central African Rep."}, new String[]{"TD", "Chad"}, new String[]{"CL", "Chile"}, new String[]{"CN", "China"}, new String[]{"CX", "Christmas Island"}, new String[]{"CC", "Cocos (Keeling) Isl."}, new String[]{"CO", "Colombia"}, new String[]{"KM", "Comoros"}, new String[]{"CG", "Congo"}, new String[]{"CK", "Cook Islands"}, new String[]{"CR", "Costa Rica"}, new String[]{"HR", "Croatia"}, new String[]{"CU", "Cuba"}, new String[]{"CY", "Cyprus"}, new String[]{"CZ", "Czech Republic"}, new String[]{"DK", "Denmark"}, new String[]{"DJ", "Djibouti"}, new String[]{"DM", "Dominica"}, new String[]{"DO", "Dominican Republic"}, new String[]{"TP", "East Timor"}, new String[]{"EC", "Ecudaor"}, new String[]{"EG", "Egypt"}, new String[]{"SV", "El Salvador"}, new String[]{"GQ", "Equatorial Guinea"}, new String[]{"ER", "Eritrea"}, new String[]{"EE", "Estonia"}, new String[]{"ET", "Ethiopia"}, new String[]{"FK", "Falkland Isl.(Malvinas)"}, new String[]{"FO", "Faroe Islands"}, new String[]{"FJ", "Fiji"}, new String[]{"FI", "Finland"}, new String[]{"FR", "France"}, new String[]{"FX", "France (European Ter.)"}, new String[]{"FX", "France, Metropolitan"}, new String[]{"GF", "French Guiana"}, new String[]{"PF", "Polynesia (Fr.)"}, new String[]{"TF", "French Southern Terr."}, new String[]{"GA", "Gabon"}, new String[]{"GM", "Gambia"}, new String[]{"GE", "Georgia"}, new String[]{"DE", "Germany"}, new String[]{"GH", "Ghana"}, new String[]{"GI", "Gibraltar"}, new String[]{"UK", "Great Britain (UK)"}, new String[]{"GR", "Greece"}, new String[]{"GL", "Greenland"}, new String[]{"GD", "Grenada"}, new String[]{"GP", "Guadeloupe"}, new String[]{"GU", "Guam"}, new String[]{"GT", "Guatemala"}, new String[]{"GN", "Guinea"}, new String[]{"GW", "Guinea-Bissau"}, new String[]{"GY", "Guyana"}, new String[]{"HT", "Haiti"}, new String[]{"HM", "Heard & Mc Donald Isl."}, new String[]{"HN", "Honduras"}, new String[]{"HK", "Hong Kong"}, new String[]{"HU", "Hungary"}, new String[]{"IS", "Iceland"}, new String[]{"IN", "India"}, new String[]{"ID", "Indonesia"}, new String[]{"IR", "Iran"}, new String[]{"IQ", "Iraq"}, new String[]{"IE", "Ireland"}, new String[]{"IL", "Israel"}, new String[]{"IT", "Italy"}, new String[]{"CI", "Ivory Coast"}, new String[]{"JM", "Jamaica"}, new String[]{"JP", "Japan"}, new String[]{"JO", "Jordan"}, new String[]{"KZ", "Kazakstan"}, new String[]{"KE", "Kenya"}, new String[]{"KI", "Kiribati"}, new String[]{"KP", "Korea (North)"}, new String[]{"KR", "Korea (South)"}, new String[]{"KW", "Kuwait"}, new String[]{"KG", "Kirgistan"}, new String[]{"LA", "Laos"}, new String[]{"LV", "Latvia"}, new String[]{"LB", "Lebanon"}, new String[]{"LS", "Lesotho"}, new String[]{"LR", "Liberia"}, new String[]{"LY", "Libya"}, new String[]{"LI", "Liechtenstein"}, new String[]{"LT", "Lithuania"}, new String[]{"LU", "Luxembourg"}, new String[]{"MO", "Macau"}, new String[]{"MK", "Macedonia"}, new String[]{"MG", "Madagascar"}, new String[]{"MW", "Malawi"}, new String[]{"MY", "Malaysia"}, new String[]{"MV", "Maldives"}, new String[]{"ML", "Mali"}, new String[]{"MT", "Malta"}, new String[]{"MH", "Marshall Islands"}, new String[]{"MQ", "Martinique  (Fr.)"}, new String[]{"MR", "Mauritania"}, new String[]{"MU", "Mauritius"}, new String[]{"TY", "Mayotte"}, new String[]{"MX", "Mexico"}, new String[]{"FM", "Micronesia"}, new String[]{"MD", "Moldova"}, new String[]{"MC", "Monaco"}, new String[]{"MN", "Mongolia"}, new String[]{"MS", "Montserrat"}, new String[]{"MA", "Morocco"}, new String[]{"MZ", "Mozambique"}, new String[]{"MM", "Myanmar"}, new String[]{"NA", "Namibia"}, new String[]{"NR", "Nauru"}, new String[]{"NP", "Nepal"}, new String[]{"NL", "Netherlands"}, new String[]{"AN", "Netherlands Antilles"}, new String[]{"NC", "New Caledonia"}, new String[]{"NZ", "New Zealand"}, new String[]{"NI", "Nicaragua"}, new String[]{"NE", "Niger"}, new String[]{"NG", "Nigeria"}, new String[]{"NU", "Niue"}, new String[]{"NF", "Norfork Island"}, new String[]{"MP", "Northern Mariana Isl."}, new String[]{"NO", "Norway"}, new String[]{"OM", "Oman"}, new String[]{"PK", "Pakistan"}, new String[]{"PW", "Palau"}, new String[]{"PA", "Panama"}, new String[]{"PG", "Papua New"}, new String[]{"PY", "Paraguay"}, new String[]{"PE", "Peru"}, new String[]{"PH", "Philippines"}, new String[]{"PN", "Pitcairn"}, new String[]{"PL", "Poland"}, new String[]{"PT", "Portugal"}, new String[]{"PR", "Puerto Rico (US)"}, new String[]{"QA", "Qatar"}, new String[]{"RE", "Reunion (Fr.)"}, new String[]{"RO", "Romania"}, new String[]{"RU", "Russian Federation"}, new String[]{"RW", "Rwanda"}, new String[]{"DN", "Saint Kitts and Nevis"}, new String[]{"LC", "Saint Lucia"}, new String[]{"VC", "St. Vincent & the Grenadines"}, new String[]{"WS", "Samoa"}, new String[]{"SM", "San Marino"}, new String[]{"ST", "St. Tome and Principe"}, new String[]{"SA", "Saudi Arabia"}, new String[]{"SN", "Senegal"}, new String[]{"SC", "Seychelles"}, new String[]{"SL", "Sierra Leone"}, new String[]{"SG", "Singapore"}, new String[]{"SK", "Slovak Republic"}, new String[]{"SI", "Slovenia"}, new String[]{"SB", "Solomon Islands"}, new String[]{"SO", "Somalia"}, new String[]{"ZA", "South Africa"}, new String[]{"GS", "South Georgia and The South Sandwich Islands"}, new String[]{"ES", "Spain"}, new String[]{"LK", "Sri Lanka"}, new String[]{"SH", "St. Helena"}, new String[]{"PM", "St. Pierre & Miquelon"}, new String[]{"SD", "Sudan"}, new String[]{"SR", "Suriname"}, new String[]{"SJ", "Svalbarn & Jan Mayen Is"}, new String[]{"SZ", "Swaziland"}, new String[]{"SE", "Sweden"}, new String[]{"CH", "Switzerland"}, new String[]{"SY", "Syria"}, new String[]{"TW", "Taiwan"}, new String[]{"TJ", "Tadjikistan"}, new String[]{"TZ", "Tanzania"}, new String[]{"TH", "Thailand"}, new String[]{"TG", "Togo"}, new String[]{"TK", "Tokelau"}, new String[]{"TO", "Tonga"}, new String[]{"TT", "Trinidad & Tobago"}, new String[]{"TN", "Tunisia"}, new String[]{"TR", "Turkey"}, new String[]{"TM", "Turkmenistan"}, new String[]{"TC", "Turks & Caicos Islands"}, new String[]{"TV", "Tuvalu"}, new String[]{"UG", "Uganda"}, new String[]{"UA", "Ukraine"}, new String[]{"AE", "United Arab Emirates"}, new String[]{"UK", "United Kingdom"}, new String[]{"US", "United States"}, new String[]{"UM", "US minor outlying Isl."}, new String[]{"UY", "Uruguay"}, new String[]{"UZ", "Uzbekistan"}, new String[]{"VU", "Vanuatu"}, new String[]{"VA", "Vatican City State"}, new String[]{"VE", "Venezuela"}, new String[]{"VN", "Vietnam"}, new String[]{"VG", "Virigan Islands (British)"}, new String[]{"VI", "Virgin Islands (US)"}, new String[]{"WF", "Wallis & Futuna Islands"}, new String[]{"EH", "Western Sahara"}, new String[]{"YE", "Yemen"}, new String[]{"YU", "Yugoslavia"}, new String[]{"ZR", "Zaire"}, new String[]{"ZM", "Zambia"}, new String[]{"ZW", "Zimbabwe"}};
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            Hashtable countryIndex = new Hashtable();
            for (int i = 0; i < strArr.length; i++) {
                countryIndex.put(strArr[i][1], strArr[i][0]);
            }
            HashSet reqInpNames = new HashSet(Arrays.asList("exClientID", "exStartDate2", "exFirstName", "exLastName", "exCompany", "exEmail", "exAddress", "exCity", "exZipCode", "exOtherState", "exPhone", "exFax", "exFirstName2", "exLastName2", "exCompany2", "exEmail2", "exAddress2", "exCity2", "exZipCode2", "exOtherState2", "exPhone2", "exFax2", "exCCName", "exDay", "exNotes", "exGraceDays", "exOutsideEmail"));
            HashSet reqSelNames = new HashSet(Arrays.asList("exState", "exCountry", "exState2", "exCountry2", "exCCType", "exMonth", "exYear"));
            input_tags = body.getElementsByTagName("input");
            for (int i2 = 0; i2 < input_tags.getLength(); i2++) {
                Element inpt = (Element) input_tags.item(i2);
                String type = inpt.getAttribute("type");
                String name = inpt.getAttribute("name");
                if (("text".equals(type) || "hidden".equals(type)) && reqInpNames.contains(name)) {
                    String value = inpt.getAttribute("value").trim();
                    if (!"".equals(value)) {
                        clientInfo.put(name, value);
                    }
                } else if ("checkbox".equals(type) && "exBillingSameAsOwner".equals(name) && inpt.getAttributeNode("checked") != null) {
                    clientInfo.put(name, new Boolean(true));
                }
            }
            NodeList selects = body.getElementsByTagName("select");
            for (int i3 = 0; i3 < selects.getLength(); i3++) {
                Element select = (Element) selects.item(i3);
                String name2 = select.getAttribute("name");
                if (reqSelNames.contains(name2)) {
                    NodeList options = select.getElementsByTagName("option");
                    int j = 0;
                    while (true) {
                        if (j < options.getLength()) {
                            Element option = (Element) options.item(j);
                            if (option.getAttributeNode("selected") == null) {
                                j++;
                            } else {
                                String value2 = option.getAttribute("value").trim();
                                if (!"".equals(value2)) {
                                    if (name2.startsWith("exCountry")) {
                                        if (countryIndex.containsKey(value2)) {
                                            clientInfo.put(name2, countryIndex.get(value2).toString());
                                        }
                                    } else {
                                        clientInfo.put(name2, value2);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (clientInfo.containsKey("exBillingSameAsOwner")) {
                if (clientInfo.containsKey("exFirstName")) {
                    clientInfo.put("exFirstName2", clientInfo.get("exFirstName").toString());
                }
                if (clientInfo.containsKey("exLastName")) {
                    clientInfo.put("exLastName2", clientInfo.get("exLastName").toString());
                }
                if (clientInfo.containsKey("exCompany")) {
                    clientInfo.put("exCompany2", clientInfo.get("exCompany").toString());
                }
                if (clientInfo.containsKey("exEmail")) {
                    clientInfo.put("exEmail2", clientInfo.get("exEmail").toString());
                }
                if (clientInfo.containsKey("exAddress")) {
                    clientInfo.put("exAddress2", clientInfo.get("exAddress").toString());
                }
                if (clientInfo.containsKey("exCity")) {
                    clientInfo.put("exCity2", clientInfo.get("exCity").toString());
                }
                if (clientInfo.containsKey("exState")) {
                    clientInfo.put("exState2", clientInfo.get("exState").toString());
                }
                if (clientInfo.containsKey("exZipCode")) {
                    clientInfo.put("exZipCode2", clientInfo.get("exZipCode").toString());
                }
                if (clientInfo.containsKey("exCountry")) {
                    clientInfo.put("exCountry2", clientInfo.get("exCountry").toString());
                }
                if (clientInfo.containsKey("exOtherState")) {
                    clientInfo.put("exOtherState2", clientInfo.get("exOtherState").toString());
                }
                if (clientInfo.containsKey("exPhone")) {
                    clientInfo.put("exPhone2", clientInfo.get("exPhone").toString());
                }
                if (clientInfo.containsKey("exFax")) {
                    clientInfo.put("exFax2", clientInfo.get("exFax").toString());
                }
            } else {
                clientInfo.put("exBillingSameAsOwner", new Boolean(false));
            }
        } catch (Exception ex) {
            System.err.println("Error getting the client info !");
            ex.printStackTrace();
        }
        return clientInfo;
    }

    public static Hashtable getDomainData(InputStream input) {
        Hashtable domainData = new Hashtable();
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            input_tags = body.getElementsByTagName("input");
            for (int i = 0; i < input_tags.getLength(); i++) {
                Element inpt = (Element) input_tags.item(i);
                String type = inpt.getAttribute("type");
                String name = inpt.getAttribute("name");
                if ("checkbox".equals(type) && inpt.getAttributeNode("checked") != null && !"exResOption_2".equals(name) && !"exResOption_2".equals(name)) {
                }
            }
            for (int i2 = 0; i2 < input_tags.getLength(); i2++) {
                Element inpt2 = (Element) input_tags.item(i2);
                String type2 = inpt2.getAttribute("type");
                String name2 = inpt2.getAttribute("name");
                String value = inpt2.getAttribute("value").trim();
                if ("text".equals(type2) && !"".equals(value)) {
                    if (!"exQuantity_2".equals(name2)) {
                        if ("exQuantity_3".equals(name2)) {
                            domainData.put("transfer", value);
                        }
                    } else {
                        domainData.put("quota", value);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error getting domain special data !");
            ex.printStackTrace();
        }
        return domainData;
    }

    public static Hashtable getManagers(InputStream input) {
        Hashtable mailManagers = new Hashtable();
        boolean setMailManagerURL = false;
        boolean setMailListURL = false;
        boolean setMysqlURL = false;
        boolean setSubList = false;
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            a_tags = body.getElementsByTagName("a");
            for (int i = 0; i < a_tags.getLength(); i++) {
                String href = ((Element) a_tags.item(i)).getAttribute("href").trim();
                if (!setMailManagerURL && href.indexOf("/cp/mailManager?exDomainPackageID") != -1) {
                    mailManagers.put("mailManager", href);
                    setMailManagerURL = true;
                    if (setMailListURL && setMysqlURL && setSubList) {
                        break;
                    }
                } else if (!setMailListURL && href.indexOf("/cp/mailinglist?exDomainPackageID") != -1) {
                    mailManagers.put("mailList", href);
                    setMailListURL = true;
                    if (setMailManagerURL && setMysqlURL && setSubList) {
                        break;
                    }
                } else if (!setMysqlURL && href.indexOf("/cp/mysqladmin/index.php3?exDomainPackageID") != -1) {
                    mailManagers.put("mysqlURL", href);
                    setMysqlURL = true;
                    if (setMailManagerURL && setMailListURL && setSubList) {
                        break;
                    }
                } else {
                    if (!setSubList && href.indexOf("/cp/rac/nsManager.cgi?exDomainPackageID") != -1) {
                        mailManagers.put("subdomainList", href);
                        setSubList = true;
                        if (setMailManagerURL && setMailListURL && setMysqlURL) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error getting domain mail managers !");
            ex.printStackTrace();
        }
        return mailManagers;
    }

    public static Hashtable getMailURLList(InputStream input) {
        Hashtable mailURLs = new Hashtable();
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            a_tags = body.getElementsByTagName("a");
            for (int i = 0; i < a_tags.getLength(); i++) {
                String href = ((Element) a_tags.item(i)).getAttribute("href").trim();
                if (href.startsWith("/cp/mailManager?exUser=")) {
                    linkedList.add(href);
                } else if (href.startsWith("/cp/mailManager?exList=")) {
                    String name = href.substring("/cp/mailManager?exList=".length());
                    int lastIndex = name.indexOf("&exLanguage");
                    if (lastIndex > -1) {
                        name = name.substring(0, lastIndex);
                    }
                    linkedList2.add(name);
                }
            }
            if (!linkedList.isEmpty()) {
                mailURLs.put("mailBoxes", linkedList);
            }
            if (!linkedList2.isEmpty()) {
                mailURLs.put("mailLists", linkedList2);
            }
        } catch (Exception ex) {
            System.err.println("Error getting mail manager list !");
            ex.printStackTrace();
        }
        return mailURLs;
    }

    public static Hashtable getMailBoxInfo(InputStream input) {
        Hashtable mailBoxInfo = new Hashtable();
        LinkedList linkedList = new LinkedList();
        LinkedList linkedList2 = new LinkedList();
        new LinkedList();
        boolean isForwardToOthers = false;
        boolean isForwardToOutside = false;
        boolean isAutoresponse = false;
        try {
            body = (Element) getRoot(input).getElementsByTagName("body").item(0);
            input_tags = body.getElementsByTagName("input");
            for (int i = 0; i < input_tags.getLength(); i++) {
                Element inpt = (Element) input_tags.item(i);
                String type = inpt.getAttribute("type");
                String name = inpt.getAttribute("name").trim();
                String value = inpt.getAttribute("value").trim();
                if ("checkbox".equals(type) && inpt.getAttributeNode("checked") != null) {
                    if ("exCheckForwardOwner".equals(name)) {
                        mailBoxInfo.put("isForwardToOwner", new Boolean(true));
                    } else if ("exCheckForwardBoxes".equals(name)) {
                        isForwardToOthers = true;
                    } else if (name.startsWith("exItem_")) {
                        linkedList.add(value);
                    } else if ("exCheckForwardOutside".equals(name)) {
                        isForwardToOutside = true;
                        mailBoxInfo.put("isForwardToOutside", new Boolean(true));
                    } else if ("exCheckAutoResponse".equals(name)) {
                        isAutoresponse = true;
                        mailBoxInfo.put("isAutoresponse", new Boolean(true));
                    }
                } else if ("exUser".equals(name) && !"".equals(value)) {
                    mailBoxInfo.put("ownerName", value);
                }
            }
            textarea_tags = body.getElementsByTagName("textarea");
            for (int i2 = 0; i2 < textarea_tags.getLength(); i2++) {
                Element textarea = (Element) textarea_tags.item(i2);
                String name2 = textarea.getAttribute("name").trim();
                child = textarea.getFirstChild();
                if (child != null) {
                    String value2 = nbspClear(child.getNodeValue().trim());
                    if (isForwardToOutside && "exEmailAddr".equals(name2) && !"".equals(value2)) {
                        StringTokenizer st = new StringTokenizer(value2);
                        while (st.hasMoreTokens()) {
                            linkedList2.add(st.nextToken().toString());
                        }
                    } else if (isAutoresponse && "exAutoResponse".equals(name2) && !"".equals(value2)) {
                        mailBoxInfo.put("autoresponse", value2);
                    }
                }
            }
            if (isForwardToOthers) {
                mailBoxInfo.put("isForwardToOthers", new Boolean(true));
                mailBoxInfo.put("forwardToOthers", linkedList);
            }
            if (!linkedList2.isEmpty()) {
                mailBoxInfo.put("forwardToOutside", linkedList2);
            }
        } catch (Exception ex) {
            System.err.println("Error getting mail manager list !");
            ex.printStackTrace();
        }
        return mailBoxInfo;
    }

    public static Hashtable getMySQLPages(InputStream input) {
        Hashtable dbPages = new Hashtable();
        try {
            input_tags = getRoot(input).getElementsByTagName("frame");
            for (int i = 0; i < input_tags.getLength(); i++) {
                Element inpt = (Element) input_tags.item(i);
                if ("nav".equals(inpt.getAttribute("name"))) {
                    dbPages.put("left", inpt.getAttribute("src").trim());
                } else if ("main".equals(inpt.getAttribute("name"))) {
                    dbPages.put("main", inpt.getAttribute("src").trim());
                }
            }
        } catch (Exception ex) {
            System.err.println("Error getting the MySQL link pages !");
            ex.printStackTrace();
        }
        return dbPages;
    }

    public static List getMySQLDBNames(InputStream input) {
        List dbNames = new LinkedList();
        try {
            input_tags = getRoot(input).getElementsByTagName("font");
            for (int i = 0; i < input_tags.getLength(); i++) {
                Element inpt = (Element) input_tags.item(i);
                String dbName = inpt.getFirstChild().getNodeValue().trim();
                if (dbName != null && !"Home".equals(dbName)) {
                    dbNames.add(dbName);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error getting the MySQL DB names !");
            ex.printStackTrace();
        }
        return dbNames;
    }

    public static List getSubdomains(InputStream input) {
        List subdomains = new LinkedList();
        try {
            tr_tags = getRoot(input).getElementsByTagName("tr");
            int length = tr_tags.getLength() - 1;
            for (int i = 7; i < length; i++) {
                Element one_tr_tag = (Element) tr_tags.item(i);
                td_tags = one_tr_tag.getElementsByTagName("td");
                Element one_td_tag = (Element) td_tags.item(0);
                Element one_b_tag = (Element) one_td_tag.getElementsByTagName("b").item(0);
                String one_subdomain = one_b_tag.getFirstChild().getNodeValue().trim();
                System.out.println("Subdomain -> " + one_subdomain);
                subdomains.add(one_subdomain);
            }
        } catch (Exception ex) {
            System.err.println("Error getting the subdomains !");
            ex.printStackTrace();
        }
        if (subdomains.isEmpty()) {
            System.out.println("Subdomain list is empty");
        }
        return subdomains;
    }

    public static List getBillPeriods(InputStream input) {
        List bills = new LinkedList();
        try {
            input_tags = getRoot(input).getElementsByTagName("input");
            for (int i = 0; i < input_tags.getLength(); i++) {
                Element inpt = (Element) input_tags.item(i);
                String inpName = inpt.getAttribute("name");
                String inpValue = inpt.getAttribute("value");
                if (inpName.startsWith("exField_month!month!")) {
                    bills.add(inpValue);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting billing periods !");
            e.printStackTrace();
        }
        if (bills.isEmpty()) {
            System.out.println("Billing period list is empty");
        }
        return bills;
    }

    /* JADX WARN: Code restructure failed: missing block: B:33:0x005d, code lost:
        r0.put("name", r0.getAttribute("value").trim());
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.util.Hashtable getMysqlDbName(java.io.InputStream r5) {
        /*
            java.util.Hashtable r0 = new java.util.Hashtable
            r1 = r0
            r1.<init>()
            r6 = r0
            java.lang.String r0 = "db"
            r7 = r0
            r0 = r5
            org.w3c.dom.Element r0 = getRoot(r0)     // Catch: java.lang.Exception -> L7e
            java.lang.String r1 = "input"
            org.w3c.dom.NodeList r0 = r0.getElementsByTagName(r1)     // Catch: java.lang.Exception -> L7e
            psoft.hsphere.converter.alabanza.MigrationProvider.input_tags = r0     // Catch: java.lang.Exception -> L7e
            r0 = 0
            r8 = r0
        L1d:
            r0 = r8
            org.w3c.dom.NodeList r1 = psoft.hsphere.converter.alabanza.MigrationProvider.input_tags     // Catch: java.lang.Exception -> L7e
            int r1 = r1.getLength()     // Catch: java.lang.Exception -> L7e
            if (r0 >= r1) goto L7b
            org.w3c.dom.NodeList r0 = psoft.hsphere.converter.alabanza.MigrationProvider.input_tags     // Catch: java.lang.Exception -> L7e
            r1 = r8
            org.w3c.dom.Node r0 = r0.item(r1)     // Catch: java.lang.Exception -> L7e
            org.w3c.dom.Element r0 = (org.w3c.dom.Element) r0     // Catch: java.lang.Exception -> L7e
            r9 = r0
            java.lang.String r0 = "hidden"
            r1 = r9
            java.lang.String r2 = "type"
            java.lang.String r1 = r1.getAttribute(r2)     // Catch: java.lang.Exception -> L7e
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Exception -> L7e
            if (r0 == 0) goto L75
            java.lang.String r0 = "db"
            r1 = r9
            java.lang.String r2 = "name"
            java.lang.String r1 = r1.getAttribute(r2)     // Catch: java.lang.Exception -> L7e
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Exception -> L7e
            if (r0 == 0) goto L75
            r0 = r6
            java.lang.String r1 = "name"
            r2 = r9
            java.lang.String r3 = "value"
            java.lang.String r2 = r2.getAttribute(r3)     // Catch: java.lang.Exception -> L7e
            java.lang.String r2 = r2.trim()     // Catch: java.lang.Exception -> L7e
            java.lang.Object r0 = r0.put(r1, r2)     // Catch: java.lang.Exception -> L7e
            goto L7b
        L75:
            int r8 = r8 + 1
            goto L1d
        L7b:
            goto L8c
        L7e:
            r8 = move-exception
            java.io.PrintStream r0 = java.lang.System.err
            java.lang.String r1 = "Error getting the MySQL DB name !"
            r0.println(r1)
            r0 = r8
            r0.printStackTrace()
        L8c:
            r0 = r6
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.converter.alabanza.MigrationProvider.getMysqlDbName(java.io.InputStream):java.util.Hashtable");
    }

    private static Element getRoot(InputStream input) {
        try {
            parser = new DOMParser();
            tidy = new Tidy();
            tidy.setQuiet(true);
            tidy.setShowWarnings(false);
            tidy.setMakeClean(false);
            tidy.setTidyMark(false);
            tidy.setErrout(new PrintWriter(new FileOutputStream("/dev/null")));
            tidy.setXHTML(true);
            doc = tidy.parseDOM(input, (OutputStream) null);
            root = doc.getDocumentElement();
        } catch (Exception e) {
            doc = new DocumentImpl();
            root = doc.createElement("html");
            body = doc.createElement("body");
            root.appendChild(body);
        }
        return root;
    }

    private static String nbspClear(String input) {
        StringBuffer result = new StringBuffer();
        StringTokenizer token = new StringTokenizer(input, " ");
        while (token.hasMoreTokens()) {
            result.append(token.nextToken());
        }
        return result.toString();
    }

    public static void main(String[] arg) throws IOException {
        if (arg.length != 1) {
            System.out.println("\nUsage: java psoft.hsphere.converter.MigrationProvider <input file>\n");
        } else {
            getClientDomains(new BufferedInputStream(new FileInputStream(arg[0])));
        }
    }
}
