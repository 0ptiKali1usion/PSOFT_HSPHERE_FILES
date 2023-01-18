package psoft.epayment;

import com.PNPssl.pnpapi;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/* loaded from: hsphere.zip:psoft/epayment/PlugnPay.class */
public class PlugnPay extends GenericMerchantGateway {
    protected String certDir;
    protected String username;
    protected String password;
    protected String dontsndmail;
    protected String email;
    private static final SimpleDateFormat expFormat = new SimpleDateFormat("MM/yy");

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.username = getValue(v, "USERNAME");
        this.password = getValue(v, "PASSWORD");
        this.email = getValue(v, "EMAIL");
        this.certDir = getValue(v, "CERT_DIR");
        this.dontsndmail = getValue(v, "DONTSNDMAIL");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("USERNAME", this.username);
        map.put("PASSWORD", this.password);
        map.put("EMAIL", this.email);
        map.put("CERT_DIR", this.certDir);
        map.put("DONTSNDMAIL", this.dontsndmail);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = doTransaction(0, id, amount, description, cc, null);
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = doTransaction(1, id, amount, description, cc, null);
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = doTransaction(4, id, amount, description, cc, data);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = doTransaction(3, id, amount, description, cc, data);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Plug n Pay (www.plugnpay.com)";
    }

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        boolean success = false;
        HashMap retval = new HashMap();
        String error = "";
        Properties result = new Properties();
        Properties prop = new Properties();
        pnpapi pnp = new pnpapi();
        long trId = writeLog(id, amount, trType);
        prop.put("cert_dir", this.certDir);
        prop.put("publisher-name", this.username);
        prop.put("publisher-email", this.email);
        prop.put("dontsndmail", this.dontsndmail);
        prop.put("accID", Long.toString(id));
        if (4 == trType || 3 == trType) {
            prop.put("publisher-password", this.password);
            prop.put("card-amount", Double.toString(amount));
            prop.put("mode", 4 == trType ? "mark" : "void");
            prop.put("orderID", data.get("id"));
        } else {
            prop.put("card-name", cc.getName());
            prop.put("card-number", cc.getNumber());
            prop.put("card-address1", cc.getAddress());
            prop.put("card-city", cc.getCity());
            prop.put("card-state", cc.getState());
            prop.put("card-zip", cc.getZip());
            prop.put("card-country", cc.getCountry());
            prop.put("card-exp", cc.getExp(expFormat));
            prop.put("card-email", cc.getEmail());
            prop.put("card-amount", Double.toString(amount));
            prop.put("item1", Long.toString(id));
            prop.put("cost1", Double.toString(amount));
            prop.put("quantity1", "1");
            prop.put("description1", description);
            if (0 == trType) {
                prop.put("authtype", "authpostauth");
            }
        }
        try {
            result = pnp.doTransaction(prop);
            success = "success".equals(result.get("FinalStatus"));
            if (success) {
                retval.put("id", result.get("orderID"));
                retval.put("amount", new Double(amount));
            } else {
                error = (String) result.get("auth-msg");
            }
        } catch (Exception e) {
            error = e.getMessage();
        }
        writeLog(trId, id, amount, trType, prop.toString(), result.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        return retval;
    }
}
