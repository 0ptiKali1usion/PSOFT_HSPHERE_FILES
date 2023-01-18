package psoft.hsphere;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/AbstractMenuItem.class */
public class AbstractMenuItem implements TemplateModel, TemplateHashModel {
    protected String name;
    protected String label;
    protected String URL;
    protected MenuItemsHolder parent;
    protected boolean active;
    protected String tip;
    protected String new_window;
    protected String resource;
    protected String platform_type;

    public String getName() {
        return this.name;
    }

    public String getLabel() {
        return Localizer.translateMenu(this.label, null);
    }

    public String getTip() {
        return Localizer.translateMenu(this.tip, null);
    }

    public String getNew_Window() {
        return Localizer.translateMenu(this.new_window, null);
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isHolder() {
        return this instanceof MenuItemsHolder;
    }

    public TemplateModel FM_isHolder() {
        return new SimpleScalar(isHolder());
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("active")) {
            return new SimpleScalar(this.active);
        }
        if (key.equals("tip")) {
            return new SimpleScalar(getTip());
        }
        if (key.equals("new_window")) {
            return new SimpleScalar(getNew_Window());
        }
        if (key.equals("platform_type")) {
            return new TemplateString(this.platform_type);
        }
        return key.equals("resource") ? new TemplateString(this.resource) : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public List getSiblings() {
        return this.parent.getChildren();
    }

    public MenuItemsHolder getParent() {
        return this.parent;
    }

    public void turnActivitySign() {
        AbstractMenuItem abstractMenuItem = this;
        while (true) {
            AbstractMenuItem item = abstractMenuItem;
            if (item.parent != null) {
                item.setActive(true);
                item.getParent().setActiveItem(this);
                abstractMenuItem = item.getParent();
            } else {
                return;
            }
        }
    }

    public void FM_turnActivitySign() {
        turnActivitySign();
    }

    public void setActive(boolean val) {
        this.active = val;
    }

    public void FM_setActive(String val) {
        setActive(new Boolean(val).booleanValue());
    }

    protected boolean isItemDrawable(AbstractMenuItem item, Plan plan, String platformType) {
        try {
            String itemPlatformType = item.getPlatformType();
            if ("".equals(itemPlatformType) || itemPlatformType.equals(platformType)) {
                String pattern = item.getResourceName();
                if (pattern == null || "".equals(pattern)) {
                    return true;
                }
                if (plan != null) {
                    if (plan.areResourcesAvailable(pattern)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            Session.getLog().debug("Unable to check whether a menu item should be shown or not.", e);
            return false;
        }
    }

    protected boolean isMenuHolderDrawable(MenuItemsHolder holder, Plan plan, String platformType) {
        if (isItemDrawable(holder, plan, platformType)) {
            for (int i = 0; i < holder.items.size(); i++) {
                AbstractMenuItem item = (AbstractMenuItem) holder.items.get(i);
                if (item instanceof MenuItem) {
                    if (isItemDrawable(item, plan, platformType)) {
                        return true;
                    }
                } else if (isMenuHolderDrawable((MenuItemsHolder) item, plan, platformType)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean hasToBeDrawn() {
        String platformType;
        Plan plan;
        Account a = Session.getAccount();
        if (a != null) {
            platformType = a.getPlatformType();
            if (platformType == null) {
                platformType = "";
            }
            plan = Session.getAccount().getPlan();
        } else {
            platformType = "";
            plan = null;
        }
        return this instanceof MenuItemsHolder ? isMenuHolderDrawable((MenuItemsHolder) this, plan, platformType) : isItemDrawable(this, plan, platformType);
    }

    public TemplateModel FM_hasToBeDrawn() {
        return new TemplateString(hasToBeDrawn());
    }

    public String getResourceName() {
        return this.resource;
    }

    public String getPlatformType() {
        return this.platform_type != null ? this.platform_type : "";
    }
}
