package psoft.epayment;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/epayment/NTPNow.class */
public class NTPNow extends GenericMerchantGateway {
    private String server;
    private int port;
    private String path;
    private String loginID;
    private static DateFormat month = new SimpleDateFormat("MM");
    private static DateFormat year = new SimpleDateFormat("yy");
    private String avsCheck;
    protected String adminEmail;

    static {
        defaultValues.put("SERVER", "ntpnow.com");
        defaultValues.put("PORT", "443");
        defaultValues.put("PATH", "payV2.asp");
        defaultValues.put("AVS", "TRUE");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("PATH", this.path);
        map.put("LOGINID", this.loginID);
        map.put("AVS", this.avsCheck);
        map.put("NOTIFICATIONEMAIL", this.adminEmail);
        return null;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
        this.server = getValue(values, "SERVER");
        this.port = Integer.parseInt(getValue(values, "PORT"));
        this.path = getValue(values, "PATH");
        this.loginID = getValue(values, "LOGINID");
        this.avsCheck = getValue(values, "AVS");
        this.adminEmail = getValue(values, "NOTIFICATIONEMAIL");
    }

    private HashMap processTransaction(int trType, long id, String description, double amount, long transID, CreditCard cc) throws Exception {
        long logId = writeLog(id, amount, trType);
        boolean success = false;
        HashMap result = new HashMap();
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        String error = "";
        int trtype = 1;
        try {
            switch (trType) {
                case 0:
                    trtype = 1;
                    break;
                case 1:
                    trtype = 3;
                    break;
                case 4:
                    trtype = 4;
                    break;
            }
            request.put("TransType", Integer.toString(trtype));
            request.put("NTPNowID", this.loginID);
            request.put("Amount", formatAmount(amount));
            request.put("NameOnCard", cc.getName());
            request.put("Street", cc.getAddress());
            request.put("City", cc.getCity());
            request.put("State", cc.getState());
            request.put("CreditCardNumber", cc.getNumber());
            request.put("Month", cc.getExp(month));
            request.put("Year", cc.getExp(year));
            if ("TRUE".equals(this.avsCheck)) {
                request.put("AVS", "True");
                request.put("AVSValidIf", "DXY");
            } else {
                request.put("AVS", "False");
            }
            HttpResponse httpresponse = HttpUtils.postForm("https", this.server, this.port, this.path, request);
            String message = httpresponse.getBody();
            result.put("response", message);
            StringTokenizer st = new StringTokenizer(message, "|");
            while (st.hasMoreTokens()) {
                String str = st.nextToken();
                int tmppos = str.indexOf(":");
                if (tmppos > 0) {
                    String key = str.substring(0, tmppos);
                    if (str.length() > tmppos + 1) {
                        String value = str.substring(tmppos + 1);
                        response.put(key, value);
                    }
                }
            }
            if (response.containsKey("STATUS") && "TRANSACTION SUCCESSFUL".equals(response.get("STATUS"))) {
                success = true;
                result.put("amount", new Double(amount));
                result.put("id", Long.toString(logId));
            }
            if (!success && response.containsKey("AUTHORIZATION RESPONSE")) {
                error = (String) response.get("AUTHORIZATION RESPONSE");
                if (response.containsKey("AVS RESPONSE")) {
                    error = error + " " + response.get("AVS RESPONSE");
                }
            }
        } catch (IOException ex) {
            error = "Could not connect to " + this.server + ":" + this.port + "/" + this.path;
            Session.getLog().error(error, ex);
        } catch (Exception ex2) {
            error = "Could not perform transaction";
            Session.getLog().error("Could not perform transaction", ex2);
        }
        writeLog(logId, id, amount, trType, request.toString(), response.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap result = processTransaction(0, id, description, amount, 0L, cc);
        writeCharge(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap result = processTransaction(1, id, description, amount, 0L, cc);
        writeAuthorize(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long transID = Long.parseLong((String) data.get("id"));
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        String transid = Long.toString(transID);
        retval.put("id", transid);
        writeLog(0L, id, amount, 4, "This transaction should be settled manually", "", "", true);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long transID = Long.parseLong((String) data.get("id"));
        try {
            sendEmail(this.adminEmail, 3, description, id, "");
        } catch (Exception ex) {
            Session.getLog().error("Error sending notification email: ", ex);
        }
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        String transid = Long.toString(transID);
        retval.put("id", transid);
        writeLog(0L, id, amount, 3, "This transaction should be voided manually", "", "", true);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "NTPNow.com";
    }
}
