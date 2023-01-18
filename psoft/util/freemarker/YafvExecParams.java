package psoft.util.freemarker;

import java.util.LinkedList;

/* loaded from: hsphere.zip:psoft/util/freemarker/YafvExecParams.class */
public class YafvExecParams extends LinkedList {
    public YafvExecParams() {
    }

    public YafvExecParams(String label) {
        super.add((YafvExecParams) label);
    }

    public YafvExecParams(String label, String param) {
        super.add((YafvExecParams) label);
        super.add((YafvExecParams) param);
    }

    public YafvExecParams add(String param) {
        super.add((YafvExecParams) param);
        return this;
    }
}
