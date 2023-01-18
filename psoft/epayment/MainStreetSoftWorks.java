package psoft.epayment;

import com.mainstreetsoftworks.MCVE;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/epayment/MainStreetSoftWorks.class */
public class MainStreetSoftWorks extends GenericMerchantGateway {
    private String username;
    private String password;
    private String cert;
    private String server;
    private int port;
    private int timeout;
    private static MCVE mcve = null;
    private static DateFormat dateFormat = new SimpleDateFormat("yyMM");

    static {
        System.loadLibrary("mcvejni");
        defaultValues.put("SERVER", "mcve.com");
        defaultValues.put("TIMEOUT", "30");
        defaultValues.put("PORT", "8333");
        defaultValues.put("CERTFILE", "");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("USERNAME", this.username);
        map.put("PASSWD", this.password);
        map.put("CERTFILE", this.cert);
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("TIMEOUT", Integer.toString(this.timeout));
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
        this.username = getValue(values, "USERNAME");
        this.password = getValue(values, "PASSWD");
        this.cert = getValue(values, "CERTFILE");
        this.server = getValue(values, "SERVER");
        this.port = Integer.parseInt(getValue(values, "PORT"));
        this.timeout = Integer.parseInt(getValue(values, "TIMEOUT"));
        mcve = new MCVE(this.cert);
        mcve.SetIP(this.server, this.port);
        mcve.SetTimeout(this.timeout);
        mcve.SetBlocking(1);
    }

    private HashMap processTransaction(int trType, long id, String description, double amount, long transID, CreditCard cc) throws Exception {
        String cvv;
        long trId = writeLog(id, amount, trType);
        HashMap result = new HashMap();
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        boolean success = false;
        String error = "";
        int identifier = mcve.TransNew();
        try {
            boolean z = "";
            try {
                switch (trType) {
                    case 0:
                        MCVE mcve2 = mcve;
                        z = "1";
                        break;
                    case 1:
                        MCVE mcve3 = mcve;
                        z = "2";
                        break;
                    case 3:
                        MCVE mcve4 = mcve;
                        z = "3";
                        break;
                    case 4:
                        MCVE mcve5 = mcve;
                        z = "4";
                        break;
                }
                MCVE mcve6 = mcve;
                request.put(new Integer(1), z);
                MCVE mcve7 = mcve;
                request.put(new Integer(2), this.username);
                MCVE mcve8 = mcve;
                request.put(new Integer(3), this.password);
                if (trType == 0 || trType == 1) {
                    MCVE mcve9 = mcve;
                    request.put(new Integer(4), cc.getNumber());
                    MCVE mcve10 = mcve;
                    request.put(new Integer(9), cc.getCVV());
                    MCVE mcve11 = mcve;
                    request.put(new Integer(6), cc.getExp(dateFormat));
                    MCVE mcve12 = mcve;
                    request.put(new Integer(14), Double.toString(amount));
                    MCVE mcve13 = mcve;
                    request.put(new Integer(14), Double.toString(amount));
                    MCVE mcve14 = mcve;
                    request.put(new Integer(8), cc.getZip());
                    MCVE mcve15 = mcve;
                    request.put(new Integer(7), cc.getAddress());
                    MCVE mcve16 = mcve;
                    request.put(new Integer(10), description);
                } else {
                    MCVE mcve17 = mcve;
                    request.put(new Integer(16), Long.toString(transID));
                }
            } catch (Exception ex) {
                error = "Error: " + ex.getMessage();
                mcve.DeleteResponse(identifier);
            }
            if (mcve.Connect() <= 0) {
                throw new IOException("Could not connect to MCVE!");
            }
            for (Integer key : request.keySet()) {
                mcve.TransParam(identifier, key.intValue(), (String) request.get(key));
            }
            mcve.TransSend(identifier);
            int status = mcve.ReturnStatus(identifier);
            int code = mcve.ReturnCode(identifier);
            int avsresult = mcve.TransactionAVS(identifier);
            int cvvresult = mcve.TransactionCV(identifier);
            String message = mcve.TransactionText(identifier);
            int transId = mcve.TransactionID(identifier);
            MCVE mcve18 = mcve;
            if (status == 1) {
                success = true;
                result.put("id", Integer.toString(transId));
                result.put("amount", new Double(amount));
            } else {
                error = "Transaction Denied. Error message: " + message;
            }
            response.put("id", Integer.toString(transId));
            response.put("Code", mcve.TEXT_Code(code));
            response.put("AVS Message", mcve.TEXT_AVS(avsresult));
            response.put("CVV message", mcve.TEXT_CV(cvvresult));
            response.put("raw code", mcve.ResponseParam(identifier, "raw_code"));
            response.put("raw avs", mcve.ResponseParam(identifier, "raw_avs"));
            response.put("raw cvv", mcve.ResponseParam(identifier, "raw_cv"));
            response.put("Transaction text", message);
            mcve.DeleteResponse(identifier);
            request.put("server", this.server);
            request.put("port", Integer.toString(this.port));
            request.put("username", this.username);
            writeLog(trId, id, amount, trType, request.toString(), response.toString(), error, success);
            if ((trType == 1 || trType == 0) && (cvv = cc.getCVV()) != null && !"".equals(cvv)) {
                cc.setCVVChecked(success);
            }
            if (success) {
                return result;
            }
            throw new Exception(error);
        } catch (Throwable th) {
            mcve.DeleteResponse(identifier);
            throw th;
        }
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap response = processTransaction(0, id, description, amount, 0L, cc);
        writeCharge(amount);
        return response;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap response = processTransaction(1, id, description, amount, 0L, cc);
        writeAuthorize(amount);
        return response;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long transID = Long.parseLong((String) data.get("id"));
        HashMap retval = processTransaction(4, id, description, amount, transID, cc);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long transID = Long.parseLong((String) data.get("id"));
        HashMap retval = processTransaction(3, id, description, amount, transID, cc);
        writeVoid(amount);
        return retval;
    }

    public HashMap checkCC(CreditCard cc) throws Exception {
        boolean success = false;
        String error = "";
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            try {
                HashMap data = authorize(-1L, "Checking verification number", 1.0d, cc);
                success = true;
                voidAuthorize(-1L, "Checking verification number", data, cc);
            } catch (Exception ex) {
                error = ex.getMessage();
                Session.getLog().error("Error checking cvv value: ", ex);
            }
        } else {
            cc.setCVVChecked(false);
        }
        if (!success) {
            throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. " + error);
        }
        return new HashMap();
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "www.mainstreetsoftworks.com";
    }
}
