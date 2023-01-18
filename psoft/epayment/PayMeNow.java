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

/* loaded from: hsphere.zip:psoft/epayment/PayMeNow.class */
public class PayMeNow extends GenericMerchantGateway {
    private static DateFormat monthFormat = new SimpleDateFormat("MM");
    private static DateFormat yearFormat = new SimpleDateFormat("yyyy");
    private String acctid;
    private String server;
    private int port;
    private String path;

    static {
        defaultValues.put("SERVER", "trans.atsbank.com");
        defaultValues.put("PORT", "443");
        defaultValues.put("PATH", "/cgi-bin/trans.cgi");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("ACCTID", this.acctid);
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("AUTHPATH", this.path);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
        this.acctid = getValue(values, "ACCTID");
        this.server = getValue(values, "SERVER");
        this.port = Integer.parseInt(getValue(values, "PORT"));
        this.path = getValue(values, "PATH");
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
        writeCharge(amount);
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
        HashMap result = processTransaction(3, id, description, amount, data, cc);
        writeVoid(amount);
        return result;
    }

    private HashMap processTransaction(int trType, long id, String description, double amount, HashMap data, CreditCard cc) throws Exception {
        long logId = writeLog(id, amount, trType);
        HashMap result = new HashMap();
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        String cvv = "";
        String error = "";
        boolean success = false;
        try {
            cvv = cc.getCVV();
            request.put("acctid", this.acctid);
            request.put("amount", formatAmount(amount));
            if (trType == 1 || trType == 0) {
                request.put("action", "ns_quicksale_cc");
                if (trType == 1) {
                    request.put("Authonly", "1");
                } else if (trType == 4) {
                    request.put("postonly", data.get("RefCode"));
                }
                request.put("ccname", cc.getName());
                request.put("ccnum", cc.getNumber());
                request.put("expmon", cc.getExp(monthFormat));
                request.put("expyear", cc.getExp(yearFormat));
                request.put("ci_billaddr1", cc.getAddress());
                request.put("ci_billcity", cc.getCity());
                request.put("ci_billstate", cc.getState());
                request.put("ci_billzip", cc.getZip());
                request.put("ci_billcountry", cc.getCountry());
                request.put("ci_phone", cc.getPhone());
                request.put("ci_email", cc.getEmail());
                request.put("ci_memo", description);
                request.put("merchantordernumber", Long.toString(logId));
                if (!"".equals(cvv)) {
                    request.put("cvv2", cvv);
                }
            } else if (trType == 4) {
                request.put("action", "ns_quicksale_cc");
                request.put("postonly", data.get("RefCode"));
            } else if (trType == 3) {
                request.put("action", "ns_void");
                request.put("orderkeyid", data.get("orderid"));
                request.put("historykeyid", data.get("historyid"));
            }
            HttpResponse httpresponse = HttpUtils.postForm("https", this.server, this.port, this.path, request);
            String message = httpresponse.getBody();
            int pos = message.indexOf("<plaintext>");
            if (pos > 0) {
                message = message.substring(pos + 11);
            }
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
            if (response.containsKey("Accepted")) {
                success = true;
                response.put("id", Long.toString(logId));
                response.put("amount", new Double(amount));
            } else if (response.containsKey("Declined")) {
                error = (String) response.get("Declined");
            } else {
                error = message;
            }
        } catch (IOException ex) {
            error = "Could not connect to " + this.server + ":" + this.port + "/" + this.path;
            Session.getLog().error(error, ex);
        } catch (Exception ex2) {
            error = "Could not perform transaction";
            Session.getLog().error("Could not perform transaction", ex2);
        }
        if (request.containsKey("cvv2")) {
            request.remove("cvv2");
            request.put("cvv2", "****");
        }
        writeLog(logId, id, amount, trType, request.toString(), response.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        if ((trType == 0 || trType == 1) && cvv != null && !"".equals(cvv)) {
            cc.setCVVChecked(success);
        }
        return response;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap checkCC(long acctid, CreditCard cc) throws Exception {
        return checkCCCVV(acctid, cc);
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "http://www.paymenow.com/ (Quicksale Method with a Socket Connection)";
    }
}
