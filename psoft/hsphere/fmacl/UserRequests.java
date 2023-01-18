package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateHash;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/UserRequests.class */
public class UserRequests implements TemplateHashModel {
    public TemplateModel get(String key) throws TemplateModelException {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateModel FM_isSSHRequested() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT response,  req_date, refused FROM shell_req WHERE id = ?");
            ps.setLong(1, Session.getAccountId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TemplateHash result = new TemplateHash();
                result.put("response", rs.getString("response"));
                result.put("date", rs.getTimestamp("req_date").toString());
                if (rs.getInt("refused") == 1) {
                    result.put("refused", "refused");
                }
                Session.closeStatement(ps);
                con.close();
                return result;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_SSHRequest() throws Exception {
        Date now = TimeUtils.getDate();
        if (FM_isSSHRequested() != null) {
            throw new HSUserException("sshrequest.dubl");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO shell_req (id, req_date, refused) VALUES (?, ?, ?)");
            ps.setLong(1, Session.getAccount().getId().getId());
            ps.setTimestamp(2, new Timestamp(now.getTime()));
            ps.setInt(3, 0);
            ps.executeUpdate();
            Ticket.create(Localizer.translateMessage("sshrequest.title"), 75, Localizer.translateMessage("sshrequest.message", new Object[]{Session.getUser().getLogin(), Long.toString(Session.getAccount().getId().getId()), Session.getAccount().getPlan().getDescription()}), null, 1, 1, 0, 1, 1, 0);
            TemplateOKResult templateOKResult = new TemplateOKResult();
            Session.closeStatement(ps);
            con.close();
            return templateOKResult;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_dsUpgradeRequest(String srcDsId, String dstId, String description) throws Exception {
        String message;
        DedicatedServer ds = null;
        Account a = Session.getAccount();
        String accountId = String.valueOf(a.getId().getId());
        try {
            ds = ((DedicatedServerResource) a.FM_getResource(srcDsId).get()).getDSObject();
        } catch (NullPointerException e) {
        }
        if (ds == null) {
            return new TemplateErrorResult(Localizer.translateMessage("ds.ds_unable_obtain_data", new String[]{srcDsId}));
        }
        String message2 = Localizer.translateMessage("ds_upgrade_request.mes_1", new Object[]{Session.getUser().getLogin(), accountId, ds.getName(), String.valueOf(ds.getId())}) + "\n\n";
        if (!"custom".equals(dstId)) {
            DedicatedServerTemplate dst = DSHolder.getAccessibleDSTemplate(Long.parseLong(dstId));
            message = (message2 + Localizer.translateMessage("ds_upgrade_request.mes_2_template", new String[]{dst.getName()}) + "\n") + dst.getFullDescription() + "\n\n";
        } else {
            message = message2 + Localizer.translateMessage("ds_upgrade_request.mes_2_custom") + "\n" + description + "\n\n";
        }
        Ticket.create(Localizer.translateMessage("ds_upgrade_request.title", new String[]{accountId}), 75, message + Localizer.translateMessage("ds.cur_param_description") + "\n" + ds.getFullDescription(), null, 1, 1, 0, 1, 1, 0);
        return new TemplateOKResult();
    }

    public TemplateModel FM_dsCustomBuiltRequest(String description) throws Exception {
        Account a = Session.getAccount();
        String accountId = String.valueOf(a.getId().getId());
        String message = Localizer.translateMessage("ds_custom_built_request.mes_1", new Object[]{Session.getUser().getLogin(), accountId}) + "\n\n" + Localizer.translateMessage("ds_custom_built_request.mes_2") + "\n" + description + "\n";
        Ticket.create(Localizer.translateMessage("ds_custom_built_request.title", new String[]{accountId}), 75, message, null, 1, 1, 0, 1, 1, 0);
        return new TemplateOKResult();
    }

    public TemplateModel FM_dsCancelRequest(String dsId, String schedule, String date, String description) throws Exception {
        String message;
        DedicatedServer ds = null;
        Account a = Session.getAccount();
        String accountId = String.valueOf(a.getId().getId());
        try {
            ds = ((DedicatedServerResource) a.FM_getResource(dsId).get()).getDSObject();
        } catch (NullPointerException e) {
        }
        if (ds == null) {
            return new TemplateErrorResult(Localizer.translateMessage("ds.ds_unable_obtain_data", new String[]{dsId}));
        }
        if ("period_end".equals(schedule)) {
            Calendar ca = TimeUtils.getCalendar(a.getPeriodEnd());
            ca.add(5, -1);
            Date pe = TimeUtils.dropMinutes(ca.getTime());
            ds.scheduleCancellation(pe);
            String message2 = Localizer.translateMessage("ds_cancel_request.mes_1_pe", new Object[]{Session.getUser().getLogin(), accountId, ds.getName(), String.valueOf(ds.getId())}) + "\n\n";
            message = message2 + Localizer.translateMessage("ds_cancel_request.mes_2_pe", new String[]{pe.toString()}) + "\n\n";
        } else if ("date".equals(schedule)) {
            message = Localizer.translateMessage("ds_cancel_request.mes_1_date", new Object[]{Session.getUser().getLogin(), accountId, ds.getName(), String.valueOf(ds.getId()), date}) + "\n\n";
        } else if (!"now".equals(schedule)) {
            return new TemplateErrorResult("ds.cancel_date_notspecified");
        } else {
            message = Localizer.translateMessage("ds_cancel_request.mes_1_now", new Object[]{Session.getUser().getLogin(), accountId, ds.getName(), String.valueOf(ds.getId())}) + "\n\n";
        }
        if (description != null && !"".equals(description)) {
            message = message + Localizer.translateMessage("ds_cancel_request.note") + "\n" + description + "\n\n";
        }
        Ticket.create(Localizer.translateMessage("ds_cancel_request.title", new String[]{accountId}), 75, message + Localizer.translateMessage("ds.cur_param_description") + "\n" + ds.getFullDescription(), null, 1, 1, 0, 1, 1, 0);
        return new TemplateOKResult();
    }

    public TemplateModel FM_dsRebootRequest(String dsId, String period, String day, String time) throws Exception {
        String message = null;
        DedicatedServer ds = null;
        Account a = Session.getAccount();
        String accountId = String.valueOf(a.getId().getId());
        try {
            ds = ((DedicatedServerResource) a.FM_getResource(dsId).get()).getDSObject();
        } catch (NullPointerException e) {
        }
        if (ds == null) {
            return new TemplateErrorResult(Localizer.translateMessage("ds.ds_unable_obtain_data", new String[]{dsId}));
        }
        if ("asap".equals(period)) {
            message = Localizer.translateMessage("ds_reboot_request.mes_asap", new Object[]{Session.getUser().getLogin(), accountId, ds.getName(), String.valueOf(ds.getId())}) + "\n\n";
        } else if ("date".equals(period)) {
            message = Localizer.translateMessage("ds_reboot_request.mes_date", new Object[]{Session.getUser().getLogin(), accountId, ds.getName(), String.valueOf(ds.getId()), day, time}) + "\n\n";
        }
        Ticket.create(Localizer.translateMessage("ds_reboot_request.title", new String[]{accountId}), 75, message, null, 1, 1, 0, 1, 1, 0);
        return new TemplateOKResult();
    }

    public TemplateModel FM_dsAddIPRequest(String dsId, String ipNumber, String note) throws Exception {
        DedicatedServer ds = null;
        Account a = Session.getAccount();
        String accountId = String.valueOf(a.getId().getId());
        try {
            ds = ((DedicatedServerResource) a.FM_getResource(dsId).get()).getDSObject();
        } catch (NullPointerException e) {
        }
        if (ds == null) {
            return new TemplateErrorResult(Localizer.translateMessage("ds.ds_unable_obtain_data", new String[]{dsId}));
        }
        String message = Localizer.translateMessage("ds_add_ip_request.message", new Object[]{Session.getUser().getLogin(), accountId, ipNumber, ds.getName(), String.valueOf(ds.getId())}) + "\n\n";
        Ticket.create(Localizer.translateMessage("ds_add_ip_request.title", new String[]{accountId}), 75, message + Localizer.translateMessage("ds_add_ip_request.note") + "\n" + note, null, 1, 1, 0, 1, 1, 0);
        return new TemplateOKResult();
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
