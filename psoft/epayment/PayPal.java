package psoft.epayment;

import CoreComponentTypes.apis.ebay.BasicAmountType;
import PayPalAPI.api.ebay.DoCaptureRequestType;
import PayPalAPI.api.ebay.DoCaptureResponseType;
import PayPalAPI.api.ebay.DoDirectPaymentRequestType;
import PayPalAPI.api.ebay.DoDirectPaymentResponseType;
import PayPalAPI.api.ebay.DoVoidRequestType;
import PayPalAPI.api.ebay.DoVoidResponseType;
import com.paypal.sdk.profiles.APIProfile;
import com.paypal.sdk.profiles.ProfileFactory;
import com.paypal.sdk.services.CallerServices;
import eBLBaseComponents.apis.ebay.AddressType;
import eBLBaseComponents.apis.ebay.CompleteCodeType;
import eBLBaseComponents.apis.ebay.CountryCodeType;
import eBLBaseComponents.apis.ebay.CreditCardDetailsType;
import eBLBaseComponents.apis.ebay.CreditCardTypeType;
import eBLBaseComponents.apis.ebay.CurrencyCodeType;
import eBLBaseComponents.apis.ebay.DoCaptureResponseDetailsType;
import eBLBaseComponents.apis.ebay.DoDirectPaymentRequestDetailsType;
import eBLBaseComponents.apis.ebay.ErrorType;
import eBLBaseComponents.apis.ebay.PayPalUserStatusCodeType;
import eBLBaseComponents.apis.ebay.PayerInfoType;
import eBLBaseComponents.apis.ebay.PaymentActionCodeType;
import eBLBaseComponents.apis.ebay.PaymentDetailsType;
import eBLBaseComponents.apis.ebay.PersonNameType;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Category;
import psoft.hsphere.Session;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;

