package psoft.hsphere.migrator;

import freemarker.template.TemplateHashModel;
import java.io.Reader;
import java.util.Collection;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/PhysicalSource.class */
public interface PhysicalSource extends TemplateHashModel {
    Collection exec(String str, String[] strArr, String str2) throws Exception;

    Reader exec2(String str, String[] strArr, String str2) throws Exception;
}
