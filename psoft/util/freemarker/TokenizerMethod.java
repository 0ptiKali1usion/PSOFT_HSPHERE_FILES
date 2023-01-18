package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/TokenizerMethod.class */
public class TokenizerMethod implements TemplateMethodModel {
    public static TokenizerMethod tokenizer = new TokenizerMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        if (args != null && args.size() > 0) {
            String str = (String) args.get(0);
            if (str != null) {
                boolean isEncoded = false;
                if (HTMLEncoder.isEncoded(str) == 1) {
                    str = HTMLEncoder.decode(str);
                    isEncoded = true;
                }
                try {
                    StringTokenizer st = args.size() == 1 ? new StringTokenizer(str) : new StringTokenizer(str, HTMLEncoder.decode((String) args.get(1)));
                    ArrayList list = new ArrayList();
                    while (st.hasMoreTokens()) {
                        String tmp = isEncoded ? HTMLEncoder.encode(st.nextToken()) : st.nextToken();
                        list.add(tmp);
                    }
                    return new TemplateList(list);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
        return null;
    }
}
