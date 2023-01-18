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

/* loaded from: hsphere.zip:psoft/epayment/Protx.class */
public class Protx extends GenericMerchantGateway {
    private String server;
    private int port;
    private String authpath;
    private String settlepath;
    private String login;
    private String currency;
    private String adminEmail;
    private static DateFormat date = new SimpleDateFormat("MMyy");
    private static HashMap ccTypes;

    static {
        defaultValues.put("SERVER", "ukvpstest.protx.com");
        defaultValues.put("PORT", "443");
        defaultValues.put("AUTHPATH", "/VPSDirectAuth/PaymentGateway.asp");
        defaultValues.put("REPEATPATH", "/vps200/dotransaction.dll");
        ccTypes = new HashMap();
        ccTypes.put("AX", "AMEX");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("AUTHPATH", this.authpath);
        map.put("REPEATPATH", this.settlepath);
        map.put("LOGIN", this.login);
        map.put("CURRENCY", this.currency);
        map.put("NOTIFICATIONEMAIL", this.adminEmail);
        return null;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
        this.server = getValue(values, "SERVER");
        this.port = Integer.parseInt(getValue(values, "PORT"));
        this.authpath = getValue(values, "AUTHPATH");
        this.settlepath = getValue(values, "REPEATPATH");
        this.login = getValue(values, "LOGIN");
        this.currency = getValue(values, "CURRENCY");
        this.adminEmail = getValue(values, "NOTIFICATIONEMAIL");
    }

    private HashMap processTransaction(int trType, long id, String description, double amount, HashMap data, CreditCard cc) throws Exception {
        long logId = writeLog(id, amount, trType);
        boolean success = false;
        HashMap result = new HashMap();
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        String error = "";
        String path = "";
        String cvv = "";
        try {
            request.put("VPSProtocol", "2.22");
            request.put("VendorTxCode", Long.toString(logId));
            request.put("Amount", formatAmount(amount));
            request.put("Vendor", this.login);
            String sCode = ISOCodes.getShortNameByISO(this.currency);
            request.put("Currency", sCode);
            request.put("Description", description);
            if (trType == 0 || trType == 1) {
                if (trType == 0) {
                    request.put("TxType", "PAYMENT");
                } else {
                    request.put("TxType", "PREAUTH");
                }
                request.put("CardHolder", cc.getName());
                request.put("CardNumber", cc.getNumber());
                request.put("ExpiryDate", cc.getExp(date));
                String issuenum = cc.getIssueNo();
                if (!"".equals(issuenum)) {
                    request.put("IssueNumber", issuenum);
                }
                cvv = cc.getCVV();
                if (!"".equals(cvv) && cvv != null) {
                    request.put("ApplyAVSCV2", "1");
                    request.put("CV2", cvv);
                } else if (cc.isCVVChecked() == 2) {
                    request.put("ApplyAVSCV2", "2");
                }
                request.put("CardType", getCCType(cc));
                request.put("Address", cc.getAddress());
                request.put("PostCode", cc.getZip());
                path = this.authpath;
            } else if (trType == 4) {
                request.put("service", "VendorRepeatTx");
                request.put("TxType", "REPEAT");
                request.put("RelatedVPSTxID", data.get("id"));
                request.put("RelatedVendorTxCode", data.get("LogID"));
                request.put("RelatedSecurityKey", data.get("SecurityKey"));
                request.put("RelatedTxAuthNo", data.get("TxAuthNo"));
                path = this.settlepath;
            }
            HttpResponse httpresponse = HttpUtils.postForm("https", this.server, this.port, path, request);
            String message = httpresponse.getBody();
            result.put("response", message);
            StringTokenizer st = new StringTokenizer(message, "\n");
            while (st.hasMoreTokens()) {
                String str = st.nextToken();
                int tmppos = str.indexOf("=");
                if (tmppos > 0) {
                    String key = str.substring(0, tmppos);
                    if (str.length() > tmppos + 1) {
                        String value = str.substring(tmppos + 1);
                        response.put(key, value);
                    }
                }
            }
            if (response.containsKey("Status")) {
                String status = (String) response.get("Status");
                if ("OK".equals(status)) {
                    result.put("LogID", Long.toString(logId));
                    result.put("id", response.get("VPSTxID"));
                    result.put("SecurityKey", response.get("SecurityKey"));
                    result.put("TxAuthNo", response.get("TxAuthNo"));
                    result.put("amount", new Double(amount));
                    success = true;
                }
                if (!success) {
                    error = "Status: " + status;
                    if (response.containsKey("StatusDetail")) {
                        error = error + " " + response.get("StatusDetail");
                    }
                }
            }
        } catch (IOException ex) {
            error = "Could not connect to " + this.server + ":" + this.port + "/" + path;
            Session.getLog().error(error, ex);
        } catch (Exception ex2) {
            error = "Could not perform transaction";
            Session.getLog().error("Could not perform transaction", ex2);
        }
        if (request.containsKey("CV2")) {
            request.remove("CV2");
            request.put("CV2", "****");
        }
        writeLog(logId, id, amount, trType, request.toString(), response.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        if ((trType == 0 || trType == 1) && cvv != null && !"".equals(cvv)) {
            cc.setCVVChecked(success);
        }
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap result = processTransaction(0, id, description, amount, new HashMap(), cc);
        writeCharge(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap result = processTransaction(1, id, description, amount, new HashMap(), cc);
        writeAuthorize(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap result = processTransaction(4, id, description, amount, data, cc);
        writeCapture(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        String transid = (String) data.get("id");
        try {
            sendEmail(this.adminEmail, 3, description, id, "");
        } catch (Exception ex) {
            Session.getLog().error("Error sending notification email: ", ex);
        }
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        retval.put("id", transid);
        writeLog(0L, id, amount, 3, "Transaction: \"" + transid + "\" should be voided manually", "", "", true);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap checkCC(long acctid, CreditCard cc) throws Exception {
        return checkCCCVV(acctid, cc);
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "protx.com VSP Direct v2.22";
    }

    private String getCCType(CreditCard cc) {
        String internalType = cc.getType();
        if (ccTypes.containsKey(internalType)) {
            return (String) ccTypes.get(internalType);
        }
        return internalType;
    }
}
