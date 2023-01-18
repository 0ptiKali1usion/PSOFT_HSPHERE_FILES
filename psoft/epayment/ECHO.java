package psoft.epayment;

import com.openecho.Echo;
import com.sun.net.ssl.internal.ssl.Provider;
import java.security.Security;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import psoft.hsphere.resource.HostEntry;
import psoft.yafv.yafvsym;

/* loaded from: hsphere.zip:psoft/epayment/ECHO.class */
public class ECHO extends GenericMerchantGateway {
    int counter = (int) (Math.random() * 9999.0d);
    protected String merchantEmail;
    protected String merchantId;
    protected String pin;
    protected String email;
    private static final NumberFormat money;
    private static final SimpleDateFormat expMonth;
    private static final SimpleDateFormat expYear;

    static {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        money = NumberFormat.getNumberInstance(new Locale("en", "US"));
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
        expMonth = new SimpleDateFormat("MM");
        expYear = new SimpleDateFormat("yy");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) {
        this.f4id = id;
        this.merchantId = getValue(v, "MERCHANT_ID");
        this.merchantEmail = getValue(v, "MERCHANT_EMAIL");
        this.pin = getValue(v, "MERCHANT_PIN");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("MERCHANT_ID", this.merchantId);
        map.put("MERCHANT_EMAIL", this.merchantEmail);
        map.put("MERCHANT_PIN", this.pin);
        return map;
    }

    protected Echo setMerchant() {
        Echo echo = new Echo();
        echo.merchant_echo_id = this.merchantId;
        echo.merchant_pin = this.pin;
        echo.merchant_email = this.merchantEmail;
        echo.order_type = "S";
        echo.debug = "F";
        return echo;
    }

    protected Echo prepareRequest(long id, String type, String description, double amount, CreditCard cc) throws Exception {
        Echo echo = setMerchant();
        echo.billing_first_name = cc.getFirstName();
        echo.billing_last_name = cc.getLastName();
        echo.billing_address1 = cc.getAddress();
        echo.billing_city = cc.getCity();
        echo.billing_state = cc.getState();
        echo.billing_zip = cc.getZip();
        echo.billing_phone = cc.getPhone();
        echo.billing_email = cc.getEmail();
        echo.cc_number = cc.getNumber();
        echo.billing_ip_address = "10.17.1.145";
        echo.billing_country = cc.getCountry();
        echo.ccexp_month = cc.getExp(expMonth);
        echo.ccexp_year = cc.getExp(expYear);
        echo.transaction_type = type;
        if (this.counter > 30000) {
            this.counter = 1;
        } else {
            this.counter++;
        }
        echo.counter = Integer.toString(this.counter);
        echo.merchant_trace_nbr = Long.toString(id);
        echo.grand_total = money.format(amount);
        echo.product_description = description;
        return echo;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        Echo echo = prepareRequest(id, "ES", description, amount, cc);
        echo.submit();
        writeLog(id, echo);
        if (echo.echoSuccess()) {
            writeCharge(amount);
        } else {
            processError(echo);
        }
        HashMap map = new HashMap();
        map.put("amount", new Double(amount));
        map.put("id", Long.toString(id));
        return map;
    }

