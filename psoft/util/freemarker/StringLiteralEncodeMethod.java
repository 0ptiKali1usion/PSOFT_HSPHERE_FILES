package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/StringLiteralEncodeMethod.class */
public class StringLiteralEncodeMethod implements TemplateMethodModel {
    public static final StringLiteralEncodeMethod encode = new StringLiteralEncodeMethod();

    public String encode(String str) {
        if (str == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\' || ch == '\r' || ch == '\"' || ch == '\'') {
                buf.append('\\');
            }
            buf.append(ch);
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
