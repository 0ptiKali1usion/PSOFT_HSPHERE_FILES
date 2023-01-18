package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateHashComparator.class */
public class TemplateHashComparator implements Comparator {
    protected List sortParams = new ArrayList();
    protected int reverse;

    public TemplateHashComparator(List sortParams, boolean isReverse) {
        this.sortParams.addAll(sortParams);
        this.reverse = isReverse ? -1 : 1;
    }

    @Override // java.util.Comparator
    public int compare(Object o, Object o1) {
        int result = 0;
        boolean compared = false;
        boolean isMap = false;
        boolean isTempHash = false;
        if ((o instanceof TemplateHashModel) && (o1 instanceof TemplateHashModel)) {
            isTempHash = true;
        }
        if ((o instanceof Map) && (o1 instanceof Map)) {
            isMap = true;
        }
        if (isTempHash || isMap) {
            Iterator i = this.sortParams.iterator();
            while (i.hasNext() && !compared) {
                String key = (String) i.next();
                Object value1 = null;
                if (isTempHash) {
                    try {
                        value1 = ((TemplateHashModel) o).get(key);
                    } catch (TemplateModelException e) {
                        e.printStackTrace();
                    }
                } else if (isMap) {
                    value1 = ((Map) o).get(key);
                }
                Object value2 = null;
                if (isTempHash) {
                    try {
                        value2 = ((TemplateHashModel) o1).get(key);
                    } catch (TemplateModelException e2) {
                        e2.printStackTrace();
                    }
                } else if (isMap) {
                    value2 = ((Map) o1).get(key);
                }
                result = sipleCompare(value1, value2);
                if (result != 0) {
                    compared = true;
                }
            }
        }
        if (!compared) {
            result = sipleCompare(o, o1);
        }
        return result * this.reverse;
    }

    private int sipleCompare(Object o, Object o1) {
        int result;
        if (o == null) {
            if (o1 == null) {
                return 0;
            }
            return -1;
        }
        if ((o instanceof Comparable) && (o1 instanceof Comparable)) {
            result = ((Comparable) o).compareTo(o1);
        } else {
            result = o.toString().compareTo(o1.toString());
        }
        return result;
    }
}
