package psoft.hsphere.admin.signupmanager;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.List;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplatePair;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/admin/signupmanager/TemplateSignupRecord.class */
public class TemplateSignupRecord implements TemplateHashModel {
    SignupRecord signupRecord;

    public TemplateSignupRecord(SignupRecord sRecord) {
        this.signupRecord = sRecord;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.signupRecord.getId());
        }
        if ("created".equals(key)) {
            return new TemplateString(this.signupRecord.getCreated());
        }
        if ("ip".equals(key)) {
            return new TemplateString(this.signupRecord.getIp());
        }
        if ("country".equals(key)) {
            return new TemplateString(this.signupRecord.getCountryCode());
        }
        if ("updated".equals(key)) {
            return new TemplateString(this.signupRecord.getUpdated());
        }
        if ("state".equals(key)) {
            return new TemplateString(this.signupRecord.getState());
        }
        if ("user_id".equals(key)) {
            return new TemplateString(this.signupRecord.getUserId());
        }
        if ("reseller_id".equals(key)) {
            return new TemplateString(this.signupRecord.getResellerId());
        }
        if ("msg".equals(key)) {
            return new TemplateString(this.signupRecord.getMsg());
        }
        if ("values".equals(key)) {
            List values = new ArrayList();
            if (this.signupRecord.getValues() != null) {
                for (String valueKey : this.signupRecord.getValues().keySet()) {
                    String value = (String) this.signupRecord.getValues().get(valueKey);
                    values.add(new TemplatePair(valueKey, value));
                }
            }
            return new TemplateList(values);
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