    protected void processError(Echo echo) throws PaymentInstrumentException {
        int code = Integer.parseInt(echo.echoDeclineCode());
        switch (code) {
            case 1:
                throw new CreditCardException("The card must be referred to the issuer before the transaction can be approved");
            case 2:
                throw new CreditCardException("not used");
            case 3:
                throw new MerchantException("Invalid merchant number");
            case 4:
                throw new CreditCardFraudException("The card number has been listed on the Warning Bulletin File for reasons of counterfeit, fraud, or other");
            case 5:
                throw new CreditCardException("The transaction was declined by the issuer without definition or reason.");
            case 12:
                throw new MerchantException("The transaction request presented is not supported or is not valid for the card number presented.");
            case 13:
                throw new MerchantException("The amount is below the minimum limit or above the maximum limit the issuer allows for this type of transaction.");
            case 14:
                throw new CreditCardException("The issuer has indicated this card number is not valid.");
            case 15:
                throw new CreditCardException("The issuer number is not valid.");
            case 30:
                throw new MerchantException("The transaction was not formatted properly.");
            case yafvsym.INTEGER_CONSTANT /* 41 */:
                throw new CreditCardException("This card has been reported lost.");
            case yafvsym.BOOL_CONSTANT /* 43 */:
                throw new CreditCardException("This card has been reported stolen.");
            case 51:
                throw new CreditCardException("The transaction will result in an over credit limit or insufficient funds condition..");
            case 54:
                throw new CreditCardExpiredException("The card is expired.");
            case 55:
                throw new CreditCardException("The cardholder-entered PIN is incorrect.");
            case 57:
                throw new CreditCardException("This card does not support the type of transaction requested");
            case 58:
                throw new MerchantException("The merchant's account does not support the type of transaction presented.");
            case 61:
                throw new CreditCardException("The cardholder has requested a withdrawal amount in excess of the daily defined maximum.");
            case 62:
            case 63:
                throw new CreditCardException("The card has been restricted.");
            case 65:
                throw new CreditCardException("The allowed number of daily transactions has been exceeded");
            case 75:
                throw new CreditCardException("The allowed number of PIN retries has been exceeded.");
            case 76:
                throw new MerchantException("The \"to\" (credit) account specified in the transaction does not exist or is not associated with the card number presented.");
            case 77:
                throw new MerchantException("The \"from\" (debit) account specified in the transaction does not exist or is not associated with the card number presented.");
            case 78:
                throw new MerchantException("The \"from\" (debit) or \"to\" (credit) account does not exist or is not associated with the card number presented.");
            case 81:
                throw new MerchantException("The authorization life cycle is above or below limits established by the issuer.");
            case 91:
                throw new CreditCardException("The bank is not available to authorize this transaction");
            case 92:
                throw new CreditCardException("The transaction does not contain enough information to be routed to the authorizing agency.");
            case 94:
                throw new MerchantException("The host has detected a duplicate transmission.");
            case 96:
                throw new MerchantException("A system error has occurred or the files required for authorization are not available.");
            case HostEntry.VPS_IP /* 1000 */:
                throw new PaymentInstrumentException("An unrecoverable error has occurred in the ECHONLINE processing.");
            case HostEntry.TAKEN_VPS_IP /* 1001 */:
                throw new MerchantException("The merchant account has been closed");
            case 1002:
                throw new MerchantException("Services for this system are not available.");
            case 1003:
                throw new MerchantException("The e-mail function is not available.");
            case 1012:
                throw new MerchantException("The host computer received an invalid transaction code.");
            case 1013:
                throw new MerchantException("The ECHO-ID is invalid");
            case 1015:
                throw new CreditCardException("The credit card number that was sent to the host computer was invalid");
            case 1016:
                throw new CreditCardException("The card has expired or the expiration date was invalid.");
            case 1017:
                throw new MerchantException("The dollar amount was less than 1.00 or greater than the maximum allowed for this card.");
            case 1019:
                throw new MerchantException("The state code was invalid.");
            case 1021:
                throw new MerchantException("The merchant or card holder is not allowed to perform that kind of transaction");
            case 1024:
                throw new MerchantException("The authorization number presented with this transaction is incorrect.");
            case 1025:
                throw new MerchantException("The reference number presented with this transaction is incorrect or is not numeric.");
            case 1029:
                throw new MerchantException("The contract number presented with this transaction is incorrect or is not numeric.");
            case 1030:
                throw new MerchantException("The inventory data presented with this transaction is not ASCII \"printable\".");
            case 1508:
                throw new MerchantException("Invalid or missing order_type.");
            case 1509:
                throw new MerchantException("The merchant is not approved to submit this order_type.");
            case 1510:
                throw new MerchantException("The merchant is not approved to submit this transaction_type.");
            case 1511:
                throw new MerchantException("Duplicate transaction attempt");
            case 1599:
                throw new MerchantException("An system error occurred while validating the transaction input.");
            case 1801:
                throw new CreditCardException("Address matches; ZIP does not match.");
            case 1802:
                throw new CreditCardException("9-digit ZIP matches; Address does not match.");
            case 1803:
                throw new CreditCardException("5-digit ZIP matches; Address does not match.");
            case 1804:
                throw new CreditCardException("Issuer unavailable; cannot verify.");
            case 1805:
                throw new CreditCardException("Retry; system is currently unable to process.");
            case 1806:
                throw new CreditCardException("Issuer does not support AVS.");
            case 1807:
                throw new CreditCardException("Nothing matches.");
            case 1808:
                throw new CreditCardException("Invalid AVS only response.");
            default:
                throw new MerchantException("Error code #" + code);
        }
    }

    protected void writeLog(long id, Echo echo) {
        String request = Long.toString(id) + ";" + echo.billing_first_name + " " + echo.billing_last_name + ";" + echo.ccexp_month + "/" + echo.ccexp_year + ";" + echo.billing_email + ";" + echo.billing_zip + ";" + echo.billing_address1 + ";" + echo.grand_total + ";" + echo.transaction_type;
        String response = echo.echoResponse();
        writeLog(id, request, response, "");
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        Echo echo = prepareRequest(id, "AS", description, amount, cc);
        writeLog(id, echo);
        echo.submit();
        if (echo.echoSuccess()) {
            writeAuthorize(amount);
        } else {
            processError(echo);
        }
        HashMap map = new HashMap();
        map.put("reference", echo.echoReference());
        map.put("order_number", echo.echoOrderNumber());
        map.put("id", Long.toString(id));
        map.put("authorization", echo.authorization);
        map.put("amount", new Double(amount));
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        Echo echo = prepareRequest(id, "DS", description, amount, cc);
        echo.order_number = (String) data.get("order_number");
        echo.authorization = (String) data.get("authorization");
        writeLog(id, echo);
        if (echo.submit()) {
            writeCapture(amount);
        } else {
            processError(echo);
        }
        HashMap map = new HashMap();
        map.put("amount", new Double(amount));
        map.put("id", Long.toString(id));
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        writeLog(id, "void:" + id + ";" + amount, "ok", "");
        writeVoid(amount);
        return data;
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Echo (http://www.openecho.com/)";
    }
}
