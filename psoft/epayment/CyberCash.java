package psoft.epayment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.http.HttpUtils;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/CyberCash.class */
public class CyberCash extends GenericMerchantGateway {
    protected String server;
    protected String login;
    protected String password;
    protected String key;
    protected final String script = "/hsphere/shared/scripts/CyberCashWrapper.pl";

    static {
        defaultValues.put("SERVER", "http://cr.cybercash.com/cgi-bin/");
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public void init(int id, HashMap v) throws Exception {
        this.f4id = id;
        this.server = getValue(v, "SERVER");
        this.login = getValue(v, "LOGIN");
        this.password = getValue(v, "PASSWORD");
        this.key = getValue(v, "KEY");
        if (this.password == null) {
            this.password = "";
        }
    }

    @Override // psoft.epayment.GenericMerchantGateway, psoft.epayment.MerchantGateway
    public Map getValues() {
        HashMap map = new HashMap();
        map.put("SERVER", this.server);
        map.put("LOGIN", this.login);
        map.put("PASSWORD", this.password);
        map.put("KEY", this.key);
        return map;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap charge(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        String trans_id = getTransID(id);
        request.put("CCPS_HOST", this.server);
        request.put("CYBERCASH_ID", this.login);
        request.put("HASH_SECRET", this.password);
        request.put("MERCHANT_KEY", this.key);
        request.put("x_Type", "mauthcapture");
        request.put("amount", formatAmount(amount));
        request.put("card-number", cc.getNumber());
        request.put("card-exp", formatExp(cc.getExp()));
        request.put("card-name", cc.getName());
        request.put("card-address", cc.getAddress());
        request.put("card-city", cc.getCity());
        request.put("card-state", cc.getState());
        request.put("card-zip", cc.getZip());
        request.put("card-country", cc.getCountry());
        request.put("order-id", trans_id);
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", trans_id);
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
        }
        writeLog(id, data_out, data_in, error);
        if (!success) {
            throw new Exception(error);
        }
        writeCharge(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap authorize(long id, String description, double amount, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(2);
        Hashtable request = new Hashtable();
        String trans_id = getTransID(id);
        request.put("CCPS_HOST", this.server);
        request.put("CYBERCASH_ID", this.login);
        request.put("HASH_SECRET", this.password);
        request.put("MERCHANT_KEY", this.key);
        request.put("x_Type", "mauthonly");
        request.put("amount", formatAmount(amount));
        request.put("card-number", cc.getNumber());
        request.put("card-exp", formatExp(cc.getExp()));
        request.put("card-name", cc.getName());
        request.put("card-address", cc.getAddress());
        request.put("card-city", cc.getCity());
        request.put("card-state", cc.getState());
        request.put("card-zip", cc.getZip());
        request.put("card-country", cc.getCountry());
        request.put("order-id", trans_id);
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("amount", new Double(amount));
            retval.put("id", trans_id);
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
        }
        writeLog(id, data_out, data_in, error);
        if (!success) {
            throw new Exception(error);
        }
        writeAuthorize(amount);
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap capture(long id, String description, HashMap data, CreditCard cc) throws Exception {
        HashMap retval = new HashMap(1);
        Hashtable request = new Hashtable();
        double amount = ((Double) data.get("amount")).doubleValue();
        request.put("CCPS_HOST", this.server);
        request.put("CYBERCASH_ID", this.login);
        request.put("HASH_SECRET", this.password);
        request.put("MERCHANT_KEY", this.key);
        request.put("x_Type", "postauth");
        request.put("amount", formatAmount(amount));
        request.put("order-id", data.get("id"));
        String data_out = makeForm(request);
        String error = "";
        String data_in = "";
        boolean success = true;
        try {
            HashMap result = postForm(data_out);
            data_in = (String) result.get("response");
            retval.put("id", data.get("id"));
        } catch (Exception e) {
            success = false;
            error = e.getMessage();
        }
        writeLog(id, data_out, data_in, error);
        if (!success) {
            throw new Exception(error);
        }
        writeCapture(((Double) data.get("amount")).doubleValue());
        return retval;
    }

    @Override // psoft.epayment.GenericMerchantGateway
    public HashMap voidAuthorize(long id, String description, HashMap data, CreditCard cc) throws Exception {
        double amount = ((Double) data.get("amount")).doubleValue();
        writeLog(id, "", "", "");
        writeVoid(amount);
        HashMap retval = new HashMap(1);
        retval.put("id", "unknown");
        return retval;
    }

    public String getTransID(long id) {
        return id + "_" + TimeUtils.currentTimeMillis();
    }

    protected String formatExp(String origExp) {
        StringBuffer result = new StringBuffer("");
        StringTokenizer st = new StringTokenizer(origExp, "/");
        if (st.countTokens() == 3) {
            st.nextToken();
        }
        result.append(st.nextToken()).append("/");
        String year = st.nextToken();
        if (year.length() == 4) {
            result.append(year.substring(2, 4));
        } else {
            result.append(year);
        }
        return result.toString();
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
        String error;
        HashMap result = new HashMap(2);
        StringBuffer sb = new StringBuffer();
        Process p = Runtime.getRuntime().exec("/hsphere/shared/scripts/CyberCashWrapper.pl");
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
            throw new Exception("Perl returns error:" + ((Object) err_str));
        }
        String response = sb.toString();
        result.put("response", response);
        try {
            Hashtable resList = HttpUtils.parseQueryString(response);
            try {
                String status = ((String[]) resList.get("MStatus"))[0];
                if (!status.equals("success") && !status.equals("success-duplicate")) {
                    try {
                        error = ((String[]) resList.get("MErrMsg"))[0];
                    } catch (Exception e) {
                        error = "Unknown error";
                    }
                    throw new Exception(error);
                }
                return result;
            } catch (Exception e2) {
                throw new Exception("Bad response from processor center " + response);
            }
        } catch (IllegalArgumentException e3) {
            throw new Exception("Bad response from processor center " + response);
        }
    }

    @Override // psoft.epayment.MerchantGateway
    public String getDescription() {
        return "CyberCash (www.cybercash.com)";
    }
}
