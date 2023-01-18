package psoft.hsphere.resource.registrar;

import java.sql.SQLException;
import psoft.hsphere.Localizer;
import psoft.hsphere.manager.ManagerException;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/AsyncLoggableRegistrar.class */
public abstract class AsyncLoggableRegistrar extends LoggableRegistrar implements AsyncRegistrar {
    public static final int IS_REG_COMPLETE = 10;

    public abstract boolean isRegComplete(String str, String str2, RegistrarTransactionData registrarTransactionData) throws Exception;

    public AsyncLoggableRegistrar(Integer id, String description) throws ManagerException, SQLException {
        super(id, description);
    }

    @Override // psoft.hsphere.resource.registrar.AsyncRegistrar
    public boolean isRegComplete(String domain, String tld) throws Exception {
        long id = writeLog(domain, tld, 1, 10);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            boolean result = isRegComplete(domain, tld, dat);
            updateLog(id, dat, true);
            return result;
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    public static String getTtType(int type) {
        if (type == 10) {
            String ttType = Localizer.translateMessage("registrar.is_async_reg_complete");
            return ttType;
        }
        return LoggableRegistrar.getTtType(type);
    }
}