/* loaded from: hsphere.zip:psoft/epayment/PayPal.class */
public class PayPal extends GenericMerchantGateway {
    protected String TITLE;
    protected String description;
    protected String server;
    protected String port;
    protected String path;
    protected String merchantId;
    protected String testmode;
    protected String currency;
    protected String apiUserName;
    protected String apiPassword;
    protected String certificateFile;
    protected String privateKeyPassword;
    private static Category log = Category.getInstance(PayPal.class);
    private static final SimpleDateFormat expMonthFormat = new SimpleDateFormat("MM");
    private static final SimpleDateFormat expYearFormat = new SimpleDateFormat("yyyy");

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("TITLE", this.TITLE);
        map.put("description", this.description);
        map.put("server", this.server);
        map.put("port", this.port);
        map.put("path", this.path);
        map.put("merchantId", this.merchantId);
        map.put("testmode", this.testmode);
        map.put("currency", this.currency);
        map.put("apiUserName", this.apiUserName);
        map.put("apiPassword", this.apiPassword);
        map.put(MerchantGatewayManager.MG_UPLOAD_FILE_CERT, this.certificateFile);
        map.put("privateKeyPassword", this.privateKeyPassword);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.TITLE = getValue(v, "TITLE");
        this.description = getValue(v, "description");
        this.server = getValue(v, "server");
        this.port = getValue(v, "port");
        this.path = getValue(v, "path");
        this.merchantId = getValue(v, "merchantId");
        this.testmode = getValue(v, "testmode");
        this.currency = getValue(v, "currency");
        this.apiUserName = getValue(v, "apiUserName");
        this.apiPassword = getValue(v, "apiPassword");
        this.certificateFile = getValue(v, MerchantGatewayManager.MG_UPLOAD_FILE_CERT);
        this.privateKeyPassword = getValue(v, "privateKeyPassword");
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap result = doTransaction(0, id, amount, description, cc, null);
        writeCharge(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap result = doTransaction(1, id, amount, description, cc, null);
        writeAuthorize(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap result = doTransaction(3, id, amount, description, cc, data);
        writeCapture(amount);
        return result;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        HashMap result = doTransaction(4, id, amount, "", cc, data);
        writeVoid(amount);
        return result;
    }

    private HashMap doTransaction(int trType, long id, double amount, String description, CreditCard cc, HashMap data) throws Exception {
        long trId = writeLog(id, amount, trType);
        HashMap map = new HashMap();
        DoDirectPaymentRequestDetailsType requestDetails = new DoDirectPaymentRequestDetailsType();
        PaymentDetailsType paymentDetails = new PaymentDetailsType();
        AddressType payerAddress = new AddressType();
        PayerInfoType cardHolder = new PayerInfoType();
        CreditCardDetailsType creditCard = new CreditCardDetailsType();
        DoDirectPaymentRequestType paymentReq = new DoDirectPaymentRequestType();
        DoDirectPaymentResponseType paymentResp = new DoDirectPaymentResponseType();
        BasicAmountType totalAmount = new BasicAmountType();
        DoCaptureRequestType paymentCaptureReq = new DoCaptureRequestType();
        DoDirectPaymentResponseType doCaptureResponseType = new DoCaptureResponseType();
        DoVoidRequestType paymentVoidReq = new DoVoidRequestType();
        DoDirectPaymentResponseType doVoidResponseType = new DoVoidResponseType();
        String strError = "";
        boolean success = false;
        String strReqLog = "";
        String strRespLog = "";
        String strPayPalEnvironment = "live";
        if ("on".equals(this.testmode)) {
            strReqLog = strReqLog + "TestMode=on&";
            strPayPalEnvironment = "sandbox";
        }
        switch (trType) {
            case 0:
            case 1:
                if (1 == trType) {
                    requestDetails.setPaymentAction(PaymentActionCodeType.Authorization);
                } else {
                    requestDetails.setPaymentAction(PaymentActionCodeType.Sale);
                }
                totalAmount.set_value(Double.toString(amount));
                totalAmount.setCurrencyID(CurrencyCodeType.fromString(ISOCodes.getShortNameByISO(this.currency)));
                paymentDetails.setOrderTotal(totalAmount);
                payerAddress.setStreet1(cc.getAddress());
                payerAddress.setCityName(cc.getCity());
                payerAddress.setStateOrProvince(cc.getState());
                payerAddress.setPostalCode(cc.getZip());
                payerAddress.setCountry(CountryCodeType.fromString(cc.getCountry()));
                cardHolder.setAddress(payerAddress);
                PersonNameType payerName = new PersonNameType();
                payerName.setFirstName(cc.getFirstName());
                payerName.setLastName(cc.getLastName());
                cardHolder.setPayerName(payerName);
                cardHolder.setPayer(cc.getEmail());
                cardHolder.setPayerCountry(CountryCodeType.fromString(cc.getCountry()));
                creditCard.setCreditCardNumber(cc.getNumber());
                cardHolder.setPayerID(Long.toHexString(id));
                cardHolder.setPayerStatus(PayPalUserStatusCodeType.verified);
                creditCard.setCreditCardNumber(cc.getNumber());
                Integer tmpMonth = new Integer(cc.getExp(expMonthFormat));
                creditCard.setExpMonth(tmpMonth.intValue());
                Integer tmpYear = new Integer(cc.getExp(expYearFormat));
                creditCard.setExpYear(tmpYear.intValue());
                String strCVV = cc.getCVV();
                strCVV = ("".equals(strCVV) || "null".equals(strCVV) || "null" == strCVV || null == strCVV) ? "000" : "000";
                creditCard.setCVV2(strCVV);
                creditCard.setCreditCardType(getCreditCardType(cc.getType()));
                creditCard.setCardOwner(cardHolder);
                String strReqLog2 = (((((((((((((strReqLog + "CurrencyCode=" + ISOCodes.getShortNameByISO(this.currency)) + "&Address=" + cc.getAddress()) + "&City=" + cc.getCity()) + "&State=" + cc.getState()) + "&Zip=" + cc.getZip()) + "&Country=" + cc.getCountry()) + "&FirstName=" + cc.getFirstName()) + "&LastName=" + cc.getLastName()) + "&Email=" + cc.getEmail()) + "&PayerIDhex=" + Long.toHexString(id)) + "&Status=verified") + "&ccNumber=" + cc.getNumber()) + "&ccDate=" + cc.getExp()) + "&ccType=" + cc.getType();
                requestDetails.setCreditCard(creditCard);
                requestDetails.setPaymentDetails(paymentDetails);
                if ("on".equals(this.testmode)) {
                    requestDetails.setIPAddress("12.36.5.78");
                    strReqLog = strReqLog2 + "&IPAddress=12.36.5.78";
                } else {
                    requestDetails.setIPAddress(Session.getRequest().getRemoteAddr());
                    strReqLog = strReqLog2 + "&IPAddress=" + Session.getRequest().getRemoteAddr();
                }
                paymentReq.setDoDirectPaymentRequestDetails(requestDetails);
                break;
            case 3:
                strReqLog = "AuthorizationID=" + data.get("id").toString();
                paymentVoidReq.setAuthorizationID(data.get("id").toString());
                break;
            case 4:
                totalAmount.set_value(Double.toString(amount));
                totalAmount.setCurrencyID(CurrencyCodeType.fromString(ISOCodes.getShortNameByISO(this.currency)));
                paymentCaptureReq.setAmount(totalAmount);
                paymentCaptureReq.setAuthorizationID(data.get("id").toString());
                paymentCaptureReq.setCompleteType(CompleteCodeType.Complete);
                strReqLog = (("CurrencyCode=" + ISOCodes.getShortNameByISO(this.currency)) + "&AuthorizationID=" + data.get("id").toString()) + "&CompleteType=Complete";
                break;
        }
        CallerServices callerSrv = new CallerServices();
        callerSrv.initialize();
        APIProfile profile = ProfileFactory.createAPIProfile();
        profile.setAPIUsername(this.apiUserName);
        profile.setAPIPassword(this.apiPassword);
        profile.setCertificateFile("/hsphere/local/home/cpanel/shiva/psoft_config/mg_certs/" + this.certificateFile);
        profile.setPrivateKeyPassword(this.privateKeyPassword);
        profile.setEnvironment(strPayPalEnvironment);
        callerSrv.setAPIProfile(profile);
        String strReqLog3 = (strReqLog + "&Environment=" + strPayPalEnvironment) + "&CertificateFile=" + this.certificateFile;
        try {
            DoDirectPaymentResponseType respTmp = new DoDirectPaymentResponseType();
            DoDirectPaymentResponseType doDirectPaymentResponseType = respTmp;
            switch (trType) {
                case 0:
                case 1:
                    paymentResp = (DoDirectPaymentResponseType) callerSrv.call("DoDirectPayment", paymentReq);
                    doDirectPaymentResponseType = paymentResp;
                    break;
                case 3:
                    doVoidResponseType = (DoVoidResponseType) callerSrv.call("DoVoid", paymentVoidReq);
                    doDirectPaymentResponseType = doVoidResponseType;
                    break;
                case 4:
                    doCaptureResponseType = (DoCaptureResponseType) callerSrv.call("DoCapture", paymentCaptureReq);
                    doDirectPaymentResponseType = doCaptureResponseType;
                    break;
            }
            String strRespLog2 = "Ack=" + doDirectPaymentResponseType.getAck().toString();
            if (doDirectPaymentResponseType.getErrors() != null || !"Success".equals(doDirectPaymentResponseType.getAck().toString())) {
                ErrorType er = doDirectPaymentResponseType.getErrors(0);
                strError = (strError + "Error Code: " + er.getErrorCode()) + "\nError Message: " + er.getLongMessage();
                strRespLog = strRespLog2 + "&ErrorData=" + strError;
            } else {
                strRespLog = strRespLog2 + "&ErrorData=none";
                success = true;
                switch (trType) {
                    case 0:
                    case 1:
                        strRespLog = strRespLog + "&TransactionID=" + paymentResp.getTransactionID();
                        map.put("id", paymentResp.getTransactionID());
                        break;
                    case 3:
                        strRespLog = strRespLog + "&AuthorizationID=" + doVoidResponseType.getAuthorizationID();
                        map.put("id", doVoidResponseType.getAuthorizationID());
                        break;
                    case 4:
                        new DoCaptureResponseDetailsType();
                        DoCaptureResponseDetailsType paymentRespDetail = doCaptureResponseType.getDoCaptureResponseDetails();
                        map.put("id", paymentRespDetail.getAuthorizationID());
                        strRespLog = ((strRespLog + "&AuthorizationID=" + paymentRespDetail.getAuthorizationID()) + "&ParentTransactionID=" + paymentRespDetail.getPaymentInfo().getParentTransactionID()) + "&PaymentDate=" + paymentRespDetail.getPaymentInfo().getPaymentDate().getTime().toString();
                        break;
                }
                map.put("amount", new Double(amount));
            }
        } catch (Exception e) {
            strError = strError + e.getMessage();
        }
        writeLog(trId, id, amount, trType, strReqLog3, strRespLog, strError, success);
        if (!success) {
            throw new Exception(strError);
        }
        return map;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "PayPal";
    }

    protected CreditCardTypeType getCreditCardType(String ccType) {
        if ("VISA".equals(ccType)) {
            return CreditCardTypeType.Visa;
        }
        if ("AX".equals(ccType)) {
            return CreditCardTypeType.Amex;
        }
        if ("MC".equals(ccType)) {
            return CreditCardTypeType.MasterCard;
        }
        if ("DISC".equals(ccType)) {
            return CreditCardTypeType.Discover;
        }
        return null;
    }
}
