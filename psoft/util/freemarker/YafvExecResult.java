package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;

/* loaded from: hsphere.zip:psoft/util/freemarker/YafvExecResult.class */
public class YafvExecResult implements TemplateHashModel, TemplateScalarModel {
    public static final String OK_STATUS = "OK";
    public static final String ERROR_STATUS = "ERROR";
    protected TemplateList errMessages = new TemplateList();

    public YafvExecResult() {
    }

    public YafvExecResult(TemplateModel errMessage) {
        addMessage(errMessage);
    }

    public YafvExecResult(String errMessage) {
        addMessage(errMessage);
    }

    public void addMessage(TemplateModel errMessage) {
        if (errMessage != null && !"".equals(errMessage.toString())) {
            this.errMessages.add(errMessage.toString());
        }
    }

    public void addMessage(String errMessage) {
        if (errMessage != null && !"".equals(errMessage)) {
            this.errMessages.add(errMessage);
        }
    }

    public boolean isStatusOK() {
        return this.errMessages.isEmpty();
    }

    public String getStatus() {
        return isStatusOK() ? "OK" : "ERROR";
    }

    public String getMessage() {
        return isStatusOK() ? "" : this.errMessages.get(0).toString();
    }

    public TemplateList getMessages() {
        return this.errMessages;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("status".equals(key)) {
            return new TemplateString(getStatus());
        }
        if ("msgs".equals(key)) {
            return getMessages();
        }
        if ("msg".equals(key)) {
            return new TemplateString(getMessage());
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String getAsString() throws TemplateModelException {
        return getMessage();
    }

    public String toString() {
        return getMessage();
    }
}
