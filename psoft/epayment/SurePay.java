package psoft.epayment;

import com.purepayments.xmlgw.sdk.Adjust;
import com.purepayments.xmlgw.sdk.AdjustResponse;
import com.purepayments.xmlgw.sdk.Auth;
import com.purepayments.xmlgw.sdk.AuthResponse;
import com.purepayments.xmlgw.sdk.BillingAddress;
import com.purepayments.xmlgw.sdk.LineItem;
import com.purepayments.xmlgw.sdk.Request;
import com.purepayments.xmlgw.sdk.Response;
import com.purepayments.xmlgw.sdk.ShippingAddress;
import com.sun.net.ssl.HttpsURLConnection;
import com.sun.net.ssl.internal.ssl.Provider;
import java.net.Socket;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import sun.security.provider.Sun;

/* loaded from: hsphere.zip:psoft/epayment/SurePay.class */
public class SurePay extends GenericMerchantGateway {
    private static int VOID = 0;
    private static int SETTLE = 1;
    protected String merchantId;
    protected String password;
    protected String urlHost;
    protected int urlPort;
    protected String urlFile;
    protected String currency;
    private static final SimpleDateFormat expDate;

    static {
        Security.addProvider(new Sun());
        Security.addProvider(new Provider());
        expDate = new SimpleDateFormat("MM/yy");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.merchantId = getValue(v, "MERCHANTID");
        this.password = getValue(v, "PASSWORD");
        this.urlHost = getValue(v, "URLHOST");
        this.urlPort = Integer.parseInt(getValue(v, "URLPORT"));
        this.urlFile = getValue(v, "URLFILE");
        this.currency = getValue(v, "CURRCODE");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("MERCHANTID", this.merchantId);
        map.put("PASSWORD", this.password);
        map.put("URLHOST", this.urlHost);
        map.put("URLPORT", Integer.toString(this.urlPort));
        map.put("URLFILE", this.urlFile);
        map.put("CURRCODE", this.currency);
        return map;
    }

    protected Response process(Request r) throws Exception {
        Socket socket = null;
        try {
            socket = HttpsURLConnection.getDefaultSSLSocketFactory().createSocket(new Socket(this.urlHost, this.urlPort), this.urlHost, this.urlPort, true);
            Response res = r.send(socket, this.urlFile);
            String error = res.getError();
            if (error != null) {
                throw new Exception(error);
            }
            if (socket != null) {
                socket.close();
            }
            return res;
        } catch (Throwable th) {
            if (socket != null) {
                socket.close();
            }
            throw th;
        }
    }

    protected AuthResponse processAuth(Request r) throws Exception {
        return process(r).getAuthResponse()[0];
    }

    protected AdjustResponse processAdjust(Request r) throws Exception {
        return process(r).getAdjustResponse()[0];
    }

    protected Request setMerchant() {
        Request req = new Request();
        req.setMerchant(this.merchantId);
        req.setPassword(this.password);
        return req;
    }

    protected Request prepareAdjust(String transactionId, int key) {
        Request req = setMerchant();
        Adjust a = new Adjust(req, transactionId);
        if (key == VOID) {
            a.setMarkFor("void");
        } else {
            a.setMarkFor("fulfilled");
        }
        req.addAdjust(a);
        return req;
    }

    protected Request prepareAuth(long id, String description, double amount, CreditCard cc) throws Exception {
        Request request = setMerchant();
        String orderId = new Long(id).toString();
        int length = orderId.length();
        if (length > 16) {
            orderId = orderId.substring(0, length - 16);
        }
        Auth a = new Auth(request, orderId);
        a.setIPAddress("127.0.0.1");
        com.purepayments.xmlgw.sdk.CreditCard card = new com.purepayments.xmlgw.sdk.CreditCard(request, cc.getNumber(), cc.getExp(expDate));
        BillingAddress bi = new BillingAddress(request, cc.getName(), new String[]{cc.getAddress()}, cc.getCity(), cc.getState(), cc.getCountry(), cc.getZip());
        card.setBillingAddress(bi);
        a.setShippingAddress(new ShippingAddress(request, cc.getName(), new String[]{cc.getAddress()}, cc.getCity(), cc.getState(), cc.getCountry(), cc.getZip()));
        a.setCreditCard(card);
        LineItem l = new LineItem(request, description, 1, Long.toString(id), "0.00", Double.toString(amount) + ISOCodes.getShortNameByISO(this.currency));
        a.addLineItem(l);
        request.addAuth(a);
        return request;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        Request req = prepareAuth(id, description, amount, cc);
        Response res = process(req);
        AuthResponse auth = res.getAuthResponse()[0];
        String error = auth.getError();
        if (error == null) {
            Request req2 = prepareAdjust(auth.getTransactionID(), SETTLE);
            Response res2 = process(req2);
            AdjustResponse adj = res2.getAdjustResponse()[0];
            String error2 = adj.getError();
            if (error2 == null) {
                writeCharge(amount);
                writeLog(id, req2.toString(), adj.toString(), error2);
                HashMap map = new HashMap();
                map.put("id", adj.getTransactionID());
                map.put("amount", new Double(amount));
                return map;
            }
            writeLog(id, req2.toString(), adj.toString(), error2);
            Request req3 = prepareAdjust(auth.getTransactionID(), VOID);
            Response res3 = process(req3);
            AdjustResponse adj2 = res3.getAdjustResponse()[0];
            String error3 = adj2.getError();
            writeLog(id, req3.toString(), adj2.toString(), error3);
            throw new Exception(error3);
        }
        writeLog(id, req.toString(), auth.toString(), error);
        throw new Exception(error);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        Request req = prepareAuth(id, description, amount, cc);
        Response res = process(req);
        AuthResponse auth = res.getAuthResponse()[0];
        String error = auth.getError();
        writeLog(id, req.toString(), auth.toString(), error);
        if (error == null) {
            writeAuthorize(amount);
            HashMap map = new HashMap();
            map.put("id", auth.getTransactionID());
            map.put("amount", new Double(amount));
            return map;
        }
        throw new Exception(error);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        Request req = prepareAdjust((String) data.get("id"), SETTLE);
        Response res = process(req);
        AdjustResponse adj = res.getAdjustResponse()[0];
        String error = adj.getError();
        double amount = ((Double) data.get("amount")).doubleValue();
        writeLog(id, req.toString(), adj.toString(), error);
        if (error == null) {
            writeCapture(amount);
            HashMap map = new HashMap();
            map.put("id", adj.getTransactionID());
            map.put("amount", new Double(amount));
            return map;
        }
        throw new Exception(error);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        Request req = prepareAdjust((String) data.get("id"), SETTLE);
        Response res = process(req);
        AdjustResponse adj = res.getAdjustResponse()[0];
        String error = adj.getError();
        writeLog(id, req.toString(), adj.toString(), error);
        double amount = ((Double) data.get("amount")).doubleValue();
        if (error == null) {
            writeVoid(amount);
            HashMap map = new HashMap();
            map.put("id", adj.getTransactionID());
            map.put("amount", new Double(amount));
            return map;
        }
        throw new Exception(error);
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "SurePay (http://www.SurePay.com/)";
    }
}
