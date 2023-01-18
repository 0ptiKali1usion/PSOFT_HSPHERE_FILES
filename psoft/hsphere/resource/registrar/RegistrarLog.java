package psoft.hsphere.resource.registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/RegistrarLog.class */
public interface RegistrarLog {
    long write(String str, String str2, int i, int i2, int i3, String str3);

    void write(long j, RegistrarTransactionData registrarTransactionData, boolean z);
}
