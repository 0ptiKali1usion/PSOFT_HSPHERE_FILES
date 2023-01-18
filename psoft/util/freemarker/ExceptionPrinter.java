package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/* loaded from: hsphere.zip:psoft/util/freemarker/ExceptionPrinter.class */
public class ExceptionPrinter implements TemplateMethodModel, TemplateScalarModel {

    /* renamed from: e */
    Exception f259e;
    String msg;
    String stack;

    public ExceptionPrinter(Exception e, String msg) {
        this.f259e = e;
        this.msg = msg;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        this.stack = sw.toString();
    }

    public boolean isEmpty() {
        return false;
    }

    public String getAsString() {
        return "<hr><b>Exception</b> -- " + this.msg + "<br>\n<pre> " + this.stack + "</pre>\n<hr>\n";
    }

    public TemplateModel exec(List args) {
        return new SimpleScalar(getAsString());
    }
}
