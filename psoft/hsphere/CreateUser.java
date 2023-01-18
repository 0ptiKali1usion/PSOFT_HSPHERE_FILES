package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.user.UserDuplicateLoginException;
import psoft.util.FakeRequest;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/CreateUser.class */
public class CreateUser implements TemplateHashModel {
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    long signupId = -1;

    protected void setSignupId() {
        Session.getLog().debug("CREATEUSER:REMOTE_ADDR is " + Session.getRequest().getRemoteAddr());
        if (this.signupId == -1 || Session.getRequest().getParameter("signup_id") == null) {
            FakeRequest fr = new FakeRequest(this.request);
            this.signupId = SignupManager.saveRequest(fr);
            Session.getLog().debug("SIGNUP_ID GENERATED:" + this.signupId);
            Session.setRequest(fr);
        }
    }

    public CreateUser(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public TemplateModel signup() {
        setSignupId();
        try {
            Session.getLog().debug("Creating new user.....");
            User u = User.createNewUser(this.request, this.response);
            return u;
        } catch (UserDuplicateLoginException ex) {
            Session.getLog().debug("Create user error:" + ex.getMessage());
            return new TemplateErrorResult(ex, "DUP");
        } catch (Exception t) {
            Session.getLog().warn("Create User error: ", t);
            if (t instanceof HSUserException) {
                return new TemplateErrorResult(t.getMessage());
            }
            Ticket ticket = Ticket.create(t, this, "Signup failed");
            long _signupId = Long.parseLong(Session.getRequest().getParameter("signup_id"));
            try {
                User.setTTvalueBySignupId(_signupId, ticket.getId());
            } catch (Exception e) {
                Session.getLog().debug("Failed to store Trouble Ticket number for signup with id " + _signupId, e);
            }
            return new TemplateErrorResult("Internal Error, Your request has been submitted and will be processed by the administrator so your account can be created.You will be informed by an additional e-mail sent to the address you've entered during signup ");
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("signup".equals(key)) {
            return signup();
        }
        if ("signup_id".equals(key)) {
            setSignupId();
            return new TemplateString(this.signupId);
        }
        try {
        } catch (Exception ex) {
            Session.getLog().error("Error accured during getting admZones", ex);
        }
        if ("free_zones".equals(key)) {
            return new ListAdapter(AdmDNSZone.getFreeZones());
        }
        if ("hosted_zones".equals(key)) {
            return new ListAdapter(AdmDNSZone.getHostedZones());
        }
        if ("REMOTE_ADDR".equals(key)) {
            return new TemplateString(this.request.getRemoteAddr());
        }
        if ("plan".equals(key)) {
            String plan_id = Session.getRequest().getParameter("plan_id");
            try {
                return Plan.getPlan(plan_id);
            } catch (UnknownResellerException ex2) {
                Session.getLog().error("Error getting Plan", ex2);
            }
        } else if ("his_plan_accessible".equals(key)) {
            User u = null;
            boolean newUser = true;
            try {
                u = User._getUser(this.request);
                newUser = false;
            } catch (Exception e) {
            }
            try {
                Plan p = Plan.getPlan(this.request.getParameter("plan_id"));
                if ((!newUser || p.isAccessible(0)) && (newUser || u.isAccessible(p))) {
                    return new TemplateString("1");
                }
                return null;
            } catch (UnknownResellerException ex3) {
                Session.getLog().error("Error getting Plan", ex3);
            }
        }
        if ("getTLDPrices".equals(key)) {
            try {
                return getTLDPrices();
            } catch (Exception ex4) {
                Session.getLog().error("Error occured during getting TLD prices", ex4);
            }
        }
        if ("USER_CP_URL".equals(key)) {
            return new TemplateString(getCpURL());
        }
        if ("reseller_url".equals(key)) {
            return new TemplateString(getResellerURL());
        }
        if ("reseller_context_url".equals(key)) {
            return new TemplateString(getCpContextURL());
        }
        return null;
    }

    public String getResellerURL() {
        try {
            Reseller resel = Reseller.getReseller(Session.getResellerId());
            String protocol = resel.getProtocol();
            String port = resel.getPort().trim();
            return protocol + resel.getURL() + ((port == null || "".equals(port)) ? "" : ":" + port);
        } catch (Exception e) {
            Session.getLog().error("Cant get reseller URL", e);
            return null;
        }
    }

    public String getCpURL() {
        try {
            String uri = Session.getPropertyString("CP_URI");
            return getResellerURL() + (uri == null ? "" : uri);
        } catch (Exception e) {
            Session.getLog().error("Cant get reseller URL", e);
            return null;
        }
    }

    public String getCpContextURL() {
        try {
            String uri = Session.getPropertyString("CP_URI");
            String uri2 = uri == null ? "" : uri;
            int pos = uri2.lastIndexOf(47);
            if (pos > 0) {
                uri2 = uri2.substring(0, pos + 1);
            }
            return getResellerURL() + uri2;
        } catch (Exception e) {
            Session.getLog().error("Cant get reseller URL with Context", e);
            return null;
        }
    }

    public TemplateModel getTLDPrices() throws Exception {
        String ext = Session.getRequest().getParameter("ext");
        String tld = ext.substring(1);
        String plan_id = Session.getRequest().getParameter("plan_id");
        Plan plan = Plan.getPlan(plan_id);
        ResourceType rt = plan.getResourceType(TypeRegistry.getTypeId("opensrs"));
        return new TemplateMap(DomainRegistrar.getTLDPrices(tld, rt));
    }

    public TemplateModel getDomainTransferPrice() throws Exception {
        String ext = Session.getRequest().getParameter("ext");
        String tld = ext.substring(1);
        String plan_id = Session.getRequest().getParameter("plan_id");
        Plan plan = Plan.getPlan(plan_id);
        ResourceType rt = plan.getResourceType(TypeRegistry.getTypeId("domain_transfer"));
        return new TemplateString(DomainRegistrar.getTransferPrice(tld, rt));
    }
}
