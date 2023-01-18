package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.resource.apache.ErrorDocumentResource;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/MenuItemsHolder.class */
public class MenuItemsHolder extends AbstractMenuItem implements TemplateHashModel, TemplateModel {
    protected ArrayList items = new ArrayList();
    protected String defItemId;
    private AbstractMenuItem activeItem;
    private AbstractMenuItem defaultItem;

    public MenuItemsHolder(String name, String label, String defItemId, String platform_type, String resource, MenuItemsHolder parent, String tip) {
        this.name = name;
        this.label = label;
        this.defItemId = defItemId;
        this.platform_type = platform_type;
        this.resource = resource;
        this.parent = parent;
        this.active = false;
        this.tip = tip;
    }

    public List getMenuItems() {
        ArrayList tlist = new ArrayList();
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i) instanceof MenuItem) {
                tlist.add(this.items.get(i));
            }
        }
        return tlist;
    }

    public List getSubMenus() {
        ArrayList tlist = new ArrayList();
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i) instanceof MenuItemsHolder) {
                tlist.add(this.items.get(i));
            }
        }
        return tlist;
    }

    public List getAllItems() {
        return this.items;
    }

    public void addItem(MenuItem mi) {
        this.items.add(mi);
    }

    public void addHolder(MenuItemsHolder mih) {
        this.items.add(mih);
    }

    @Override // psoft.hsphere.AbstractMenuItem
    public MenuItemsHolder getParent() {
        return this.parent;
    }

    public List getChildren() {
        return this.items;
    }

    public void setDefaultItem() {
        this.defaultItem = getItemByName(this.defItemId);
    }

    private static TemplateList convertToSimpleList(List lst) {
        TemplateList slst = new TemplateList();
        Iterator i = lst.iterator();
        while (i.hasNext()) {
            AbstractMenuItem mi = (AbstractMenuItem) i.next();
            if (mi instanceof MenuItem) {
                if (((MenuItem) mi).hasToBeDrawn()) {
                    slst.add((TemplateModel) mi);
                }
            } else if (((MenuItemsHolder) mi).hasToBeDrawn()) {
                ((MenuItemsHolder) mi).setDefaultItem();
                slst.add((TemplateModel) mi);
            }
        }
        return slst;
    }

    @Override // psoft.hsphere.AbstractMenuItem
    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.AbstractMenuItem
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("name") ? new TemplateString(this.name) : key.equals("label") ? new TemplateString(getLabel()) : key.equals("parent") ? this.parent : key.equals("siblings") ? convertToSimpleList(getSiblings()) : key.equals("children") ? convertToSimpleList(getChildren()) : key.equals("activeitem") ? this.activeItem : key.equals("default_item") ? this.defaultItem : key.equals(ErrorDocumentResource.MTYPE_URL) ? new TemplateString(this.URL) : super.get(key);
    }

    public TemplateModel FM_getItemByName(String name) {
        return getItemByName(name);
    }

    public AbstractMenuItem getItemByName(String name) {
        AbstractMenuItem found;
        AbstractMenuItem result = null;
        if (this.name.equals(name)) {
            return this;
        }
        int i = 0;
        while (true) {
            if (i >= this.items.size()) {
                break;
            }
            AbstractMenuItem currEl = (AbstractMenuItem) this.items.get(i);
            if ((currEl instanceof MenuItem) && name.equals(currEl.getName())) {
                result = currEl;
                break;
            } else if (!(currEl instanceof MenuItemsHolder) || (found = ((MenuItemsHolder) currEl).getItemByName(name)) == null) {
                i++;
            } else {
                result = found;
                break;
            }
        }
        if (result != null && (result instanceof MenuItem) && !result.hasToBeDrawn()) {
            result = getFirstEnabledItem(result.getParent());
        }
        return result;
    }

    public TemplateModel FM_getMenu(String activeItemName) {
        AbstractMenuItem activeItem = getItemByName(activeItemName);
        if (activeItem == null) {
            return null;
        }
        AbstractMenuItem currItem = activeItem;
        List lst = new ArrayList();
        do {
            lst.add(convertToSimpleList(currItem.getSiblings()));
            currItem = currItem.parent;
        } while (currItem.parent != null);
        TemplateList res = new TemplateList();
        for (int i = lst.size() - 1; i >= 0; i--) {
            TemplateList tlist = (TemplateList) lst.get(i);
            res.add((TemplateModel) tlist);
        }
        return res;
    }

    public AbstractMenuItem getActiveItem() {
        return this.activeItem;
    }

    public void setActiveItem(AbstractMenuItem item) {
        this.activeItem = item;
    }

    public static AbstractMenuItem getFirstEnabledItem(MenuItemsHolder parent) {
        for (int i = 0; i < parent.getChildren().size(); i++) {
            if (parent.getChildren().get(i) instanceof MenuItem) {
                if (((MenuItem) parent.getChildren().get(i)).hasToBeDrawn()) {
                    return (AbstractMenuItem) parent.getChildren().get(i);
                }
            } else {
                return getFirstEnabledItem((MenuItemsHolder) parent.getChildren().get(i));
            }
        }
        Session.getLog().error("Failed to get first enabled active item");
        return null;
    }
}
