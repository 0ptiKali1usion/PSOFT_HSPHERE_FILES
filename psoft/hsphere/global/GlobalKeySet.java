package psoft.hsphere.global;

import freemarker.template.TemplateHashModel;
import java.util.List;
import psoft.hsphere.Reseller;

/* loaded from: hsphere.zip:psoft/hsphere/global/GlobalKeySet.class */
public interface GlobalKeySet extends TemplateHashModel {
    boolean isKeyAvailable(String str) throws Exception;

    List getAvailableKeys() throws Exception;

    String getName();

    String getPrefix();

    String getKeyDescription(String str) throws Exception;

    boolean isConfigured();

    boolean isDisabledByDefault();

    int isKeyDisabled(String str) throws Exception;

    int isKeyDisabled(String str, Reseller reseller) throws Exception;

    int isKeyDisabled(String str, int i) throws Exception;

    List getEnabledKeys() throws Exception;
}
