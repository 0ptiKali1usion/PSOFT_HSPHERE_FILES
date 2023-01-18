package psoft.hsphere.tools;

import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ExternalCP.class */
public class ExternalCP extends C0004CP {

    /* renamed from: cp */
    public static C0004CP f223cp;

    public static void initCP(long resellerId) throws Exception {
        f223cp = new ExternalCP();
        Session.setResellerId(resellerId);
    }

    public static void initCP() throws Exception {
        initCP(1L);
    }

    public ExternalCP() throws Exception {
        super("psoft_config.hsphere");
        Session.setLanguage(new Language(null));
    }
}
