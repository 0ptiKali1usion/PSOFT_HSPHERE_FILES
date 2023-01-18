package psoft.epayment;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import payandshop.Request;
import payandshop.Response;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/PayAndShop.class */
public class PayAndShop extends GenericMerchantGateway {
    protected String secret;
    protected String merchantId;
    protected String currency;
    protected String account;
    protected int precision;
    private static final SimpleDateFormat expFormat = new SimpleDateFormat("MMyy");

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.secret = getValue(v, "SECRET");
        this.merchantId = getValue(v, "MERCHANT_ID");
        this.account = getValue(v, "ACCOUNT");
        this.currency = getValue(v, "CURRCODE");
        this.precision = ISOCodes.getPrecisionByISOShortName(this.currency);
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SECRET", this.secret);
        map.put("MERCHANT_ID", this.merchantId);
        map.put("ACCOUNT", this.account);
        map.put("CURRCODE", this.currency);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        return charge(id, description, amount, cc, true);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        return charge(id, description, amount, cc, false);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        Request request = new Request("settle");
        setMerchant(request);
        request.set("orderid", (String) data.get("id"));
        request.set("amount", formatAmount(((Double) data.get("amount")).doubleValue()));
        request.set("cardnumber", data.get("cardnumber"));
        request.set("authcode", (String) data.get("authcode"));
        request.set("pasref", (String) data.get("pasref"));
        doTransaction(id, request);
        writeCapture(((Double) data.get("amount")).doubleValue());
        return data;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        Request request = new Request("void");
        setMerchant(request);
        request.set("orderid", (String) data.get("id"));
        request.set("authcode", (String) data.get("authcode"));
        request.set("pasref", (String) data.get("pasref"));
        HashMap cRes = doTransaction(id, request);
        writeVoid(amount);
        return cRes;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "PayAndShop (payandshop.com)";
    }

    protected HashMap charge(long id, String description, double amount, CreditCard cc, boolean autosettle) throws Exception {
        Request request = new Request("auth");
        setMerchant(request);
        request.set("orderid", getTransID(id));
        request.set("amount", formatAmount(amount));
        setCC(request, cc);
        request.set("comment1", description);
        request.set("autosettle", autosettle ? "1" : "0");
        HashMap cRes = doTransaction(id, request);
        if (autosettle) {
            writeCharge(amount);
        } else {
            writeAuthorize(amount);
        }
        cRes.put("amount", new Double(amount));
        cRes.put("cardnumber", cc.getNumber());
        return cRes;
    }

    protected HashMap doTransaction(long id, Request request) throws Exception {
        Response response = null;
        try {
            if (request.isValid()) {
                Response response2 = request.send();
                String result = response2.get("result");
                if (!result.startsWith("00")) {
                    throw new Exception(response2.get("message"));
                }
                HashMap map = new HashMap();
                map.put("result", response2.get("result"));
                map.put("message", response2.get("message"));
                map.put("authcode", response2.get("authcode"));
                map.put("tss", response2.get("tss"));
                map.put("pasref", response2.get("pasref"));
                map.put("id", request.get("orderid"));
                writeLog(id, request.generateXML(), response2.getXML(), "");
                return map;
            }
            throw new Exception("Invalid Credit Card Request");
        } catch (Exception e) {
            if (0 == 0) {
                writeLog(id, request.generateXML(), "", e.getMessage());
            } else {
                writeLog(id, request.generateXML(), response.getXML(), e.getMessage());
            }
            throw e;
        }
    }

    protected void setMerchant(Request request) {
        request.setSecret(this.secret);
        request.set("merchantid", this.merchantId);
        request.set("account", this.account);
        request.set("currency", this.currency);
    }

    protected void setCC(Request request, CreditCard cc) throws Exception {
        request.set("cardnumber", cc.getNumber());
        request.set("cardexpdate", cc.getExp(expFormat));
        request.set("cardtype", cc.getType());
        request.set("cardholdername", cc.getName());
        request.set("billingcode", cc.getZip());
        request.set("billingcountry", ISOCodes.getISOByCountry(cc.getCountry()));
        if (("SWITCH".equals(cc.getType()) || "SOLO".equals(cc.getType())) && !"".equals(cc.getIssueNo())) {
            request.set("issueno", cc.getIssueNo());
        }
    }

    public String getTransID(long id) {
        return id + "_" + TimeUtils.currentTimeMillis();
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public String formatAmount(double amount) {
        return Long.toString(Math.round(amount * Math.pow(10.0d, this.precision)));
    }
}
