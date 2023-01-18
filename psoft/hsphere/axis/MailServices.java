package psoft.hsphere.axis;

import freemarker.template.TemplateHashModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.dns.CNAMERecord;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.resource.email.AntiVirus;
import psoft.hsphere.resource.email.Autoresponder;
import psoft.hsphere.resource.email.MailAlias;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.MailDomainAlias;
import psoft.hsphere.resource.email.MailForward;
import psoft.hsphere.resource.email.MailQuota;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.email.MailingList;
import psoft.hsphere.resource.email.SPFResource;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/axis/MailServices.class */
public class MailServices {
    private static Category log = Category.getInstance(MailServices.class.getName());

    public String getDescription() {
        return "Functions to work with Mail";
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

    private MailDomain getMailDomain(Account a, String domainOrDomainAlias) throws Exception {
        MailDomainAlias mda;
        Resource r = null;
        MailDomain md = null;
        try {
            r = Utils.getDomain(a, domainOrDomainAlias);
        } catch (Exception e) {
        }
        if (r == null) {
            try {
                r = Utils.getDomainAlias(a, domainOrDomainAlias);
            } catch (Exception e2) {
            }
        }
        if (r == null) {
            throw new Exception("No such domain/domain alias: " + domainOrDomainAlias);
        }
        try {
            md = (MailDomain) r.getId().findChild("mail_service").findChild("mail_domain").get();
        } catch (Exception e3) {
        }
        if (md == null && (r instanceof DomainAlias) && (mda = (MailDomainAlias) r.getId().findChild("mail_domain_alias").get()) != null) {
            md = getMailDomain(a, mda.getDomainName());
        }
        if (md == null) {
            throw new Exception("Can't find mail domain : " + domainOrDomainAlias);
        }
        return md;
    }

    public void addMailbox(AuthToken at, String email, String domain, String password, String description) throws Exception {
        List values = new ArrayList();
        values.add(email);
        values.add(password);
        values.add(description);
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        md.addChild("mailbox", "", values);
        md.FM_getReportJoined(email);
    }

    public void deleteMailbox(AuthToken at, String email, String domain) throws Exception {
        getMailbox(Utils.getAccount(at), email, domain).FM_cdelete(0);
        getMailDomain(Utils.getAccount(at), domain).FM_getReportJoined("");
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
        Account a = Utils.getAccount(at);
        MailDomain md = getMailDomain(a, domain);
        Collection<ResourceId> col = md.getId().findChildren("mailbox");
        Object[] result = new Object[col.size() + 1];
        Object[] entry = new Object[4];
        entry[0] = "postmaster";
        entry[1] = "postmaster@" + domain;
        entry[2] = new Boolean(md.isDiscard());
        entry[3] = new Double(10.0d);
        int counter = 0 + 1;
        result[0] = entry;
        for (ResourceId resourceId : col) {
            Mailbox mb = (Mailbox) resourceId.get();
            Object[] entry2 = new Object[4];
            String local = mb.getEmail();
            entry2[0] = local;
            entry2[1] = local + "@" + domain;
            entry2[2] = new Boolean(mb.isDiscard());
            entry2[3] = new Double(mb.getId().findChild("mail_quota").get().getAmount());
            int i = counter;
            counter++;
            result[i] = entry2;
        }
        return result;
    }

    public void addMailingList(AuthToken at, String email, String domain, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(email);
        values.add(description);
        md.addChild("mailing_list", "", values);
        md.FM_getReportJoined(email);
    }

    public void deleteMailingList(AuthToken at, String email, String domain) throws Exception {
        MailingList ml = getMailingList(Utils.getAccount(at), email, domain);
        ml.FM_cdelete(0);
        getMailDomain(Utils.getAccount(at), domain).FM_getReportJoined("");
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

    public void setMailingListTrailer(AuthToken at, String email, String domain, String trailer) throws Exception {
        MailingList ml = getMailingList(Utils.getAccount(at), email, domain);
        if (trailer != null && trailer.length() > 0) {
            ml.FM_put_trailer(trailer);
        } else {
            ml.FM_del_trailer();
        }
    }

    public void addAlias(AuthToken at, String local, String domain, String foreign, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(local);
        values.add(foreign);
        values.add(description);
        md.addChild("mailbox_alias", "", values);
        md.FM_getReportJoined(local);
    }

    public void deleteAlias(AuthToken at, String local, String domain) throws Exception {
        getMailAlias(Utils.getAccount(at), local, domain).FM_cdelete(0);
        getMailDomain(Utils.getAccount(at), domain).FM_getReportJoined("");
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
            alias[2] = ma.getLocal() + "@" + domain;
            int i = counter;
            counter++;
            result[i] = alias;
        }
        return result;
    }

    public void addMailForward(AuthToken at, String local, String domain, String[] foreign, String description) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(local);
        values.add(foreign[0]);
        values.add(description);
        ResourceId rid = md.addChild("mail_forward", "", values);
        if (foreign.length > 1) {
            MailForward mf = (MailForward) rid.get();
            for (int i = 1; i < foreign.length; i++) {
                mf.addSubscriber(foreign[i]);
            }
        }
        md.FM_getReportJoined(local);
    }

