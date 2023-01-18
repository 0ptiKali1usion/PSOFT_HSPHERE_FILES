package psoft.util.freemarker;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.io.PrintWriter;
import java.io.StringWriter;

/* loaded from: hsphere.zip:psoft/util/freemarker/Template2String.class */
public class Template2String {
    public static String process(Template t, TemplateModel value) {
        SimpleHash root = new SimpleHash();
        root.put("root", value);
        return process(t, (TemplateModelRoot) root);
    }

    public static String process(Template t, TemplateModelRoot root) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        t.process(root, writer);
        writer.flush();
        writer.close();
        return out.toString();
    }
}
