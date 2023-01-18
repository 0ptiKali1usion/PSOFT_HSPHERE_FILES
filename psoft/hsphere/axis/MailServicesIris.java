package psoft.hsphere.axis;

import freemarker.template.TemplateHashModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.dns.CNAMERecord;
import psoft.hsphere.resource.email.Autoresponder;
import psoft.hsphere.resource.email.MailAlias;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.MailForward;
import psoft.hsphere.resource.email.MailQuota;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.email.MailingList;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/axis/MailServicesIris.class */
public class MailServicesIris {
    private static Category log = Category.getInstance(MailServicesIris.class.getName());

    public String getDescription() {
        return "Functions to work with Iris Mail";
    }

    private Autoresponder getAutoResponder(Account a, String local, String domain) throws Exception {
        MailDomain md = getMailDomain(a, domain);
        for (ResourceId resourceId : md.getId().findChildren("responder")) {
            Autoresponder ar = (Autoresponder) resourceId.get();
            if (ar.getLocal().equalsIgnoreCase(local)) {
                return ar;
            }
        }
        throw new Exception("No such auto responder: " + local + "@" + domain);
    }

    private MailForward getMailForward(Account a, String local, String domain) throws Exception {
        MailDomain md = getMailDomain(a, domain);
        for (ResourceId resourceId : md.getId().findChildren("mail_forward")) {
            MailForward mf = (MailForward) resourceId.get();
            if (mf.getLocal().equalsIgnoreCase(local)) {
                return mf;
            }
        }
        throw new Exception("No such mail forward: " + local + "@" + domain);
    }

    private MailAlias getMailAlias(Account a, String local, String domain) throws Exception {
        MailDomain md = getMailDomain(a, domain);
        for (ResourceId resourceId : md.getId().findChildren("mailbox_alias")) {
            MailAlias ma = (MailAlias) resourceId.get();
            if (ma.getLocal().equalsIgnoreCase(local)) {
                return ma;
            }
        }
        throw new Exception("No such alias: " + local + "@" + domain);
    }

    private MailingList getMailingList(Account a, String email, String domain) throws Exception {
        MailDomain d = getMailDomain(a, domain);
        Collection<ResourceId> col = d.getId().findChildren("mailing_list");
        for (ResourceId resourceId : col) {
            MailingList ml = (MailingList) resourceId.get();
            if (ml.getEmail().equalsIgnoreCase(email)) {
                return ml;
            }
        }
        throw new Exception("No such mailing list: " + email + "@" + domain);
    }

    private Mailbox getMailbox(Account a, String email, String domain) throws Exception {
        MailDomain d = getMailDomain(a, domain);
        Collection<ResourceId> col = d.getId().findChildren("mailbox");
        for (ResourceId resourceId : col) {
            Mailbox mb = (Mailbox) resourceId.get();
            if (mb.getEmail().equalsIgnoreCase(email)) {
                return mb;
            }
        }
        throw new Exception("No such mailbox " + email + "@" + domain);
    }

    private MailDomain getMailDomain(Account a, String name) throws Exception {
        return (MailDomain) Utils.getDomain(a, name).getId().findChild("mail_service").findChild("mail_domain").get();
    }

    public void addMailbox(AuthToken at, String email, String domain, String password, String description) throws Exception {
        Account a = Utils.getAccount(at);
        List values = new ArrayList();
        values.add(email);
        values.add(password);
        values.add(description);
        getMailDomain(a, domain).addChild("mailbox", "", values);
    }

    public void deleteMailbox(AuthToken at, String email, String domain) throws Exception {
        getMailbox(Utils.getAccount(at), email, domain).FM_cdelete(0);
    }

    public void changeMailboxPassword(AuthToken at, String email, String domain, String newPassword) throws Exception {
        getMailbox(Utils.getAccount(at), email, domain).changePassword(newPassword);
    }

    public double getMailboxQuota(AuthToken at, String email, String domain) throws Exception {
        Mailbox mb = getMailbox(Utils.getAccount(at), email, domain);
        Quota q = (Quota) mb.getId().findChild("mail_quota").get();
        return q.getAmount();
    }

