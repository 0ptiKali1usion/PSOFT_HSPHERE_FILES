package psoft.validators;

import java.util.Enumeration;
import java.util.Hashtable;
import psoft.hsphere.Session;
import psoft.hsphere.util.XMLHashtableTranslator;

/* loaded from: hsphere.zip:psoft/validators/UtilsAccessor.class */
public class UtilsAccessor implements Accessor {
    protected static Hashtable classes = null;

    private void init() {
        try {
            XMLHashtableTranslator translator = new XMLHashtableTranslator("UTILS");
            classes = translator.translate("function", "name", "class");
        } catch (Exception e) {
            Session.getLog().error("Impossible to get 'function' from 'functions.xml'", e);
            classes = new Hashtable();
        }
    }

    @Override // psoft.validators.Accessor
    public String get(String name) {
        if (classes == null) {
            init();
        }
        return (String) classes.get(name);
    }

    @Override // psoft.validators.Accessor
    public Object getObject(Object key) {
        if (classes == null) {
            init();
        }
        return classes.get(key);
    }

    @Override // psoft.validators.Accessor
    public Enumeration keys() {
        if (classes == null) {
            init();
        }
        return classes.keys();
    }
}
