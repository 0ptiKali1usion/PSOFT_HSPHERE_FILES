package psoft.hsphere.resource.p004tt;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.tt.Message */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/Message.class */
public class Message implements TemplateHashModel {
    protected Date created;
    protected String msg;
    protected String note;
    protected int type;
    protected List encryptedParts;
    protected boolean hasEncryptedParts;
    protected List attachments = new ArrayList();
    protected TemplateList decryptedPhrases = new TemplateList();

    public Message(Date created, String msg, String note, int type) throws Exception {
        this.encryptedParts = null;
        this.msg = msg;
        if (containsEncryptTag(msg)) {
            this.hasEncryptedParts = true;
            this.encryptedParts = getEncryptedParts(Ticket.PGP_MESSAGE_BEGIN, Ticket.PGP_MESSAGE_END, msg);
        } else {
            this.hasEncryptedParts = false;
        }
        this.note = note;
        this.created = created;
        this.type = type;
    }

    public void add(Attachment a) {
        this.attachments.add(a);
    }

    public List getAttachments() {
        return this.attachments;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("message".equals(key)) {
            return new TemplateString(this.msg);
        }
        if ("note".equals(key)) {
            return new TemplateString(this.note);
        }
        if ("created".equals(key)) {
            return new TemplateString(this.created);
        }
        if ("type".equals(key)) {
            return new TemplateString(this.type);
        }
        if ("attachments".equals(key)) {
            return new TemplateList(this.attachments);
        }
        if ("HTMLmessage".equals(key)) {
            try {
                if (this.hasEncryptedParts) {
                    String clearedMsg = excludeText(Ticket.PGP_MESSAGE_BEGIN, Ticket.PGP_MESSAGE_END, this.msg);
                    return new TemplateString(toHTML(clearedMsg));
                }
                return new TemplateString(toHTML(this.msg));
            } catch (Exception e) {
                Session.getLog().warn("Failed to format a message. ", e);
                return null;
            }
        } else if ("HTMLnote".equals(key)) {
            try {
                return new TemplateString(toHTML(this.note));
            } catch (Exception e2) {
                Session.getLog().warn("Failed to format a note. ", e2);
                return null;
            }
        } else if ("HasEncryptedParts".equals(key)) {
            return this.hasEncryptedParts ? new TemplateString("true") : new TemplateString("false");
        } else if ("EncryptedParts".equals(key)) {
            if (this.decryptedPhrases.isEmpty()) {
                try {
                    this.decryptedPhrases = decryptEncryptedParts();
                } catch (Exception e3) {
                    Session.getLog().error("Faild to decrypt encrypted phrases ", e3);
                    return null;
                }
            }
            return this.decryptedPhrases;
        } else {
            return null;
        }
    }

    protected String toHTML(String str) throws Exception {
        String[] codes = {"&lt;", "&gt;", "&amp;", "&quot;", "<br>"};
        if (str == null || "".equals(str)) {
            return "";
        }
        StringBuffer res = new StringBuffer();
        int lastIndex = str.length();
        for (int i = 0; i < lastIndex; i++) {
            char ch = str.charAt(i);
            int index = "<>&\"\n".indexOf(ch);
            if (index == -1) {
                res.append(ch);
            } else {
                res.append(codes[index]);
            }
        }
        res.append("<br>\n");
        return res.toString();
    }

    protected boolean containsEncryptTag(String message) throws Exception {
        int searcher = message.indexOf(Ticket.PGP_MESSAGE_BEGIN);
        return searcher != -1;
    }

    protected String excludeText(String blockBegin, String blockEnd, String block) throws Exception {
        String result = new String("");
        int currentIndex = 0;
        int previewsIndex = 0;
        do {
            currentIndex = block.indexOf(blockBegin, currentIndex);
            if (currentIndex != -1) {
                result = result + block.substring(previewsIndex, currentIndex) + blockBegin + "secured information";
                currentIndex = block.indexOf(blockEnd, currentIndex);
            } else {
                result = result + block.substring(previewsIndex);
            }
            previewsIndex = currentIndex;
        } while (currentIndex != -1);
        return result;
    }

    private List getEncryptedParts(String blockBegin, String blockEnd, String block) throws Exception {
        List result = new ArrayList();
        int currentIndex = 0;
        do {
            int currentIndex2 = block.indexOf(blockBegin, currentIndex);
            if (currentIndex2 == -1) {
                return result;
            }
            currentIndex = block.indexOf(blockEnd, currentIndex2) + blockEnd.length();
            result.add(block.substring(currentIndex2, currentIndex));
        } while (currentIndex != -1);
        return result;
    }

    private TemplateList decryptEncryptedParts() throws Exception {
        TemplateList result = new TemplateList();
        if (this.hasEncryptedParts) {
            if (this.encryptedParts.isEmpty()) {
                this.encryptedParts = getEncryptedParts(Ticket.PGP_MESSAGE_BEGIN, Ticket.PGP_MESSAGE_END, this.msg);
            }
            for (String str : this.encryptedParts) {
                result.add(HsphereToolbox.toolbox.FM_formatForHTML(pgpDecode(str)));
            }
            return result;
        }
        return null;
    }

    private String pgpDecode(String sentence) throws Exception {
        String result;
        try {
            result = C0007PGPSecurity.decrypt(sentence + "\n");
        } catch (Exception e) {
            result = new String("Impossible to decrypt information\n\n" + sentence);
            Session.getLog().error("Error during information decryption", e);
        }
        return result;
    }
}
