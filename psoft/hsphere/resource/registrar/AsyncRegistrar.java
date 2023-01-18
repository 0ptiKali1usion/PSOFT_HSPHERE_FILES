package psoft.hsphere.resource.registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/AsyncRegistrar.class */
public interface AsyncRegistrar {
    boolean isAsyncTLD(String str);

    int getRegCheckInterval(String str);

    int getRegTimeout(String str);

    boolean isRegComplete(String str, String str2) throws Exception;
}
