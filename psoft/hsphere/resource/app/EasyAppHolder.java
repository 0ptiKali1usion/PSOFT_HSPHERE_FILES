package psoft.hsphere.resource.app;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import org.apache.log4j.Category;
import psoft.hsphere.ResourceId;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/EasyAppHolder.class */
public class EasyAppHolder implements TemplateHashModel {
    private static Category log = Category.getInstance(EasyAppHolder.class.getName());

    /* renamed from: id */
    protected int f183id;
    protected int type;
    protected String version;
    protected ResourceId vhost;
    protected String path;

    /* renamed from: r */
    EasyAppResource f184r;
    EasyApp app;

    public EasyAppHolder(int id, int type, String version, ResourceId vhost, String path, EasyAppResource r) {
        this.f183id = id;
        this.type = type;
        this.version = version;
        this.vhost = vhost;
        this.path = path;
        this.f184r = r;
    }

    public EasyAppHolder(EasyApp app) {
        this.f183id = app.getId();
        this.type = app.getType();
        this.version = app.getVersion();
        this.vhost = app.getVhost();
        this.path = app.getPath();
        this.app = app;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.f183id);
        }
        if ("vhost".equals(key)) {
            return this.vhost;
        }
        if ("path".equals(key)) {
            return new TemplateString(this.path);
        }
        if ("version".equals(key)) {
            return new TemplateString(this.version);
        }
        if ("type".equals(key)) {
            return new TemplateString(this.type);
        }
        if ("app".equals(key)) {
            try {
                return getApp();
            } catch (Exception e) {
                log.error("Error retrieving EasyApp", e);
                throw new TemplateModelException(e);
            }
        } else if ("upgradable".equals(key)) {
            return isUpgradable();
        } else {
            return null;
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public EasyApp getApp() throws Exception, IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {
        if (this.app == null) {
            this.app = EasyApp.getApp(this.f184r, this.type, this.f183id, this.version, this.vhost, this.path);
            this.f184r = null;
        }
        return this.app;
    }

    public int getType() {
        return this.type;
    }

    private TemplateModel isUpgradable() throws TemplateModelException {
        try {
            if (EasyApp.isUpgradable(this.f184r, this.type, this.version)) {
                return TemplateString.TRUE;
            }
            return TemplateString.FALSE;
        } catch (Exception e) {
            log.error("Error on isUpgradable check", e);
            throw new TemplateModelException(e);
        }
    }
}
