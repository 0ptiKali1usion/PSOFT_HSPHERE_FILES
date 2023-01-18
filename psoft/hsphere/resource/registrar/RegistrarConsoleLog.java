package psoft.hsphere.resource.registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/RegistrarConsoleLog.class */
public class RegistrarConsoleLog implements RegistrarLog {
    @Override // psoft.hsphere.resource.registrar.RegistrarLog
    public long write(String domain, String tld, int period, int trtype, int registrarId, String signature) {
        System.out.println("**************** " + signature + "transaction data ************");
        System.out.println("  domain:           " + domain + "." + tld);
        System.out.println("  period:           " + period);
        System.out.println("  transaction type: " + getTtType(trtype));
        System.out.println("*******************************************");
        return 0L;
    }

    @Override // psoft.hsphere.resource.registrar.RegistrarLog
    public void write(long id, RegistrarTransactionData data, boolean success) {
        System.out.println("**************** transaction Results ************");
        System.out.println("  result:          " + success);
        System.out.println("  request:         " + data.getRequest());
        System.out.println("  response:        " + data.getResponse());
    }

    public static String getTtType(int type) {
        String ttType = "unknown";
        if (type == 1) {
            ttType = "Register";
        } else if (type == 2) {
            ttType = "Renew";
        } else if (type == 3) {
            ttType = "Lookup";
        } else if (type == 4) {
            ttType = "Change Contacts";
        } else if (type == 5) {
            ttType = "Change Password";
        } else if (type == 6) {
            ttType = "Change Name servers";
        } else if (type == 7) {
            ttType = "Transfer";
        } else if (type == 8) {
            ttType = "Check Is Transfered";
        } else if (type == 9) {
            ttType = "Transfer Lookup";
        }
        return ttType;
    }
}
