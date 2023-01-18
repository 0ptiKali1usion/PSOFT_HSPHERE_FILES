package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import psoft.util.USFormat;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/MathMethod.class */
public class MathMethod implements TemplateMethodModel {
    public static final int ADD = 0;
    public static final int SUB = 1;
    public static final int MUL = 2;
    public static final int DIV = 3;
    public static final int MOD = 4;

    /* renamed from: GT */
    public static final int f263GT = 5;

    /* renamed from: LT */
    public static final int f264LT = 6;

    /* renamed from: GE */
    public static final int f265GE = 7;

    /* renamed from: LE */
    public static final int f266LE = 8;
    public static final int MAX = 9;
    public static final int MIN = 10;

    /* renamed from: EQ */
    public static final int f267EQ = 11;
    public static final int CEIL = 12;
    public static final int PERCENT = 13;
    public static final MathMethod add = new MathMethod(0);
    public static final MathMethod sub = new MathMethod(1);
    public static final MathMethod mul = new MathMethod(2);
    public static final MathMethod div = new MathMethod(3);
    public static final MathMethod mod = new MathMethod(4);

    /* renamed from: gt */
    public static final MathMethod f268gt = new MathMethod(5);

    /* renamed from: lt */
    public static final MathMethod f269lt = new MathMethod(6);

    /* renamed from: ge */
    public static final MathMethod f270ge = new MathMethod(7);

    /* renamed from: le */
    public static final MathMethod f271le = new MathMethod(8);
    public static final MathMethod max = new MathMethod(9);
    public static final MathMethod min = new MathMethod(10);

    /* renamed from: eq */
    public static final MathMethod f272eq = new MathMethod(11);
    public static final MathMethod ceil = new MathMethod(12);
    public static final MathMethod percent = new MathMethod(13);
    protected int type;

    public boolean isEmpty() {
        return false;
    }

    public MathMethod(int type) {
        this.type = type;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        String x;
        double c;
        try {
            Iterator i = HTMLEncoder.decode(l).iterator();
            switch (this.type) {
                case 0:
                    c = 0.0d;
                    while (i.hasNext()) {
                        c += getDouble((String) i.next());
                    }
                    break;
                case 1:
                    c = getDouble((String) i.next());
                    while (i.hasNext()) {
                        c -= getDouble((String) i.next());
                    }
                    break;
                case 2:
                    c = 1.0d;
                    while (i.hasNext()) {
                        c *= getDouble((String) i.next());
                    }
                    break;
                case 3:
                    c = getDouble((String) i.next());
                    while (i.hasNext()) {
                        c /= getDouble((String) i.next());
                    }
                    break;
                case 4:
                    c = getDouble((String) i.next());
                    while (i.hasNext()) {
                        c %= getDouble((String) i.next());
                    }
                    break;
                case 5:
                    c = getDouble((String) i.next());
                    if (i.hasNext()) {
                        if (c > getDouble((String) i.next())) {
                            return new TemplateString("1");
                        }
                        return new TemplateString("0");
                    }
                    break;
                case 6:
                    c = getDouble((String) i.next());
                    if (i.hasNext()) {
                        if (c < getDouble((String) i.next())) {
                            return new TemplateString("1");
                        }
                        return new TemplateString("0");
                    }
                    break;
                case 7:
                    c = getDouble((String) i.next());
                    if (i.hasNext()) {
                        if (c >= getDouble((String) i.next())) {
                            return new TemplateString("1");
                        }
                        return new TemplateString("0");
                    }
                    break;
                case 8:
                    c = getDouble((String) i.next());
                    if (i.hasNext()) {
                        if (c <= getDouble((String) i.next())) {
                            return new TemplateString("1");
                        }
                        return new TemplateString("0");
                    }
                    break;
                case 9:
                    c = getDouble((String) i.next());
                    while (i.hasNext()) {
                        c = Math.max(c, getDouble((String) i.next()));
                    }
                    break;
                case 10:
                    c = getDouble((String) i.next());
                    while (i.hasNext()) {
                        c = Math.min(c, getDouble((String) i.next()));
                    }
                    break;
                case 11:
                    c = getDouble((String) i.next());
                    if (i.hasNext()) {
                        if (c == getDouble((String) i.next())) {
                            return new TemplateString("1");
                        }
                        return new TemplateString("0");
                    }
                    break;
                case 12:
                    c = Math.ceil(getDouble((String) i.next()));
                    break;
                case 13:
                    c = Math.round((getDouble((String) i.next()) / getDouble((String) i.next())) * 100.0d);
                    break;
                default:
                    return null;
            }
            return new TemplateString(USFormat.format(c));
        } catch (Exception e) {
            Iterator i1 = l.iterator();
            String str = "";
            while (true) {
                x = str;
                if (!i1.hasNext()) {
                    break;
                }
                str = x + i1.next() + ":";
            }
            throw new TemplateModelException("Input :" + x);
        }
    }

    protected double getDouble(String str) {
        if (str == null || "".equals(str)) {
            return 0.0d;
        }
        try {
            return USFormat.parseDouble(str);
        } catch (ParseException e) {
            return 0.0d;
        }
    }
}
