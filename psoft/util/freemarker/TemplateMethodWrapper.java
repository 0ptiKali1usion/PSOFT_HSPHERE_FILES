package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateMethodWrapper.class */
public class TemplateMethodWrapper implements TemplateMethodModel {
    public static final String PREFIX = "FM_";
    private static HashMap cache = new HashMap();
    private static final Class[] argT = {String.class};

    /* renamed from: o */
    protected Object f276o;
    protected String name;
    protected boolean empty = true;

    private static Map getMethodsFromClass(Class c) {
        HashMap m = (Map) cache.get(c);
        if (m == null) {
            synchronized (c) {
                Map m2 = (Map) cache.get(c);
                if (m2 != null) {
                    return m2;
                }
                m = new HashMap();
                Method[] methods = c.getMethods();
                for (Method mt : methods) {
                    if (mt.getName().startsWith(PREFIX)) {
                        m.put(mt.getName() + '|' + mt.getParameterTypes().length, mt);
                        m.put(mt.getName(), mt);
                    }
                }
                cache.put(c, m);
            }
        }
        return m;
    }

    public TemplateMethodWrapper() {
    }

    public void set(Object o, String name) {
        this.f276o = o;
        this.name = name;
        this.empty = getMethodsFromClass(o.getClass()).get(name) == null;
    }

    public TemplateMethodWrapper(Object o, String name) {
        set(o, name);
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public TemplateModel _exec(List args) throws Exception {
        Object[] argv = args.toArray();
        Method m = getMethod(argv.length);
        Class[] paramTypes = m.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        for (int j = 0; j < params.length; j++) {
            String tmpArg = HTMLEncoder.decode((String) argv[j]);
            params[j] = newObject(tmpArg, paramTypes[j]);
        }
        Object result = m.invoke(this.f276o, params);
        if (result == null) {
            return null;
        }
        if (result instanceof TemplateModel) {
            return (TemplateModel) result;
        }
        return new TemplateString(result);
    }

    public TemplateModel exec(List args) throws TemplateModelException {
        try {
            return _exec(args);
        } catch (Exception e) {
            throw new TemplateModelException("Exception for method " + this.name + "<br>\n" + e.getMessage());
        }
    }

    protected Method getMethod(int argc) throws TemplateModelException {
        Method mt = (Method) getMethodsFromClass(this.f276o.getClass()).get(this.name + '|' + argc);
        if (mt != null) {
            return mt;
        }
        throw new TemplateModelException("Wrong number of arguments for method " + this.name + ": " + argc);
    }

    protected Object newObject(String param, Class aClass) throws Exception {
        Object[] argv = new Object[1];
        argv[0] = param != null ? param : "";
        if (String.class.isInstance(aClass)) {
            return param;
        }
        if (aClass.isPrimitive()) {
            String s = aClass.toString();
            if ("boolean".equals(s)) {
                return param == null ? Boolean.FALSE : Boolean.valueOf(param);
            } else if ("char".equals(s)) {
                return param == null ? new Character(' ') : new Character(param.charAt(0));
            } else {
                if (param == null) {
                    param = "0";
                }
                if ("int".equals(s)) {
                    return new Integer(param);
                }
                if ("double".equals(s)) {
                    return new Double(param);
                }
                if ("float".equals(s)) {
                    return new Float(param);
                }
                if ("long".equals(s)) {
                    return new Long(param);
                }
                if ("short".equals(s)) {
                    return new Short(param);
                }
                if ("byte".equals(s)) {
                    return new Byte(param);
                }
            }
        }
        Constructor c = aClass.getConstructor(argT);
        return c.newInstance(argv);
    }

    public static TemplateMethodModel getMethod(Object o, String name) {
        return new TemplateMethodWrapper(o, PREFIX + name);
    }
}
