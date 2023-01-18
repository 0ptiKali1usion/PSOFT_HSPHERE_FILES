package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.security.Security;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/SkipJack.class */
public class SkipJack extends GenericMerchantGateway {
    protected String server;
    protected int port;
    protected String accountNum = "";
    protected String email = "";
    protected String develAccountNum = "";
    private static final NumberFormat money = NumberFormat.getNumberInstance(new Locale("en", "US"));
    private static DateFormat dfMonth = new SimpleDateFormat("MM");
    private static DateFormat dfYear = new SimpleDateFormat("yy");
    private static final String authorizePath = "/scripts/evolvcc.dll?AuthorizeAPI";
    private static final String settlePath = "/scripts/evolvcc.dll?SJAPI_TransactionChangeStatusRequest";

    static {
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.accountNum = getValue(v, "ACCOUNT");
        this.email = getValue(v, "EMAIL");
        this.develAccountNum = getValue(v, "DEVELACCOUNT");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("ACCOUNT", this.accountNum);
        map.put("EMAIL", this.email);
        map.put("DEVELACCOUNT", this.develAccountNum);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap respons = doTransaction(1, id, amount, description, cc, null);
        try {
            HashMap retval = doTransaction(4, id, amount, description, cc, respons);
            writeCharge(amount);
            return retval;
        } catch (Exception e) {
            String error = "Charge transaction not success\n" + e.getMessage();
            try {
                doTransaction(3, id, amount, description, cc, respons);
            } catch (Exception er) {
                error = error + "\n" + er.getMessage();
            }
            throw new Exception(error);
        }
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

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        boolean success = false;
        HashMap retval = new HashMap();
        HashMap results = new HashMap();
        String error = "";
        String response = "";
        String subPath = authorizePath;
        Hashtable req = new Hashtable();
        long trId = writeLog(id, amount, trType);
        String orderString = Long.toString(id) + "~Hosting services with you control panel account~$" + money.format(amount) + "~1~N~||";
        if (1 == trType) {
            req.put("sjname", cc.getName());
            req.put("Email", cc.getEmail());
            req.put("Streetaddress", cc.getAddress());
            req.put("City", cc.getCity());
            req.put("State", cc.getState());
            req.put("Zipcode", cc.getZip());
            req.put("Ordernumber", Long.toString(TimeUtils.currentTimeMillis()));
            req.put("Accountnumber", cc.getNumber());
            req.put("Month", cc.getExp(dfMonth));
            req.put("Year", cc.getExp(dfYear));
            req.put("Serialnumber", this.accountNum);
            req.put("Transactionamount", money.format(amount));
            req.put("Orderstring", orderString);
            req.put("Shiptophone", cc.getPhone());
        } else {
            subPath = settlePath;
            req.put("szSerialnumber", this.accountNum);
            req.put("szDeveloperSerialNumber", this.develAccountNum);
            req.put("szOrderNumber", data.get("id"));
            if (4 == trType) {
                req.put("szDesiredStatus", "SETTLE");
            } else {
                req.put("szDesiredStatus", "DELETE");
            }
            req.put("szAmount", money.format(amount));
        }
        try {
            HttpResponse resp = HttpUtils.postForm("https", this.server, this.port, subPath, req);
            response = resp.getBody();
            int offset = response.indexOf("\n");
            if (offset < 0) {
                error = error + "Bad response from processor center" + response;
            } else {
                String head = response.substring(0, offset);
                String values = response.substring(offset + 1);
                StringTokenizer st = new StringTokenizer(values, ",");
                results.put("response", response);
                if (1 == trType) {
                    StringTokenizer sthead = new StringTokenizer(head, ",");
                    while (sthead.hasMoreTokens() && st.hasMoreTokens()) {
                        String key = sthead.nextToken();
                        String value = st.nextToken();
                        while (!value.endsWith("\"") && st.hasMoreTokens()) {
                            value = value + st.nextToken();
                        }
                        results.put(key.substring(1, key.length() - 1), value.substring(1, value.length() - 1));
                    }
                    if ("1".equals(results.get("szIsApproved"))) {
                        retval.put("amount", new Double(amount));
                        retval.put("id", results.get("szOrderNumber"));
                        success = true;
                    } else {
                        String validationRetCode = (String) results.get("szReturnCode");
                        if (!"1".equals(validationRetCode)) {
                            error = error + getErrDescr(validationRetCode);
                        } else {
                            error = error + ((String) results.get("szAuthorizationDeclinedMessage"));
                        }
                    }
                } else {
                    HashMap resList = new HashMap();
                    int i = 0;
                    while (st.hasMoreTokens()) {
                        String value2 = st.nextToken();
                        resList.put(Integer.toString(i), value2.substring(1, value2.length() - 1));
                        i++;
                    }
                    if (resList.size() < 7) {
                        results.put("response", values);
                        results.put("StatusResponse", "FAILED");
                        results.put("message", "Processing error" + values);
                    } else {
                        results.put("serialNum", resList.get("0").toString());
                        results.put("amount", resList.get("1").toString());
                        results.put("DesiredStatus", resList.get("2").toString());
                        results.put("StatusResponse", resList.get("3").toString());
                        results.put("StatusResponseMessage", resList.get("4").toString());
                        results.put("OrderNumber", resList.get("5").toString());
                        results.put("TransactionId", resList.get("6").toString());
                        results.put("message", values);
                    }
                    if ("SUCCESSFUL".equals(results.get("StatusResponse"))) {
                        retval.put("amount", new Double(amount));
                        retval.put("id", results.get("OrderNumber"));
                        success = true;
                    } else {
                        error = error + ((String) results.get("message"));
                    }
                }
            }
        } catch (Exception e) {
            error = error + e.getMessage();
        }
        writeLog(trId, id, amount, trType, req.toString(), response, error, success);
        if (!success) {
            throw new Exception(error);
        }
        return retval;
    }

