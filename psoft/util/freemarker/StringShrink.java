package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.Iterator;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/StringShrink.class */
public class StringShrink implements TemplateMethodModel {
    public static final StringShrink shrink = new StringShrink();
    private static final String medium = "...";
    private static final int mediumLength = medium.length();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        try {
            Iterator iter = args.iterator();
            String str = (String) iter.next();
            int len = Integer.parseInt((String) iter.next());
            if (HTMLEncoder.isEncoded(str) == 1) {
                return new TemplateString(HTMLEncoder.encode(doShrink(HTMLEncoder.decode(str), len)));
            }
            return new TemplateString(doShrink(str, len));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String doShrink(String inpString, int maxLength) throws Exception {
        if (inpString == null) {
            return null;
        }
        int inpLength = inpString.length();
        if (inpLength <= maxLength) {
            return inpString;
        }
        if (maxLength <= mediumLength) {
            return medium;
        }
        int maxLength2 = maxLength - mediumLength;
        int end = maxLength2 / 2;
        int begin = maxLength2 - end;
        return inpString.substring(0, begin) + medium + inpString.substring(inpLength - end);
    }
}
