package psoft.epayment;

import com.clearcommerce.ccxclientapi.CcApiDocument;
import com.clearcommerce.ccxclientapi.CcApiMoney;
import com.clearcommerce.ccxclientapi.CcApiRecord;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/epayment/EPDQ.class */
public class EPDQ extends GenericMerchantGateway {
    protected String server;
    protected String login;
    protected int port;
    protected int clientId;
    protected String password;
    protected String currency;
    protected String ipAddress;
    protected String mode;
    protected String pipeline;
    protected int precision;
    private static HashMap ccTypes = new HashMap();

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.ipAddress = getValue(v, "IP_ADDRESS");
        this.login = getValue(v, "USERNAME");
        this.clientId = Integer.parseInt(getValue(v, "CLIENT_ID"));
        this.password = getValue(v, "PASSWORD");
        this.currency = getValue(v, "CURRENCY");
        this.pipeline = getValue(v, "PIPELINE");
        if (this.pipeline == null || "".equals(this.pipeline)) {
            this.pipeline = "PaymentNoFraud";
        }
        this.mode = getValue(v, "MODE");
        this.precision = getPrecision(this.currency);
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PORT", Integer.toString(this.port));
        map.put("IP_ADDRESS", this.ipAddress);
        map.put("USERNAME", this.login);
        map.put("CLIENT_ID", Integer.toString(this.clientId));
        map.put("PASSWORD", this.password);
        map.put("CURRENCY", this.currency);
        map.put("MODE", this.mode);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap cRes = doTransaction(0, id, amount, description, cc, null);
        writeCharge(amount);
        return cRes;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap cRes = doTransaction(1, id, amount, description, cc, null);
        writeAuthorize(amount);
        return cRes;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap cRes = doTransaction(4, id, amount, description, cc, data);
        writeCapture(amount);
        return cRes;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap cRes = doTransaction(3, id, amount, "", cc, data);
        writeVoid(amount);
        return cRes;
    }

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        String error;
        CcApiDocument docEngine = new CcApiDocument();
        new CcApiRecord();
        new CcApiRecord();
        CcApiRecord order = new CcApiRecord();
        long trId = writeLog(id, amount, trType);
        HashMap map = new HashMap();
        int errCode = -1;
        StringBuffer preError = new StringBuffer();
        CcApiDocument docResult = new CcApiDocument();
        String request = "";
        String response = "";
        StringWriter out = new StringWriter();
        String errMess = "\n Credit card processing error: ";
        try {
            switch (trType) {
                case 0:
                case 1:
                    CcApiRecord recOrderFormDoc = prepareRequest(id, docEngine);
                    CcApiRecord recConsumer = recOrderFormDoc.addRecord("Consumer");
                    recConsumer.setFieldString("Email", cc.getEmail());
                    CcApiRecord recPaymentMech = recConsumer.addRecord("PaymentMech");
                    recPaymentMech.setFieldString("Type", "CreditCard");
                    CcApiRecord recCreditCard = recPaymentMech.addRecord("CreditCard");
                    recCreditCard.setFieldS32("Type", getType(cc.getType()));
                    recCreditCard.setFieldString("Number", cc.getNumber());
                    recCreditCard.setFieldExpirationDate("Expires", cc.getExp());
                    if ("SWITCH".equals(cc.getType()) || "SOLO".equals(cc.getType())) {
                        if (!"".equals(cc.getIssueNo())) {
                            recCreditCard.setFieldString("IssueNum", cc.getIssueNo());
                        }
                        if (!"".equals(cc.getStartDate())) {
                            recCreditCard.setFieldStartDate("StartDate", cc.getStartDate());
                        }
                    }
                    CcApiRecord recBillTo = recConsumer.addRecord("BillTo");
                    CcApiRecord recLocation = recBillTo.addRecord("Location");
                    CcApiRecord recAddress = recLocation.addRecord("Address");
                    recAddress.setFieldString("Name", cc.getName());
                    recAddress.setFieldString("Street1", cc.getAddress());
                    recAddress.setFieldString("City", cc.getCity());
                    recAddress.setFieldString("StateProv", cc.getState());
                    recAddress.setFieldString("PostalCode", cc.getZip());
                    recAddress.setFieldString("Country", ISOCodes.getISOByCountry(cc.getCountry()));
                    CcApiRecord recTransaction = recOrderFormDoc.addRecord("Transaction");
                    if (0 == trType) {
                        recTransaction.setFieldString("Type", "Auth");
                    } else {
                        recTransaction.setFieldString("Type", "PreAuth");
                    }
                    CcApiRecord recCurTotals = recTransaction.addRecord("CurrentTotals");
                    CcApiRecord recTotals = recCurTotals.addRecord("Totals");
                    CcApiMoney money = new CcApiMoney(formatAmount(amount), this.currency);
                    recTotals.setFieldMoney("Total", money);
                    break;
                case 3:
                case 4:
                    CcApiRecord recOrderFormDoc2 = prepareRequest(id, docEngine);
                    recOrderFormDoc2.setFieldString("Id", (String) data.get("id"));
                    CcApiRecord recTransaction2 = recOrderFormDoc2.addRecord("Transaction");
                    if (3 == trType) {
                        recTransaction2.setFieldString("Type", "Void");
                        break;
                    } else {
                        recTransaction2.setFieldString("Type", "PostAuth");
                        break;
                    }
            }
            docResult = docEngine.process(this.server, (short) this.port, true);
            CcApiRecord recEngine = docResult.getFirstRecord("EngineDoc");
            CcApiRecord recMessageList = recEngine.getFirstRecord("MessageList");
            if (recMessageList != null) {
                for (CcApiRecord recMessage = recMessageList.getFirstRecord("Message"); recMessage != null; recMessage = recMessageList.getNextRecord("Message")) {
                    preError.append(recMessage.getFieldString("Text")).append("\n");
                }
            }
            order = recEngine.getFirstRecord("OrderFormDoc");
            errCode = order.getFirstRecord("Transaction").getFirstRecord("CardProcResp").getFieldS32("CcErrCode").intValue();
            errMess = errMess + order.getFirstRecord("Transaction").getFirstRecord("CardProcResp").getFieldString("CcReturnMsg");
        } catch (Exception e) {
            String str = "" + e.getMessage();
        }
        if (errCode == 1) {
            map.put("id", order != null ? order.getFieldString("Id") : "unknown");
            map.put("amount", new Double(amount));
            error = "";
        } else {
            error = preError.toString() + errMess;
        }
        try {
            docEngine.writeTo(out);
            request = out.toString();
            StringWriter out2 = new StringWriter();
            docResult.writeTo(out2);
            response = out2.toString();
        } catch (Exception e2) {
            error = error + "\n Error getting dump of CC request/response";
            e2.printStackTrace();
        }
        writeLog(trId, id, amount, trType, dataReplase(request.toString(), cc.getNumber()), dataReplase(response.toString(), cc.getNumber()), error, 1 == errCode);
        if (errCode != 1) {
            throw new Exception(error);
        }
        return map;
    }

    protected String dataReplase(String dataStr, String ccNam) {
        String rez = dataStr.replaceAll("Number=\"" + ccNam, "Number=\"#######");
        return rez.replaceAll("Name=\"" + this.login, "Name=\"-------").replaceAll("Password=\"" + this.password, "Password=\"*******");
    }

    private CcApiRecord prepareRequest(long id, CcApiDocument docEngine) throws Exception {
        docEngine.setFieldString("DocVersion", "1.0");
        CcApiRecord recEngine = docEngine.addRecord("EngineDoc");
        recEngine.setFieldString("DocumentId", "1");
        recEngine.setFieldString("ContentType", "OrderFormDoc");
        recEngine.setFieldString("SourceId", "mySource");
        recEngine.setFieldString("IPAddress", this.ipAddress);
        CcApiRecord recUser = recEngine.addRecord("User");
        recUser.setFieldString("Name", this.login);
        recUser.setFieldString("Password", this.password);
        recUser.setFieldS32("ClientId", this.clientId);
        CcApiRecord recInstructions = recEngine.addRecord("Instructions");
        recInstructions.setFieldString("Pipeline", this.pipeline);
        CcApiRecord recOrderFormDoc = recEngine.addRecord("OrderFormDoc");
        recOrderFormDoc.setFieldString("UserId", Long.toString(id));
        recOrderFormDoc.setFieldString("Mode", this.mode);
        return recOrderFormDoc;
    }

    static {
        ccTypes.put("VISA", "1");
        ccTypes.put("MC", "2");
        ccTypes.put("DISC", "3");
        ccTypes.put("AX", "8");
        ccTypes.put("DINERS", "4");
        ccTypes.put("BLANCHE", "5");
        ccTypes.put("JCB", "6");
        ccTypes.put("ENROUTE", "7");
        ccTypes.put("SOLO", "9");
        ccTypes.put("SWITCH", "10");
        ccTypes.put("ELECTRON", "11");
    }

    private int getType(String type) throws Exception {
        String t = (String) ccTypes.get(type);
        if (t == null) {
            throw new Exception("Unable to resolve CC type: " + type);
        }
        return Integer.parseInt(t);
    }

    protected int getPrecision(String currency) {
        return ISOCodes.getPrecisionByISOCode(currency);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public String formatAmount(double amount) {
        return Long.toString(Math.round(amount * Math.pow(10.0d, this.precision)));
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "ePDQ (http://www.epdq.co.uk/)";
    }
}
