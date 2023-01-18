package psoft.epayment;

import com.tucows.paygw.client.CCTransaction;
import com.tucows.paygw.client.CCWSManager_Port;
import com.tucows.paygw.client.CCWSManager_ServiceLocator;
import com.tucows.paygw.client.Customer;
import com.tucows.paygw.client.TransactionResult;
import com.tucows.paygw.client.User;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Session;
import psoft.hsphere.resource.epayment.GenericCreditCard;

/* loaded from: hsphere.zip:psoft/epayment/Tucows.class */
public class Tucows extends GenericMerchantGateway {
    private String login;
    private String password;
    private String mode;
    private String currency;
    private String adminEmail;
    private String avsLevel;

    static {
        defaultValues.put("MODE", "FALSE");
        defaultValues.put("CURRENCY", "840");
        defaultValues.put("AVSLEVEL", "");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("MODE", this.mode);
        map.put("LOGIN", this.login);
        map.put("PASSWD", this.password);
        map.put("CURRENCY", this.currency);
        map.put("NOTIFICATIONEMAIL", this.adminEmail);
        map.put("AVSLEVEL", this.avsLevel);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap values) throws Exception {
        this.f4id = id;
        this.mode = getValue(values, "MODE");
        this.login = getValue(values, "LOGIN");
        this.password = getValue(values, "PASSWD");
        this.currency = getValue(values, "CURRENCY");
        this.adminEmail = getValue(values, "NOTIFICATIONEMAIL");
        this.avsLevel = getValue(values, "AVSLEVEL");
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Tucows.com";
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = processPayment(0, id, description, amount, 0L, cc);
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = processPayment(1, id, description, amount, 0L, cc);
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long transID = Long.parseLong((String) data.get("id"));
        HashMap retval = processPayment(4, id, description, amount, transID, cc);
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long transID = Long.parseLong((String) data.get("id"));
        try {
            sendEmail(this.adminEmail, 3, description, id, "");
        } catch (Exception ex) {
            Session.getLog().error("Error sending notification email: ", ex);
        }
        HashMap retval = new HashMap();
        retval.put("amount", new Double(amount));
        String transid = Long.toString(transID);
        retval.put("id", transid);
        writeLog(0L, id, amount, 3, "This transaction (ID=" + transid + ") should be voided manually", "", "", true);
        writeVoid(amount);
        return retval;
    }

    private HashMap processPayment(int trType, long id, String description, double amount, long transID, CreditCard cc) throws Exception {
        String cvv;
        HashMap retval = new HashMap();
        long trId = writeLog(id, amount, trType);
        long transId = trId;
        if (trType == 3 || trType == 4) {
            transId = transID;
        }
        HashMap request = new HashMap();
        HashMap response = new HashMap();
        String error = "";
        String sCode = ISOCodes.getShortNameByISO(this.currency);
        boolean success = true;
        try {
            TucowsPayment payment = new TucowsPayment(this.login, this.password, id, description, formatAmount(amount), sCode, cc, trType, trId, this.mode);
            request = payment.getRequest();
            response = payment.process(transId);
            long res = ((Long) response.get("txRes")).longValue();
            if (res == 200) {
                retval.put("id", response.get("trid").toString());
                retval.put("amount", new Double(amount));
            } else if (res == 201) {
                success = false;
                error = "Transaction declined by bank, Transaction ID: " + response.get("trid");
            } else if (res == 202) {
                success = false;
                error = "Transaction failed on AVS check, Transaction ID: " + response.get("trid");
            } else {
                success = false;
                error = "Transaction failed. " + response.get("txMes");
            }
        } catch (IOException e) {
            success = false;
            error = "Connect error, " + e.getMessage();
        } catch (Exception e2) {
            success = false;
            error = e2.getMessage();
            Session.getLog().error("Error processing CC:", e2);
        }
        writeLog(trId, id, amount, trType, request.toString(), response.toString(), error, success);
        if ((trType == 1 || trType == 0) && (cvv = cc.getCVV()) != null && !"".equals(cvv)) {
            cc.setCVVChecked(success);
        }
        if (!success) {
            throw new Exception(error);
        }
        return retval;
    }

    public HashMap checkCC(CreditCard cc) throws Exception {
        boolean success = false;
        String error = "";
        String cvv = cc.getCVV();
        if (cvv != null && !"".equals(cvv)) {
            try {
                HashMap data = authorize(-1L, "Checking verification number", 1.0d, cc);
                success = true;
                voidAuthorize(-1L, "Checking verification number", data, cc);
            } catch (Exception ex) {
                error = ex.getMessage();
                Session.getLog().error("Error checking cvv value: ", ex);
            }
        } else {
            cc.setCVVChecked(false);
        }
        if (!success) {
            throw new Exception("Error checking cc verification value. Probably, verification value is incorrect. " + error);
        }
        return new HashMap();
    }

    /* loaded from: hsphere.zip:psoft/epayment/Tucows$TucowsPayment.class */
    public class TucowsPayment {
        public User user;
        public Customer customer;
        public com.tucows.paygw.client.CreditCard creditcard;

