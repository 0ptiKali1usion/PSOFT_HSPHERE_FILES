package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.text.SimpleDateFormat;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/util/freemarker/DateManager.class */
public class DateManager implements TemplateHashModel {
    public static DateManager dateman = new DateManager();
    protected static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("today".equals(key)) {
            return new TemplateString(DAY_FORMAT.format(TimeUtils.getDate()));
        }
        return null;
    }
}
