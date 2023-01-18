package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/SimpleCounter.class */
public class SimpleCounter implements TemplateListModel, TemplateHashModel, TemplateScalarModel, Serializable, TemplateMethodModel {
    public static SimpleCounter counter = new SimpleCounter();
    private static final long serialVersionUID = 1014;
    protected int start;
    protected int end;
    protected int inc;
    protected int current;

    public TemplateModel get(int index) {
        return null;
    }

    public TemplateModel exec(List l) {
        List l2 = HTMLEncoder.decode(l);
        int start = 0;
        int end = Integer.MAX_VALUE;
        int inc = 1;
        if (null != l2) {
            Iterator i = l2.iterator();
            try {
                if (i.hasNext()) {
                    start = Integer.parseInt(i.next().toString());
                }
                if (i.hasNext()) {
                    end = Integer.parseInt(i.next().toString());
                }
                if (i.hasNext()) {
                    inc = Integer.parseInt(i.next().toString());
                }
            } catch (NullPointerException e) {
            }
        }
        return new SimpleCounter(start, end, inc);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (!key.equals("next")) {
            if (!key.equals("prev")) {
                if (!key.equals("restart")) {
                    if (!key.equals("reset")) {
                        if (!key.equals("stop")) {
                            if (key.equals("value")) {
                                return new SimpleScalar(getAsString());
                            }
                            return null;
                        }
                        this.current = this.end;
                        return null;
                    }
                    rewind();
                    return null;
                }
                rewind();
                return null;
            }
            prev();
            return null;
        }
        next();
        return null;
    }

    public String getAsString() throws TemplateModelException {
        return Integer.toString(this.current);
    }

    public SimpleCounter() {
        this(0);
    }

    public SimpleCounter(int start) {
        this(start, Integer.MAX_VALUE);
    }

    public SimpleCounter(int start, int end) {
        this(start, end, 1);
    }

    public SimpleCounter(int start, int end, int inc) {
        this.start = start;
        this.end = end;
        this.inc = inc;
        this.current = start;
    }

    public void rewind() throws TemplateModelException {
        this.current = this.start;
    }

    public boolean isRewound() throws TemplateModelException {
        return this.current == this.start;
    }

    public boolean hasNext() throws TemplateModelException {
        return this.current < this.end;
    }

    public TemplateModel next() throws TemplateModelException {
        this.current += this.inc;
        return this;
    }

    public TemplateModel prev() throws TemplateModelException {
        this.current -= this.inc;
        return this;
    }

    public boolean isEmpty() throws TemplateModelException {
        return this.start >= this.end;
    }
}
