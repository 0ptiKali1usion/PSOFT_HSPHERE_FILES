package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.email.MailDomain;
import psoft.util.freemarker.TemplateMap;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/FrontPageResource.class */
public class FrontPageResource extends Resource implements HostDependentResource {
    public FrontPageResource(int type, Collection init) throws Exception {
        super(type, init);
    }

    public FrontPageResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        String login = recursiveGet("login").toString();
        String hostnum = recursiveGet("hostnum").toString();
        he.exec("frontpage-install.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"user-name", login}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        he.exec("frontpage-uninstall.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        initDoneInternal();
    }

    protected void initDoneInternal() throws Exception {
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("getProperties") ? new GetProperties() : key.equals("setProperties") ? new SetProperties() : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        delInternal();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/FrontPageResource$GetProperties.class */
    class GetProperties implements TemplateMethodModel {
        GetProperties() {
            FrontPageResource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                return new TemplateMap(FrontPageResource.this.getProperties());
            } catch (Exception e) {
                Session.getLog().error("Error getting FP props", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/FrontPageResource$SetProperties.class */
    class SetProperties implements TemplateMethodModel {
        SetProperties() {
            FrontPageResource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                HashMap map = new HashMap();
                Iterator i = l.iterator();
                while (i.hasNext()) {
                    String key = HTMLEncoder.decode((String) i.next());
                    String val = HTMLEncoder.decode((String) i.next());
                    Session.getLog().debug("Inside IIS.FrontPage::set prop key=" + key + " val=" + val);
                    map.put(key, val);
                }
                FrontPageResource.this.setProperties(map);
                return new TemplateOKResult();
            } catch (Exception e) {
                Session.getLog().error("Error setting FP props", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    protected Map getProperties(List l) throws Exception {
        return getProperties();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    protected Map getProperties() throws Exception {
        String hostname = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        Collection col = he.exec("frontpage-getinfo.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", hostname}});
        Map res = new HashMap();
        if (col.size() == 2) {
            Iterator j = col.iterator();
            if (j.hasNext()) {
                res.put("MailSender", j.next());
            }
            if (j.hasNext()) {
                res.put("MailReplyTo", j.next());
            }
        }
        return res;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    protected void setProperties(Map map) throws Exception {
        String hostname = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        String mailsender = (String) map.get("MailSender");
        String mailreplyto = (String) map.get("MailReplyTo");
        ResourceId mdid = null;
        boolean isNodomain = false;
        try {
            ResourceId domain = getParent().get().getParent();
            isNodomain = "nodomain".equals(domain.getNamedType());
            if (!isNodomain) {
                mdid = domain.findChild("mail_domain");
            }
        } catch (Exception e) {
        }
        if (mdid == null) {
            mdid = Session.getAccount().getId().findChild("mail_domain");
        }
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        if (isNodomain) {
            he.exec("frontpage-update.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", hostname}, new String[]{"mailsender", mailsender}, new String[]{"mailreplyto", mailreplyto}});
            return;
        }
        long hostid = ((MailDomain) mdid.get()).getHostId();
        String smtp = HostManager.getHost(hostid).getPFirstIP();
        he.exec("frontpage-update.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", hostname}, new String[]{"mailsender", mailsender}, new String[]{"mailreplyto", mailreplyto}, new String[]{"smtp", smtp}});
    }

    protected void delInternal() throws Exception {
        if (this.initialized) {
            physicalDelete(getHostId());
        }
    }

    public TemplateModel FM_fix() throws Exception {
        Map properties = null;
        try {
            properties = getProperties();
        } catch (Exception e) {
        }
        delInternal();
        initDoneInternal();
        if (properties != null) {
            try {
                setProperties(properties);
            } catch (Exception ex) {
                Session.getLog().debug("Unable to set old properties. ", ex);
            }
        }
        return this;
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.frontpage.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.frontpage.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.frontpage.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.frontpage.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("frontpage.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