    public void setMailboxQuota(AuthToken at, String email, String domain, double quota) throws Exception {
        Mailbox mb = getMailbox(Utils.getAccount(at), email, domain);
        Quota q = (Quota) mb.getId().findChild("mail_quota").get();
        q.FM_cdelete(0);
        List values = new ArrayList();
        values.add(Double.toString(quota));
        mb.addChild("mail_quota", "", values);
    }

    public void setDiscardMail(AuthToken at, String email, String domain, boolean discard) throws Exception {
        getMailbox(Utils.getAccount(at), email, domain).FM_discardMail(discard ? "enable" : "");
    }

    public boolean getDiscardMail(AuthToken at, String email, String domain) throws Exception {
        return getMailbox(Utils.getAccount(at), email, domain).isDiscard();
    }

    public Object[] getMailboxes(AuthToken at, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        Collection<ResourceId> col = md.getId().findChildren("mailbox");
        Object[] result = new Object[col.size()];
        int counter = 0;
        for (ResourceId resourceId : col) {
            Mailbox mb = (Mailbox) resourceId.get();
            Object[] entry = new Object[4];
            entry[0] = mb.getEmail();
            entry[1] = mb.getDescription();
            entry[2] = new Boolean(mb.isDiscard());
            entry[3] = new Double(mb.getId().findChild("mail_quota").get().getAmount());
            int i = counter;
            counter++;
            result[i] = entry;
        }
        return result;
    }

    public void addMailingList(AuthToken at, String email, String domain, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(email);
        values.add(description);
        md.addChild("mailing_list", "", values);
    }

    public void deleteMailingList(AuthToken at, String email, String domain) throws Exception {
        MailingList ml = getMailingList(Utils.getAccount(at), email, domain);
        ml.FM_cdelete(0);
    }

    public void subscribeToMailingList(AuthToken at, String email, String domain, String[] sub, boolean mod) throws Exception {
        MailingList ml = getMailingList(Utils.getAccount(at), email, domain);
        for (String str : sub) {
            ml.subscribe(str, mod);
        }
    }

    public void unsubscribeFromMailingList(AuthToken at, String email, String domain, String[] sub, boolean mod) throws Exception {
        MailingList ml = getMailingList(Utils.getAccount(at), email, domain);
        for (String str : sub) {
            ml.unsubscribe(str, mod);
        }
    }

    public void unsubscribeAllFromMailingList(AuthToken at, String email, String domain) throws Exception {
        MailingList ml = getMailingList(Utils.getAccount(at), email, domain);
        ml.unsubscribeAll();
    }

    public void addAlias(AuthToken at, String local, String domain, String foreign, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(local);
        values.add(foreign);
        values.add(description);
        md.addChild("mailbox_alias", "", values);
    }

    public void deleteAlias(AuthToken at, String local, String domain) throws Exception {
        getMailAlias(Utils.getAccount(at), local, domain).FM_cdelete(0);
    }

    public Object[] getAliases(AuthToken at, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        Collection<ResourceId> col = md.getId().findChildren("mailbox_alias");
        Object[] result = new Object[col.size()];
        int counter = 0;
        for (ResourceId resourceId : col) {
            String[] alias = new String[3];
            MailAlias ma = (MailAlias) resourceId.get();
            alias[0] = ma.getLocal();
            alias[1] = ma.getForeign();
            alias[2] = ma.getDescription();
            int i = counter;
            counter++;
            result[i] = alias;
        }
        return result;
    }

    public void addMailForward(AuthToken at, String local, String domain, String foreign, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(local);
        values.add(foreign);
        values.add(description);
        md.addChild("mail_forward", "", values);
    }

    public void deleteMailForward(AuthToken at, String local, String domain) throws Exception {
        getMailForward(Utils.getAccount(at), local, domain).FM_cdelete(0);
    }

