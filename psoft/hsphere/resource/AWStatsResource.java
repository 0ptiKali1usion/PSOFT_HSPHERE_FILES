package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/AWStatsResource.class */
public class AWStatsResource extends Resource implements HostDependentResource {
    public AWStatsResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public AWStatsResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(Long.parseLong(recursiveGet("host_id").toString()));
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        physicalDelete(Long.parseLong(recursiveGet("host_id").toString()));
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("awstats_url")) {
            try {
                HostEntry he = HostManager.getHost(Long.parseLong(recursiveGet("host_id").toString()));
                String domain = _getName();
                if (he instanceof WinHostEntry) {
                    String url = "http://" + domain + "/AWStats/cgi-bin/awstats.pl?config=" + domain;
                    return new TemplateString(url);
                }
                String url2 = "http://" + domain + "/cgi-bin/awstats.pl";
                return new TemplateString(url2);
            } catch (Exception e) {
                return null;
            }
        }
        return super.get(key);
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.awstats.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.awstats.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.awstats.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.awstats.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        Domain domain = (Domain) getParent().get().getParent().get().getParent().get();
        String ip = domain.get("ip").get().toString();
        String name = _getName();
        String aliases = '\"' + name;
        if (!name.equals(ip)) {
            aliases = aliases + ' ' + ip;
        }
        Collection col = domain.getId().findChildren("domain_alias");
        for (Object o : col) {
            aliases = aliases + " " + ((DomainAlias) ((ResourceId) o).get()).alias;
        }
        String aliases2 = aliases + '\"';
        if (he instanceof WinHostEntry) {
            ((WinHostEntry) he).exec("awstats-install.asp", (String[][]) new String[]{new String[]{"user-name", recursiveGet("login").toString().trim()}, new String[]{"password", recursiveGet("password").toString().trim()}, new String[]{"hostname", recursiveGet("real_name").toString().trim()}, new String[]{"aliases", aliases2.trim()}, new String[]{"real_name", recursiveGet("real_name").toString().trim()}});
            return;
        }
        List arguments = new ArrayList();
        arguments.add(recursiveGet("login").toString().trim());
        arguments.add(recursiveGet("group").toString().trim());
        arguments.add(name);
        arguments.add(aliases2.trim());
        arguments.add(recursiveGet("real_name").toString().trim());
        he.exec("awstats-setup", arguments);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v4, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (this.initialized) {
            HostEntry he = HostManager.getHost(targetHostId);
            if (he instanceof WinHostEntry) {
                ((WinHostEntry) he).exec("awstats-remove.asp", (String[][]) new String[]{new String[]{"user-name", recursiveGet("login").toString()}, new String[]{"hostname", _getName()}});
                return;
            }
            List l = new ArrayList();
            l.add(recursiveGet("login").toString());
            l.add(recursiveGet("real_name").toString().trim());
            he.exec("awstats-remove", l);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
