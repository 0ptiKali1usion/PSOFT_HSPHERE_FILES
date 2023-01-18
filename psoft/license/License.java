package psoft.license;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.encryption.MD5;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/license/License.class */
public class License implements TemplateHashModel {
    protected Map values;
    protected boolean valid;

    public boolean isValid() {
        return this.valid;
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public String getValue(String key) {
        return (String) this.values.get(key);
    }

    public Date getIssued() throws Exception {
        String tmp_date = (String) this.values.get("ISSUED");
        long tmpDate = Long.parseLong(tmp_date);
        return new Date(tmpDate);
    }

    public boolean isLicenseExpired() throws Exception {
        String tmp_date = (String) this.values.get("ISSUED");
        long tmpDate = Long.parseLong(tmp_date);
        GregorianCalendar issuedCal = new GregorianCalendar();
        issuedCal.setTime(new Date(tmpDate));
        GregorianCalendar today = new GregorianCalendar();
        today.setTime(new Date());
        issuedCal.add(5, 30);
        if (today.after(issuedCal)) {
            return true;
        }
        return false;
    }

    public TemplateModel get(String key) {
        if ("valid".equals(key)) {
            if (isValid()) {
                return TemplateString.TRUE;
            }
            return TemplateString.FALSE;
        } else if ("ISSUED".equals(key)) {
            try {
                Date dateToFormat = getIssued();
                DateFormat df = DateFormat.getDateInstance(1);
                return new TemplateString(df.format(dateToFormat));
            } catch (Exception e) {
                return null;
            }
        } else if ("isLicenceExpired".equals(key)) {
            try {
                boolean isExpired = isLicenseExpired();
                if (isExpired) {
                    return new TemplateString("1");
                }
                return new TemplateString("0");
            } catch (Exception e2) {
                return null;
            }
        } else {
            Object val = this.values.get(key);
            if (val != null) {
                return new TemplateString(val);
            }
            return null;
        }
    }

    public License(String text) {
        update(text);
    }

    public void update(String text) {
        this.valid = false;
        this.values = new HashMap();
        if (text != null) {
            StringTokenizer st = new StringTokenizer(text, "\n");
            StringBuffer buf = new StringBuffer();
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                if (!line.startsWith("--")) {
                    int index = line.indexOf("=");
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    this.values.put(key, value);
                    if (!"LICENSE_KEY".equals(key) && !"CERTIFICATE".equals(key)) {
                        buf.append(value);
                    }
                }
            }
            buf.append("POSITIVE_GOD_TODAY");
            MD5 md5 = new MD5(buf.toString());
            this.valid = md5.asHex().equals(this.values.get("LICENSE_KEY"));
        }
    }
}
