package psoft.hsphere.admin.signupmanager;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/admin/signupmanager/RequestSignupRecordWrapper.class */
public class RequestSignupRecordWrapper {
    HttpServletRequest request;

    /* renamed from: sr */
    SignupRecord f64sr = null;

    public RequestSignupRecordWrapper(HttpServletRequest request) {
        this.request = request;
    }

    public SignupRecord process() {
        long srId = getRequestSignupId(getRequest());
        this.f64sr = new SimpleSignupRecord(srId, getResellerId());
        this.f64sr.setIp(this.request.getRemoteAddr());
        this.f64sr.setCountryCode(SignupManager.getCountryCode(this.f64sr.getIp()));
        Date now = TimeUtils.getDate();
        this.f64sr.setCreated(now);
        this.f64sr.setState(0);
        this.f64sr.setUpdated(now);
        Map values = new Hashtable();
        Enumeration en = this.request.getParameterNames();
        while (en.hasMoreElements()) {
            String name = (String) en.nextElement();
            String val = this.request.getParameter(name);
            if ("_bi_cc_number".equals(name)) {
                val = GenericCreditCard.getHiddenNumber(val);
            } else if ("_bi_cc_cvv".equals(name)) {
                val = GenericCreditCard.getHiddenCVV(val);
            }
            if (val != null) {
                if (val.length() > 255) {
                    val = val.substring(0, 255);
                }
            } else {
                val = "";
            }
            values.put(name, val);
        }
        this.f64sr.setValues(values);
        return this.f64sr;
    }

    protected HttpServletRequest getRequest() {
        return this.request;
    }

    protected long getResellerId() {
        try {
            return Session.getResellerId();
        } catch (UnknownResellerException e) {
            Session.getLog().error("Unable to get ResellerId", e);
            return 1L;
        }
    }

    protected long getRequestSignupId(HttpServletRequest request) {
        String tmpSignupId = request.getParameter("signup_id");
        int signupId = -1;
        if (tmpSignupId != null) {
            try {
                signupId = Integer.parseInt(tmpSignupId);
            } catch (Exception e) {
            }
        }
        return signupId;
    }
}