    public Object[] getMailForwards(AuthToken at, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        Collection<ResourceId> col = md.getId().findChildren("mail_forward");
        Object[] result = new Object[col.size()];
        int counter = 0;
        for (ResourceId resourceId : col) {
            Object[] alias = new Object[3];
            MailForward mf = (MailForward) resourceId.get();
            alias[0] = mf.getLocal();
            alias[1] = mf.getSubscribers().toArray();
            alias[2] = mf.getDescription();
            int i = counter;
            counter++;
            result[i] = alias;
        }
        return result;
    }

    public void addAutoResponder(AuthToken at, String local, String domain, String subject, String message, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(local);
        values.add("");
        values.add(subject);
        values.add(message);
        values.add(description);
        md.addChild("responder", "", values);
    }

    public void deleteAutoResponder(AuthToken at, String local, String domain) throws Exception {
        getAutoResponder(Utils.getAccount(at), local, domain).FM_cdelete(0);
    }

    public void updateAutoResponder(AuthToken at, String local, String domain, String subject, String message) throws Exception {
        updateAutoResponder(at, local, domain, subject, message, 0);
    }

    public void updateAutoResponder(AuthToken at, String local, String domain, String subject, String message, int includeIncoming) throws Exception {
        getAutoResponder(Utils.getAccount(at), local, domain).FM_updateResponder("", subject, message, includeIncoming);
    }

    public void setCatchAll(AuthToken at, String email, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        if (email == null || email.length() == 0) {
            md.FM_disableCatchAll();
        } else {
            md.FM_setCatchAll(email);
        }
    }

    private Collection getMailDomainsList(AuthToken at, String domainName) throws Exception {
        return Utils.getDomain(Utils.getAccount(at), domainName).getId().findChild("mail_service").findAllChildren("mail_domain");
    }

    private NamedParameter[] getObjectsForType(AuthToken at, String domainName, String type, NamedParameter[] pars) throws Exception {
        String name;
        String obj;
        TemplateList list = getTemplateForObject(at, domainName, type);
        NamedParameter[] namedParameterArr = pars;
        while (true) {
            NamedParameter[] params = namedParameterArr;
            if (list.hasNext()) {
                TemplateHashModel template = list.next();
                if (type.equalsIgnoreCase("mail_domain_alias")) {
                    name = "mail_domain_alias";
                    obj = template.get("fulllocal").toString();
                } else if (type.equalsIgnoreCase("responder")) {
                    name = "responder";
                    obj = template.get("fullemail").toString();
                } else if (type.equalsIgnoreCase("mailing_list")) {
                    name = "mailing_list";
                    obj = template.get("fullemail").toString();
                } else if (type.equalsIgnoreCase("mailbox")) {
                    name = "mailbox";
                    obj = template.get("fullemail").toString();
                } else if (type.equalsIgnoreCase("mail_forward")) {
                    name = "mail_forward";
                    obj = template.get("fullemail").toString();
                } else {
                    throw new Exception("Type -" + type + " not known");
                }
                String value = obj;
                namedParameterArr = NamedParameter.addParameter(name, value, params);
            } else {
                if (type.equalsIgnoreCase("mailbox")) {
                    String postmaster = getMailDomain(Utils.getAccount(at), domainName).getPostmaster();
                    params = NamedParameter.addParameter("mailbox", postmaster, params);
                }
                return params;
            }
        }
    }

    public NamedParameter[] getCurrentMailObjects(AuthToken at, String domainName) throws Exception {
        NamedParameter[] params = new NamedParameter[0];
        return getObjectsForType(at, domainName, "mail_forward", getObjectsForType(at, domainName, "mailbox", getObjectsForType(at, domainName, "mailing_list", getObjectsForType(at, domainName, "responder", getObjectsForType(at, domainName, "mail_domain_alias", params)))));
    }

    public Object getSMTPServer(AuthToken at, String domainName) throws Exception {
        String ms_name;
        MailDomain dom = getMailDomain(Utils.getAccount(at), domainName);
        if (dom == null) {
            throw new Exception("No such mail domain");
        }
        CNAMERecord cname = (CNAMERecord) dom.getParent().FM_getChild("cname_record").get();
        if (cname == null) {
            ms_name = dom.getParent().get().get("mail_server_name").toString();
        } else {
            ms_name = cname.get("name").toString();
        }
        return ms_name;
    }