        /* renamed from: tx */
        public CCTransaction f10tx;
        private CCWSManager_Port ccManager;
        String wsdlURL;
        String query;
        int trType;

        public TucowsPayment(String username, String password, long id, String description, String amount, String currency, CreditCard cc, int txType, long transid, String mode) throws Exception {
            Tucows.this = r7;
            SimpleDateFormat expMonth = new SimpleDateFormat("MM");
            SimpleDateFormat expYear = new SimpleDateFormat("yy");
            this.user = new User();
            this.user.setName(username);
            this.user.setPassword(password);
            this.customer = new Customer();
            this.customer.setFirstName(cc.getFirstName());
            this.customer.setLastName(cc.getLastName());
            this.customer.setAddress1(cc.getAddress());
            this.customer.setCity(cc.getCity());
            this.customer.setPostal(cc.getZip());
            this.customer.setState(cc.getState());
            this.customer.setCountry(cc.getCountry());
            this.customer.setEmailAddress(cc.getEmail());
            this.customer.setIpAddress("");
            this.creditcard = new com.tucows.paygw.client.CreditCard();
            this.creditcard.setNumber(cc.getNumber());
            this.creditcard.setCvv2(cc.getCVV());
            this.creditcard.setExpiryMonth(cc.getExp(expMonth));
            this.creditcard.setExpiryYear(cc.getExp(expYear));
            this.creditcard.setName(cc.getName());
            this.creditcard.setBillingAddress(cc.getAddress());
            this.creditcard.setBillingZip(cc.getZip());
            this.f10tx = new CCTransaction();
            this.f10tx.setAmount(amount);
            this.f10tx.setCurrency(currency);
            this.f10tx.setOrderID(Long.toString(transid));
            this.f10tx.setAcceptedAVS(r7.avsLevel);
            this.wsdlURL = "TRUE".equals(mode) ? "https://cfs-test.r.tucows.com:5710/glue/cctransact" : "https://cfs.r.tucows.com:5710/glue/cctransact";
            CCWSManager_ServiceLocator service = new CCWSManager_ServiceLocator();
            this.ccManager = service.getCCWSManager(new URL(this.wsdlURL));
            this.trType = txType;
        }

        public HashMap getRequest() {
            HashMap data = new HashMap();
            if (this.user != null) {
                data.put("User Login", this.user.getName());
                data.put("User Passwd", this.user.getPassword());
            }
            if (this.customer != null) {
                data.put("Customer First Name", this.customer.getFirstName());
                data.put("Customer Last Name", this.customer.getLastName());
                data.put("Customer addr", this.customer.getAddress1());
                data.put("Customer city", this.customer.getCity());
                data.put("Customer postal", this.customer.getPostal());
                data.put("Customer state", this.customer.getState());
                data.put("Customer country", this.customer.getCountry());
                data.put("Customer email", this.customer.getEmailAddress());
            }
            if (this.creditcard != null) {
                data.put("CC number", GenericCreditCard.getHiddenNumber(this.creditcard.getNumber()));
                data.put("CC cvv", GenericCreditCard.getHiddenCVV(this.creditcard.getCvv2()));
                data.put("CC exp month", this.creditcard.getExpiryMonth());
                data.put("CC exp year", this.creditcard.getExpiryYear());
                data.put("CC name", this.creditcard.getName());
                data.put("CC addr", this.creditcard.getBillingAddress());
                data.put("CC zip", this.creditcard.getBillingZip());
            }
            if (this.f10tx != null) {
                data.put("amount", this.f10tx.getAmount());
                data.put("currency", this.f10tx.getCurrency());
                data.put("orderID", this.f10tx.getOrderID());
                data.put("acceptedAVS", this.f10tx.getAcceptedAVS());
            }
            return data;
        }

        public HashMap process(long id) throws Exception {
            HashMap res = new HashMap();
            TransactionResult trRes = null;
            switch (this.trType) {
                case 0:
                    trRes = this.ccManager.purchasePayment(this.f10tx, this.user, this.creditcard, this.customer);
                    break;
                case 1:
                    trRes = this.ccManager.authPayment(this.f10tx, this.user, this.creditcard, this.customer);
                    break;
                case 3:
                    trRes = this.ccManager.refundPayment(this.f10tx, this.user, id);
                    break;
                case 4:
                    trRes = this.ccManager.postPayment(this.f10tx, this.user, id);
                    break;
            }
            if (trRes == null) {
                throw new Exception("Transaction type: " + GenericMerchantGateway.getTrDescription(this.trType) + " was not processed");
            }
            res.put("trid", new Long(trRes.getID()));
            res.put("txBankAVS", trRes.getTxBankAVS());
            res.put("txBankMsg", trRes.getTxBankMsg());
            res.put("txBankRes", new Long(trRes.getTxBankRes()));
            res.put("txGwMess", trRes.getTxGwMsg());
            res.put("txGwRes", new Long(trRes.getTxGwRes()));
            res.put("txMes", trRes.getTxMsg());
            res.put("txRes", new Long(trRes.getTxRes()));
            return res;
        }
    }
}
