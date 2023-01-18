package psoft.hsphere.resource.registrar;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import psoft.hsphere.Localizer;
import psoft.hsphere.manager.Entity;
import psoft.hsphere.manager.ManagerException;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/LoggableRegistrar.class */
public abstract class LoggableRegistrar extends Entity implements Registrar {
    public static final int REGISTER = 1;
    public static final int RENEW = 2;
    public static final int LOOKUP = 3;
    public static final int CHANGECONTACTS = 4;
    public static final int CHANGEPASSWD = 5;
    public static final int CHANGENAMESERVERS = 6;
    public static final int TRANSFER = 7;
    public static final int IS_TRANSFERED = 8;
    public static final int TRANSFER_LOOKUP = 9;
    protected RegistrarLog log;

    public abstract boolean lookup(String str, String str2, RegistrarTransactionData registrarTransactionData) throws Exception;

    public abstract void renew(String str, String str2, String str3, int i, Map map, RegistrarTransactionData registrarTransactionData) throws Exception;

    public abstract void register(String str, String str2, String str3, String str4, int i, Map map, Map map2, Map map3, Map map4, Collection collection, RegistrarTransactionData registrarTransactionData) throws Exception;

    public abstract void register(String str, String str2, String str3, String str4, int i, Map map, Map map2, Map map3, Map map4, Collection collection, Map map5, RegistrarTransactionData registrarTransactionData) throws Exception;

    public abstract void changeContacts(String str, String str2, String str3, String str4, Map map, Map map2, Map map3, Map map4, RegistrarTransactionData registrarTransactionData) throws Exception;

    public abstract void setPassword(String str, String str2, String str3, String str4, String str5, RegistrarTransactionData registrarTransactionData) throws Exception;

    public LoggableRegistrar(Integer id, String description) throws ManagerException, SQLException {
        super(id, description);
        this.log = new RegistrarDbLog();
    }

    public void setLog(RegistrarLog log) {
        this.log = log;
    }

    public long writeLog(String domain, String tld, int period, int trtype) {
        return this.log.write(domain, tld, period, trtype, getId(), getSignature());
    }

    public void updateLog(long id, RegistrarTransactionData data, boolean success) {
        this.log.write(id, data, success);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public Date isTransfered(String domain, String tld) throws Exception {
        long id = writeLog(domain, tld, 0, 8);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            Date result = isTransfered(domain, tld, dat);
            updateLog(id, dat, true);
            return result;
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    public Date isTransfered(String name, String tld, RegistrarTransactionData dat) throws Exception {
        throw new Exception("NOT SUPPORTED");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public boolean isTransferable(String name, String tld) throws Exception {
        long id = writeLog(name, tld, 0, 9);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            boolean result = isTransferable(name, tld, dat);
            updateLog(id, dat, true);
            return result;
        } catch (Exception e) {
            dat.setError(e.getMessage());
            updateLog(id, dat, false);
            throw e;
        }
    }

    public boolean isTransferable(String name, String tld, RegistrarTransactionData dat) throws Exception {
        throw new Exception("NOT SUPPORTED");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public boolean lookup(String domain, String tld) throws Exception {
        long id = writeLog(domain, tld, 0, 3);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            boolean result = lookup(domain, tld, dat);
            updateLog(id, dat, true);
            return result;
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void renew(String domain, String tld, String currentExpYear, int period, Map registrant) throws Exception {
        long id = writeLog(domain, tld, period, 2);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            renew(domain, tld, currentExpYear, period, registrant, dat);
            updateLog(id, dat, true);
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void transfer(String domain, String tld, String login, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns) throws Exception {
        long id = writeLog(domain, tld, 1, 7);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            transfer(domain, tld, login, password, registrant, tech, admin, billing, dns, dat);
            updateLog(id, dat, true);
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    public void transfer(String domain, String tld, String login, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        throw new Exception("Not Implemented");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void register(String domain, String tld, String login, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns) throws Exception {
        long id = writeLog(domain, tld, period, 1);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            register(domain, tld, login, password, period, registrant, tech, admin, billing, dns, dat);
            updateLog(id, dat, true);
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void register(String domain, String tld, String login, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, Map extraInfo) throws Exception {
        long id = writeLog(domain, tld, period, 1);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            register(domain, tld, login, password, period, registrant, tech, admin, billing, dns, extraInfo, dat);
            updateLog(id, dat, true);
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void changeContacts(String domain, String tld, String login, String password, Map registrant, Map tech, Map admin, Map billing) throws Exception {
        long id = writeLog(domain, tld, 0, 4);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            changeContacts(domain, tld, login, password, registrant, tech, admin, billing, dat);
            updateLog(id, dat, true);
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void setPassword(String domain, String tld, String login, String password, String newPassword) throws Exception {
        long id = writeLog(domain, tld, 0, 5);
        RegistrarTransactionData dat = new RegistrarTransactionData();
        try {
            setPassword(domain, tld, login, password, newPassword, dat);
            updateLog(id, dat, true);
        } catch (Exception ex) {
            dat.setError(ex.getMessage());
            updateLog(id, dat, false);
            throw ex;
        }
    }

    public static String getTtType(int type) {
        String ttType = "unknown";
        if (type == 1) {
            ttType = Localizer.translateMessage("registrar.register");
        } else if (type == 2) {
            ttType = Localizer.translateMessage("registrar.renew");
        } else if (type == 3) {
            ttType = Localizer.translateMessage("registrar.lookup");
        } else if (type == 4) {
            ttType = Localizer.translateMessage("registrar.change_contacts");
        } else if (type == 5) {
            ttType = Localizer.translateMessage("registrar.change_passwd");
        } else if (type == 6) {
            ttType = Localizer.translateMessage("registrar.change_name_servers");
        } else if (type == 7) {
            ttType = Localizer.translateMessage("registrar.transfer");
        } else if (type == 8) {
            ttType = Localizer.translateMessage("registrar.check_is_transfered");
        } else if (type == 9) {
            ttType = Localizer.translateMessage("registrar.transfer_lookup");
        }
        return ttType;
    }
}
