package psoft.hsphere.resource.miva;

import java.util.Hashtable;
import psoft.hsphere.Resource;
import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/MivaOperator.class */
public interface MivaOperator {
    public static final int MERCHANT3 = 1;
    public static final int MERCHANT4 = 2;
    public static final int MERCHANT5 = 3;
    public static final int UNIX = 1;
    public static final int WIN = 2;

    void installMivaEmpresa() throws Exception;

    void installMerchantBundle() throws Exception;

    void uninstallMivaEmpresa() throws Exception;

    void uninstallMivaMerchant() throws Exception;

    Hashtable configureMerchant() throws Exception;

    void setMivaResource(Resource resource) throws Exception;

    void setHost(HostEntry hostEntry);

    String getMivaMerchantSetupURL() throws Exception;

    String getMivaMerchantAdminURL() throws Exception;

    String getMivaMerchantURL() throws Exception;

    String getMivaMerchantUninstallURL() throws Exception;
}
