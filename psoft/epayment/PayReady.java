package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.security.Security;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/epayment/PayReady.class */
public class PayReady extends GenericMerchantGateway {
    protected String server;
    protected String authPath;
    protected String capturePath;
    protected int port;
    protected String merchant_id;
    private static DateFormat dfMonth;
    private static DateFormat dfYear;
    protected static String[] authKey;
    protected static String[] captureKey;

    static {
        defaultValues.put("SERVER", "www.payready.net");
        defaultValues.put("CAPTURE_PATH", "/DMcapture.asp");
        defaultValues.put("AUTH_PATH", "/DMTransaction.asp");
        defaultValues.put("PORT", "443");
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        dfMonth = new SimpleDateFormat("MM");
        dfYear = new SimpleDateFormat("yy");
        authKey = new String[]{"MERCHANT_ID", "txtTransactionID", "txtApprovalStatus", "txtApprovalCode", "txtAVSCode", "txtAVSResponseText", "txtReferenceNumber", "txtErrorDescription"};
        captureKey = new String[]{"txtCaptureOnlyStatus", "txtTransactionID", "txtReferenceNumber", "txtErrorDescription"};
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.capturePath = getValue(v, "CAPTURE_PATH");
        this.authPath = getValue(v, "AUTH_PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.merchant_id = getValue(v, "MERCHANT_ID");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("AUTH_PATH", this.authPath);
        map.put("CAPTURE_PATH", this.capturePath);
        map.put("PORT", Integer.toString(this.port));
        map.put("MERCHANT_ID", this.merchant_id);
        return map;
    }

    protected Hashtable parse(String str, int max, String[] keys) {
        int pos = 0;
        int count = 0;
        Hashtable map = new Hashtable();
        for (int i = 0; i < str.length() && count < max; i++) {
            if (str.charAt(i) == '&') {
                map.put(keys[count], parseString(str.substring(pos, i)));
                pos = i + 1;
                count++;
            }
        }
        return map;
    }

    protected char hexToChar(char h, char l) {
        return (char) ((hexToInt(h) * 16) + hexToInt(l));
    }

    protected int hexToInt(char ch) {
        switch (ch) {
            case '0':
                return 0;
            case '1':
                return 1;
            case MySQLResource.MAX_DB_NAME /* 50 */:
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case SignupGuard.TRIAL_ACCOUNT_FLAG /* 64 */:
            default:
                return 0;
            case 'A':
                return 10;
            case 'B':
                return 11;
            case 'C':
                return 12;
            case 'D':
                return 13;
            case 'E':
                return 14;
            case 'F':
                return 15;
        }
    }

    protected String parseString(String str) {
        StringBuffer buf = new StringBuffer();
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == '+') {
                buf.append(' ');
            } else if (str.charAt(i) == '%') {
                buf.append(hexToChar(str.charAt(i + 1), str.charAt(i + 2)));
                i += 2;
            } else {
                buf.append(str.charAt(i));
            }
            i++;
        }
        return buf.toString();
    }

    protected String getAuthError(Hashtable hash) {
        String code = (String) hash.get("txtApprovalStatus");
        if (code == null) {
            return "Unknown Error";
        }
        try {
            switch (Integer.parseInt(code)) {
                case 1:
                    return "Declined";
                case 2:
                    return "Not Authorized";
                case 3:
                    return "Error Occurred";
                case 4:
                    return "Missing Information";
                case 5:
                    return "Unavailable Card Type";
                default:
                    return "Unknown Response From Merchant Gateway";
            }
        } catch (Exception e) {
            return "Invalid Response From Merchant Gateway";
        }
    }

    protected String getCaptureError(Hashtable hash) {
        String code = (String) hash.get("txtCaptureStatus");
        if (code == null) {
            return "Unknown Error";
        }
        try {
            switch (Integer.parseInt(code)) {
                case 1:
                    return "Invalid PayReady ID";
                case 2:
                    return "Invalid Transaction ID";
                case 3:
                    return "Invalid Reference Number";
                case 4:
                    return "Specified transaction has not been approved";
                case 5:
                    return "Specified transaction ID is expired";
                case 6:
                    return "Specified transaction ID is expired";
                case 7:
                    return "The transaction has already been captured";
                case 8:
                    return "Error occurred";
                case 9:
                    return "Error occurred";
                default:
                    return "Unknown Response From Merchant Gateway";
            }
        } catch (Exception e) {
            return "Invalid Response From Merchant Gateway";
        }
    }

    protected HashMap getRequest(boolean capture) {
        HashMap request = new HashMap();
        if (capture) {
            request.put("txtReturnURL", "TextOnly");
        } else {
            request.put("txtResponseURL", "TextOnly");
        }
        request.put("txtShowTranPage", "0");
        request.put("txtPayReadyID", this.merchant_id);
        return request;
    }

    protected String getCCTypeId(String type) {
        return "VISA".equals(type) ? "1" : "MX".equals(type) ? "2" : "AMEX".equals(type) ? "3" : "DISC".equals(type) ? "4" : "0";
    }

    protected void setData(HashMap request, long id, String description, double amount, CreditCard cc) throws Exception {
        request.put("txtInvoiceNumber", Long.toString(id));
        request.put("txtOrderDescription", description);
        request.put("txtTotalAmount", Double.toString(amount));
        request.put("txtCreditCardNumber", cc.getNumber());
        request.put("txtCreditCardType", getCCTypeId(cc.getType()));
        request.put("txtCreditCardExpirationMonth", cc.getExp(dfMonth));
        request.put("txtCreditCardExpirationYear", cc.getExp(dfYear));
        request.put("txtConsumerFirstName", cc.getFirstName());
        request.put("txtConsumerLastName", cc.getLastName());
        request.put("txtBillingStreet", cc.getAddress());
        request.put("txtBillingCity", cc.getCity());
        request.put("txtBillingState", cc.getState());
        request.put("txtBillingZip", cc.getZip());
        request.put("txtBillingCountry", cc.getCountry());
        request.put("txtEMail", cc.getEmail());
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = doTransaction(0, id, amount, description, cc, null);
        writeCharge(amount);
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
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 3);
        long transID = Long.parseLong((String) data.get("txtTransactionID"));
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        String transid = Long.toString(transID);
        retval.put("id", transid);
        writeLog(trId, id, amount, 3, "This transaction (ID=" + transid + ") should be voided manually", "", "", true);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "PayReady (www.cardready.com)";
    }

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        HashMap retval = new HashMap();
        String strResp = "";
        Hashtable responseRez = null;
        HashMap request = new HashMap();
        String path = "";
        String error = "";
        boolean success = false;
        long trId = writeLog(id, amount, trType);
        String resultStatusKey = "";
        switch (trType) {
            case 0:
            case 1:
                resultStatusKey = "txtApprovalStatus";
                path = this.authPath;
                request = getRequest(false);
                setData(request, id, description, amount, cc);
                if (0 == trType) {
                    request.put("txtAuthOnly", "0");
                    break;
                } else {
                    request.put("txtAuthOnly", "1");
                    break;
                }
            case 4:
                resultStatusKey = "txtCaptureOnlyStatus";
                path = this.capturePath;
                request = getRequest(true);
                request.put("txtCaptureOnly", "1");
                request.put("txtTransactionID", data.get("txtTransactionID"));
                request.put("txtReferenceNumber", data.get("txtReferenceNumber"));
                break;
        }
        try {
            HttpResponse response = HttpUtils.postForm("https", this.server, this.port, path, request);
            strResp = response.getBody();
            if (response.getResponseCode() != 200) {
                error = response.getResponseMessage();
            } else if (4 == trType) {
                responseRez = parse(response.getBody(), 5, captureKey);
            } else {
                responseRez = parse(response.getBody(), 8, authKey);
            }
            if ("0".equals(responseRez.get(resultStatusKey))) {
                success = true;
                retval.put("id", Long.toString(id));
                retval.put("amount", new Double(amount));
                retval.put("txtTransactionID", responseRez.get("txtTransactionID"));
                retval.put("txtReferenceNumber", responseRez.get("txtReferenceNumber"));
            } else {
                error = error + "\n" + (trType == 4 ? getCaptureError(responseRez) : getAuthError(responseRez));
            }
        } catch (Exception e) {
            error = error + "\n" + e.getMessage();
        }
        writeLog(trId, id, amount, trType, request.toString(), strResp, error, success);
        if (!success) {
            throw new Exception(error);
        }
        return retval;
    }
}
