package psoft.hsphere;

import java.util.HashMap;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/hsphere/TypeCounter.class */
public class TypeCounter {
    protected Map map = new HashMap();

    public void inc(int _type, double delta) {
        Integer type = new Integer(_type);
        Double val = (Double) this.map.get(type);
        this.map.put(type, val == null ? new Double(delta) : new Double(val.doubleValue() + delta));
    }

    public void dec(int _type, double delta) {
        Integer type = new Integer(_type);
        Double val = (Double) this.map.get(type);
        if (val == null) {
            return;
        }
        double newVal = val.doubleValue() - delta;
        if (newVal <= 0.0d) {
            this.map.remove(type);
        } else {
            this.map.put(type, new Double(newVal));
        }
    }

    public void setValue(int type, double value) {
        this.map.put(new Integer(type), new Double(value));
    }

    public double getValue(int type) {
        Double val = (Double) this.map.get(new Integer(type));
        if (val == null) {
            return 0.0d;
        }
        return val.doubleValue();
    }

    public void clear() {
        this.map.clear();
    }

    public TypeCounter() {
    }

    public TypeCounter(TypeCounter t) {
        this.map.putAll(t.map);
    }
}
