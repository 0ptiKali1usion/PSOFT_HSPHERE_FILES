package psoft.hsphere;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.InvocationTargetException;
import psoft.hsphere.admin.CCEncryption;
import psoft.hsphere.admin.PrivateKeyNotLoadedException;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.resource.registrar.RegistrarException;

/* loaded from: hsphere.zip:psoft/hsphere/HSUserException.class */
public class HSUserException extends Exception {
    protected String code;
    protected String msg;

    public static TemplateModel processError(Throwable t, Object resource, String arg) throws TemplateModelException {
        try {
            if (t instanceof InvocationTargetException) {
                Throwable tr = ((InvocationTargetException) t).getTargetException();
                return processError(tr, resource, arg);
            } else if (t instanceof AccessError) {
                Session.getLog().error("Access violation: ", t);
                return new TemplateErrorResult("Access violation: " + arg + ": " + t.getMessage());
            } else if (t instanceof PrivateKeyNotLoadedException) {
                if (!CCEncryption.get().isKeyLoadTicketSent()) {
                    Ticket.create("Private Key is not loaded", 1, Localizer.translateMessage("admin.CCEncryption.privateKeyNotLoadedAdminError"), null, 0, 1, 0, 0, 0, 0);
                    CCEncryption.get().justSentKeyLoadTicket();
                }
                Session.getLog().error("Private Key not loaded", t);
                return new TemplateErrorResult(t.getMessage());
            } else if (t instanceof HSUserException) {
                if (((HSUserException) t).getCode() != null) {
                    return new TemplateErrorResult(t.getMessage(), ((HSUserException) t).getCode());
                }
                return new TemplateErrorResult(t.getMessage());
            } else {
                Ticket.create(t, resource, arg);
                Session.getLog().error(arg, t);
                return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
            }
        } catch (Exception e) {
            Session.getLog().error("Error processing exception", e);
            Session.getLog().error("Original error", t);
            throw new TemplateModelException(e.getMessage());
        }
    }

    public HSUserException(String error) {
        super(error);
        this.msg = Localizer.translateMessage(error, null);
    }

    public HSUserException(String error, Object[] args) {
        super(error);
        this.msg = Localizer.translateMessage(error, args);
    }

    public HSUserException(String error, String code, Object[] args) {
        super(error);
        this.code = code;
        this.msg = Localizer.translateMessage(error, args);
    }

    public HSUserException(String error, String code) {
        super(error);
        this.code = code;
        this.msg = Localizer.translateMessage(error, null);
    }

    public HSUserException(RegistrarException ex) {
        super("Response from registrar server: " + ex.getMessage());
        this.msg = "Response from registrar server: " + ex.getMessage();
        this.code = "" + ex.getCode();
    }

    public String getCode() {
        return this.code;
    }

    @Override // java.lang.Throwable
    public String getLocalizedMessage() {
        return this.msg;
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        return this.msg;
    }

    @Override // java.lang.Throwable
    public String toString() {
        return this.msg;
    }
}
