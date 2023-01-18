package psoft.hsphere.p001ds;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Session;

/* renamed from: psoft.hsphere.ds.DedicatedServerState */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DedicatedServerState.class */
public class DedicatedServerState {
    protected int intState;
    protected String strState;
    protected static Hashtable states = new Hashtable();
    protected static List allStrStates = new ArrayList();
    public static final DedicatedServerState DISABLED = new DedicatedServerState(0, "DISABLED");
    public static final DedicatedServerState AVAILABLE = new DedicatedServerState(1, "AVAILABLE");
    public static final DedicatedServerState IN_USE = new DedicatedServerState(2, "IN_USE");
    public static final DedicatedServerState ON_HOLD = new DedicatedServerState(3, "ON_HOLD");
    public static final DedicatedServerState CLEAN_UP = new DedicatedServerState(4, "CLEAN_UP");
    public static final DedicatedServerState UNKNOWN = new DedicatedServerState(-1, "UNKNOWN");

    private DedicatedServerState(int intState, String strState) {
        try {
            this.strState = strState;
            this.intState = intState;
            Integer integerState = new Integer(intState);
            states.put(integerState, this);
            states.put(strState, this);
            if (intState >= 0) {
                allStrStates.add(strState);
            }
        } catch (Exception ex) {
            Session.getLog().error("Cannot initialize the dedicated server state #" + intState, ex);
        }
    }

    public String toString() {
        return this.strState;
    }

    public int toInt() {
        return this.intState;
    }

    public static DedicatedServerState get(int state) {
        DedicatedServerState st = (DedicatedServerState) states.get(new Integer(state));
        return st != null ? st : UNKNOWN;
    }

    public static DedicatedServerState get(String state) {
        DedicatedServerState st = (DedicatedServerState) states.get(state);
        return st != null ? st : UNKNOWN;
    }

    public static List getAllStrStates() {
        return new ArrayList(allStrStates);
    }
}