    public Object getPOP3Server(AuthToken at, String domainName) throws Exception {
        return getSMTPServer(at, domainName);
    }

    public Object getMailTraffic(AuthToken at, String domainName) throws Exception {
        MailDomain dom = getMailDomain(Utils.getAccount(at), domainName);
        if (dom == null) {
            throw new Exception("No such mail domain");
        }
        Domain domain = (Domain) dom.getParent().get().get("parent").get();
        Traffic traffic = (Traffic) domain.FM_getChild("mail_traffic").get();
        traffic.reload();
        return traffic.get("text_traffic").toString();
    }

    private TemplateList getTemplateForObject(AuthToken at, String domainName, String type) throws Exception {
        MailDomain dom = getMailDomain(Utils.getAccount(at), domainName);
        if (dom == null) {
            throw new Exception("No such mail domain");
        }
        TemplateList template = null;
        if (type.equalsIgnoreCase("mail_alias")) {
            template = new TemplateList(dom.getChildHolder().getChildrenByName("mailbox_alias"));
        } else if (type.equalsIgnoreCase("responder")) {
            template = new TemplateList(dom.getChildHolder().getChildrenByName("responder"));
        } else if (type.equalsIgnoreCase("mailing_list")) {
            template = new TemplateList(dom.getChildHolder().getChildrenByName("mailing_list"));
        } else if (type.equalsIgnoreCase("mailbox")) {
            template = new TemplateList(dom.getChildHolder().getChildrenByName("mailbox"));
        } else if (type.equalsIgnoreCase("mail_forward")) {
            template = new TemplateList(dom.getChildHolder().getChildrenByName("mail_forward"));
        }
        return template;
    }

    private NamedParameter[] addIrisParam(String paramName, TemplateMap messages, NamedParameter[] params) {
        if (messages.get(paramName) != null) {
            return NamedParameter.addParameter(paramName, messages.get(paramName).toString(), params);
        }
        return params;
    }

