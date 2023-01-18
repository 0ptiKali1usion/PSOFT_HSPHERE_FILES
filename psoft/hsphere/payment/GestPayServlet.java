package psoft.hsphere.payment;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownServiceException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.email.SPFResource;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.USFormat;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/GestPayServlet.class */
public class GestPayServlet extends ExternalPayServlet {
    private static Category log = Category.getInstance(GestPayServlet.class.getName());
    private static final String GEST_PAY_URL = "https://ecomm.sella.it/gestpay/pagam.asp";

    @Override // psoft.hsphere.payment.ExternalPayServlet
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        C0004CP.getCP().setConfig();
        Session.setLanguage(new Language(null));
        try {
            Session.setResellerId(1L);
        } catch (UnknownResellerException ex) {
            Session.getLog().error("Error setting default reseller ID: ", ex);
        }
        String action = request.getParameter("action");
        if ("ok".equals(action)) {
            getOKForm(request, response);
        } else if (SPFResource.DEFAULT_PROCESSING_VALUE.equals(action)) {
            getFailForm(request, response);
        } else if ("process".equals(action)) {
            processPayment(request, response);
        } else if ("redirect".equals(action)) {
            getProcessForm(request, response);
        } else {
            processPayment(request, response);
        }
    }

    protected void getProcessForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            try {
                C0004CP.getCP().setConfig();
                response.setContentType("text/html");
                log.warn("Begin process Gest Pay");
                System.out.println("Begin process Gest Pay");
                Session.save();
                String transId = request.getParameter("trans_id");
                if (transId == null || "".equals(transId)) {
                    throw new Exception("Empty transaction ID");
                }
                String amount = request.getParameter("amount");
                if (amount == null || "".equals(amount)) {
                    throw new Exception("Empty amount for trans ID:" + transId);
                }
                String amount2 = formatAmount(amount);
                setResellerId(transId);
                String transKey = paymentPrepare(Double.parseDouble(amount2), transId);
                String enable = Settings.get().getValue("GestPay_enabled");
                if (!"1".equals(enable)) {
                    throw new Exception("Gest pay is not available");
                }
                String login = Settings.get().getValue("GestPay_LOGIN");
                String language = Settings.get().getValue("GestPay_LANGUAGE");
                String currency = Settings.get().getValue("GestPay_CURRENCY");
                GestPayCrypt gpc = new GestPayCrypt();
                gpc.SetShopLogin(login);
                try {
                    Session.getLog().debug("Test value1:" + amount2);
                    amount2 = USFormat.format(HsphereToolbox.parseLocalizedNumber(amount2));
                    String newTransId = transKey + "-" + transId;
                    Session.getLog().debug("Amount:" + amount2 + " ShopTransactionID:" + newTransId + " language:" + language + " CustomInfo:" + transId + " Currency:" + currency);
                    gpc.SetAmount(amount2.trim());
                    gpc.SetShopTransactionID(newTransId.trim());
                    gpc.SetCurrency(currency.trim());
                    gpc.Encrypt();
                    if (!"0".equals(gpc.GetErrorCode())) {
                        throw new Exception("Unable to encrypt data:" + gpc.GetErrorDescription());
                    }
                    String a = gpc.GetShopLogin();
                    String b = gpc.GetEncryptedString();
                    Template template = Session.getTemplate("submit/billing/gestpay.sbm");
                    if (null == template) {
                        processEmptyResponse(request, response, "Template submit/billing/gestpay.sbm is not exist");
                        try {
                            Session.restore();
                            return;
                        } catch (Exception e) {
                            return;
                        }
                    }
                    SimpleHash root = new SimpleHash();
                    root.put("toolbox", HsphereToolbox.toolbox);
                    root.put("config", new MapAdapter(C0004CP.getCP().getConfig()));
                    root.put("settings", new MapAdapter(Settings.get().getMap()));
                    root.put("design", Session.getDesign());
                    root.put("a", new TemplateString(a));
                    root.put("b", new TemplateString(b));
                    root.put("url", new TemplateString(GEST_PAY_URL));
                    root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
                    template.process(root, response.getWriter());
                    try {
                        Session.restore();
                    } catch (Exception e2) {
                    }
                } catch (Exception e3) {
                    throw new Exception("Unable to parse the amount:" + amount2);
                }
            } catch (Exception e4) {
                log.warn("Unable to process transaction", e4);
                Ticket.create(e4, null);
                processEmptyResponse(request, response, "The payment can't be processed: " + e4.getMessage());
                try {
                    Session.restore();
                } catch (Exception e5) {
                }
            }
        } catch (Throwable th) {
            try {
                Session.restore();
            } catch (Exception e6) {
            }
            throw th;
        }
    }

    protected void getOKForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void getFailForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void processPayment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            try {
                C0004CP.getCP().setConfig();
                String login = request.getParameter("a").trim();
                String encryptedData = request.getParameter("b").trim();
                GestPayCrypt gpc = new GestPayCrypt();
                gpc.SetShopLogin(login);
                gpc.SetEncryptedString(encryptedData);
                gpc.Decrypt();
                String errorCode = gpc.GetErrorCode().trim();
                gpc.GetErrorDescription().trim();
                String transactRes = gpc.GetTransactionResult().trim();
                if (!"OK".equals(transactRes) || !"0".equals(errorCode)) {
                    throw new Exception("Transaction result: " + transactRes + " Processing error: " + gpc.GetErrorDescription() + " Error code: " + errorCode);
                }
                String transId = gpc.GetShopTransactionID();
                if (transId.indexOf("-") <= 0) {
                    throw new Exception(" Unable to parce result transId " + transId);
                }
                String realTransId = transId.substring(transId.indexOf("-") + 1);
                Session.getLog().debug(" Processed real transId: " + realTransId + " old transId: " + transId);
                setResellerId(realTransId);
                double amount = USFormat.parseDouble(gpc.GetAmount());
                paymentSet(amount, transId, "GestPay");
                processOkResponse(request, response, transactRes);
                try {
                    Session.restore();
                } catch (Exception e) {
                }
            } catch (DuplicatePaymentException e2) {
                processOkResponse(request, response, "OK");
                log.warn("A duplicate payment has been received, possibly a client has been redirected back to hsphere.");
                try {
                    Session.restore();
                } catch (Exception e3) {
                }
            } catch (Exception e4) {
                log.error("Problem adding payment. Error code: 0Error message: ", e4);
                Ticket.create(e4, null);
                processEmptyResponse(request, response, e4.getMessage());
                try {
                    Session.restore();
                } catch (Exception e5) {
                }
            }
        } catch (Throwable th) {
            try {
                Session.restore();
            } catch (Exception e6) {
            }
            throw th;
        }
    }

    protected void processOkResponse(HttpServletRequest request, HttpServletResponse response, String transactRes) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(200);
        PrintWriter out1 = new PrintWriter(response.getWriter());
        out1.println("<html><body>");
        out1.println("Transaction has been Authorized: <br>");
        out1.println(transactRes);
        out1.println("</body></html>");
        out1.close();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/payment/GestPayServlet$GestPayCrypt.class */
    public class GestPayCrypt {
        private String TransactionResult;
        private String ProtocolAuthServer;
        private String DomainName;
        private String Min;
        private String CVV;
        private String country;
        private String vbvrisp;
        private String vbv;
        private String Version = "2.0";
        private String ShopLogin = "";
        private String Currency = "";
        private String Amount = "";
        private String ShopTransactionID = "";
        private String BuyerName = "";
        private String BuyerEmail = "";
        private String Language = "";
        private String CustomInfo = "";
        private String AuthorizationCode = "";
        private String ErrorCode = "";
        private String ErrorDescription = "";
        private String BankTransactionID = "";
        private String AlertCode = "";
        private String AlertDescription = "";
        private String EncryptedString = "";
        private String ToBeEncript = "";
        private String Decripted = "";
        private String separator = "*P1*";
        private String errDescription = "";
        private String errNumber = "0";

        public GestPayCrypt() {
            GestPayServlet.this = r4;
            this.ProtocolAuthServer = "";
            this.DomainName = "";
            this.Min = "";
            this.CVV = "";
            this.country = "";
            this.vbvrisp = "";
            this.vbv = "";
            this.ProtocolAuthServer = "http://";
            this.DomainName = "ecomm.sella.it/CryptHTTP";
            this.Min = "";
            this.CVV = "";
            this.country = "";
            this.vbvrisp = "";
            this.vbv = "";
        }

        public void SetShopLogin(String xstr) {
            this.ShopLogin = xstr;
        }

        public void SetCurrency(String xstr) {
            this.Currency = xstr;
        }

        public void SetAmount(String xstr) {
            this.Amount = xstr;
        }

        public void SetShopTransactionID(String xstr) {
            this.ShopTransactionID = URLEncoder.encode(xstr.trim());
        }

        public void SetMIN(String xstr) {
            this.Min = xstr;
        }

        public void SetCVV(String xstr) {
            this.CVV = xstr;
        }

        public void SetBuyerName(String xstr) {
            this.BuyerName = URLEncoder.encode(xstr.trim());
        }

        public void SetBuyerEmail(String xstr) {
            this.BuyerEmail = xstr.trim();
        }

        public void SetLanguage(String xstr) {
            this.Language = xstr.trim();
        }

        public void SetCustomInfo(String xstr) {
            this.CustomInfo = URLEncoder.encode(xstr.trim());
        }

        public void SetEncryptedString(String xstr) {
            this.EncryptedString = xstr;
        }

        public String GetShopLogin() {
            return this.ShopLogin;
        }

        public String GetCurrency() {
            return this.Currency;
        }

        public String GetAmount() {
            return this.Amount;
        }

        public String GetCountry() {
            return this.country;
        }

        public String GetVBV() {
            return this.vbv;
        }

        public String GetVBVrisp() {
            return this.vbvrisp;
        }

        public String GetShopTransactionID() {
            String app = "";
            try {
                app = URLDecode(this.ShopTransactionID);
            } catch (Exception e) {
            }
            return app;
        }

        public String GetBuyerName() {
            String appBuyername;
            try {
                appBuyername = URLDecode(this.BuyerName);
            } catch (Exception e) {
                appBuyername = "errore";
            }
            return appBuyername;
        }

        public String GetBuyerEmail() {
            return this.BuyerEmail;
        }

        public String GetCustomInfo() {
            String appCustom = "";
            try {
                appCustom = URLDecode(this.CustomInfo);
            } catch (Exception e) {
            }
            return appCustom;
        }

        public String GetAuthorizationCode() {
            return this.AuthorizationCode;
        }

        public String GetErrorCode() {
            return this.ErrorCode;
        }

        public String GetErrorDescription() {
            return this.ErrorDescription;
        }

        public String GetBankTransactionID() {
            return this.BankTransactionID;
        }

        public String GetTransactionResult() {
            return this.TransactionResult;
        }

        public String GetAlertCode() {
            return this.AlertCode;
        }

        public String GetAlertDescription() {
            return this.AlertDescription;
        }

        public String GetEncryptedString() {
            return this.EncryptedString;
        }

        public boolean Encrypt() {
            this.ErrorCode = "0";
            this.ErrorDescription = "";
            try {
                if (this.ShopLogin.length() <= 0) {
                    this.ErrorCode = "546";
                    this.ErrorDescription = "IDshop not valid";
                    return false;
                } else if (this.Currency.length() <= 0) {
                    this.ErrorCode = "552";
                    this.ErrorDescription = "Currency not valid";
                    return false;
                } else if (this.Amount.length() <= 0) {
                    this.ErrorCode = "553";
                    this.ErrorDescription = "Amount not valid";
                    return false;
                } else if (this.ShopTransactionID.length() <= 0) {
                    this.ErrorCode = "551";
                    this.ErrorDescription = "Shop Transaction ID not valid";
                    return false;
                } else {
                    this.ToBeEncript = "";
                    if (this.CVV.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_CVV=" + this.CVV;
                    }
                    if (this.Min.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_MIN=" + this.Min;
                    }
                    if (this.Currency.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_UICCODE=" + this.Currency;
                    }
                    if (this.Amount.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_AMOUNT=" + this.Amount;
                    }
                    if (this.ShopTransactionID.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_SHOPTRANSACTIONID=" + this.ShopTransactionID;
                    }
                    if (this.BuyerName.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_CHNAME=" + this.BuyerName;
                    }
                    if (this.BuyerEmail.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_CHEMAIL=" + this.BuyerEmail;
                    }
                    if (this.Language.length() > 0) {
                        this.ToBeEncript += this.separator + "PAY1_IDLANGUAGE=" + this.Language;
                    }
                    if (this.CustomInfo.length() > 0) {
                        this.ToBeEncript += this.separator + this.CustomInfo;
                    }
                    String urlString = this.ProtocolAuthServer + this.DomainName + "/Encrypt.asp?a=" + this.ShopLogin + "&b=" + this.ToBeEncript.substring(4, this.ToBeEncript.length()) + "&c=" + this.Version;
                    URL url = new URL(urlString);
                    URLConnection connection = url.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = "";
                    while (line != null) {
                        line = in.readLine();
                        if (line != null) {
                            int nStart = line.indexOf("#cryptstring#");
                            int nEnd = line.lastIndexOf("#/cryptstring#");
                            if ((nStart != -1) & (nEnd > nStart + 14)) {
                                this.EncryptedString = line.substring(nStart + 13, nEnd);
                            }
                            int nStart2 = line.indexOf("#error#");
                            int nEnd2 = line.lastIndexOf("#/error#");
                            if ((nStart2 != -1) & (nEnd2 > nStart2 + 8)) {
                                String sErr = line.substring(nStart2 + 7, nEnd2);
                                int intsep = sErr.indexOf("-");
                                this.ErrorCode = sErr.substring(0, intsep);
                                this.ErrorDescription = sErr.substring(intsep + 1, sErr.length());
                                return false;
                            }
                        }
                    }
                    in.close();
                    return true;
                }
            } catch (MalformedURLException e) {
                this.ErrorCode = "9999";
                this.ErrorDescription = "Bad URL";
                return false;
            } catch (UnknownServiceException e2) {
                this.ErrorCode = "9999";
                this.ErrorDescription = "ServiceException occurred.";
                return false;
            } catch (IOException e3) {
                this.ErrorCode = "9999";
                this.ErrorDescription = "Bad URL Request";
                return false;
            }
        }

        public boolean Decrypt() {
            this.ErrorCode = "0";
            this.ErrorDescription = "";
            if (this.ShopLogin.length() <= 0) {
                this.ErrorCode = "546";
                this.ErrorDescription = "IDshop not valid";
                return false;
            } else if (this.EncryptedString.length() <= 0) {
                this.ErrorCode = "1009";
                this.ErrorDescription = "String to Decrypt not valid";
                return false;
            } else {
                try {
                    String urlString = this.ProtocolAuthServer + this.DomainName + "/Decrypt.asp?a=" + this.ShopLogin + "&b=" + this.EncryptedString + "&c=" + this.Version;
                    URL url = new URL(urlString);
                    URLConnection connection = url.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = "";
                    while (line != null) {
                        line = in.readLine();
                        if (line != null) {
                            int nStart = line.indexOf("#decryptstring#");
                            int nEnd = line.lastIndexOf("#/decryptstring#");
                            if ((nStart != -1) & (nEnd > nStart + 16)) {
                                this.Decripted = line.substring(nStart + 15, nEnd);
                            }
                            int nStart2 = line.indexOf("#error#");
                            int nEnd2 = line.lastIndexOf("#/error#");
                            if ((nStart2 != -1) & (nEnd2 > nStart2 + 8)) {
                                String sErr = line.substring(nStart2 + 7, nEnd2);
                                int intsep = sErr.indexOf("-");
                                this.ErrorCode = sErr.substring(0, intsep);
                                this.ErrorDescription = sErr.substring(intsep + 1, sErr.length());
                                return false;
                            }
                        }
                    }
                    in.close();
                    if (this.Decripted.trim() == "") {
                        this.ErrorCode = "9999";
                        this.ErrorDescription = "Void String";
                        return false;
                    } else if (!Parsing(this.Decripted)) {
                        return false;
                    } else {
                        return true;
                    }
                } catch (MalformedURLException e) {
                    this.ErrorCode = "9999";
                    this.ErrorDescription = "Bad URL";
                    return false;
                } catch (UnknownServiceException e2) {
                    this.ErrorCode = "9999";
                    this.ErrorDescription = "Service Exception occurred.";
                    return false;
                } catch (IOException e3) {
                    this.ErrorCode = "9999";
                    this.ErrorDescription = "Bad URL Request";
                    return false;
                }
            }
        }

        private boolean Parsing(String StringToBeParsed) {
            this.ErrorCode = "";
            this.ErrorDescription = "";
            try {
                int nStart = StringToBeParsed.indexOf("PAY1_UICCODE");
                if (nStart != -1) {
                    int nEnd = StringToBeParsed.indexOf(this.separator, nStart);
                    if (nEnd == -1) {
                        this.Currency = StringToBeParsed.substring(nStart + 13, StringToBeParsed.length());
                        if (nStart >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart);
                        }
                    } else {
                        this.Currency = StringToBeParsed.substring(nStart + 13, nEnd);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart) + StringToBeParsed.substring(nEnd + 4, StringToBeParsed.length());
                    }
                }
                int nStart2 = StringToBeParsed.indexOf("PAY1_AMOUNT");
                if (nStart2 != -1) {
                    int nEnd2 = StringToBeParsed.indexOf(this.separator, nStart2);
                    if (nEnd2 == -1) {
                        this.Amount = StringToBeParsed.substring(nStart2 + 12, StringToBeParsed.length());
                        if (nStart2 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart2 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart2);
                        }
                    } else {
                        this.Amount = StringToBeParsed.substring(nStart2 + 12, nEnd2);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart2) + StringToBeParsed.substring(nEnd2 + 4, StringToBeParsed.length());
                    }
                }
                int nStart3 = StringToBeParsed.indexOf("PAY1_SHOPTRANSACTIONID");
                if (nStart3 != -1) {
                    int nEnd3 = StringToBeParsed.indexOf(this.separator, nStart3);
                    if (nEnd3 == -1) {
                        this.ShopTransactionID = StringToBeParsed.substring(nStart3 + 23, StringToBeParsed.length());
                        if (nStart3 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart3 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart3);
                        }
                    } else {
                        this.ShopTransactionID = StringToBeParsed.substring(nStart3 + 23, nEnd3);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart3) + StringToBeParsed.substring(nEnd3 + 4, StringToBeParsed.length());
                    }
                }
                int nStart4 = StringToBeParsed.indexOf("PAY1_CHNAME");
                if (nStart4 != -1) {
                    int nEnd4 = StringToBeParsed.indexOf(this.separator, nStart4);
                    if (nEnd4 == -1) {
                        this.BuyerName = StringToBeParsed.substring(nStart4 + 12, StringToBeParsed.length());
                        if (nStart4 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart4 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart4);
                        }
                    } else {
                        this.BuyerName = StringToBeParsed.substring(nStart4 + 12, nEnd4);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart4) + StringToBeParsed.substring(nEnd4 + 4, StringToBeParsed.length());
                    }
                }
                int nStart5 = StringToBeParsed.indexOf("PAY1_CHEMAIL");
                if (nStart5 != -1) {
                    int nEnd5 = StringToBeParsed.indexOf(this.separator, nStart5);
                    if (nEnd5 == -1) {
                        this.BuyerEmail = StringToBeParsed.substring(nStart5 + 13, StringToBeParsed.length());
                        if (nStart5 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart5 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart5);
                        }
                    } else {
                        this.BuyerEmail = StringToBeParsed.substring(nStart5 + 13, nEnd5);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart5) + StringToBeParsed.substring(nEnd5 + 4, StringToBeParsed.length());
                    }
                }
                int nStart6 = StringToBeParsed.indexOf("PAY1_AUTHORIZATIONCODE");
                if (nStart6 != -1) {
                    int nEnd6 = StringToBeParsed.indexOf(this.separator, nStart6);
                    if (nEnd6 == -1) {
                        this.AuthorizationCode = StringToBeParsed.substring(nStart6 + 23, StringToBeParsed.length());
                        if (nStart6 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart6 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart6);
                        }
                    } else {
                        this.AuthorizationCode = StringToBeParsed.substring(nStart6 + 23, nEnd6);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart6) + StringToBeParsed.substring(nEnd6 + 4, StringToBeParsed.length());
                    }
                }
                int nStart7 = StringToBeParsed.indexOf("PAY1_ERRORCODE");
                if (nStart7 != -1) {
                    int nEnd7 = StringToBeParsed.indexOf(this.separator, nStart7);
                    if (nEnd7 == -1) {
                        this.ErrorCode = StringToBeParsed.substring(nStart7 + 15, StringToBeParsed.length());
                        if (nStart7 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart7 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart7);
                        }
                    } else {
                        this.ErrorCode = StringToBeParsed.substring(nStart7 + 15, nEnd7);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart7) + StringToBeParsed.substring(nEnd7 + 4, StringToBeParsed.length());
                    }
                }
                int nStart8 = StringToBeParsed.indexOf("PAY1_ERRORDESCRIPTION");
                if (nStart8 != -1) {
                    int nEnd8 = StringToBeParsed.indexOf(this.separator, nStart8);
                    if (nEnd8 == -1) {
                        this.ErrorDescription = StringToBeParsed.substring(nStart8 + 22, StringToBeParsed.length());
                        if (nStart8 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart8 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart8);
                        }
                    } else {
                        this.ErrorDescription = StringToBeParsed.substring(nStart8 + 22, nEnd8);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart8) + StringToBeParsed.substring(nEnd8 + 4, StringToBeParsed.length());
                    }
                }
                int nStart9 = StringToBeParsed.indexOf("PAY1_BANKTRANSACTIONID");
                if (nStart9 != -1) {
                    int nEnd9 = StringToBeParsed.indexOf(this.separator, nStart9);
                    if (nEnd9 == -1) {
                        this.BankTransactionID = StringToBeParsed.substring(nStart9 + 23, StringToBeParsed.length());
                        if (nStart9 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart9 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart9);
                        }
                    } else {
                        this.BankTransactionID = StringToBeParsed.substring(nStart9 + 23, nEnd9);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart9) + StringToBeParsed.substring(nEnd9 + 4, StringToBeParsed.length());
                    }
                }
                int nStart10 = StringToBeParsed.indexOf("PAY1_ALERTCODE");
                if (nStart10 != -1) {
                    int nEnd10 = StringToBeParsed.indexOf(this.separator, nStart10);
                    if (nEnd10 == -1) {
                        this.AlertCode = StringToBeParsed.substring(nStart10 + 15, StringToBeParsed.length());
                        if (nStart10 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart10 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart10);
                        }
                    } else {
                        this.AlertCode = StringToBeParsed.substring(nStart10 + 15, nEnd10);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart10) + StringToBeParsed.substring(nEnd10 + 4, StringToBeParsed.length());
                    }
                }
                int nStart11 = StringToBeParsed.indexOf("PAY1_ALERTDESCRIPTION");
                if (nStart11 != -1) {
                    int nEnd11 = StringToBeParsed.indexOf(this.separator, nStart11);
                    if (nEnd11 == -1) {
                        this.AlertDescription = StringToBeParsed.substring(nStart11 + 22, StringToBeParsed.length());
                        if (nStart11 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart11 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart11);
                        }
                    } else {
                        this.AlertDescription = StringToBeParsed.substring(nStart11 + 22, nEnd11);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart11) + StringToBeParsed.substring(nEnd11 + 4, StringToBeParsed.length());
                    }
                }
                int nStart12 = StringToBeParsed.indexOf("PAY1_COUNTRY");
                if (nStart12 != -1) {
                    int nEnd12 = StringToBeParsed.indexOf(this.separator, nStart12);
                    if (nEnd12 == -1) {
                        this.country = StringToBeParsed.substring(nStart12 + 13, StringToBeParsed.length());
                        if (nStart12 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart12 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart12);
                        }
                    } else {
                        this.country = StringToBeParsed.substring(nStart12 + 13, nEnd12);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart12) + StringToBeParsed.substring(nEnd12 + 4, StringToBeParsed.length());
                    }
                }
                int nStart13 = StringToBeParsed.indexOf("PAY1_VBVRISP");
                if (nStart13 != -1) {
                    int nEnd13 = StringToBeParsed.indexOf(this.separator, nStart13);
                    if (nEnd13 == -1) {
                        this.vbvrisp = StringToBeParsed.substring(nStart13 + 13, StringToBeParsed.length());
                        if (nStart13 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart13 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart13);
                        }
                    } else {
                        this.vbvrisp = StringToBeParsed.substring(nStart13 + 13, nEnd13);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart13) + StringToBeParsed.substring(nEnd13 + 4, StringToBeParsed.length());
                    }
                }
                int nStart14 = StringToBeParsed.indexOf("PAY1_VBV");
                if (nStart14 != -1) {
                    int nEnd14 = StringToBeParsed.indexOf(this.separator, nStart14);
                    if (nEnd14 == -1) {
                        this.vbv = StringToBeParsed.substring(nStart14 + 9, StringToBeParsed.length());
                        if (nStart14 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart14 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart14);
                        }
                    } else {
                        this.vbv = StringToBeParsed.substring(nStart14 + 9, nEnd14);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart14) + StringToBeParsed.substring(nEnd14 + 4, StringToBeParsed.length());
                    }
                }
                int nStart15 = StringToBeParsed.indexOf("PAY1_IDLANGUAGE");
                if (nStart15 != -1) {
                    int nEnd15 = StringToBeParsed.indexOf(this.separator, nStart15);
                    if (nEnd15 == -1) {
                        this.Language = StringToBeParsed.substring(nStart15 + 16, StringToBeParsed.length());
                        if (nStart15 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart15 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart15);
                        }
                    } else {
                        this.Language = StringToBeParsed.substring(nStart15 + 16, nEnd15);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart15) + StringToBeParsed.substring(nEnd15 + 4, StringToBeParsed.length());
                    }
                }
                int nStart16 = StringToBeParsed.indexOf("PAY1_TRANSACTIONRESULT");
                if (nStart16 != -1) {
                    int nEnd16 = StringToBeParsed.indexOf(this.separator, nStart16);
                    if (nEnd16 == -1) {
                        this.TransactionResult = StringToBeParsed.substring(nStart16 + 23, StringToBeParsed.length());
                        if (nStart16 >= 4) {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart16 - 4);
                        } else {
                            StringToBeParsed = StringToBeParsed.substring(0, nStart16);
                        }
                    } else {
                        this.TransactionResult = StringToBeParsed.substring(nStart16 + 23, nEnd16);
                        StringToBeParsed = StringToBeParsed.substring(0, nStart16) + StringToBeParsed.substring(nEnd16 + 4, StringToBeParsed.length());
                    }
                }
                this.CustomInfo = StringToBeParsed.trim();
                return true;
            } catch (Exception e) {
                this.ErrorCode = "9999";
                this.ErrorDescription = "Error parsing String";
                return false;
            }
        }

        public String URLDecode(String str) throws Exception {
            if (str == null) {
                return null;
            }
            char[] res = new char[str.length()];
            int didx = 0;
            int sidx = 0;
            while (sidx < str.length()) {
                char ch = str.charAt(sidx);
                if (ch == '+') {
                    int i = didx;
                    didx++;
                    res[i] = ' ';
                } else if (ch == '%') {
                    try {
                        int i2 = didx;
                        didx++;
                        res[i2] = (char) Integer.parseInt(str.substring(sidx + 1, sidx + 3), 16);
                        sidx += 2;
                    } catch (NumberFormatException e) {
                        int didx2 = didx - 1;
                        didx = didx2 + 1;
                        res[didx2] = ch;
                    }
                } else {
                    int i3 = didx;
                    didx++;
                    res[i3] = ch;
                }
                sidx++;
            }
            return String.valueOf(res, 0, didx);
        }
    }
}
