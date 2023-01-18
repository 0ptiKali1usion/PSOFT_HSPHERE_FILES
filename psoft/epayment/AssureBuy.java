package psoft.epayment;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/epayment/AssureBuy.class */
public class AssureBuy extends GenericMerchantGateway {
    protected String title;
    protected String server;
    protected int port;
    protected String path;
    protected String user_id;
    protected String password;
    protected String currency;
    protected String testmode;
    protected String avslevel;
    private static Category log = Category.getInstance(AssureBuy.class);
    private static HashMap ccTypes = new HashMap();

    static {
        ccTypes.put("VISA", "V");
        ccTypes.put("MC", "M");
        ccTypes.put("DISC", "D");
        ccTypes.put("AX", "AM");
        ccTypes.put("DINERS", "DC");
        ccTypes.put("DISC", "DI");
        ccTypes.put("EUROCARD", "EC");
        ccTypes.put("JCB", "JC");
        ccTypes.put("MC", "MC");
        ccTypes.put("VISA", "VI");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("title", this.title);
        map.put("server", this.server);
        map.put("port", Integer.toString(this.port));
        map.put("path", this.path);
        map.put("user_id", this.user_id);
        map.put("password", this.password);
        map.put("currency", this.currency);
        map.put("testmode", this.testmode);
        map.put("avslevel", this.avslevel);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.title = getValue(v, "title");
        this.server = getValue(v, "server");
        String sPort = getValue(v, "port");
        this.port = Integer.parseInt(sPort);
        this.path = getValue(v, "path");
        this.user_id = getValue(v, "user_id");
        this.password = getValue(v, "password");
        this.currency = getValue(v, "currency");
        this.testmode = getValue(v, "testmode");
        this.avslevel = getValue(v, "avslevel");
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap result = processTransaction(0, id, description, amount, new HashMap(), cc);
        writeCharge(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        new HashMap();
        HashMap result = processTransaction(1, id, description, amount, new HashMap(), cc);
        writeAuthorize(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap result = processTransaction(3, id, description, amount, data, cc);
        writeVoid(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap result = processTransaction(4, id, description, amount, data, cc);
        writeCapture(amount);
        return result;
    }

    private HashMap processTransaction(int trType, long id, String description, double amount, HashMap data, CreditCard cc) throws Exception {
        NumberFormat money = NumberFormat.getNumberInstance(new Locale("en", "US"));
        String error = "";
        boolean success = false;
        long logId = writeLog(id, amount, trType);
        HashMap result = new HashMap();
        Hashtable request = new Hashtable();
        request.put("Ecom_UserID", this.user_id);
        request.put("Ecom_Password", this.password);
        if ("on".equals(this.testmode)) {
            request.put("Ecom_Mode", "T");
        } else {
            request.put("Ecom_Mode", "P");
        }
        request.put("Ecom_GatewayVersion", "4.4");
        request.put("Ecom_ReturnCode", "1");
        request.put("Ecom_ResponseType", "1");
        request.put("Ecom_SesssionID", Long.toString(logId));
        request.put("Ecom_Order_Source", "IN");
        request.put("Ecom_Order_CurrencyCode", ISOCodes.getShortNameByISO(this.currency));
        request.put("Ecom_BillTo_Postal_Name_First", cc.getFirstName());
        request.put("Ecom_BillTo_Postal_Name_Last", cc.getLastName());
        request.put("Ecom_BillTo_Postal_Street_Line1", cc.getAddress());
        request.put("Ecom_BillTo_Postal_City", cc.getCity());
        request.put("Ecom_BillTo_Postal_StateProv", cc.getState());
        request.put("Ecom_BillTo_Postal_PostalCode", cc.getZip());
        request.put("Ecom_BillTo_Postal_CountryCode", cc.getCountry());
        request.put("Ecom_BillTo_Telecom_DayPhone", cc.getPhone());
        request.put("Ecom_BillTo_Online_Email", cc.getEmail());
        String sAmount = money.format(amount);
        request.put("Ecom_Order_Cost_SubTotal", sAmount);
        request.put("Ecom_Order_Cost_Total", sAmount);
        request.put("Ecom_Order_Cost_AmountPaid", sAmount);
        switch (trType) {
            case 0:
                request.put("Ecom_Payment_Action", "S");
                break;
            case 1:
                request.put("Ecom_Payment_Action", "A");
                break;
            case 3:
                request.put("Ecom_Payment_Action", "V");
                break;
            case 4:
                request.put("Ecom_Payment_Action", "E");
                break;
        }
        request.put("Ecom_Payment_Type", getType(cc.getType()));
        request.put("Ecom_Payment_Number", cc.getNumber());
        request.put("Ecom_Payment_PayerName", cc.getName());
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            request.put("Ecom_Payment_Verification", cvv);
        }
        DateFormat month = new SimpleDateFormat("MM");
        DateFormat year = new SimpleDateFormat("yyyy");
        request.put("Ecom_Payment_ExpDate_Year", cc.getExp(year));
        request.put("Ecom_Payment_ExpDate_Month", cc.getExp(month));
        if (trType == 4 || trType == 3) {
            request.put("Ecom_Payment_OriginalTransaction", data.get("id"));
        }
        HashMap response = new HashMap();
        try {
            response = postForm(request);
            String res = "";
            if (response.containsKey("Ecom_Trans_Result")) {
                res = (String) response.get("Ecom_Trans_Result");
            }
            if ("Y".equals(res)) {
                String avsAddrRes = "";
                String avsZipRes = "";
                if (response.containsKey("Ecom_Trans_AVS_Address")) {
                    avsAddrRes = (String) response.get("Ecom_Trans_AVS_Address");
                }
                if (response.containsKey("Ecom_Trans_AVS_Zip")) {
                    avsZipRes = (String) response.get("Ecom_Trans_AVS_Zip");
                }
                success = true;
                result.put("id", response.get("Ecom_Trans_AuthCode"));
                result.put("amount", new Double(amount));
                if (!checkAVS(avsAddrRes + avsZipRes) && (trType == 1 || trType == 0)) {
                    try {
                        processTransaction(3, id, description, amount, result, cc);
                    } catch (Exception ex) {
                        log.error("transaction cancellation error: ", ex);
                    }
                    throw new Exception("The payment can't be approved due to the AVS error.  AVS Addr verification result: " + avsAddrRes + " AVS ZIP verification result: " + avsZipRes);
                }
            } else {
                error = "Transaction can't be processed: ";
                if (response.containsKey("Ecom_Error_Code")) {
                    error = (error + " Error code: ") + ((String) response.get("Ecom_Error_Code"));
                }
                if (response.containsKey("Ecom_Error_Description")) {
                    error = (error + " Error description: ") + ((String) response.get("Ecom_Error_Description"));
                }
            }
        } catch (IOException ioex) {
            error = ioex.getMessage();
        } catch (Exception ex2) {
            error = ex2.getMessage();
        }
        if (request.containsKey("Ecom_Payment_Verification")) {
            request.remove("Ecom_Payment_Verification");
            request.put("Ecom_Payment_Verification", "xxxx");
        }
        writeLog(logId, id, amount, trType, request.toString(), response.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        return result;
    }

    private HashMap postForm(Hashtable request) throws Exception {
        HashMap results = new HashMap();
        HttpResponse httpresponse = HttpUtils.postForm("https", this.server, this.port, "/" + this.path, request);
        String response = httpresponse.getBody();
        StringTokenizer st = new StringTokenizer(response, "&");
        while (st.hasMoreTokens()) {
            String newToken = st.nextToken();
            int pos = newToken.indexOf(61);
            if (pos > 0) {
                String key = newToken.substring(0, pos).trim();
                String val = newToken.substring(pos + 1).trim();
                results.put(key, val);
            }
        }
        return results;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "AssureBuy";
    }

    private String getType(String type) throws Exception {
        String typeCC = (String) ccTypes.get(type);
        if (typeCC == null) {
            throw new Exception("Unable to convert CC type: " + type);
        }
        return typeCC;
    }

    protected boolean checkAVS(String avsRes) throws Exception {
        switch (this.avslevel.charAt(0)) {
            case 'F':
                return avsRes.indexOf("YY") >= 0;
            case 'L':
                return avsRes.indexOf("NN") < 0;
            case 'M':
                return avsRes.indexOf(89) >= 0;
            default:
                return true;
        }
    }
}
