package psoft.hsphere.resource.registrar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/EnomResponse.class */
public class EnomResponse {
    protected HashMap response = new HashMap();
    protected StringBuffer fullResponse = new StringBuffer();

    public EnomResponse(HttpURLConnection con) throws RegistrarException {
        parse(con);
    }

    public EnomResponse(HashMap request) throws RegistrarException {
        String command = (String) request.get("Command");
        if (EnomRequest.CHECK.equals(command)) {
            this.response.put("RRPCode", "210");
            this.response.put("RRPText", "Success");
            this.response.put("Command", EnomRequest.CHECK);
            this.response.put("ErrCount", "0");
            this.response.put("Done", "True");
        } else if (EnomRequest.PURCHASE.equals(command)) {
            this.response.put("OrderID", "777");
            this.response.put("RRPCode", "200");
            this.response.put("RRPText", "Success");
            this.response.put("Command", EnomRequest.PURCHASE);
            this.response.put("ErrCount", "0");
            this.response.put("Done", "True");
        }
    }

    public HashMap getResponse() {
        return this.response;
    }

    public String getFullResponse() {
        return this.fullResponse.toString();
    }

    public String getBalance() throws RegistrarException {
        return getResponseValue("Balance", true);
    }

    public Collection getErrors() throws RegistrarException {
        List l = new ArrayList();
        int count = getErrorCount();
        for (int i = 1; i <= count; i++) {
            l.add(getResponseValue("Err" + i, false));
        }
        return l;
    }

    public int getErrorCount() throws RegistrarException {
        String errCount = getResponseValue("ErrCount", true);
        try {
            return Integer.parseInt(errCount);
        } catch (NumberFormatException e) {
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, Localizer.translateMessage("registrar.unexpected_param_value", new String[]{"ErrCount", errCount}), getFullResponse());
        }
    }

    public boolean isOk() throws RegistrarException {
        return 0 == getErrorCount();
    }

    public void isSuccess() throws RegistrarException {
        Session.getLog().debug("Inside EnomResponse:isSuccess isSuccess=" + isOk());
        if (!"TRUE".equals(Session.getPropertyString("ENOM_SLIENT_MODE")) && !isOk()) {
            StringBuffer st = new StringBuffer();
            for (String str : getErrors()) {
                st.append(str).append("\n");
            }
            throw new RegistrarException(RegistrarException.OPERATION_FAILED, st.toString(), getFullResponse());
        }
    }

    public String getText() throws RegistrarException {
        return getResponseValue("RRPText", false);
    }

    public int getCode() throws RegistrarException {
        String resultCode = getResponseValue("RRPCode", true);
        try {
            return Integer.parseInt(resultCode);
        } catch (NumberFormatException e) {
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, Localizer.translateMessage("registrar.unexpected_param_value", new String[]{"ErrCount", resultCode}), getFullResponse());
        }
    }

    private void parse(HttpURLConnection con) throws RegistrarException {
        boolean debug = "TRUE".equals(Session.getPropertyString("REGISTRAR_DEBUG_MODE"));
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    this.fullResponse.append(line + "\n");
                    int index = line.indexOf(61);
                    if (index != -1) {
                        this.response.put(line.substring(0, index), line.substring(index + 1));
                    }
                } catch (IOException e) {
                    throw new RegistrarException(RegistrarException.IOERROR, Localizer.translateMessage("registrar.ioerror_process"), getFullResponse());
                }
            }
            if (debug) {
                Session.getLog().debug("\n\t---BEGIN OF ENOM RESPONSE---" + this.fullResponse.toString() + "\t---END OF ENOM RESPONSE---");
            }
            if (debug) {
                Session.getLog().debug("\n\t---END OF ENOM RESPONSE---");
            }
        } catch (UnsupportedEncodingException e2) {
            throw new RegistrarException(RegistrarException.UNABLE_TO_CONNECT, Localizer.translateMessage("registrar.wrong_encoding"));
        } catch (IOException e3) {
            throw new RegistrarException(RegistrarException.UNABLE_TO_CONNECT, Localizer.translateMessage("registrar.ioerror_process"));
        }
    }

    public String getResponseValue(String valueName, boolean mandatory) throws RegistrarException {
        if (this.response.keySet().contains(valueName)) {
            return (String) this.response.get(valueName);
        }
        if (mandatory) {
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, Localizer.translateMessage("registrar.missing_response_param", new String[]{valueName}), getFullResponse());
        }
        return null;
    }
}
