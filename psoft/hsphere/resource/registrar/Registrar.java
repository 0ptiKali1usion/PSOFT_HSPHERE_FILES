package psoft.hsphere.resource.registrar;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/Registrar.class */
public interface Registrar {
    int getId();

    String[] getSupportedTLDs() throws Exception;

    String getSignature();

    boolean lookup(String str, String str2) throws Exception;

    void renew(String str, String str2, String str3, int i, Map map) throws Exception;

    void transfer(String str, String str2, String str3, String str4, Map map, Map map2, Map map3, Map map4, Collection collection) throws Exception;

    void register(String str, String str2, String str3, String str4, int i, Map map, Map map2, Map map3, Map map4, Collection collection) throws Exception;

    void register(String str, String str2, String str3, String str4, int i, Map map, Map map2, Map map3, Map map4, Collection collection, Map map5) throws Exception;

    void changeContacts(String str, String str2, String str3, String str4, Map map, Map map2, Map map3, Map map4) throws Exception;

    void setPassword(String str, String str2, String str3, String str4, String str5) throws Exception;

    String get(String str);

    void checkLogin() throws RegistrarException;

    Date isTransfered(String str, String str2) throws Exception;

    boolean isTransferable(String str, String str2) throws Exception;
}
