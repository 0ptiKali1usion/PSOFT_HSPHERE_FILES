package psoft.util;

/* loaded from: hsphere.zip:psoft/util/HttpResponse.class */
public class HttpResponse {
    int code;
    String message;
    String body;

    public HttpResponse(int code, String message, String body) {
        this.code = code;
        this.message = message;
        this.body = body;
    }

    public int getResponseCode() {
        return this.code;
    }

    public String getResponseMessage() {
        return this.message;
    }

    public String getBody() {
        return this.body;
    }
}
