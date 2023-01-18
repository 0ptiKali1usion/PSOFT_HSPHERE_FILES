package psoft.hsphere;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.Iterator;
import java.util.List;
import psoft.util.freemarker.LingualScalar;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/HSLingualScalar.class */
public class HSLingualScalar extends LingualScalar implements TemplateMethodModel {
    public HSLingualScalar() {
    }

    protected HSLingualScalar(String key) {
        super(key);
    }

    @Override // psoft.util.freemarker.LingualScalar
    public TemplateModel get(String key) {
        if (this.buf == null) {
            return new HSLingualScalar(key);
        }
        this.buf.append(".").append(key);
        return this;
    }

    @Override // psoft.util.freemarker.LingualScalar
    public String getAsString() {
        return Localizer.translateLabel(this.buf.toString());
    }

    public TemplateModel exec(List list) {
        if (list == null) {
            return null;
        }
        boolean isEncoded = false;
        String[] args = new String[list.size()];
        int index = 0;
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            String arg = (String) iter.next();
            if (HTMLEncoder.isEncoded(arg) == 1) {
                arg = HTMLEncoder.decodeForcibly(arg);
                isEncoded = true;
            }
            int i = index;
            index++;
            args[i] = arg;
        }
        String res = Localizer.translateLabel(this.buf.toString(), args);
        return new TemplateString(isEncoded ? HTMLEncoder.encodeForcibly(res) : res);
    }
}
