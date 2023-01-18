package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.admin.Settings;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/UnixQuota.class */
public class UnixQuota extends Quota {
    public UnixQuota(ResourceId id) throws Exception {
        super(id);
    }

    public UnixQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.Quota
    public String[] getActualQuotaReport() {
        String[] rep = new String[4];
        try {
            HostEntry he = recursiveGet("host");
            String dir = recursiveGet("dir").toString();
            String cmd = "rsh " + he.getFiler() + " quota report " + he.getFilerPath() + dir.substring("/hsphere/local".length(), dir.length());
            getLog().debug("geting quotas : " + cmd);
            Collection<Object> col = exec(cmd);
            String tmp = "";
            for (Object obj : col) {
                tmp = obj.toString();
            }
            StringTokenizer tok = new StringTokenizer(tmp);
            if (tok.countTokens() != 8) {
                return quotaNA;
            }
            for (int j = 0; j < 8; j++) {
                String t = tok.nextToken();
                switch (j) {
                    case 4:
                        rep[0] = USFormat.format(Integer.parseInt(t) / 1024.0d);
                        break;
                    case 5:
                        rep[1] = USFormat.format(Integer.parseInt(t) / 1024.0d);
                        break;
                    case 6:
                        rep[2] = t;
                        break;
                    case 7:
                        if ("-".equals(t)) {
                            rep[3] = "unlimited";
                            break;
                        } else {
                            rep[3] = t;
                            break;
                        }
                }
            }
            return rep;
        } catch (Throwable e) {
            getLog().warn("can not get quota for " + getId(), e);
            return quotaNA;
        }
    }

    private String quotaCmd(int size) throws Exception {
        HostEntry he = recursiveGet("host");
        return "/hsphere/shared/scripts/fileserver-quota.pl  --filer=" + he.getFiler() + " --qtree=" + he.getFilerPath() + " --action=set --uid=" + recursiveGet("uid") + " --space=" + size + " --files=-";
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO quotas VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.size);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            exec(quotaCmd(this.size));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            HostEntry he = recursiveGet("host");
            String cmd = "/hsphere/shared/scripts/fileserver-quota.pl  --filer=" + he.getFiler() + " --qtree=" + he.getFilerPath() + " --action=del --uid=" + recursiveGet("uid");
            exec(cmd);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM quotas WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected Collection exec(String cmd) throws Exception {
        String tmp;
        Process p = Runtime.getRuntime().exec(cmd);
        Collection col = new LinkedList();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while (true) {
            String readLine = br.readLine();
            tmp = readLine;
            if (null == readLine) {
                break;
            }
            col.add(tmp);
        }
        Session.getLog().debug("UnixQuota.initDone " + cmd);
        int exit = p.waitFor();
        if (0 != exit) {
            Session.getLog().debug("Quota.initDone errors" + exit + " " + tmp);
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String t = er.readLine();
                if (null == t) {
                    break;
                }
                tmp = tmp + t;
            }
            throw new Exception("failed to set quota: " + cmd + "\nSTDERR: " + tmp);
        }
        return col;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("limitMb".equals(key)) {
            return new TemplateString(quotaReport(1));
        }
        if ("limitFiles".equals(key)) {
            return new TemplateString(quotaReport(3));
        }
        if ("usedMb".equals(key)) {
            return new TemplateString(quotaReport(0));
        }
        if ("usedFiles".equals(key)) {
            return new TemplateString(quotaReport(2));
        }
        if ("reload".equals(key)) {
            this.report.put("time", null);
            return new TemplateOKResult();
        } else if ("info".equals(key)) {
            return new TemplateString(info());
        } else {
            if ("start_date".equals(key)) {
                return new TemplateString(DateFormat.getDateInstance(2).format(getPeriodBegin()));
            }
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        int warn;
        try {
            double value = USFormat.parseDouble(quotaReport(0));
            try {
                warn = Integer.parseInt(Settings.get().getValue("quota_warn"));
            } catch (Exception e) {
                warn = 80;
            }
            return (value * 100.0d) / ((double) this.size) > ((double) warn);
        } catch (ParseException e2) {
            return false;
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public String info() {
        try {
            Resource unixuser = getParent().get();
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"Unix quota", FMACLManager.USER, unixuser.get("login").toString(), quotaReport(0), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
