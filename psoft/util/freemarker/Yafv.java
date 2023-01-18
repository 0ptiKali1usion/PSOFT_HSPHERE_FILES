package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/Yafv.class */
public class Yafv implements TemplateHashModel {
    protected String pack;
    protected boolean wasInternalError = false;
    protected final String classDescription = "Server-Side Validation";
    protected final String ticketDescription = "Server-Side Validation Error";

    public Yafv(String pack) {
        this.pack = pack;
    }

    public TemplateModel get(String key) {
        if ("checkall".equals(key)) {
            return new YafvCheckAll();
        }
        return new YafvCheckField(key);
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel internalCall(String fullQualifiedMethod, List params) throws Exception {
        String vClass;
        String vMethod;
        if (fullQualifiedMethod != null) {
            int lastIndex = fullQualifiedMethod.lastIndexOf(46);
            if (lastIndex > 0) {
                vClass = fullQualifiedMethod.substring(0, lastIndex);
                vMethod = fullQualifiedMethod.substring(lastIndex + 1);
            } else {
                vClass = "";
                vMethod = fullQualifiedMethod;
            }
            return new YafvCheckField(vClass, vMethod).exec(params);
        }
        throw new Exception("Method is not specified.");
    }

    /* loaded from: hsphere.zip:psoft/util/freemarker/Yafv$YafvCheckField.class */
    public class YafvCheckField implements TemplateHashModel, TemplateMethodModel, TemplateScalarModel {
        protected StringBuffer vClass;
        protected String vMethod;

        public YafvCheckField(String key) {
            Yafv.this = r6;
            this.vClass = new StringBuffer(r6.pack);
            this.vMethod = key;
        }

        public YafvCheckField(String vClass, String vMethod) {
            Yafv.this = r6;
            this.vClass = new StringBuffer(r6.pack).append('.').append(vClass);
            this.vMethod = vMethod;
        }

        public TemplateModel get(String key) {
            this.vClass.append(".").append(this.vMethod);
            this.vMethod = key;
            return this;
        }

        public TemplateModel exec(List args) {
            try {
                Class c = Class.forName(this.vClass.toString());
                Class[] paramTypes = new Class[args.size()];
                String[] paramValues = new String[args.size()];
                int i = 0;
                Iterator iter = args.iterator();
                while (iter.hasNext()) {
                    paramTypes[i] = String.class;
                    paramValues[i] = HTMLEncoder.decode((String) iter.next());
                    i++;
                }
                Method method = c.getMethod(this.vMethod, paramTypes);
                TemplateModel msg = (TemplateModel) method.invoke(null, paramValues);
                return new YafvExecResult(msg);
            } catch (Exception e) {
                Session.getLog().debug("Server-Side Validation", e);
                if (!Yafv.this.wasInternalError) {
                    String errMes = "An error occured during the call of method '" + this.vMethod + "'.\nThe message: " + e.toString() + "\nPossible solution: Check whether all the templates were compiled completely.";
                    Ticket.create(new Exception("Server-Side Validation Error"), "Server-Side Validation", errMes);
                    Yafv.this.wasInternalError = true;
                    return new YafvExecResult(Localizer.translateMessage("yafv.server_side_error", null));
                }
                return new YafvExecResult();
            }
        }

        public boolean isEmpty() throws TemplateModelException {
            return false;
        }

        public String getAsString() {
            String errMes = "Detected attempt to get the result value without calling the necessary method '" + this.vMethod + "'.\nPossible solution: Check the mentioned template to be written correctly.";
            Ticket.create(new Exception("Server-Side Validation Error"), "Server-Side Validation", errMes);
            Yafv.this.wasInternalError = true;
            return Localizer.translateMessage("yafv.server_side_error", null);
        }
    }

    /* loaded from: hsphere.zip:psoft/util/freemarker/Yafv$YafvCheckAll.class */
    public class YafvCheckAll implements TemplateMethodModel {
        public YafvCheckAll() {
            Yafv.this = r4;
        }

        public TemplateModel exec(List args) {
            YafvExecResult result = new YafvExecResult();
            if (!Yafv.this.wasInternalError) {
                for (Object param : args) {
                    if (param != null) {
                        result.addMessage(param.toString());
                    }
                }
            } else {
                result.addMessage(Localizer.translateMessage("yafv.server_side_error", null));
            }
            return result;
        }

        public boolean isEmpty() throws TemplateModelException {
            return false;
        }
    }
}