    private String getErrDescr(String code) {
        String mess;
        int errnum = Integer.parseInt(code);
        switch (errnum) {
            case -95:
                mess = "Pos_error_Voice_Authorizations_Not_Allowed";
                break;
            case -94:
                mess = "Pos_error_Blind_Credits_Failed";
                break;
            case -93:
                mess = "Pos_error_Blind_Credits_Not_Allowed";
                break;
            case -92:
                mess = "Pos_error_Error_Approval_Code";
                break;
            case -91:
                mess = "Pos_error_CVV2";
                break;
            case -90:
            case -89:
            case -88:
            case -87:
            case -86:
            case -85:
            case -78:
            case -77:
            case -76:
            case -75:
            case -74:
            case -73:
            case -72:
            case -71:
            case -70:
            case -63:
            case -50:
            case -49:
            case -48:
            case -47:
            case -46:
            case -45:
            case -44:
            case -43:
            case -42:
            case -41:
            case -40:
            case -38:
            case -36:
            default:
                mess = "Unknown error";
                break;
            case -84:
                mess = "Pos error duplicate ordernumber";
                break;
            case -83:
                mess = "Error length shipto phone";
                break;
            case -82:
                mess = "Error length customer state";
                break;
            case -81:
                mess = "Error length customer location";
                break;
            case -80:
                mess = "Error length shipto customer name";
                break;
            case -79:
                mess = "Error length customer name";
                break;
            case -69:
                mess = "Error empty state";
                break;
            case -68:
                mess = "Error empty city";
                break;
            case -67:
                mess = "Error empty street address";
                break;
            case -66:
                mess = "Error empty email";
                break;
            case -65:
                mess = "Error empty name";
                break;
            case -64:
                mess = "Error invalid phone number";
                break;
            case -62:
                mess = "Error length order string";
                break;
            case -61:
                mess = "Error length shipto state";
                break;
            case -60:
                mess = "Error length state";
                break;
            case -59:
                mess = "Error length location";
                break;
            case -58:
                mess = "Error length name";
                break;
            case -57:
                mess = "Error length transaction amount";
                break;
            case -56:
                mess = "Error length shipto street address";
                break;
            case -55:
                mess = "Error length street address";
                break;
            case -54:
                mess = "Error length account number date";
                break;
            case -53:
                mess = "Error length expiration date";
                break;
            case -52:
                mess = "Error length shipto zip code";
                break;
            case -51:
                mess = "Error length zip code";
                break;
            case -39:
                mess = "Error length serial number";
                break;
            case -37:
                mess = "Error failed communication";
                break;
            case -35:
                mess = "Error invalid credit card number";
                break;
        }
        return mess;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "SkipJack ()";
    }

    private void sendEmail(String subject, String description, long id, String mess) throws Exception {
        String message = "";
        if ("VOID".equals(subject)) {
            message = "You need to void transaction with account " + Long.toString(id);
        } else if ("AVS".equals(subject)) {
            message = "The transaction with account " + Long.toString(id) + " returned AVS warning.";
        }
        String body = "Dear administrator,\n" + message + "\ndescription: " + description + "\n" + mess;
        Session.getMailer().sendMessage(this.email, message, body, Session.getCurrentCharset());
    }
}
