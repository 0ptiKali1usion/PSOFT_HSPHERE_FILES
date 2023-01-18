package psoft.hsphere.migrator.extractor;

import java.util.Collection;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.hsphere.resource.email.Autoresponder;
import psoft.hsphere.resource.email.MailAlias;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.MailForward;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.email.MailingList;
import psoft.util.Base64;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/extractor/MailServiceExtractor.class */
public class MailServiceExtractor extends InfoExtractorUtils {
    private static Category log = Category.getInstance(MailServiceExtractor.class.getName());
    private boolean force;

    public MailServiceExtractor(Document doc, boolean force) {
        super(doc);
        this.force = force;
    }

    public Element getAllMailServiceInfoAsXML(Domain domain) throws Exception {
        return getDomainMailService(domain);
    }

    public Element getDomainAliasMailService(DomainAlias alias) throws Exception {
        Element node = createNode("mailservice");
        MailDomain mailDom = getMailService(alias);
        getMailServiceObjects(node, mailDom);
        return checkChildren(node);
    }

    public Element getDomainMailService(Domain domain) throws Exception {
        Element node = createNode("mailservice");
        MailDomain mailDom = getMailService(domain);
        getMailServiceObjects(node, mailDom);
        return checkChildren(node);
    }

    private void getMailServiceObjects(Element parent, MailDomain mailDom) throws Exception {
        if (mailDom == null) {
            return;
        }
        try {
            String catchAll = mailDom.get("catchAll").toString();
            if (catchAll.trim().length() > 0) {
                parent.setAttributeNode(createAttribute("catchall", catchAll));
            }
            getAutoresponders(parent, mailDom);
            getMailboxes(parent, mailDom);
            getMaillists(parent, mailDom);
            getMailforwards(parent, mailDom);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
    }

    private MailDomain getMailService(Domain domain) throws Exception {
        ResourceId mid;
        MailDomain mailDom = null;
        try {
            ResourceId rid = domain.getId().findChild("mail_service");
            if (rid != null && (mid = rid.findChild("mail_domain")) != null) {
                mailDom = (MailDomain) mid.get();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return mailDom;
    }

    private MailDomain getMailService(DomainAlias alias) throws Exception {
        ResourceId mid;
        MailDomain mailDom = null;
        try {
            ResourceId rid = alias.getId().findChild("mail_service");
            if (rid != null && (mid = rid.findChild("mail_domain")) != null) {
                mailDom = (MailDomain) mid.get();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return mailDom;
    }

    private void getMailAliases(Element node, MailDomain mailDomain, Mailbox mailbox) throws Exception {
        try {
            Collection<ResourceId> mailAliases = mailDomain.getId().findChildren("mailbox_alias");
            for (ResourceId rid : mailAliases) {
                MailAlias alias = (MailAlias) rid.get();
                if (mailbox.getEmail().equals(alias.getForeign())) {
                    Element mailNode = createNode("mailalias");
                    mailNode.setAttributeNode(createAttribute("foreign", alias.getForeign()));
                    node.appendChild(mailNode);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
    }

    private void getMailboxes(Element parent, MailDomain domain) throws Exception {
        Collection<ResourceId> mailboxes = domain.getId().findChildren("mailbox");
        for (ResourceId rid : mailboxes) {
            Mailbox mailbox = (Mailbox) Mailbox.get(rid);
            appendChildNode(parent, getMailbox(domain, mailbox));
        }
    }

    private void getMailforwards(Element parent, MailDomain domain) throws Exception {
        Collection<ResourceId> mailforwards = domain.getId().findChildren("mail_forward");
        for (ResourceId rid : mailforwards) {
            MailForward forward = (MailForward) MailForward.get(rid);
            appendChildNode(parent, getMailforward(forward));
        }
    }

    private void getMaillists(Element parent, MailDomain domain) throws Exception {
        Collection<ResourceId> maillists = domain.getId().findChildren("mailing_list");
        for (ResourceId rid : maillists) {
            MailingList maillist = (MailingList) MailingList.get(rid);
            appendChildNode(parent, getMaillist(maillist));
        }
    }

    private Element getMailbox(MailDomain domain, Mailbox mailbox) throws Exception {
        Element node = createNode("mailbox");
        try {
            node.setAttributeNode(createAttribute("name", mailbox.getEmail()));
            node.setAttributeNode(createAttribute("password", mailbox.getPassword()));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        getMailAliases(node, domain, mailbox);
        if (isEmptyWebmaster(mailbox, node)) {
            return null;
        }
        return checkChildren(node);
    }

    private boolean isEmptyWebmaster(Mailbox mailbox, Node node) {
        if ("webmaster".equals(mailbox.getEmail())) {
            int ch = node.getChildNodes().getLength();
            if (ch == 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void getAutoresponders(Element parent, MailDomain domain) throws Exception {
        Collection<ResourceId> reponders = domain.getId().findChildren("responder");
        for (ResourceId rid : reponders) {
            appendChildNode(parent, getAutoresponder(rid));
        }
    }

    private Element getAutoresponder(ResourceId rid) throws Exception {
        Element node = null;
        try {
            Autoresponder responder = (Autoresponder) rid.get();
            node = createNode("autoresponder", Base64.encode(responder.getMessage().trim()));
            node.setAttributeNode(createAttribute("local", responder.getLocal()));
            node.setAttributeNode(createAttribute("subject", Base64.encode(responder.getSubject())));
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getSubscriber(String subscriber) throws Exception {
        Element node = createNode("subscriber");
        node.setAttributeNode(createAttribute("email", subscriber));
        return checkChildren(node);
    }

    private Element getModerator(String moderator) throws Exception {
        Element node = createNode("moderator");
        node.setAttributeNode(createAttribute("email", moderator));
        return checkChildren(node);
    }

    private Element getMailforward(MailForward forward) throws Exception {
        Element node = createNode("forward");
        if (forward == null) {
            return null;
        }
        try {
            node.setAttributeNode(createAttribute("name", forward.getLocal()));
            for (Object obj : forward.getSubscribers()) {
                appendChildNode(node, getSubscriber(obj.toString()));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }

    private Element getMaillist(MailingList mailingList) throws Exception {
        Element node = createNode("maillist");
        if (mailingList == null) {
            return null;
        }
        try {
            node.setAttributeNode(createAttribute("name", mailingList.getEmail()));
            TemplateList subscrs = mailingList.FM_list();
            while (subscrs.hasNext()) {
                appendChildNode(node, getSubscriber(subscrs.next().toString()));
            }
            TemplateList moders = mailingList.FM_listMod();
            while (moders.hasNext()) {
                appendChildNode(node, getModerator(moders.next().toString()));
            }
            Element trailer = createNode("messagetrailer", mailingList.FM_get_trailer().toString());
            appendChildNode(node, trailer);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (!this.force) {
                throw exc;
            }
        }
        return checkChildren(node);
    }
}
