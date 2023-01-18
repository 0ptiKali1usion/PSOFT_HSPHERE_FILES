package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/StringComposer.class */
public class StringComposer implements TemplateMethodModel {
    public static final StringComposer compose = new StringComposer();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        try {
            Object[] argv = new Object[args.size() - 1];
            Iterator iter = args.iterator();
            String msg = (String) iter.next();
            int i = 0;
            boolean isEncoded = false;
            while (iter.hasNext()) {
                String elem = (String) iter.next();
                if (HTMLEncoder.isEncoded(elem) == 1) {
                    isEncoded = true;
                    elem = HTMLEncoder.decodeForcibly(elem);
                }
                int i2 = i;
                i++;
                argv[i2] = elem != null ? elem : "";
            }
            String res = MessageFormat.format(msg, argv);
            return new TemplateString(isEncoded ? HTMLEncoder.encode(res) : res);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
