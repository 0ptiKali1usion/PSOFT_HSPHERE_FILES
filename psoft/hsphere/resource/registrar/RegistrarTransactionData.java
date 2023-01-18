package psoft.hsphere.resource.registrar;

import java.sql.Timestamp;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/RegistrarTransactionData.class */
public class RegistrarTransactionData {
    Timestamp date;
    private String request = "";
    private String response = "";
    private String error = "";

    public RegistrarTransactionData() {
        TimeUtils.getSQLTimestamp();
    }

    public void setRequest(String request) {
        this.request = request;
        this.response = "";
        this.error = "";
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getResponse() {
        return this.response;
    }

    public String getRequest() {
        return this.request;
    }

    public String getError() {
        return this.error;
    }
}
