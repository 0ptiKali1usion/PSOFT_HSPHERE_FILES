package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/SubstringMethod.class */
public class SubstringMethod implements TemplateMethodModel {
    public static SubstringMethod substring = new SubstringMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        String str;
        String res;
        String res2;
        try {
            if (args.size() == 2) {
                String str2 = (String) args.get(0);
                if (str2 != null) {
                    int from = Integer.parseInt((String) args.get(1));
                    if (HTMLEncoder.isEncoded(str2) == 1) {
                        res2 = HTMLEncoder.encode(HTMLEncoder.decode(str2).substring(from));
                    } else {
                        res2 = str2.substring(from);
                    }
                    return new TemplateString(res2);
                }
                return null;
            } else if (args.size() == 3 && (str = (String) args.get(0)) != null) {
                int from2 = Integer.parseInt((String) args.get(1));
                int to = Integer.parseInt((String) args.get(2));
                if (HTMLEncoder.isEncoded(str) == 1) {
                    res = HTMLEncoder.encode(HTMLEncoder.decode(str).substring(from2, to));
                } else {
                    res = str.substring(from2, to);
                }
                return new TemplateString(res);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
