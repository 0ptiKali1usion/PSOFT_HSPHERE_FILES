package psoft.hsphere;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateMethodWrapper;

/* loaded from: hsphere.zip:psoft/hsphere/AccessTemplateMethodWrapper.class */
public class AccessTemplateMethodWrapper extends TemplateMethodWrapper {
    private static Category log = Category.getInstance(AccessTemplateMethodWrapper.class.getName());

    /* renamed from: st */
    private static LinkedList f20st = new LinkedList();

    static {
        for (int i = 0; i < 100; i++) {
            f20st.addFirst(new AccessTemplateMethodWrapper());
        }
    }

    protected static TemplateMethodWrapper getWrapper(Object o, String name) {
        AccessTemplateMethodWrapper w;
        synchronized (f20st) {
            if (f20st.isEmpty()) {
                w = new AccessTemplateMethodWrapper();
            } else {
                w = (AccessTemplateMethodWrapper) f20st.removeFirst();
            }
            w.set(o, name);
            if (w.isEmpty()) {
                f20st.addFirst(w);
                return null;
            }
            return w;
        }
    }

    protected AccessTemplateMethodWrapper() {
    }

    protected AccessTemplateMethodWrapper(Object o, String name) {
        super(o, name);
    }

    @Override // psoft.util.freemarker.TemplateMethodWrapper
    public TemplateModel exec(List args) throws TemplateModelException {
        try {
            try {
                TemplateModel _exec = _exec(args);
                synchronized (f20st) {
                    f20st.addFirst(this);
                }
                return _exec;
            } catch (InvocationTargetException ie) {
                Throwable t = ie.getTargetException();
                if (t instanceof AccessError) {
                    Session.getLog().error("Access violation: ", t);
                    TemplateErrorResult templateErrorResult = new TemplateErrorResult("Access violation for method " + this.name + ": " + t.getMessage());
                    synchronized (f20st) {
                        f20st.addFirst(this);
                        return templateErrorResult;
                    }
                } else if (!(t instanceof HSUserException)) {
                    Ticket.create(t, this.f276o, "AccessMethodModel: method: " + this.name + ", arguments = " + args);
                    Session.getLog().error("Method Wrapper error for method " + this.name, t);
                    TemplateErrorResult templateErrorResult2 = new TemplateErrorResult("Internal Error, Tech Support Was Notified");
                    synchronized (f20st) {
                        f20st.addFirst(this);
                        return templateErrorResult2;
                    }
                } else if (((HSUserException) t).getCode() != null) {
                    TemplateErrorResult templateErrorResult3 = new TemplateErrorResult(t.getMessage(), ((HSUserException) t).getCode());
                    synchronized (f20st) {
                        f20st.addFirst(this);
                        return templateErrorResult3;
                    }
                } else {
                    TemplateErrorResult templateErrorResult4 = new TemplateErrorResult(t.getMessage());
                    synchronized (f20st) {
                        f20st.addFirst(this);
                        return templateErrorResult4;
                    }
                }
            } catch (Exception t2) {
                Ticket.create(t2, this.f276o, "AccessMethodModel: method: " + this.name + ", arguments = " + args);
                Session.getLog().error("Method Wrapper", t2);
                Session.addMessage(t2.getMessage());
                throw new TemplateModelException("Exception for method " + this.name + "<br>\n" + t2.getMessage());
            }
        } catch (Throwable th) {
            synchronized (f20st) {
                f20st.addFirst(this);
                throw th;
            }
        }
    }

    public static TemplateMethodModel getMethod(Object o, String name) {
        return getWrapper(o, TemplateMethodWrapper.PREFIX + name);
    }
}
