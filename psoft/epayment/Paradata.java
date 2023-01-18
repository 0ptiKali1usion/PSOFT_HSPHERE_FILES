package psoft.epayment;

import com.paygateway.CreditCardRequest;
import com.paygateway.CreditCardResponse;
import com.paygateway.TransactionClient;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/epayment/Paradata.class */
public class Paradata extends GenericMerchantGateway {
    protected String accountToken;
    private static HashMap ccTypes = new HashMap();

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.accountToken = (String) v.get("ACCOUNT_TOKEN");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("ACCOUNT_TOKEN", this.accountToken);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        CreditCardRequest cReq = prepareRequest(id, description, amount, cc);
        cReq.setChargeType("SALE");
        CreditCardResponse cRes = doTransaction(id, cReq);
        writeCharge(amount);
        return toHashMap(cRes, amount);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        CreditCardRequest cReq = prepareRequest(id, description, amount, cc);
        cReq.setChargeType("AUTH");
        CreditCardResponse cRes = doTransaction(id, cReq);
        writeAuthorize(amount);
        return toHashMap(cRes, amount);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        CreditCardRequest cReq = new CreditCardRequest();
        cReq.setOrderId((String) data.get("orderId"));
        cReq.setCaptureReferenceId((String) data.get("captureId"));
        cReq.setChargeType("CAPTURE");
        cReq.setChargeTotal(amount);
        cReq.setCreditCardNumber(cc.getNumber());
        cReq.setExpireMonth(getMonth(cc.getExp()));
        cReq.setExpireYear(getYear(cc.getExp()));
        cReq.setBillEmail(cc.getEmail());
        cReq.setBillFirstName(cc.getFirstName());
        cReq.setBillLastName(cc.getLastName());
        CreditCardResponse cRes = doTransaction(id, cReq);
        writeCapture(amount);
        return toHashMap(cRes, amount);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        CreditCardRequest cReq = new CreditCardRequest();
        cReq.setCreditCardNumber(cc.getNumber());
        cReq.setExpireMonth(getMonth(cc.getExp()));
        cReq.setExpireYear(getYear(cc.getExp()));
        cReq.setBillEmail(cc.getEmail());
        cReq.setBillFirstName(cc.getFirstName());
        cReq.setBillLastName(cc.getLastName());
        cReq.setOrderId((String) data.get("orderId"));
        cReq.setChargeType("VOID");
        cReq.setChargeTotal(amount);
        CreditCardResponse cRes = doTransaction(id, cReq);
        writeVoid(amount);
        return toHashMap(cRes, amount);
    }

    private CreditCardResponse doTransaction(long id, CreditCardRequest cReq) throws Exception {
        CreditCardResponse cRes = (CreditCardResponse) TransactionClient.doTransaction(cReq, this.accountToken);
        writeLog(id, cReq, cRes);
        checkError(cRes);
        return cRes;
    }

    private void writeLog(long id, CreditCardRequest cReq, CreditCardResponse cRes) {
        String request = "";
        String response = "";
        try {
            request = cReq.getPostString();
            response = cRes.getResponseString();
        } catch (Exception e) {
            System.err.println("Error getting dump of CC request/response");
            e.printStackTrace();
        }
        super.writeLog(id, request, response, cRes.getResponseCode() != 1 ? cRes.getResponseCodeText() : "");
    }

    private HashMap toHashMap(CreditCardResponse cRes, double amount) {
        HashMap map = new HashMap();
        map.put("amount", new Double(amount));
        map.put("id", cRes.getBankTransactionId());
        map.put("orderId", cRes.getOrderId());
        map.put("captureId", cRes.getCaptureReferenceId());
        map.put("cRes", cRes);
        return map;
    }

    private void checkError(CreditCardResponse cRes) throws Exception {
        if (cRes.getResponseCode() != 1) {
            throw new Exception(cRes.getResponseCodeText());
        }
    }

    private CreditCardRequest prepareRequest(long id, String description, double amount, CreditCard cc) throws Exception {
        CreditCardRequest cReq = new CreditCardRequest();
        cReq.setCreditCardNumber(cc.getNumber());
        cReq.setExpireMonth(getMonth(cc.getExp()));
        cReq.setExpireYear(getYear(cc.getExp()));
        cReq.setBillEmail(cc.getEmail());
        cReq.setBillFirstName(cc.getFirstName());
        cReq.setBillLastName(cc.getLastName());
        cReq.setChargeTotal(amount);
        cReq.setCardBrand(getType(cc.getType()));
        cReq.setOrderDescription(description);
        cReq.setBillCompany(cc.getName());
        cReq.setBillAddressOne(cc.getAddress());
        cReq.setBillCity(cc.getCity());
        cReq.setBillStateOrProvince(cc.getState());
        cReq.setBillPostalCode(cc.getZip());
        cReq.setBillCountryCode(cc.getCountry());
        cReq.setBillPhone(cc.getPhone());
        return cReq;
    }

    static {
        ccTypes.put("VISA", "VISA");
        ccTypes.put("AX", "AMEX");
        ccTypes.put("DINERS", "DINERS");
        ccTypes.put("DISC", "DISCOVER");
        ccTypes.put("EUROCARD", "EUROCARD");
        ccTypes.put("MC", "MASTERCARD");
        ccTypes.put("NOVA", "NOVA");
    }

    private String getType(String type) {
        String t = (String) ccTypes.get(type);
        if (t == null) {
            return type;
        }
        return t;
    }

    private String getYear(String exp) {
        StringTokenizer st = new StringTokenizer(exp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        st.nextToken();
        String year = st.nextToken();
        if (year.length() == 2) {
            year = year + "20" + year;
        }
        return year;
    }

    private String getMonth(String exp) {
        StringTokenizer st = new StringTokenizer(exp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        return st.nextToken();
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Paradata (www.paradata.com)";
    }
}
