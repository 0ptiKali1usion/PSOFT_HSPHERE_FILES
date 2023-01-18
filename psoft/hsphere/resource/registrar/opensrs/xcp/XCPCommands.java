package psoft.hsphere.resource.registrar.opensrs.xcp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/XCPCommands.class */
public class XCPCommands {
    public static XCPMessage auth(String username) {
        return new XCPMessage("authenticate", FMACLManager.USER, DTAssoc.newInstance().add("crypt_type", "blowfish").add("username", username).add("password", username));
    }

    public static XCPMessage checkVersion() {
        return new XCPMessage("check", "version", DTAssoc.newInstance().add("sender", "H-Sphere Client").add("version", "XML:0.1").add("state", "ready"));
    }

    public static XCPMessage cookie(String ip, String domain, String username, String password) {
        XCPMessage msg = new XCPMessage();
        msg.data.add(new DTAssoc().add(new ItemTag("protocol", "XCP")).add(new ItemTag("action", "set")).add(new ItemTag("object", "cookie")).add(new ItemTag("registrant_ip", ip)).add(new ItemTag("attributes", new DTAssoc().add(new ItemTag("domain", domain)).add(new ItemTag("reg_username", username)).add(new ItemTag("reg_password", password)))));
        return msg;
    }

    public static XCPMessage isTransferable(String domain) {
        return new XCPMessage("check_transfer", domain, DTAssoc.newInstance().add("domain", domain).add("get_request_address", "1"));
    }

    public static XCPMessage domainLookup(String domain) {
        return new XCPMessage("lookup", "domain", DTAssoc.newInstance().add("domain", domain));
    }

    public static XCPMessage domainPrice(String domain, int period) {
        return new XCPMessage("get_price", "domain", DTAssoc.newInstance().add("domain", domain).add("period", Integer.toString(period)));
    }

    public static XCPMessage belongsToRSP(String domain) {
        return new XCPMessage("belongs_to_rsp", "domain", DTAssoc.newInstance().add("domain", domain));
    }

    public static XCPMessage getDomainInfo(String cookieId, String domain) {
        return new XCPMessage("get", "domain", cookieId, DTAssoc.newInstance().add("domain", domain).add("type", "waiting_history"));
    }

    public static XCPMessage register(String domain, String username, String password, String period, Map registrant, Map tech, Map admin, Map billing, Collection nameservers) {
        return register(domain, username, password, period, registrant, tech, admin, billing, nameservers, null);
    }

    public static XCPMessage checkTransfer(String domain) {
        Tag tag = DTAssoc.newInstance();
        tag.add("domain", domain);
        tag.add("get_request_address", "1");
        return new XCPMessage("check_transfer", "domain", tag);
    }

    public static XCPMessage transfer(String domain, String username, String password, Map registrant, Map tech, Map admin, Map billing, Collection nameservers) {
        boolean customNS = nameservers.size() > 1;
        Tag tag = DTAssoc.newInstance();
        tag.add("domain", domain).add("encoding_type", "").add("reg_type", "transfer").add("reg_username", username).add("reg_password", password).add("custom_nameservers", customNS ? "1" : "0").add("affiliate_id", "301").add("period", "1").add("bulk_order", "0").add("custom_tech_contact", "1").add("link_domains", "0").add("contact_set", DTAssoc.newInstance().add(FMACLManager.ADMIN, admin).add("billing", billing).add("owner", registrant).add("tech", tech)).add("nameserver_list", getNameServers(nameservers)).add("auto_renew", "0");
        return new XCPMessage("sw_register", "domain", tag);
    }

    public static XCPMessage register(String domain, String username, String password, String period, Map registrant, Map tech, Map admin, Map billing, Collection nameservers, Map extra) {
        boolean customNS = nameservers.size() > 1;
        Tag tag = DTAssoc.newInstance();
        tag.add("domain", domain).add("encoding_type", "").add("reg_type", "new").add("reg_username", username).add("reg_password", password).add("custom_nameservers", customNS ? "1" : "0").add("affiliate_id", "301").add("period", period).add("bulk_order", "0").add("custom_tech_contact", "1").add("link_domains", "0").add("contact_set", DTAssoc.newInstance().add(FMACLManager.ADMIN, admin).add("billing", billing).add("owner", registrant).add("tech", tech)).add("nameserver_list", getNameServers(nameservers)).add("auto_renew", "0");
        if (extra != null) {
            if ("us".equals(extra.get("tld"))) {
                tag.add("tld_data", DTAssoc.newInstance().add("nexus", DTAssoc.newInstance().add("app_purpose", (String) extra.get("app_purpose")).add("category", (String) extra.get("nexus_category")).add("validator", (String) extra.get("nexus_validator"))));
            } else if ("ca".equals(extra.get("tld"))) {
                tag.add("domain_description", (String) extra.get("domain_description")).add("isa_trademark", (String) extra.get("isa_trademark")).add("legal_type", (String) extra.get("legal_type")).add("lang_pref", (String) extra.get("lang_pref"));
            }
        }
        return new XCPMessage("sw_register", "domain", tag);
    }

    public static XCPMessage changeNameServer(String cookieId, List names) {
        DTArray list = DTArray.newInstance();
        int count = 1;
        Iterator i = names.iterator();
        while (i.hasNext()) {
            list.add(DTAssoc.newInstance().add("name", (String) i.next()).add("sortorder", Integer.toString(count)).add("action", "update"));
            count++;
        }
        return new XCPMessage("modify", "domain", cookieId, DTAssoc.newInstance().add("data", "nameserver_list").add("nameserver_list", list));
    }

    public static XCPMessage changeContactInfo(String cookieId, String type, Map info) {
        return new XCPMessage("modify", "domain", cookieId, DTAssoc.newInstance().add("data", "contact_info").add("affect_domains", "0").add("contact_set", DTAssoc.newInstance().add(type, info)));
    }

    public static XCPMessage changePassword(String cookie, String newPassword) {
        return new XCPMessage("change", "password", cookie, DTAssoc.newInstance().add("reg_password", newPassword));
    }

    public static XCPMessage change(String cookieId, Map admin, Map owner, Map billing, Map tech) {
        return new XCPMessage("modify", "domain", cookieId, DTAssoc.newInstance().add("data", "contact_info").add("affect_domains", "0").add("contact_set", DTAssoc.newInstance().add(FMACLManager.ADMIN, owner).add("owner", tech).add("billing", billing).add("tech", tech)));
    }

    public static XCPMessage renew(String domainName, String expYear, int period) {
        return new XCPMessage("renew", "domain", DTAssoc.newInstance().add("domain", domainName).add("currentexpirationyear", expYear).add("period", Integer.toString(period)).add("handle", "process").add("auto_renew", "0"));
    }

    public static DTArray getNameServers(Collection nameservers) {
        DTArray nss = DTArray.newInstance();
        int j = 0;
        Iterator i = nameservers.iterator();
        while (i.hasNext()) {
            String nsName = (String) i.next();
            nss.add(DTAssoc.newInstance().add("sortorder", "" + (j + 1)).add("name", nsName));
            j++;
        }
        return nss;
    }
}
