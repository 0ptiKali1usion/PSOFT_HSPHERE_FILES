package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/JavaScriptEncoderMethod.class */
public class JavaScriptEncoderMethod implements TemplateMethodModel {
    public static final JavaScriptEncoderMethod encode = new JavaScriptEncoderMethod();

    public String encode(String str) {
        if (str == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '<') {
                buf.append("&lt;");
            } else if (ch == '>') {
                buf.append("&gt;");
            } else if (ch == '\n' || ch == '\r') {
                buf.append("\\n");
            } else if (ch == '\"') {
                buf.append("&quot;");
            } else {
                if (ch == '\\' || ch == '\"' || ch == '\'') {
                    buf.append('\\');
                }
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        return new TemplateString(encode(HTMLEncoder.decode((String) args.get(0))));
    }
}
