package psoft.epayment;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/GenericMerchantGateway.class */
public abstract class GenericMerchantGateway implements MerchantGateway {
    protected static HashMap defaultValues = new HashMap();

    /* renamed from: id */
    protected int f4id;
    protected MerchantGatewayLog log = null;

    @Override // psoft.epayment.MerchantGateway
    public abstract Map getValues();

    @Override // psoft.epayment.MerchantGateway
    public abstract void init(int i, HashMap hashMap) throws Exception;

    public abstract HashMap charge(long j, String str, double d, CreditCard creditCard) throws Exception;

    public abstract HashMap authorize(long j, String str, double d, CreditCard creditCard) throws Exception;

    public abstract HashMap capture(long j, String str, HashMap hashMap, CreditCard creditCard) throws Exception;

    public abstract HashMap voidAuthorize(long j, String str, HashMap hashMap, CreditCard creditCard) throws Exception;

    public static Map getDefaultValues() {
        return defaultValues;
    }

    @Override // psoft.epayment.MerchantGateway
    public int getId() {
        return this.f4id;
    }

    @Override // psoft.epayment.MerchantGateway
    public void setLog(MerchantGatewayLog log) {
        this.log = log;
    }

    public String getValue(HashMap values, String key) {
        Object obj = values.get(key);
        if (obj == null) {
            obj = defaultValues.get(key);
        }
        return (String) obj;
    }

    public String formatAmount(double amount) {
        NumberFormat engFormat = NumberFormat.getNumberInstance(new Locale("en", "US"));
        engFormat.setMinimumFractionDigits(2);
        engFormat.setMinimumIntegerDigits(1);
        engFormat.setMaximumFractionDigits(2);
        engFormat.setGroupingUsed(false);
        return engFormat.format(amount);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap checkCVV(long acctid, PaymentInstrument pi) throws Exception {
        return checkCC(acctid, (CreditCard) pi);
    }

    public HashMap checkCC(long acctid, CreditCard cc) throws Exception {
        Session.getLog().debug("cvv validation is not implemented");
        return new HashMap();
    }

    public HashMap checkCCCVV(long acctid, CreditCard cc) throws Exception {
        boolean success = false;
        String error = "";
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            try {
                cc.checkValid();
                HashMap data = authorize(acctid, "Checking verification number", 1.0d, cc);
                success = true;
                voidAuthorize(acctid, "Checking verification number", data, cc);
            } catch (Exception ex) {
                error = ex.getMessage();
                Session.getLog().error("Error checking cvv value: ", ex);
            }
        } else {
            success = true;
            cc.setCVVChecked(false);
        }
        if (!success) {
            throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. " + error);
        }
        return new HashMap();
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap charge(String description, double amount, PaymentInstrument pi) throws Exception {
        return charge(-1L, description, amount, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap charge(long id, String description, double amount, PaymentInstrument pi) throws Exception {
        try {
            System.setProperty("sun.net.inetaddr.ttl", "0");
            HashMap result = charge(id, description, amount, (CreditCard) pi);
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            return result;
        } catch (Throwable th) {
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            throw th;
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap authorize(String description, double amount, PaymentInstrument pi) throws Exception {
        return authorize(-1L, description, amount, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap authorize(long id, String description, double amount, PaymentInstrument pi) throws Exception {
        try {
            System.setProperty("sun.net.inetaddr.ttl", "0");
            HashMap authorize = authorize(id, description, amount, (CreditCard) pi);
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            return authorize;
        } catch (Throwable th) {
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            throw th;
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap capture(String description, HashMap data, PaymentInstrument pi) throws Exception {
        return capture(-1L, description, data, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap capture(long id, String description, HashMap data, PaymentInstrument pi) throws Exception {
        try {
            System.setProperty("sun.net.inetaddr.ttl", "0");
            HashMap result = capture(id, description, data, (CreditCard) pi);
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            return result;
        } catch (Throwable th) {
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            throw th;
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap voidAuthorize(String description, HashMap data, PaymentInstrument pi) throws Exception {
        return voidAuthorize(-1L, description, data, pi);
    }

    @Override // psoft.epayment.MerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, PaymentInstrument pi) throws Exception {
        try {
            System.setProperty("sun.net.inetaddr.ttl", "0");
            HashMap voidAuthorize = voidAuthorize(id, description, data, (CreditCard) pi);
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            return voidAuthorize;
        } catch (Throwable th) {
            System.setProperty("sun.net.inetaddr.ttl", C0004CP.INETADDRTTL);
            throw th;
        }
    }

    public long writeLog(long trid, long accid, double amount, int trtype, String dataOut, String dataIn, String error, boolean success) {
        if (this.log != null) {
            return this.log.write(trid, accid, amount, trtype, dataOut, dataIn, error, success, this.f4id);
        }
        return 0L;
    }

    public long writeLog(long accid, double amount, int trtype) {
        return writeLog(0L, accid, amount, trtype, "", "", "", false);
    }

    public long writeLog(long accid, String dataOut, String dataIn, String error) {
        boolean result = false;
        if (error == null || "".equals(error)) {
            result = true;
        }
        return writeLog(0L, accid, 0.0d, 100, dataOut, dataIn, error, result);
    }

    public void writeCharge(double amount) throws Exception {
        if (this.log != null) {
            this.log.transaction(getId(), amount, TimeUtils.getDate(), 0);
        }
    }

    public void writeVoid(double amount) throws Exception {
        if (this.log != null) {
            this.log.transaction(getId(), amount, TimeUtils.getDate(), 3);
        }
    }

    public void writeCapture(double amount) throws Exception {
        if (this.log != null) {
            this.log.transaction(getId(), amount, TimeUtils.getDate(), 4);
        }
    }

    public void writeAuthorize(double amount) throws Exception {
        if (this.log != null) {
            this.log.transaction(getId(), amount, TimeUtils.getDate(), 1);
        }
    }

    public static String getTrDescription(int id) {
        String descr;
        switch (id) {
            case 0:
                descr = "Charge";
                break;
            case 1:
                descr = "Authorize";
                break;
            case 2:
                descr = "Refund";
                break;
            case 3:
                descr = "Void";
                break;
            case 4:
                descr = "Capture";
                break;
            default:
                descr = "";
                break;
        }
        return descr;
    }

    public void sendEmail(String email, int trtype, String description, long id, String mess) throws Exception {
        String message = "";
        switch (trtype) {
            case 3:
                message = "You need to void transaction with account " + Long.toString(id);
                break;
            case 4:
                message = "Transaction with the " + Long.toString(id) + " account should be settled manually";
                break;
        }
        String body = "Dear administrator,\n" + message + "\n" + mess + "\n Transaction description:" + description;
        Session.getMailer().sendMessage(email, message, body, Session.getCurrentCharset());
    }
}
