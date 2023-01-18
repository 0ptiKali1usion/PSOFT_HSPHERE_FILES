package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import org.apache.axis.AxisFault;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.VHResource;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/ErrorDocumentResource.class */
public class ErrorDocumentResource extends VHResource implements HostDependentResource {
    final int TYPE_PATH = 0;
    final int TYPE_URL = 2;
    final String MTYPE_PATH = "FILE";
    final String MTYPE_URL = "URL";
    protected int code;
    protected String msg;
    protected int type;
    protected int subcode;

    public ErrorDocumentResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.TYPE_PATH = 0;
        this.TYPE_URL = 2;
        this.MTYPE_PATH = "FILE";
        this.MTYPE_URL = psoft.hsphere.resource.apache.ErrorDocumentResource.MTYPE_URL;
        this.subcode = 0;
        Iterator i = initValues.iterator();
        String[] fullCode = ((String) i.next()).split("\\.");
        this.code = Integer.parseInt(fullCode[0]);
        if (fullCode.length > 1) {
            this.subcode = Integer.parseInt(fullCode[1]);
        }
        this.msg = (String) i.next();
        if (i.hasNext()) {
            this.type = getType((String) i.next());
        } else {
            this.type = 0;
        }
    }

    public ErrorDocumentResource(ResourceId rid) throws Exception {
        super(rid);
        this.TYPE_PATH = 0;
        this.TYPE_URL = 2;
        this.MTYPE_PATH = "FILE";
        this.MTYPE_URL = psoft.hsphere.resource.apache.ErrorDocumentResource.MTYPE_URL;
        this.subcode = 0;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT code, subcode, msg, type FROM apache_edoc WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.code = rs.getInt("code");
                this.subcode = rs.getInt("subcode");
                this.msg = Session.getClobValue(rs, "msg");
                this.type = rs.getInt("type");
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v4, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String hostname = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        if (WinService.isSOAPSupport()) {
            try {
                ?? r2 = new String[6];
                String[] strArr = new String[2];
                strArr[0] = "resourcename";
                strArr[1] = "errordocument";
                r2[0] = strArr;
                String[] strArr2 = new String[2];
                strArr2[0] = "hostname";
                strArr2[1] = hostname;
                r2[1] = strArr2;
                String[] strArr3 = new String[2];
                strArr3[0] = "code";
                strArr3[1] = Integer.toString(this.code);
                r2[2] = strArr3;
                String[] strArr4 = new String[2];
                strArr4[0] = "type";
                strArr4[1] = getMType();
                r2[3] = strArr4;
                String[] strArr5 = new String[2];
                strArr5[0] = "location";
                strArr5[1] = this.msg;
                r2[4] = strArr5;
                String[] strArr6 = new String[2];
                strArr6[0] = "subcode";
                strArr6[1] = this.subcode > 0 ? Integer.toString(this.subcode) : "";
                r2[5] = strArr6;
                he.invokeMethod("create", r2);
                return;
            } catch (AxisFault fault) {
                if (WinService.getFaultDetailValue(fault, "exception").equals("WrongStausCodeException")) {
                    throw new HSUserException("iis.error_code_not_supported");
                }
                throw AxisFault.makeFault(fault);
            }
        }
        he.exec("httperror-set.asp", (String[][]) new String[]{new String[]{"hostname", hostname}, new String[]{"hostnum", hostnum}, new String[]{"error", Integer.toString(this.code)}, new String[]{"ext", ""}, new String[]{"file", this.msg}, new String[]{"mtype", getMType()}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String hostname = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "errordocument"}, new String[]{"hostname", hostname}, new String[]{"code", Integer.toString(this.code)}});
        } else {
            he.exec("httperror-set.asp", (String[][]) new String[]{new String[]{"hostname", hostname}, new String[]{"hostnum", hostnum}, new String[]{"error", Integer.toString(this.code)}, new String[]{"ext", ""}, new String[]{"file", ""}});
        }
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
        Connection con = Session.getDb();
        try {
            try {
                long parentId = getParent().getId();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM apache_edoc WHERE parent_id = ? AND code = ? AND subcode = ?");
                ps.setLong(1, parentId);
                ps.setInt(2, this.code);
                ps.setInt(3, this.subcode);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("Error document exists");
                }
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO apache_edoc (id, code, subcode, msg, type, parent_id) VALUES (?, ?, ?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setInt(2, this.code);
                ps2.setInt(3, this.subcode);
                Session.setClobValue(ps2, 4, this.msg);
                ps2.setInt(5, this.type);
                ps2.setLong(6, parentId);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting edoc", e);
                throw new HSUserException("errordoc.exists");
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_edoc WHERE id = ?");
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

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        StringBuffer buf = new StringBuffer();
        buf.append("ErrorDocument ").append(this.code).append(" ");
        buf.append(this.msg).append("\n");
        return buf.toString();
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("code")) {
            return new TemplateString(this.code + (this.subcode > 0 ? "." + this.subcode : ""));
        }
        return key.equals("msg") ? new TemplateString(this.msg) : key.equals("doctype") ? new TemplateString(this.type) : key.equals("mtype") ? new TemplateString(getMType()) : super.get(key);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_update(String msg, String mType) throws Exception {
        int type = getType(mType);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE apache_edoc SET msg = ?, type=? WHERE id = ?");
            Session.setClobValue(ps, 1, msg);
            ps.setInt(2, type);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.msg = msg;
            this.type = type;
            String hostname = recursiveGet("real_name").toString();
            String hostnum = recursiveGet("hostnum").toString();
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            if (WinService.isSOAPSupport()) {
                he.invokeMethod("update", new String[]{new String[]{"resourcename", "errordocument"}, new String[]{"hostname", hostname}, new String[]{"code", Integer.toString(this.code)}, new String[]{"type", getMType()}, new String[]{"location", msg}});
            } else {
                he.exec("httperror-set.asp", (String[][]) new String[]{new String[]{"hostname", hostname}, new String[]{"hostnum", hostnum}, new String[]{"error", Integer.toString(this.code)}, new String[]{"ext", ""}, new String[]{"file", msg}, new String[]{"mtype", getMType()}});
            }
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.errordoc.setup", new Object[]{String.valueOf(this.code), _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.errordoc.recurrent", new Object[]{getPeriodInWords(), String.valueOf(this.code), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.errordoc.refund", new Object[]{String.valueOf(this.code), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.errordoc.refundall", new Object[]{String.valueOf(this.code), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("errordoc.desc", new Object[]{recursiveGet("real_name").toString(), new Integer(this.code).toString()});
    }

    protected String getMType() {
        return this.type == 2 ? psoft.hsphere.resource.apache.ErrorDocumentResource.MTYPE_URL : "FILE";
    }

    protected int getType(String mType) {
        return psoft.hsphere.resource.apache.ErrorDocumentResource.MTYPE_URL.equals(mType) ? 2 : 0;
    }
}
