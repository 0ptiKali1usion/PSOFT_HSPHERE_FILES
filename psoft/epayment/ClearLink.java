package psoft.epayment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.http.HttpUtils;
import psoft.hsphere.Session;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/epayment/ClearLink.class */
public class ClearLink extends GenericMerchantGateway {
    protected String accountType;
    protected String transitRouting;
    protected String micr;
    protected String checkNumber;
    protected String configfile;
    protected String keyFile;
    protected String host;
    protected String port;
    protected String testmode;
    protected String avsLevel;
    protected static final int ChargeType_Sale = 0;
    protected static final int ChargeType_Preauth = 1;
    protected static final int ChargeType_Postauth = 2;
    protected static final int ChargeType_Credit = 3;
    protected static final int ChargeType_Voidsale = 9;
    protected static final int Result_Live = 0;
    protected static final int Result_Good = 1;
    protected static final int Result_Duplicate = 2;
    protected static final int Result_Decline = 3;
    protected static int Field_Result;
    protected static final String script = "/hsphere/shared/scripts/ccapi_order";
    protected static Boolean sync = new Boolean(true);
    private static final SimpleDateFormat expFormat = new SimpleDateFormat("MMyy");

    static {
        defaultValues.put("AVS", "N");
        defaultValues.put("MODE", "TRUE");
        defaultValues.put("PORT", "443");
        defaultValues.put("HOST", "secure.linkpt.net");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.configfile = getValue(v, "CONFIG_FILE");
        this.keyFile = getValue(v, "KEY_FILE");
        this.host = getValue(v, "HOST");
        this.port = getValue(v, "PORT");
        this.testmode = getValue(v, "MODE");
        this.avsLevel = getValue(v, "AVS");
        Field_Result = this.testmode.equals("TRUE") ? 1 : 0;
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("CONFIG_FILE", this.configfile);
        map.put("KEY_FILE", this.keyFile);
        map.put("HOST", this.host);
        map.put("PORT", this.port);
        map.put("AVS", this.avsLevel);
        map.put("MODE", this.testmode);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 0);
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        request.put("CONFIG_FILE", this.configfile);
        request.put("KEY_FILE", this.keyFile);
        request.put("HOST", this.host);
        request.put("PORT", this.port);
        request.put("OrderField_Userid", Long.toString(id));
        request.put("OrderField_Result", Integer.toString(Field_Result));
        request.put("OrderField_Chargetype", Integer.toString(0));
        request.put("OrderField_Chargetotal", formatAmount(amount));
        setCC(request, cc);
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(data_out);
            if (!"APPROVED".equals(result.get("Appr"))) {
                success = false;
                error = (String) result.get("Err");
            }
            data_in = (String) result.get("response");
            retval.put("amount", new Double(amount));
            retval.put("id", result.get("id"));
        } catch (Exception e) {
            Session.getLog().debug("Eror processing credit card", e);
            success = false;
            error = "Error processing credit card: " + error;
        }
        writeLog(trId, id, amount, 0, data_out, data_in, error, success);
        if (!success) {
            throw new Exception(error);
        }
        if (!checkAVS(result)) {
            try {
                voidAuthorize(id, "Voided due to AVS warning", retval, cc);
            } catch (Exception e2) {
                Session.getLog().warn("Void error, id=" + result.get("id").toString());
                e2.printStackTrace();
            }
            throw new Exception("Transaction has been voided due to AVS warning");
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        long trId = writeLog(id, amount, 1);
        HashMap retval = new HashMap(2);
        Hashtable request = new Hashtable();
        request.put("CONFIG_FILE", this.configfile);
        request.put("KEY_FILE", this.keyFile);
        request.put("HOST", this.host);
        request.put("PORT", this.port);
        request.put("OrderField_Userid", Long.toString(id));
        request.put("OrderField_Result", Integer.toString(Field_Result));
        request.put("OrderField_Chargetype", Integer.toString(1));
        request.put("OrderField_Chargetotal", formatAmount(amount));
        setCC(request, cc);
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        HashMap result = new HashMap();
        try {
            result = postForm(data_out);
            if (!"APPROVED".equals(result.get("Appr"))) {
                success = false;
                error = (String) result.get("Err");
            }
            data_in = (String) result.get("response");
            retval.put("amount", new Double(amount));
            retval.put("id", result.get("id"));
        } catch (Exception e) {
            Session.getLog().debug("Eror processing credit card", e);
            success = false;
            error = "Error processing credit card: " + error;
        }
        writeLog(trId, id, amount, 1, data_out, data_in, error, success);
        if (!success) {
            throw new Exception(error);
        }
        if (!checkAVS(result)) {
            try {
                Thread.sleep(500L);
                voidAuthorize(id, "Voided due to AVS warning", retval, cc);
            } catch (Exception e2) {
                Session.getLog().warn("Void error, id=" + result.get("id").toString());
                e2.printStackTrace();
            }
            throw new Exception("Transaction has been voided due to AVS warning");
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 4);
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        request.put("CONFIG_FILE", this.configfile);
        request.put("KEY_FILE", this.keyFile);
        request.put("HOST", this.host);
        request.put("PORT", this.port);
        request.put("OrderField_Userid", Long.toString(id));
        request.put("OrderField_Chargetype", Integer.toString(2));
        request.put("OrderField_Result", Integer.toString(Field_Result));
        request.put("OrderField_Chargetotal", formatAmount(amount));
        request.put("OrderField_Oid", data.get("id"));
        setCC(request, cc);
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            if (!"APPROVED".equals(result.get("Appr"))) {
                success = false;
                error = (String) result.get("Err");
            }
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
        } catch (Exception e) {
            Session.getLog().debug("Eror processing credit card", e);
            success = false;
            error = "Error processing credit card: " + error;
        }
        writeLog(trId, id, amount, 4, data_out, data_in, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeCapture(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        long trId = writeLog(id, amount, 3);
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        request.put("CONFIG_FILE", this.configfile);
        request.put("KEY_FILE", this.keyFile);
        request.put("HOST", this.host);
        request.put("PORT", this.port);
        request.put("OrderField_Userid", Long.toString(id));
        request.put("OrderField_Chargetype", Integer.toString(9));
        request.put("OrderField_Result", Integer.toString(Field_Result));
        request.put("OrderField_Oid", data.get("id"));
        setCC(request, cc);
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            if (!"APPROVED".equals(result.get("Appr"))) {
                success = false;
                error = (String) result.get("Err");
            }
            data_in = (String) result.get("response");
            retval.put("id", result.get("id"));
        } catch (Exception e) {
            Session.getLog().debug("Eror processing credit card", e);
            success = false;
            error = "Error processing credit card: " + error;
        }
        writeLog(trId, id, amount, 3, data_out, data_in, error, success);
        if (!success) {
            throw new Exception(error);
        }
        writeVoid(amount);
        return retval;
    }

    protected String makeForm(Hashtable req) {
        if (req.size() == 0) {
            return "";
        }
        Enumeration e = req.keys();
        Object key = e.nextElement();
        StringBuffer buf = new StringBuffer();
        buf.append(key).append("=").append(URLEncoder.encode((String) req.get(key)));
        while (e.hasMoreElements()) {
            Object key2 = e.nextElement();
            buf.append("&").append(key2).append("=").append(URLEncoder.encode((String) req.get(key2)));
        }
        return buf.toString();
    }

    protected HashMap postForm(String data) throws Exception {
        HashMap result = new HashMap(2);
        StringBuffer sb = new StringBuffer();
        Process p = Runtime.getRuntime().exec(script);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        out.write(data);
        out.flush();
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            sb.append(line);
        }
        in.close();
        if (p.waitFor() != 0) {
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuffer err_str = new StringBuffer();
            while (true) {
                String line2 = err.readLine();
                if (line2 == null) {
                    break;
                }
                err_str.append(line2);
            }
            err.close();
            throw new Exception("Script returns error:" + ((Object) err_str));
        }
        String response = sb.toString();
        result.put("response", response);
        try {
            Hashtable resList = HttpUtils.parseQueryString(response);
            String avsRes = "";
            try {
                String status = ((String[]) resList.get("Appr"))[0];
                String trans_id = ((String[]) resList.get("Ord"))[0];
                String code = ((String[]) resList.get("Code"))[0];
                String error = ((String[]) resList.get("Err"))[0];
                Session.getLog().debug("CODE: " + code);
                try {
                    StringTokenizer st = new StringTokenizer(code, ":");
                    if (st.countTokens() > 1) {
                        st.nextToken();
                        avsRes = st.nextToken();
                    }
                } catch (Exception ex) {
                    Session.getLog().warn("Can't get AVS code from the Code field " + code, ex);
                }
                result.put("id", trans_id);
                result.put("Appr", status);
                result.put("code", code);
                result.put("avs", avsRes);
                result.put("Err", error);
                return result;
            } catch (Exception e) {
                throw new Exception("Bad response from processor center " + response);
            }
        } catch (IllegalArgumentException e2) {
            throw new Exception("Bad response from processor center " + response);
        }
    }

    private String getYear(String exp) {
        StringTokenizer st = new StringTokenizer(exp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        st.nextToken();
        String year = st.nextToken();
        return year.length() > 2 ? year.substring(year.length() - 2) : year;
    }

    private String getMonth(String exp) {
        StringTokenizer st = new StringTokenizer(exp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        return st.nextToken();
    }

    protected void setCC(Hashtable request, CreditCard cc) throws Exception {
        request.put("OrderField_Bname", cc.getName());
        request.put("OrderField_Baddr1", cc.getAddress());
        request.put("OrderField_Bcity", cc.getCity());
        request.put("OrderField_Bstate", cc.getState());
        request.put("Orderfield_Bzip", cc.getZip());
        request.put("OrderField_Bcountry", cc.getCountry());
        request.put("OrderField_Phone", cc.getPhone());
        request.put("OrderField_Email", cc.getEmail());
        request.put("OrderField_Cardnumber", cc.getNumber());
        request.put("OrderField_Expmonth", getMonth(cc.getExp()));
        request.put("OrderField_Expyear", getYear(cc.getExp()));
        if ("SWITCH".equals(cc.getType()) || "SOLO".equals(cc.getType())) {
            if (!"".equals(cc.getIssueNo())) {
                request.put("OrderField_SwitchIssueNumber", cc.getIssueNo());
            }
            if (!"".equals(cc.getStartDate())) {
                request.put("OrderField_SwitchStartDate", cc.getStartDate(expFormat));
            }
        }
    }

    protected boolean checkAVS(HashMap resp) throws Exception {
        if (this.avsLevel == null || "".equals(this.avsLevel)) {
            return true;
        }
        String avsRes = resp.get("avs").toString();
        if (avsRes == null || "".equals(avsRes)) {
            avsRes = "XX";
        } else if (avsRes.length() > 2) {
            avsRes = avsRes.substring(0, 2);
        }
        Session.getLog().debug("AVS resp result: " + avsRes);
        switch (this.avsLevel.charAt(0)) {
            case 'F':
                return avsRes.indexOf("YY") >= 0;
            case 'L':
                return avsRes.indexOf("NN") < 0;
            case 'M':
                return avsRes.indexOf(89) >= 0;
            default:
                return true;
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "LinkPoint (www.linkpoint.net)";
    }
}
