package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/AntiVirus.class */
public class AntiVirus extends Resource implements HostDependentResource {
    public static final String DEFAULT_PROCESSING_VALUE = "remove";
    protected HashMap htVirusPreferences;
    protected int useMdomainPrefs;
    protected String local;
    protected String fullEmail;

    public AntiVirus(ResourceId id) throws Exception {
        super(id);
        this.htVirusPreferences = new HashMap();
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT use_mdomain_prefs, local FROM antivirus  WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.useMdomainPrefs = rs.getInt(1);
                this.local = rs.getString(2);
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("SELECT full_email FROM mailobject WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                this.fullEmail = rs2.getString(1);
            } else {
                ps2.close();
                ps2 = con.prepareStatement("INSERT INTO mailobject VALUES (?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, getEmail());
                ps2.executeUpdate();
            }
            Session.closeStatement(ps2);
            con.close();
            if (this.useMdomainPrefs == 0) {
                this.htVirusPreferences = ((MailDomain) getParent().get()).loadAntiVirusPreferences(getId().getId());
            } else {
                loadMailDomainVirusPrefs();
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public AntiVirus(int type, Collection values) throws Exception {
        super(type, values);
        this.htVirusPreferences = new HashMap();
        Iterator i = values.iterator();
        this.local = ((String) i.next()).toLowerCase();
        this.useMdomainPrefs = 1;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT a.id FROM parent_child p, antivirus a WHERE p.parent_id = ? AND p.parent_type = 1001 AND p.child_id = a.id AND p.child_type = 1012 AND a.local = ?");
                ps.setLong(1, getParent().getId());
                ps.setString(2, this.local);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("AntiVirus " + getEmail() + " already exists");
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO antivirus (use_mdomain_prefs, id, local) VALUES (?, ?, ?)");
                ps2.setInt(1, this.useMdomainPrefs);
                ps2.setLong(2, getId().getId());
                ps2.setString(3, this.local);
                ps2.executeUpdate();
                ps2.close();
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO mailobject (id, full_email)  VALUES (?, ?)");
                ps3.setLong(1, getId().getId());
                ps3.setString(2, getEmail());
                ps3.executeUpdate();
                Session.closeStatement(ps3);
                con.close();
                onMailDomainPreferences("init");
            } catch (SQLException e) {
                Session.getLog().debug("Error creating antivirus", e);
                throw new HSUserException("antivirus.exists", new Object[]{getEmail()});
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
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM antivirus WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM mailobject WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            cleanMailPreferencesDB();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("useMdomainPrefs".equals(key)) {
            return new TemplateString(this.useMdomainPrefs);
        }
        if ("virusProcessing".equals(key)) {
            try {
                return new TemplateString(getVirusProcessing());
            } catch (Exception e) {
                Session.getLog().debug("Can not virus processing preference ", e);
                throw new TemplateModelException(e.getMessage());
            }
        } else if ("email".equals(key)) {
            return new TemplateString(getEmail());
        } else {
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.antivirus.refund", new Object[]{getEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.antivirus.setup", new Object[]{getEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.antivirus.recurrent", new Object[]{getPeriodInWords(), getEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.antivirus.refundall", new Object[]{getEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return recursiveGet("mail_server").getId();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        String virusProcessingScriptValue = "noedit";
        for (String key : this.htVirusPreferences.keySet()) {
            if (key.equalsIgnoreCase("virusProcessing")) {
                virusProcessingScriptValue = this.htVirusPreferences.get(key).toString();
            }
        }
        MailServices.setAntiVirusPreferences(HostManager.getHost(targetHostId), getEmail(), virusProcessingScriptValue);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        MailServices.setAntiVirusPreferences(HostManager.getHost(targetHostId), getEmail(), "delete");
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public TemplateModel FM_setAntiVirusPreferences(int mdomain_synchronization, String newVirusProcessing) throws Exception {
        setAntiVirusPreferences(mdomain_synchronization, newVirusProcessing);
        return this;
    }

    public void setAntiVirusPreferences(int mdomainSynchronization, String newVirusProcessing) throws Exception {
        HashMap virusPrefs = new HashMap();
        for (String key : this.htVirusPreferences.keySet()) {
            virusPrefs.put(key, this.htVirusPreferences.get(key));
        }
        String processingValue = virusPrefs.get("virusProcessing") != null ? virusPrefs.get("virusProcessing").toString() : "";
        String virusProcessingScriptValue = ((MailDomain) getParent().get()).getMailPreferevceSaveAction(processingValue, newVirusProcessing, "remove");
        if (!virusProcessingScriptValue.equalsIgnoreCase("noedit")) {
            if (this.useMdomainPrefs == 1) {
                if (mdomainSynchronization == 0) {
                    this.useMdomainPrefs = 0;
                    updateUseMdomainPrefsInDB();
                    ((MailDomain) getParent().get()).insertMailDomainPreferenceAfterUnlink(getId().getId(), "virus_processing", newVirusProcessing, newVirusProcessing, "remove");
                }
            } else {
                ((MailDomain) getParent().get()).saveMailPreference(getId().getId(), "virus_processing", processingValue, newVirusProcessing, newVirusProcessing, "remove");
            }
        }
        if (!virusProcessingScriptValue.equalsIgnoreCase("noedit")) {
            virusPrefs.put("virusProcessing", newVirusProcessing);
            if (virusProcessingScriptValue.equalsIgnoreCase("edit")) {
                virusProcessingScriptValue = newVirusProcessing;
            }
        }
        MailServices.setAntiVirusPreferences(recursiveGet("mail_server"), getEmail(), virusProcessingScriptValue);
        for (String key2 : virusPrefs.keySet()) {
            this.htVirusPreferences.put(key2, virusPrefs.get(key2));
        }
    }

    private void loadMailDomainVirusPrefs() throws Exception {
        if (((MailDomain) getParent().get()).getIsVirusPrefsLoaded()) {
            if (this.htVirusPreferences.size() > 0) {
                this.htVirusPreferences.clear();
            }
            for (String key : ((MailDomain) getParent().get()).getVirusPreferences().keySet()) {
                this.htVirusPreferences.put(key, ((MailDomain) getParent().get()).getVirusPreferences().get(key));
            }
            return;
        }
        this.htVirusPreferences = ((MailDomain) getParent().get()).loadAntiVirusPreferences(getParent().get().getId().getId());
    }

    private void updateUseMdomainPrefsInDB() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE antivirus SET use_mdomain_prefs = ? WHERE id = ?");
            ps.setInt(1, this.useMdomainPrefs);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_onMailDomainPreferences() throws Exception {
        onMailDomainPreferences("");
        return this;
    }

    public void onMailDomainPreferences(String mode) throws Exception {
        HashMap ht;
        if (!mode.equalsIgnoreCase("init")) {
            this.useMdomainPrefs = 1;
            updateUseMdomainPrefsInDB();
            cleanMailPreferencesDB();
        }
        new HashMap();
        if (!((MailDomain) getParent().get()).getIsVirusPrefsLoaded()) {
            ht = ((MailDomain) getParent().get()).loadAntiVirusPreferences(getParent().get().getId().getId());
        } else {
            ht = ((MailDomain) getParent().get()).getVirusPreferences();
        }
        String virusProcessing = "remove";
        if (ht.size() > 0) {
            virusProcessing = ht.get("virusProcessing") == null ? "remove" : ht.get("virusProcessing").toString();
        }
        setAntiVirusPreferences(1, virusProcessing);
    }

    public TemplateModel FM_offMailDomainPreferences() throws Exception {
        offMailDomainPreferences();
        return this;
    }

    public void offMailDomainPreferences() throws Exception {
        this.useMdomainPrefs = 0;
        MailServices.setAntiVirusPreferences(recursiveGet("mail_server"), getEmail(), "delete");
        updateUseMdomainPrefsInDB();
        if (this.htVirusPreferences.size() > 0) {
            this.htVirusPreferences.clear();
        }
    }

    private void cleanMailPreferencesDB() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mail_preferences WHERE mobject_id = ?");
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

    public int getUseMdomainPrefs() throws Exception {
        return this.useMdomainPrefs;
    }

    public String getLocal() throws Exception {
        return this.local;
    }

    public String getEmail() throws TemplateModelException {
        if (this.fullEmail != null) {
            return this.fullEmail;
        }
        this.fullEmail = this.local + "@" + getParent().get("name");
        return this.fullEmail;
    }

    public String getVirusProcessing() throws Exception {
        if (this.htVirusPreferences.get("virusProcessing") == null && this.useMdomainPrefs == 1) {
            loadMailDomainVirusPrefs();
        }
        if (this.htVirusPreferences.get("virusProcessing") == null || "".equals(this.htVirusPreferences.get("virusProcessing").toString())) {
            return "remove";
        }
        return this.htVirusPreferences.get("virusProcessing").toString();
    }
}
