package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.Arrays;
import java.util.Hashtable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/HtmlEncodedRequest.class */
public class HtmlEncodedRequest extends HttpServletRequestWrapper implements TemplateHashModel {
    Hashtable encodedRequest;

    public HtmlEncodedRequest(HttpServletRequest request) {
        super(request);
        this.encodedRequest = new Hashtable();
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        String[] value;
        if (key != null && !"".equals(key) && (value = getParameterValues(key)) != null) {
            if (value.length == 1) {
                return new TemplateString(value[0]);
            }
            return new TemplateList(Arrays.asList(value));
        }
        return null;
    }

    public String[] getParameterValues(String name) {
        String[] encodedValues = (String[]) this.encodedRequest.get(name);
        if (encodedValues == null) {
            String[] paramValues = super.getParameterValues(name);
            if (paramValues == null) {
                return null;
            }
            encodedValues = new String[paramValues.length];
            for (int i = 0; i < paramValues.length; i++) {
                encodedValues[i] = HTMLEncoder.encode(paramValues[i]);
            }
            this.encodedRequest.put(name, encodedValues);
        }
        return encodedValues;
    }
}