    public void addMailForwardSubscribers(AuthToken at, String local, String domain, String[] foreign) throws Exception {
        MailForward mf = getMailForward(Utils.getAccount(at), local, domain);
        for (String str : foreign) {
            mf.addSubscriber(str);
        }
    }

    public void deleteMailForward(AuthToken at, String local, String domain) throws Exception {
        getMailForward(Utils.getAccount(at), local, domain).FM_cdelete(0);
        getMailDomain(Utils.getAccount(at), domain).FM_getReportJoined("");
    }

    public void deleteMailForwardSubscribers(AuthToken at, String local, String domain, String[] foreign) throws Exception {
        MailForward mf = getMailForward(Utils.getAccount(at), local, domain);
        for (String str : foreign) {
            mf.removeSubscriber(str);
        }
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
            alias[2] = mf.getLocal() + "@" + domain;
            int i = counter;
            counter++;
            result[i] = alias;
        }
        return result;
    }

    public void addAutoResponder(AuthToken at, String local, String domain, String foreign, String subject, String message, String description, int includeIncoming) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        List values = new ArrayList();
        values.add(local);
        values.add(foreign);
        values.add(subject);
        values.add(message);
        values.add(description);
        values.add(new Integer(includeIncoming).toString());
        md.addChild("responder", "", values);
    }

    public void addAutoResponder(AuthToken at, String local, String domain, String foreign, String subject, String message, String description) throws Exception {
        addAutoResponder(at, local, domain, foreign, subject, message, description, 0);
    }

    public void deleteAutoResponder(AuthToken at, String local, String domain) throws Exception {
        getAutoResponder(Utils.getAccount(at), local, domain).FM_cdelete(0);
        getMailDomain(Utils.getAccount(at), domain).FM_getReportJoined("");
    }

    public void updateAutoResponder(AuthToken at, String local, String domain, String foreign, String subject, String message) throws Exception {
        updateAutoResponder(at, local, domain, foreign, subject, message, 0);
    }

    public void updateAutoResponder(AuthToken at, String local, String domain, String foreign, String subject, String message, int includeIncoming) throws Exception {
        getAutoResponder(Utils.getAccount(at), local, domain).FM_updateResponder(foreign, subject, message, includeIncoming);
    }

    public void setCatchAll(AuthToken at, String email, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        if (email == null || email.length() == 0) {
            md.FM_disableCatchAll();
        } else {
            md.FM_setCatchAll(email);
        }
    }

    public Object[] getAutoResponders(AuthToken at, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        Collection<ResourceId> col = md.getId().findChildren("responder");
        Object[] result = new Object[col.size()];
        int counter = 0;
        for (ResourceId resourceId : col) {
            Object[] responder = new Object[6];
            Autoresponder ar = (Autoresponder) resourceId.get();
            responder[0] = ar.getLocal();
            responder[1] = ar.getLocal() + "@" + domain;
            responder[2] = ar.getSubject();
            responder[3] = ar.getMessage();
            responder[4] = new Integer(ar.getIncludeIncoming()).toString();
            responder[5] = ar.getAttachments();
            int i = counter;
            counter++;
            result[i] = responder;
        }
        return result;
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
                if (type.equalsIgnoreCase("mailbox_alias")) {
                    name = "mailbox_alias";
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
        return getObjectsForType(at, domainName, "mail_forward", getObjectsForType(at, domainName, "mailbox", getObjectsForType(at, domainName, "mailing_list", getObjectsForType(at, domainName, "responder", getObjectsForType(at, domainName, "mailbox_alias", params)))));
    }

    public String getSMTPServer(AuthToken at, String domainName) throws Exception {
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

    public String getPOP3Server(AuthToken at, String domainName) throws Exception {
        return getSMTPServer(at, domainName);
    }

    public String getMailTraffic(AuthToken at, String domainName) throws Exception {
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
        if (type.equalsIgnoreCase("mailbox_alias")) {
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
            if (type.equalsIgnoreCase("mailbox_alias")) {
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
                    NamedParameter[] params5 = NamedParameter.addParameter("message", message, params4);
                    String foreign2 = template.get("foreign").toString();
                    NamedParameter[] params6 = NamedParameter.addParameter("foreign", foreign2, params5);
                    String subject = template.get("subject").toString();
                    NamedParameter[] params7 = NamedParameter.addParameter("subject", subject, params6);
                    String include_incoming = template.get("include_incoming").toString();
                    params = NamedParameter.addParameter("include_incoming", include_incoming, params7);
                    String id = template.get("id").toString();
                    String type_id = template.get("type_id").toString();
                    Autoresponder ar = (Autoresponder) Utils.getAccount(at).FM_getResource(id + "_" + type_id).get();
                    TemplateList attachments = ar.FM_getAttachments();
                    while (attachments.hasNext()) {
                        TemplateMap attachment = attachments.next();
                        params = NamedParameter.addParameter("attachment_size", attachment.get("size").toString(), NamedParameter.addParameter("attachment_name", attachment.get("name").toString(), params));
                    }
                }
            } else if (type.equalsIgnoreCase("mailing_list")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String email = template.get("email").toString();
                    NamedParameter[] params8 = NamedParameter.addParameter("email", email, params);
                    String description2 = template.get("description").toString();
                    NamedParameter[] params9 = NamedParameter.addParameter("description", description2, params8);
                    String id2 = template.get("id").toString();
                    String type_id2 = template.get("type_id").toString();
                    MailingList mailingList = (MailingList) Utils.getAccount(at).FM_getResource(id2 + "_" + type_id2).get();
                    TemplateList subscribers = mailingList.FM_list();
                    while (subscribers.hasNext()) {
                        TemplateString subscriber = subscribers.next();
                        params9 = NamedParameter.addParameter("subscriber", subscriber.toString(), params9);
                    }
                    TemplateList moderators = mailingList.FM_listMod();
                    while (moderators.hasNext()) {
                        TemplateString moderator = moderators.next();
                        params9 = NamedParameter.addParameter("moderator", moderator.toString(), params9);
                    }
                    String trailer = mailingList.FM_get_trailer().toString();
                    NamedParameter[] params10 = NamedParameter.addParameter("trailer", trailer, params9);
                    String listowner = mailingList.FM_stat().get("owner").toString();
                    NamedParameter[] params11 = NamedParameter.addParameter("listowner", listowner, params10);
                    String listFlags = mailingList.FM_stat().get("flag").toString();
                    params = NamedParameter.addParameter("listflags", listFlags, params11);
                    if (!Session.getPropertyString("IRIS_USER").equals("")) {
                        TemplateMap messages = (TemplateMap) mailingList.FM_getIrisMessages();
                        params = addIrisParam("welcome", messages, addIrisParam("subject-prepend", messages, addIrisParam("message-header", messages, addIrisParam("message-footer", messages, addIrisParam("help-introduction", messages, addIrisParam("help-unsubscribe", messages, addIrisParam("help-commands", messages, addIrisParam("helpblurb", messages, addIrisParam("help", messages, addIrisParam("farewell", messages, addIrisParam("err-unsub-confirm-bad", messages, addIrisParam("err-sub-confirm-bad", messages, addIrisParam("err-private-list", messages, addIrisParam("err-not-subscribed", messages, addIrisParam("err-no-commands", messages, addIrisParam("err-name-missing", messages, addIrisParam("err-members-only", messages, addIrisParam("err-already-subscribed", messages, addIrisParam("confirm-unsubscribe", messages, addIrisParam("confirm-subscribe", messages, params))))))))))))))))))));
                    }
                }
            } else if (type.equalsIgnoreCase("mailbox")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String email2 = template.get("email").toString();
                    NamedParameter[] params12 = NamedParameter.addParameter("email", email2, params);
                    String description3 = template.get("description").toString();
                    NamedParameter[] params13 = NamedParameter.addParameter("description", description3, params12);
                    String id3 = template.get("id").toString();
                    String type_id3 = template.get("type_id").toString();
                    Mailbox mailbox = (Mailbox) Utils.getAccount(at).FM_getResource(id3 + "_" + type_id3).get();
                    MailQuota mailQuota = (MailQuota) mailbox.FM_getChild("mail_quota").get();
                    String quota = mailQuota.get("usedMb").toString();
                    NamedParameter[] params14 = NamedParameter.addParameter("quota", quota, params13);
                    String password = template.get("password").toString();
                    params = NamedParameter.addParameter("password", password, params14);
                }
            } else if (type.equalsIgnoreCase("mail_forward")) {
                if (objName.equalsIgnoreCase(template.get("fullemail").toString())) {
                    String local3 = template.get("local").toString();
                    NamedParameter[] params15 = NamedParameter.addParameter("local", local3, params);
                    String description4 = template.get("description").toString();
                    params = NamedParameter.addParameter("description", description4, params15);
                    TemplateList subscribers2 = template.get("subscribers");
                    while (subscribers2.hasNext()) {
                        TemplateString subscriber2 = subscribers2.next();
                        params = NamedParameter.addParameter("subscriber", subscriber2.toString(), params);
                    }
                }
            } else {
                throw new Exception("Type -" + type + " not known");
            }
        }
        return params;
    }

    public NamedParameter[] getAliasProperties(AuthToken at, String domainName, String aliasName) throws Exception {
        return getProperties(at, domainName, "mailbox_alias", aliasName);
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

    public void setMaildomainAntiSpamPreferences(AuthToken at, String domainName, String whiteList, String blackList, String spamLevel, String spamProcessing, String maxScore) throws Exception {
        MailDomain dom = null;
        Iterator i = Utils.getDomain(Utils.getAccount(at), domainName).getId().findChild("mail_service").getChildHolder().getChildrenByName("mail_domain").iterator();
        if (i.hasNext()) {
            while (i.hasNext()) {
                ResourceId mailServiceId = (ResourceId) i.next();
                dom = (MailDomain) mailServiceId.get();
                if (domainName.equals(dom.get("name").toString())) {
                    break;
                }
                dom = null;
            }
        } else {
            dom = getMailDomain(Utils.getAccount(at), domainName);
        }
        if (dom == null) {
            throw new Exception("No such mail domain");
        }
        dom.loadAntiSpamPreferences(dom.getId().getId());
        dom.setAntiSpamPreferences(whiteList, blackList, spamLevel, spamProcessing, maxScore);
    }

    protected AntiSpam addAntiSpam(Account a, String domainName, String local) throws Exception {
        ArrayList params = new ArrayList();
        params.add(local);
        ResourceId rid = getMailDomain(a, domainName).addChild("antispam", "", params);
        return (AntiSpam) rid.get();
    }

    public void setAntiSpamPreferences(AuthToken at, String domainName, String local, String whiteList, String blackList, String spamLevel, String spamProcessing, String maxScore) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        for (ResourceId resourceId : md.getId().findChildren("antispam")) {
            AntiSpam asp = (AntiSpam) resourceId.get();
            if (asp.getLocal().equalsIgnoreCase(local)) {
                asp.setAntiSpamPreferences(0, whiteList, blackList, spamLevel, spamProcessing, maxScore);
                return;
            }
        }
    }

    protected void changeUseMaildomainAntiSpamPrefs(AntiSpam as, boolean isUseMaildomainPrefs) throws Exception {
        if (isUseMaildomainPrefs) {
            as.onMailDomainPreferences("init");
        } else {
            as.offMailDomainPreferences();
        }
    }

    protected AntiVirus addAntiVirus(Account a, String domainName, String local) throws Exception {
        ArrayList params = new ArrayList();
        params.add(local);
        ResourceId rid = getMailDomain(a, domainName).addChild("antivirus", "", params);
        return (AntiVirus) rid.get();
    }

    public void setMaildomainAntiVirusPreferences(AuthToken at, String domainName, String virusProcessing) throws Exception {
        MailDomain dom = null;
        Iterator i = Utils.getDomain(Utils.getAccount(at), domainName).getId().findChild("mail_service").getChildHolder().getChildrenByName("mail_domain").iterator();
        if (i.hasNext()) {
            while (i.hasNext()) {
                ResourceId mailServiceId = (ResourceId) i.next();
                dom = (MailDomain) mailServiceId.get();
                if (domainName.equals(dom.get("name").toString())) {
                    break;
                }
                dom = null;
            }
        } else {
            dom = getMailDomain(Utils.getAccount(at), domainName);
        }
        if (dom == null) {
            throw new Exception("No such mail domain");
        }
        dom.loadAntiVirusPreferences(dom.getId().getId());
        dom.setAntiVirusPreferences(virusProcessing);
    }

    protected void changeUseMaildomainAntiVirusPrefs(AntiVirus av, boolean isUseMaildomainPrefs) throws Exception {
        if (isUseMaildomainPrefs) {
            av.onMailDomainPreferences("init");
        } else {
            av.offMailDomainPreferences();
        }
    }

    public void setAntiVirusPreferences(AuthToken at, String domainName, String local, String virusProcessing) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        for (ResourceId resourceId : md.getId().findChildren("antivirus")) {
            AntiVirus av = (AntiVirus) resourceId.get();
            if (av.getLocal().equalsIgnoreCase(local)) {
                av.setAntiVirusPreferences(0, virusProcessing);
                return;
            }
        }
    }

    public void addAntiVirusResources(AuthToken at, String domainName, String range) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        md.FM_addAntiVirusResources(range);
    }

    public Object[] getAntiViruses(AuthToken at, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        Collection<ResourceId> col = md.getId().findChildren("antivirus");
        Object[] result = new Object[col.size()];
        int counter = 0;
        for (ResourceId resourceId : col) {
            String[] antivirus = new String[3];
            AntiVirus av = (AntiVirus) resourceId.get();
            antivirus[0] = av.getLocal();
            antivirus[1] = av.getLocal() + "@" + domain;
            antivirus[2] = av.getVirusProcessing();
            int i = counter;
            counter++;
            result[i] = antivirus;
        }
        return result;
    }

    public void addAntiSpamResources(AuthToken at, String domainName, String range) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        md.FM_addAntiSpamResources(range);
    }

    public Object[] getAntiSpams(AuthToken at, String domain) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domain);
        Collection<ResourceId> col = md.getId().findChildren("antispam");
        Object[] result = new Object[col.size()];
        int counter = 0;
        for (ResourceId resourceId : col) {
            String[] antispam = new String[7];
            AntiSpam as = (AntiSpam) resourceId.get();
            antispam[0] = as.getLocal();
            antispam[1] = as.getLocal() + "@" + domain;
            antispam[2] = as.getPreferenceValue("whiteList");
            antispam[3] = as.getPreferenceValue("blackList");
            antispam[4] = as.getPreferenceValue("spamLevel");
            antispam[5] = as.getPreferenceValue("spamProcessing");
            antispam[6] = as.getPreferenceValue("spamMaxScore");
            int i = counter;
            counter++;
            result[i] = antispam;
        }
        return result;
    }

    public void deleteAntiSpamResources(AuthToken at, String domainName) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        for (ResourceId resourceId : md.getId().findChildren("antispam")) {
            resourceId.get().FM_cdelete(0);
        }
    }

    public void deleteAntiVirusResources(AuthToken at, String domainName) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        for (ResourceId resourceId : md.getId().findChildren("antivirus")) {
            resourceId.get().FM_cdelete(0);
        }
    }

    public void addSPF(AuthToken at, String domainName, String processing) throws Exception {
        MailService ms = (MailService) getMailDomain(Utils.getAccount(at), domainName).getParent().get();
        List values = new ArrayList();
        values.add(processing);
        ms.addChild("spf", "", values);
    }

    public void deleteSPF(AuthToken at, String domainName) throws Exception {
        MailService ms = (MailService) getMailDomain(Utils.getAccount(at), domainName).getParent().get();
        SPFResource spf = (SPFResource) ms.getId().findChild("spf").get();
        spf.FM_cdelete(0);
    }

    public void enableMailService(AuthToken at, String domainName) throws Exception {
        Domain domain = Utils.getDomain(Utils.getAccount(at), domainName);
        domain.addChild("mail_service", "", new ArrayList());
    }

    public void disableMailService(AuthToken at, String domainName) throws Exception {
        Domain domain = Utils.getDomain(Utils.getAccount(at), domainName);
        MailService ms = (MailService) domain.getId().findChild("mail_service").get();
        ms.FM_cdelete(0);
    }

    public String getCatchAll(AuthToken at, String domainName) throws Exception {
        MailDomain md = getMailDomain(Utils.getAccount(at), domainName);
        return md.getCatchAll();
    }
}
