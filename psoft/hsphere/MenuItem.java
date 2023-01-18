package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.hsphere.resource.apache.ErrorDocumentResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/MenuItem.class */
public class MenuItem extends AbstractMenuItem implements TemplateHashModel {
    public MenuItem(String name, String label, String URL, String platform_type, String resource, MenuItemsHolder parent, String tip, String new_window) {
        this.name = name;
        this.label = label;
        this.URL = URL;
        this.platform_type = platform_type;
        this.resource = resource;
        this.parent = parent;
        this.active = false;
        this.tip = tip;
        this.new_window = new_window;
    }

    public String getURL() {
        return this.URL;
    }

    @Override // psoft.hsphere.AbstractMenuItem
    public String getNew_Window() {
        return this.new_window;
    }

    @Override // psoft.hsphere.AbstractMenuItem
    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.AbstractMenuItem
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("name") ? new TemplateString(this.name) : key.equals("label") ? new TemplateString(getLabel()) : key.equals(ErrorDocumentResource.MTYPE_URL) ? new TemplateString(this.URL) : key.equals("new_window") ? new TemplateString(this.new_window) : key.equals("parent") ? this.parent : super.get(key);
    }
}
