package psoft.hsphere.resource.mpf.hostedexchange;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Localizer;
import psoft.hsphere.report.DataComparator;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/HePlanSettings.class */
public class HePlanSettings implements TemplateModel, TemplateHashModel {
    private String planName;
    private String planDescription;
    public static HashMap owaSegmentProp = new HashMap();
    private HashMap planFeatures = new HashMap();

    /* renamed from: dc */
    protected DataComparator f205dc = new DataComparator();

    static {
        owaSegmentProp.put("tierMessaging", new Long(1L));
        owaSegmentProp.put("tierCalendar", new Long(2L));
        owaSegmentProp.put("tierContacts", new Long(4L));
        owaSegmentProp.put("tierTasks", new Long(8L));
        owaSegmentProp.put("tierJournal", new Long(16L));
        owaSegmentProp.put("tierStickyNotes", new Long(32L));
        owaSegmentProp.put("tierPublicFolders", new Long(64L));
        owaSegmentProp.put("tierReminders", new Long(128L));
        owaSegmentProp.put("tierNewMail", new Long(256L));
        owaSegmentProp.put("tierRichClient", new Long(512L));
        owaSegmentProp.put("tierSpellCheck", new Long(1024L));
        owaSegmentProp.put("tierSMime", new Long(2048L));
        owaSegmentProp.put("tierSearchFolders", new Long(4096L));
        owaSegmentProp.put("tierSignature", new Long(8192L));
        owaSegmentProp.put("tierRules", new Long(16384L));
        owaSegmentProp.put("tierThemes", new Long(32768L));
        owaSegmentProp.put("tierJunkEmail", new Long(65536L));
        owaSegmentProp.put("tierAll", new Long(-1L));
    }

    public HePlanSettings(Node plan) throws Exception {
        this.planName = XPathAPI.selectSingleNode(plan, "planName/text()").getNodeValue();
        this.planDescription = XPathAPI.selectSingleNode(plan, "planDescription/text()").getNodeValue();
        NodeList features = XPathAPI.selectNodeList(plan, "planFeatures/feature");
        for (int i = 0; i < features.getLength(); i++) {
            Node feature = features.item(i);
            HashMap map = new HashMap();
            map.put("description", XPathAPI.selectSingleNode(feature, "featureDescription/text()").getNodeValue());
            if (XPathAPI.selectSingleNode(feature, "featureValue") != null) {
                map.put("value", XPathAPI.selectSingleNode(feature, "featureValue/text()").getNodeValue());
            }
            this.planFeatures.put(XPathAPI.selectSingleNode(feature, "featureName/text()").getNodeValue(), map);
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(this.planName);
        }
        if ("description".equals(key)) {
            return new TemplateString(this.planDescription != null ? this.planDescription : "");
        } else if ("MailboxSize".equals(key)) {
            String size = (String) ((HashMap) this.planFeatures.get("MailboxSize")).get("value");
            return new TemplateString(size);
        } else if ("featuresSet".equals(key)) {
            ArrayList detailedSet = new ArrayList();
            for (String name : this.planFeatures.keySet()) {
                HashMap map = new HashMap();
                String value = (String) ((HashMap) this.planFeatures.get(name)).get("value");
                if ("OWA".equals(name) || "OMA".equals(name) || "POP".equals(name) || "IMAP".equals(name)) {
                    if ("1".equals(value)) {
                    }
                } else {
                    map.put("value", value);
                    map.put("description", ((HashMap) this.planFeatures.get(name)).get("description"));
                }
                map.put(MerchantGatewayManager.MG_KEY_PREFIX, name);
                map.put("name", Localizer.translateLabel("label.msexchange.features." + name));
                detailedSet.add(map);
            }
            this.f205dc.setConstrain("name", true);
            Collections.sort(detailedSet, this.f205dc);
            return new TemplateList(detailedSet);
        } else {
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public TemplateModel FM_getOwaSegmentDescr(String bitMask) {
        new ArrayList();
        TemplateList list = new TemplateList();
        long bitmask = Long.parseLong(bitMask);
        for (String prop : owaSegmentProp.keySet()) {
            long val = ((Long) owaSegmentProp.get(prop)).longValue();
            if ((bitmask & val) == val) {
                list.add(Localizer.translateLabel("label.msexchange.features.owa_segment_" + prop));
            }
        }
        return list;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String toString() {
        return "Plan Name: " + this.planName + " Plan Description " + this.planDescription + " plan Features: " + this.planFeatures;
    }
}
