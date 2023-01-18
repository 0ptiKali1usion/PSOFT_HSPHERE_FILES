package psoft.hsphere.cron;

import java.lang.reflect.Constructor;
import psoft.hsphere.C0004CP;

/* loaded from: hsphere.zip:psoft/hsphere/cron/CronStructure.class */
public class CronStructure {
    private String name;
    private String cClass;
    private long period;
    private Object cObject;
    private int disabled = 0;
    private int priority = 5;

    public CronStructure(String name, String cClass, long period) throws Exception {
        this.name = name;
        this.cClass = cClass;
        this.period = period * 60000;
    }

    public String getCronName() {
        return this.name;
    }

    public String getCronClass() {
        return this.cClass;
    }

    public long getCronPeriod() {
        return this.period / 60000;
    }

    public void setCronPeriod(long period) {
        this.period = period * 60000;
    }

    public int getCronPriority() {
        return this.priority;
    }

    public void setCronPriority(int priority) {
        this.priority = priority;
    }

    public int getCronDisabled() {
        return this.disabled;
    }

    public void setCronDisabled(int disabled) {
        this.disabled = disabled;
    }

    public Object getCronObject(C0004CP cp) throws Exception {
        Class cron = Class.forName(this.cClass);
        Class[] parameters = {Long.class, C0004CP.class};
        Constructor constructor = cron.getConstructor(parameters);
        Object[] params = {new Long(this.period), cp};
        this.cObject = constructor.newInstance(params);
        return this.cObject;
    }

    public Object getCronObject() throws Exception {
        return this.cObject;
    }

    public void setCronObject(Object cObj) {
        this.cObject = cObj;
    }
}
