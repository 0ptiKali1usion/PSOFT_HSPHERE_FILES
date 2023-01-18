package psoft.hsphere.axis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/axis/NamedParameter.class */
public class NamedParameter implements Serializable {
    String name;
    String value;

    public NamedParameter() {
        this.name = null;
        this.value = null;
    }

    public NamedParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public NamedParameter(NamedParameter param) {
        this.name = param.name;
        this.value = param.value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public static NamedParameter[] addParameter(NamedParameter param, NamedParameter[] array) {
        List newHolder = new ArrayList(Arrays.asList(array));
        newHolder.add(param);
        return (NamedParameter[]) newHolder.toArray(new NamedParameter[0]);
    }

    public static NamedParameter[] addParameter(String name, String value, NamedParameter[] array) {
        return addParameter(new NamedParameter(name, value), array);
    }

    public static HashMap getParametersAsHashMap(NamedParameter[] parameters) {
        HashMap result = new HashMap();
        for (int i = 0; i < parameters.length; i++) {
            result.put(parameters[i].name, parameters[i].value);
        }
        return result;
    }

    public static NamedParameter[] getParameters(HashMap map) {
        List params = new ArrayList();
        for (Object obj : map.keySet()) {
            String name = obj.toString();
            params.add(new NamedParameter(name, map.get(name).toString()));
        }
        return (NamedParameter[]) params.toArray(new NamedParameter[0]);
    }
}
