package psoft.util.freemarker.acl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;

/* loaded from: hsphere.zip:psoft/util/freemarker/acl/FMACLObjects.class */
public class FMACLObjects implements TemplateHashModel {
    Map objects = new HashMap();

    public FMACLObjects(Document doc) throws TransformerException {
        FMACLManager acl = new FMACLManager();
        acl.load(doc, this);
    }

    public void put(String key, FMACLWrapper wrapper) {
        this.objects.put(key, wrapper);
    }

    public TemplateModel get(String s) throws TemplateModelException {
        return (TemplateHashModel) this.objects.get(s);
    }

    public boolean isEmpty() throws TemplateModelException {
        return this.objects.isEmpty();
    }
}