    private NamedParameter[] getProperties(AuthToken at, String domainName, String type, String objName) throws Exception {
        TemplateList list = getTemplateForObject(at, domainName, type);
        NamedParameter[] params = new NamedParameter[0];
        while (list.hasNext()) {
            TemplateHashModel template = list.next();
            if (type.equalsIgnoreCase("mail_domain_alias")) {
                if (objName.equalsIgnoreCase(template.get("fulllocal").toString())) {
                    String local = template.get("local").toString();
                    NamedParameter[] params2 = NamedParameter.addParameter("local", local, params);
                    String foreign = template.get("foreign").toString();
                    NamedParameter[] params3 = NamedParameter.addParameter("foreign", foreign, params2);
                    String description = template.get("description").toString();
                    params = NamedParameter.addParameter("description", description, params3);
                }
            } else if (type.equalsIgnoreCase("responder")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String local2 = template.get("local").toString();
                    NamedParameter[] params4 = NamedParameter.addParameter("local", local2, params);
                    String message = template.get("message").toString();
                    params = NamedParameter.addParameter("message", message, params4);
                }
            } else if (type.equalsIgnoreCase("mailing_list")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String email = template.get("email").toString();
                    NamedParameter[] params5 = NamedParameter.addParameter("email", email, params);
                    String description2 = template.get("description").toString();
                    NamedParameter[] params6 = NamedParameter.addParameter("description", description2, params5);
                    String id = template.get("id").toString();
                    String type_id = template.get("type_id").toString();
                    MailingList mailingList = (MailingList) Utils.getAccount(at).FM_getResource(id + "_" + type_id).get();
                    TemplateList subscribers = mailingList.FM_list();
                    while (subscribers.hasNext()) {
                        TemplateString subscriber = subscribers.next();
                        params6 = NamedParameter.addParameter("subscriber", subscriber.toString(), params6);
                    }
                    TemplateList moderators = mailingList.FM_listMod();
                    while (moderators.hasNext()) {
                        TemplateString moderator = moderators.next();
                        params6 = NamedParameter.addParameter("moderator", moderator.toString(), params6);
                    }
                    String listowner = mailingList.FM_stat().get("owner").toString();
                    NamedParameter[] params7 = NamedParameter.addParameter("listowner", listowner, params6);
                    String listFlags = mailingList.FM_stat().get("flag").toString();
                    NamedParameter[] params8 = NamedParameter.addParameter("listflags", listFlags, params7);
                    TemplateMap messages = (TemplateMap) mailingList.FM_getIrisMessages();
                    params = addIrisParam("welcome", messages, addIrisParam("subject-prepend", messages, addIrisParam("message-header", messages, addIrisParam("message-footer", messages, addIrisParam("help-introduction", messages, addIrisParam("help-unsubscribe", messages, addIrisParam("help-commands", messages, addIrisParam("helpblurb", messages, addIrisParam("help", messages, addIrisParam("farewell", messages, addIrisParam("err-unsub-confirm-bad", messages, addIrisParam("err-sub-confirm-bad", messages, addIrisParam("err-private-list", messages, addIrisParam("err-not-subscribed", messages, addIrisParam("err-no-commands", messages, addIrisParam("err-name-missing", messages, addIrisParam("err-members-only", messages, addIrisParam("err-already-subscribed", messages, addIrisParam("confirm-unsubscribe", messages, addIrisParam("confirm-subscribe", messages, params8))))))))))))))))))));
                }
            } else if (type.equalsIgnoreCase("mailbox")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String email2 = template.get("email").toString();
                    NamedParameter[] params9 = NamedParameter.addParameter("email", email2, params);
                    String description3 = template.get("description").toString();
                    NamedParameter[] params10 = NamedParameter.addParameter("description", description3, params9);
                    String id2 = template.get("id").toString();
                    String type_id2 = template.get("type_id").toString();
                    Mailbox mailbox = (Mailbox) Utils.getAccount(at).FM_getResource(id2 + "_" + type_id2).get();
                    MailQuota mailQuota = (MailQuota) mailbox.FM_getChild("mail_quota").get();
                    String quota = mailQuota.get("usedMb").toString();
                    NamedParameter[] params11 = NamedParameter.addParameter("quota", quota, params10);
                    String password = template.get("password").toString();
                    params = NamedParameter.addParameter("password", password, params11);
                }
            } else if (type.equalsIgnoreCase("mail_forward")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String local3 = template.get("local").toString();
                    NamedParameter[] params12 = NamedParameter.addParameter("local", local3, params);
                    String description4 = template.get("description").toString();
                    NamedParameter[] params13 = NamedParameter.addParameter("description", description4, params12);
                    TemplateString subscriber2 = template.get("subscribers").next();
                    params = NamedParameter.addParameter("subscriber", subscriber2.toString(), params13);
                }
            } else {
                throw new Exception("Type -" + type + " not known");
            }
        }
        return params;
    }

    public NamedParameter[] getAliasProperties(AuthToken at, String domainName, String aliasName) throws Exception {
        return getProperties(at, domainName, "mail_domain_alias", aliasName);
    }

    public NamedParameter[] getAutoresponderProperties(AuthToken at, String domainName, String responderName) throws Exception {
        return getProperties(at, domainName, "responder", responderName);
    }

    public NamedParameter[] getListProperties(AuthToken at, String domainName, String listName) throws Exception {
        return getProperties(at, domainName, "mailing_list", listName);
    }

    public NamedParameter[] getBoxProperties(AuthToken at, String domainName, String boxName) throws Exception {
        return getProperties(at, domainName, "mailbox", boxName);
    }

    public NamedParameter[] getForwardProperties(AuthToken at, String domainName, String forwardName) throws Exception {
        return getProperties(at, domainName, "mail_forward", forwardName);
    }
}
