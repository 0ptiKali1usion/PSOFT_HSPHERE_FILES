package psoft.epayment;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.IOException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/epayment/Ematic.class */
public class Ematic extends GenericMerchantGateway {
    protected String server;
    protected String path;
    protected int port;
    protected String username;
    protected String password;
    private static final SimpleDateFormat expMonthFormat;
    private static final SimpleDateFormat expYearFormat;
    private static HashMap ccTypes;
    private static HashMap messages;

    static {
        defaultValues.put("SERVER", "www.ematic.com");
        defaultValues.put("PATH", "cgi-bin/ematic/profile/procgi.pl/code/vtchargeb.p");
        defaultValues.put("PORT", "443");
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        expMonthFormat = new SimpleDateFormat("MM");
        expYearFormat = new SimpleDateFormat("yyyy");
        ccTypes = new HashMap();
        messages = new HashMap();
        ccTypes.put("VISA", "V");
        ccTypes.put("MC", "M");
        ccTypes.put("DISC", "D");
        ccTypes.put("AX", "A");
        messages.put("ERROR:EMAIL", "Incorrect email addess.");
        messages.put("ERROR:MERCHANT", "Unable to find the merchant specified in the database or merchant specified was not authorized to use the virtual terminal.");
        messages.put("ERROR:CARDTYPE", "No card type specified");
        messages.put("ERROR:MONTH", "The month wasn't specified");
        messages.put("ERROR:YEAR", "The year wasn't specified");
        messages.put("ERROR:LASTNAME", "The last name wasn't specified");
        messages.put("ERROR:FIRSTNAME", "The first name wasn't specified");
        messages.put("ERROR:ADDRESS", "The address wasn't specified");
        messages.put("ERROR:CITY", "The city wasn't specified");
        messages.put("ERROR:ZIP", "The zip code wasn't specified");
        messages.put("ERROR:STATE", "The state wasn't specified");
        messages.put("ERROR:COUNTRY", "The country wasn't specified");
        messages.put("ERROR:TELNUM", "The telephone number wasn't specified");
        messages.put("ERROR:CCNUMNOTMATCH", "The credit card number doesn't match the card type");
        messages.put("ERROR:EXPDATE", "The expiration date is before today");
        messages.put("ERROR:PRDCODE", "The prdcode field was empty");
        messages.put("ERROR:PRDDESC", "The prddesc field was empty");
        messages.put("ERROR:CARDNUM", "The length of the credit card number doesn't match the type of card.");
        messages.put("ERROR:CVV2", "Incorrect ccv2 number");
        messages.put("ERROR:SSL", "Information hasn't been sent over SSL (protocol: https).");
        messages.put("DCARDEXPIRED", "The credit card is expired");
        messages.put("DCARDREFUSED", "The credit card was refused by the bank");
        messages.put("DINVALIDCARD", "The credit card is invalid");
        messages.put("DAVSNO", "The zip code doesn't match the zip code on the card");
        messages.put("DINVALIDDATA", "Invalid data was sent to our processor");
        messages.put("DINVALIDCARD", "An unknown error occurred");
        messages.put("DCV", "Incorrect CVV2 number.");
        messages.put("SUCCESS", "The order was placed, and the card was charged successfully");
    }

    private String getType(String type) throws Exception {
        String typeCC = (String) ccTypes.get(type);
        if (typeCC == null) {
            throw new Exception("Unable to resolve CC type: " + type);
        }
        return typeCC;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.path = getValue(v, "PATH");
        this.port = Integer.parseInt(getValue(v, "PORT"));
        this.username = getValue(v, "USERNAME");
        this.password = getValue(v, "PASSWORD");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("PATH", this.path);
        map.put("PORT", Integer.toString(this.port));
        map.put("USERNAME", this.username);
        map.put("PASSWORD", this.password);
        return map;
    }

    protected void writeLog(long id, Map dataOut, String dataIn, String error) {
        writeLog(id, HttpUtils.makeForm(dataOut), dataIn, error);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        return charge(id, description, amount, cc, true);
    }

    protected HashMap charge(long id, String description, double amount, CreditCard cc, boolean sale) throws Exception {
        HashMap request = new HashMap();
        request.put("email", cc.getEmail());
        request.put("cardtype", getType(cc.getType()));
        request.put("amount", formatAmount(amount));
        request.put("cardnum", cc.getNumber());
        request.put("lname", cc.getLastName());
        request.put("fname", cc.getFirstName());
        request.put("month", cc.getExp(expMonthFormat));
        request.put("year", cc.getExp(expYearFormat));
        request.put("addr1", cc.getAddress());
        request.put("city", cc.getCity());
        request.put("state", cc.getState());
        request.put("zip", cc.getZip());
        request.put("country", cc.getCountry());
        request.put("telnum", cc.getPhone());
        request.put("refnum", Long.toString(id));
        request.put("prdcode", Long.toString(id));
        request.put("prddesc", description);
        request.put("merchant", this.username);
        request.put("password", this.password);
        HashMap request2 = postForm(id, request);
        if (sale) {
            writeCharge(amount);
        } else {
            writeAuthorize(amount);
        }
        return request2;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        return charge(id, description, amount, cc, false);
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        data.remove("Return_Code");
        data.remove("Return_Message");
        HashMap data2 = postForm(id, data);
        writeCapture(Double.parseDouble((String) data2.get("Amount")));
        return data2;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        writeLog(id, "", "", "");
        HashMap retval = new HashMap(1);
        retval.put("id", "unknown");
        writeVoid(Double.parseDouble((String) data.get("Amount")));
        return data;
    }

    protected HashMap postForm(long id, HashMap data) throws Exception {
        HashMap dataIn = new HashMap(6);
        try {
            String rCode = "";
            HttpResponse response = HttpUtils.getForm("https", this.server, this.port, this.path, this.username, this.password, data);
            String message = response.getBody();
            StringTokenizer st = new StringTokenizer(message, ",\n\t");
            dataIn.put("id", Long.toString(id));
            try {
                String nextToken = st.nextToken();
                rCode = nextToken;
                dataIn.put("Return_Code", nextToken);
                String resMess = "";
                if (messages.containsKey(rCode)) {
                    resMess = (String) messages.get(rCode);
                    dataIn.put("Return_Message", resMess);
                }
                if (!"SUCCESS".equals(rCode)) {
                    throw new Exception("Transaction has been declined: " + rCode + " " + resMess);
                }
                writeLog(id, data.toString(), dataIn.toString(), "");
                return dataIn;
            } catch (NoSuchElementException e) {
                throw new Exception("Bad response from processing center: " + rCode + "\n message: " + message + " \n");
            }
        } catch (IOException ie) {
            writeLog(id, data.toString(), dataIn.toString(), "Error connecting to processing center: " + ie.getMessage());
            throw new IOException("Error connecting to processing center.");
        } catch (Exception e2) {
            writeLog(id, data.toString(), dataIn.toString(), e2.getMessage());
            throw e2;
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "Ematic (www.ematic.com)";
    }
}
