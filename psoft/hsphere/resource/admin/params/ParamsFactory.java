package psoft.hsphere.resource.admin.params;

import java.lang.reflect.Constructor;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/ParamsFactory.class */
public class ParamsFactory {
    public static BaseParam createControl(BaseParam base, String delim) throws Exception {
        if (base == null) {
            return null;
        }
        String name = base.getCurrParamName();
        String type = base.getType();
        Class clas = ParamsTypesMapper.getClassForType(type);
        if (clas == null) {
            throw new Exception("Parameter - " + name + ", unknown control type - " + type);
        }
        if (delim == null) {
            return createForOneParam(clas, base);
        }
        Class[] twoParams = {BaseParam.class, String.class};
        Object[] argV = {base, delim};
        Constructor constr = clas.getConstructor(twoParams);
        if (constr == null) {
            return createForOneParam(clas, base);
        }
        return (BaseParam) constr.newInstance(argV);
    }

    private static BaseParam createForOneParam(Class clas, BaseParam base) throws Exception {
        Class[] oneParam = {BaseParam.class};
        Constructor constr = clas.getConstructor(oneParam);
        Object[] argV = {base};
        return (BaseParam) constr.newInstance(argV);
    }
}
