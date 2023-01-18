package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateString.class */
public class TemplateString implements TemplateScalarModel, TemplateMethodModel, Serializable {
    private static final long serialVersionUID = 1000;
    protected Object obj;
    public static final TemplateString TRUE = new TemplateString("1");
    public static final TemplateString FALSE = new TemplateString("0");
    protected static final DateFormat shortDf = DateFormat.getDateTimeInstance(3, 3);
    protected static final DateFormat mediumDf = DateFormat.getDateTimeInstance(2, 2);
    protected static final DateFormat fullDf = DateFormat.getDateTimeInstance(0, 0);
    protected static final DateFormat dShortDf = DateFormat.getDateInstance(3);
    protected static final DateFormat dMediumDf = DateFormat.getDateInstance(2);
    protected static final DateFormat dFullDf = DateFormat.getDateInstance(0);

    public TemplateModel exec(List l) {
        if (this.obj instanceof Date) {
            String key = (String) l.get(0);
            if ("TS_SHORT".equalsIgnoreCase(key)) {
                return new TemplateString(shortDf.format((Date) this.obj));
            }
            if ("TS_MEDIUM".equalsIgnoreCase(key)) {
                return new TemplateString(mediumDf.format((Date) this.obj));
            }
            if ("TS_FULL".equalsIgnoreCase(key)) {
                return new TemplateString(fullDf.format((Date) this.obj));
            }
            if ("SHORT".equalsIgnoreCase(key)) {
                return new TemplateString(dShortDf.format((Date) this.obj));
            }
            if ("MEDIUM".equalsIgnoreCase(key)) {
                return new TemplateString(dMediumDf.format((Date) this.obj));
            }
            if ("FULL".equalsIgnoreCase(key)) {
                return new TemplateString(dFullDf.format((Date) this.obj));
            }
            return new TemplateString(new SimpleDateFormat(key).format(this.obj));
        }
        return null;
    }

    public TemplateString(Object obj) {
        this.obj = obj;
    }

    public TemplateString(long i) {
        this(Long.toString(i));
    }

    public TemplateString(boolean i) {
        this.obj = i ? "1" : null;
    }

    public TemplateString(double i) {
        this(Double.toString(i));
    }

    public TemplateString(int i) {
        this(Integer.toString(i));
    }

    public TemplateString(float i) {
        this(Float.toString(i));
    }

    public TemplateString(char i) {
        this(new Character(i));
    }

    public boolean isEmpty() {
        return this.obj == null || this.obj.toString().length() == 0;
    }

    public String getAsString() {
        if (this.obj instanceof Date) {
            this.obj = DateFormat.getDateTimeInstance(2, 2).format((Date) this.obj);
        }
        return this.obj.toString();
    }

    public String toString() {
        if (null == this.obj) {
            return null;
        }
        return this.obj.toString();
    }

    public Object get() {
        return this.obj;
    }
}
