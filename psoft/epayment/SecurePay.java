package psoft.epayment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/epayment/SecurePay.class */
public class SecurePay extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String merchantId;
    protected String avsReq;
    private static final SimpleDateFormat expMonthFormat = new SimpleDateFormat("MM");
    private static final SimpleDateFormat expYearFormat = new SimpleDateFormat("yy");

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("AVSREQ", this.avsReq);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
        this.server = getValue(values, "SERVER");
        this.path = getValue(values, "PATH");
        this.port = Integer.parseInt(getValue(values, "PORT"));
        this.merchantId = getValue(values, "MERCH_ID");
        this.avsReq = getValue(values, "AVSREQ");
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
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = processTransaction(4, id, description, amount, data, cc);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap retval = processTransaction(3, id, description, amount, data, cc);
        writeVoid(amount);
        return retval;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "SecurePay";
    }

    private HashMap processTransaction(int trType, long id, String description, double amount, HashMap data, CreditCard cc) throws Exception {
        String error = "";
        boolean success = false;
        long logId = writeLog(id, amount, trType);
        HashMap result = new HashMap();
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        request.put("Amount", formatAmount(amount));
        request.put("Email", cc.getEmail());
        request.put("Merch_ID", this.merchantId);
        request.put("CC_Method", "DataEntry");
        request.put("CC_Number", cc.getNumber());
        request.put("Month", cc.getExp(expMonthFormat));
        request.put("Year", cc.getExp(expYearFormat));
        request.put("Name", cc.getFirstName() + " " + cc.getLastName());
        request.put("Street", cc.getAddress());
        request.put("City", cc.getCity());
        request.put("State", cc.getState());
        request.put("Zip", cc.getZip());
        request.put("Comment1", description);
        request.put("Comment2", Long.toString(id));
        request.put("AVSREQ", this.avsReq);
        switch (trType) {
            case 0:
                request.put("Tr_Type", "SALE");
                break;
            case 1:
                request.put("Tr_Type", "PREAUTH");
                break;
            case 3:
                request.put("VoidRecNum", data.get("VoidRecNum"));
                break;
            case 4:
                request.put("APPROVNUMBER", data.get("APPROVNUMBER"));
                break;
        }
        try {
            response = postForm(request);
            if ("Y".equals(response.get("Return_Code"))) {
                success = true;
                result.put("VoidRecNum", response.get("VoidRecNum"));
                result.put("APPROVNUMBER", response.get("APPROVNUMBER"));
                result.put("amount", new Double(amount));
                result.put("id", Long.toString(logId));
            } else {
                error = (String) response.get("Card_Response");
            }
        } catch (IOException ioex) {
            error = "Problem connecting to processor center: " + ioex.getMessage();
        } catch (Exception ex) {
            error = "Problem processing Credit Card: " + ex.getMessage();
        }
        writeLog(logId, id, amount, trType, request.toString(), response.toString(), error, success);
        if (!success) {
            throw new Exception(error);
        }
        return result;
    }

    private HashMap postForm(HashMap request) throws Exception {
        HashMap results = new HashMap();
        HttpResponse httpresponse = HttpUtils.getForm("https", this.server, this.port, this.path, request);
        String response = httpresponse.getBody();
        StringTokenizer st = new StringTokenizer(response, ",");
        results.put("Return_Code", st.nextToken());
        results.put("APPROVNUMBER", st.nextToken());
        results.put("Card_Response", st.nextToken());
        results.put("AVS_Response", st.nextToken());
        results.put("VoidRecNum", st.nextToken());
        return results;
    }
}
