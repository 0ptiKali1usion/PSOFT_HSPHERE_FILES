package psoft.epayment;

import clearcommerce.ssl.JCharge;
import clearcommerce.ssl.JOrder;
import clearcommerce.ssl.JOrderItem;
import clearcommerce.ssl.JPayment;
import clearcommerce.ssl.JRequest;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/PSiGate.class */
public class PSiGate extends GenericMerchantGateway {
    protected String server;
    protected String email;
    protected String mode;
    protected String configFile;
    protected String keyFile;
    protected String certFile;
    protected short port;
    private static final SimpleDateFormat expMonth = new SimpleDateFormat("MM");
    private static final SimpleDateFormat expYear = new SimpleDateFormat("yy");

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.email = getValue(v, "EMAIL");
        String fileName = (String) v.get(MerchantGatewayManager.MG_UPLOAD_FILE_CERT);
        if (!"".equals(fileName) && null != fileName) {
            this.certFile = "/hsphere/local/home/cpanel/shiva/psoft_config/mg_certs/" + fileName;
        } else {
            this.certFile = getValue(v, "CERT_FILE");
        }
        String fileName2 = (String) v.get(MerchantGatewayManager.MG_PRIVATE_KEY_PWD);
        if (!"".equals(fileName2) && null != fileName2) {
            this.keyFile = "/hsphere/local/home/cpanel/shiva/psoft_config/mg_certs/" + fileName2;
        } else {
            this.keyFile = getValue(v, "KEY_FILE");
        }
        this.mode = getValue(v, "MODE");
        this.configFile = getValue(v, "CONFIG_FILE");
        this.port = Short.parseShort(getValue(v, "PORT"));
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("EMAIL", this.email);
        map.put(MerchantGatewayManager.MG_UPLOAD_FILE_CERT, MerchantGatewayManager.MG_CERT_PREFIX + this.f4id);
        map.put(MerchantGatewayManager.MG_PRIVATE_KEY_PWD, MerchantGatewayManager.MG_KEY_PREFIX + this.f4id);
        map.put("MODE", this.mode);
        map.put("CONFIG_FILE", this.configFile);
        map.put("PORT", Short.toString(this.port));
        return map;
    }

    protected String getRequest(JRequest myReq, JOrder order) {
        String req = "CCnum=" + myReq.getCardNumber() + ";CCdate=" + myReq.getExpMonth() + "/" + myReq.getExpYear() + ";Email=" + myReq.getEmail() + ";ConfigFile=" + myReq.getConfigFile() + ";Keyfile=" + myReq.getKeyfile() + ";Certfile=" + myReq.getCertfile() + ";ReferenceNumber=" + myReq.getReferenceNumber() + ";Zip=" + myReq.getZip() + ";Addr=" + myReq.getAddr() + ";IP=" + myReq.getIp() + ";Host=" + myReq.getHost() + ";Port=" + ((int) myReq.getPort()) + ";ChargeType=" + myReq.getChargeType() + ";Result=" + myReq.getResult() + ";ChargeType=" + myReq.getChargeType() + ";ChargeTotal=" + myReq.getChargeTotal() + ";Bname=" + order.getBname() + ";Oid=" + order.getOid();
        return req;
    }

    protected String getResponse(JCharge myCh) {
        String req = "Time=" + myCh.getTime() + ";Ref=" + myCh.getRef() + ";Approved" + myCh.getApproved() + ";Ordernum=" + myCh.getOrdernum() + ";Error=" + myCh.getError();
        return req;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap map = doTransaction(0, id, amount, description, cc, null);
        writeCharge(amount);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap map = doTransaction(1, id, amount, description, cc, null);
        writeAuthorize(amount);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap map = doTransaction(4, id, amount, "", cc, data);
        writeVoid(amount);
        return map;
    }

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        long trId = writeLog(id, amount, trType);
        HashMap map = new HashMap();
        String strError = "";
        boolean success = false;
        String strReqLog = "";
        String strRespLog = "";
        JOrder order = new JOrder();
        order.setBname(cc.getName());
        order.setBaddr1(cc.getAddress());
        order.setBcity(cc.getCity());
        order.setBstate(cc.getState());
        order.setBzip(cc.getZip());
        order.setBcountry(ISOCodes.getISOByCountry(cc.getCountry()));
        order.setComments(description);
        JOrderItem myItem = new JOrderItem();
        myItem.setItemNumber(1);
        myItem.setItemID(Long.toString(id));
        myItem.setDescription(description);
        myItem.setPrice(amount);
        myItem.setQuantity(1);
        order.addItems(myItem);
        JRequest myReq = new JRequest();
        myReq.setEmail(this.email);
        myReq.setConfigFile(this.configFile);
        myReq.setKeyfile(this.keyFile);
        myReq.setCertfile(this.certFile);
        myReq.setHost(this.server);
        myReq.setPort(this.port);
        if ("TRUE".equals(this.mode)) {
            myReq.setResult(1);
        } else {
            myReq.setResult(0);
        }
        switch (trType) {
            case 0:
            case 1:
                myReq.setCardNumber(cc.getNumber());
                myReq.setExpMonth(cc.getExp(expMonth));
                myReq.setExpYear(cc.getExp(expYear));
                myReq.setZip(cc.getZip());
                myReq.setAddr(cc.getAddress());
                myReq.setChargeTotal(amount);
                myReq.setChargeType(1 == trType ? 1 : 0);
                order.setOid(Long.toString(TimeUtils.currentTimeMillis()));
                break;
            case 4:
                myReq.setChargeType(2);
                order.setOid((String) data.get("oid"));
                break;
        }
        try {
            JPayment myPay = new JPayment();
            JCharge myCh = myPay.processTransaction(myReq, order);
            if ("APPROVED".equals(myCh.getApproved())) {
                if (1 == trType) {
                    map.put("oid", myCh.getOrdernum());
                }
                map.put("id", myCh.getRef());
                map.put("amount", new Double(amount));
                success = true;
            } else {
                strError = strError + myCh.getError();
            }
            strReqLog = getRequest(myReq, order);
            strRespLog = getResponse(myCh);
        } catch (Exception e) {
            strError = strError + e.getMessage();
        }
        writeLog(trId, id, amount, trType, strReqLog, strRespLog, strError, success);
        if (!success) {
            throw new Exception(strError);
        }
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        writeLog(0L, id, amount, 3, "This transaction (ID=" + ((String) data.get("id")) + ") should be voided", "", "", true);
        writeVoid(amount);
        return data;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "PSiGate (http://www.psigate.com/)";
    }
}
