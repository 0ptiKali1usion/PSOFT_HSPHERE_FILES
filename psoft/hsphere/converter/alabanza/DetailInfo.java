package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/DetailInfo.class */
public class DetailInfo {
    public static boolean parseDetailInfo(URL url, Document doc, Document billDoc, Element user, Element billUser, Element account, List traffic) {
        boolean result;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            Hashtable detailInfo = MigrationProvider.getClientInfo(in);
            if (!detailInfo.isEmpty()) {
                if (!user.hasAttribute("login")) {
                    user.setAttribute("login", AlabanzaConfig.systemUnknownUserPrefix + ((String) detailInfo.get("exClientID")));
                }
                if (!user.hasAttribute("password")) {
                    String tmpPsw = (String) detailInfo.get("exLastName");
                    if (tmpPsw.length() > 6) {
                        tmpPsw = tmpPsw.substring(0, 5);
                    }
                    user.setAttribute("password", tmpPsw + ((String) detailInfo.get("exClientID")));
                }
                Element cInfo = doc.createElement("info");
                cInfo.setAttribute("prefix", "_ci_");
                Element bInfo = doc.createElement("info");
                bInfo.setAttribute("prefix", "_bi_");
                Element itemFirstName = doc.createElement("item");
                itemFirstName.setAttribute("name", "first_name");
                itemFirstName.appendChild(doc.createTextNode((String) detailInfo.get("exFirstName")));
                Element itemLastName = doc.createElement("item");
                itemLastName.setAttribute("name", "last_name");
                itemLastName.appendChild(doc.createTextNode((String) detailInfo.get("exLastName")));
                Element itemCompany = doc.createElement("item");
                itemCompany.setAttribute("name", "company");
                itemCompany.appendChild(doc.createTextNode((String) detailInfo.get("exCompany")));
                Element itemEmail = doc.createElement("item");
                itemEmail.setAttribute("name", "email");
                itemEmail.appendChild(doc.createTextNode((String) detailInfo.get("exEmail")));
                Element itemType = doc.createElement("item");
                itemType.setAttribute("name", "type");
                itemType.appendChild(doc.createTextNode("Check"));
                Element itemAddress1 = doc.createElement("item");
                itemAddress1.setAttribute("name", "address1");
                itemAddress1.appendChild(doc.createTextNode((String) detailInfo.get("exAddress")));
                Element itemCity = doc.createElement("item");
                itemCity.setAttribute("name", "city");
                itemCity.appendChild(doc.createTextNode((String) detailInfo.get("exCity")));
                Element itemState = doc.createElement("item");
                itemState.setAttribute("name", "state");
                String tmpState = (String) detailInfo.get("exState");
                if (tmpState == null) {
                    tmpState = "NA";
                }
                itemState.appendChild(doc.createTextNode(tmpState));
                Element itemZip = doc.createElement("item");
                itemZip.setAttribute("name", "postal_code");
                itemZip.appendChild(doc.createTextNode((String) detailInfo.get("exZipCode")));
                Element itemCountry = doc.createElement("item");
                itemCountry.setAttribute("name", "country");
                itemCountry.appendChild(doc.createTextNode((String) detailInfo.get("exCountry")));
                Element itemPhone = doc.createElement("item");
                itemPhone.setAttribute("name", "phone");
                itemPhone.appendChild(doc.createTextNode((String) detailInfo.get("exPhone")));
                cInfo.appendChild(itemFirstName);
                cInfo.appendChild(itemLastName);
                cInfo.appendChild(itemEmail);
                cInfo.appendChild(itemCompany);
                cInfo.appendChild(itemType);
                cInfo.appendChild(itemAddress1);
                cInfo.appendChild(itemCity);
                cInfo.appendChild(itemState);
                cInfo.appendChild(itemZip);
                cInfo.appendChild(itemCountry);
                cInfo.appendChild(itemPhone);
                Element itemFirstName2 = doc.createElement("item");
                itemFirstName2.setAttribute("name", "first_name");
                itemFirstName2.appendChild(doc.createTextNode((String) detailInfo.get("exFirstName2")));
                Element itemLastName2 = doc.createElement("item");
                itemLastName2.setAttribute("name", "last_name");
                itemLastName2.appendChild(doc.createTextNode((String) detailInfo.get("exLastName2")));
                Element itemCompany2 = doc.createElement("item");
                itemCompany2.setAttribute("name", "company");
                itemCompany2.appendChild(doc.createTextNode((String) detailInfo.get("exCompany2")));
                Element itemEmail2 = doc.createElement("item");
                itemEmail2.setAttribute("name", "email");
                itemEmail2.appendChild(doc.createTextNode((String) detailInfo.get("exEmail2")));
                Element itemType2 = doc.createElement("item");
                itemType2.setAttribute("name", "type");
                itemType2.appendChild(doc.createTextNode("Check"));
                Element itemAddress2 = doc.createElement("item");
                itemAddress2.setAttribute("name", "address1");
                itemAddress2.appendChild(doc.createTextNode((String) detailInfo.get("exAddress2")));
                Element itemCity2 = doc.createElement("item");
                itemCity2.setAttribute("name", "city");
                itemCity2.appendChild(doc.createTextNode((String) detailInfo.get("exCity2")));
                Element itemState2 = doc.createElement("item");
                itemState2.setAttribute("name", "state");
                String tmpState2 = (String) detailInfo.get("exState2");
                if (tmpState2 == null) {
                    tmpState2 = "NA";
                }
                itemState2.appendChild(doc.createTextNode(tmpState2));
                Element itemZip2 = doc.createElement("item");
                itemZip2.setAttribute("name", "postal_code");
                itemZip2.appendChild(doc.createTextNode((String) detailInfo.get("exZipCode2")));
                Element itemCountry2 = doc.createElement("item");
                itemCountry2.setAttribute("name", "country");
                itemCountry2.appendChild(doc.createTextNode((String) detailInfo.get("exCountry2")));
                Element itemPhone2 = doc.createElement("item");
                itemPhone2.setAttribute("name", "phone");
                itemPhone2.appendChild(doc.createTextNode((String) detailInfo.get("exPhone2")));
                bInfo.appendChild(itemFirstName2);
                bInfo.appendChild(itemLastName2);
                bInfo.appendChild(itemEmail2);
                bInfo.appendChild(itemCompany2);
                bInfo.appendChild(itemType2);
                bInfo.appendChild(itemAddress2);
                bInfo.appendChild(itemCity2);
                bInfo.appendChild(itemState2);
                bInfo.appendChild(itemZip2);
                bInfo.appendChild(itemCountry2);
                bInfo.appendChild(itemPhone2);
                account.appendChild(cInfo);
                account.appendChild(bInfo);
            }
            in.close();
            System.out.println("Finished reading client info details.");
            result = true;
        } catch (Exception e) {
            System.out.println("Reading client info details failed. View log file for details.\n");
            AlabanzaConfig.getLog().error("Some errors occured in DetailInfo.parseDetailInfo().", e);
            result = false;
        }
        return result;
    }
}
