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
import java.util.StringTokenizer;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/AntiSpam.class */
public class AntiSpam extends Resource implements HostDependentResource {
    public static final String DEFAULT_PROCESSING_VALUE = "remove";
    public static final String DEFAULT_LEVEL_VALUE = "default";
    public static final String DEFAULT_MAXSCORE_VALUE = "undefined";
    protected HashMap htSpamPreferences;
    protected int useMdomainPrefs;
    protected String local;
    protected static HashMap levelMap = new HashMap();
    protected static HashMap maxScoreMap;
    protected String fullEmail;

    static {
        levelMap.put("very_aggressive", "2");
        levelMap.put("aggressive", "4");
        levelMap.put("normal", "7");
        levelMap.put("relaxed", "10");
        levelMap.put("permissive", "14");
        maxScoreMap = new HashMap();
        maxScoreMap.put("very_aggressive", "20");
        maxScoreMap.put("aggressive", "40");
        maxScoreMap.put("strict", "60");
        maxScoreMap.put("moderate", "80");
        maxScoreMap.put("neutral", "100");
        maxScoreMap.put("soft", "150");
        maxScoreMap.put("permissive", "200");
        maxScoreMap.put("loose", "300");
        maxScoreMap.put("very_loose", "500");
    }

    public AntiSpam(ResourceId id) throws Exception {
        super(id);
        this.htSpamPreferences = new HashMap();
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT use_mdomain_prefs, local FROM antispam  WHERE id = ?");
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
                this.htSpamPreferences = ((MailDomain) getParent().get()).loadAntiSpamPreferences(getId().getId());
            } else {
                loadMailDomainSpamPrefs();
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public AntiSpam(int type, Collection values) throws Exception {
        super(type, values);
        this.htSpamPreferences = new HashMap();
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
                PreparedStatement ps = con.prepareStatement("SELECT a.id FROM parent_child p, antispam a WHERE p.parent_id = ? AND p.parent_type = 1001 AND p.child_id = a.id AND p.child_type = 1011 AND a.local = ?");
                ps.setLong(1, getParent().getId());
                ps.setString(2, this.local);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("AntiSpam " + getEmail() + " already exists");
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO antispam (use_mdomain_prefs, id, local) VALUES (?, ?, ?)");
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
                Session.getLog().debug("Error creating antispam", e);
                throw new HSUserException("antispam.exists", new Object[]{getEmail()});
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
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM antispam WHERE id = ?");
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

    private void updateUseMdomainPrefsInDB() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE antispam SET use_mdomain_prefs = ? WHERE id = ?");
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
        if (!((MailDomain) getParent().get()).getIsSpamPrefsLoaded()) {
            ht = ((MailDomain) getParent().get()).loadAntiSpamPreferences(getParent().get().getId().getId());
        } else {
            ht = ((MailDomain) getParent().get()).getSpamPreferences();
        }
        String whiteList = "";
        String blackList = "";
        String spamLevel = DEFAULT_LEVEL_VALUE;
        String spamProcessing = "remove";
        String spamMaxScore = DEFAULT_MAXSCORE_VALUE;
        if (ht.size() > 0) {
            if (ht.get("whiteList") != null) {
                whiteList = ht.get("whiteList").toString();
            }
            if (ht.get("blackList") != null) {
                blackList = ht.get("blackList").toString();
            }
            if (ht.get("spamLevel") != null) {
                spamLevel = ht.get("spamLevel").toString();
            }
            if (ht.get("spamProcessing") != null) {
                spamProcessing = ht.get("spamProcessing").toString();
            }
            if (ht.get("spamMaxScore") != null) {
                spamMaxScore = ht.get("spamMaxScore").toString();
            }
        }
        setAntiSpamPreferences(1, whiteList, blackList, spamLevel, spamProcessing, spamMaxScore);
    }

    public TemplateModel FM_offMailDomainPreferences() throws Exception {
        offMailDomainPreferences();
        return this;
    }

    public void offMailDomainPreferences() throws Exception {
        this.useMdomainPrefs = 0;
        MailServices.setAntiSpamPreferences(recursiveGet("mail_server"), getEmail(), "delete", "delete", "delete", "delete", "delete");
        updateUseMdomainPrefsInDB();
        if (this.htSpamPreferences.size() > 0) {
            this.htSpamPreferences.clear();
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

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("useMdomainPrefs".equals(key)) {
            return new TemplateString(this.useMdomainPrefs);
        }
        if ("whiteList".equals(key)) {
            try {
                return new TemplateString(getPreferenceValue("whiteList"));
            } catch (Exception e) {
                Session.getLog().debug("Can not get antispam preference whitelist:", e);
                throw new TemplateModelException(e.getMessage());
            }
        } else if ("blackList".equals(key)) {
            try {
                return new TemplateString(getPreferenceValue("blackList"));
            } catch (Exception e2) {
                Session.getLog().debug("Can not get antispam preference blacklist:", e2);
                throw new TemplateModelException(e2.getMessage());
            }
        } else if ("spamLevel".equals(key)) {
            try {
                return new TemplateString(getPreferenceValue("spamLevel"));
            } catch (Exception e3) {
                Session.getLog().debug("Can not antispam preference level", e3);
                throw new TemplateModelException(e3.getMessage());
            }
        } else if ("spamProcessing".equals(key)) {
            try {
                return new TemplateString(getPreferenceValue("spamProcessing"));
            } catch (Exception e4) {
                Session.getLog().debug("Can not antispam processing preference ", e4);
                throw new TemplateModelException(e4.getMessage());
            }
        } else if ("spamMaxScore".equals(key)) {
            try {
                return new TemplateString(getPreferenceValue("spamMaxScore"));
            } catch (Exception e5) {
                Session.getLog().debug("Can not antispam maxScore value ", e5);
                throw new TemplateModelException(e5.getMessage());
            }
        } else if ("email".equals(key)) {
            return new TemplateString(getEmail());
        } else {
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.antispam.refund", new Object[]{getEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.antispam.setup", new Object[]{getEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.antispam.recurrent", new Object[]{getPeriodInWords(), getEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.antispam.refundall", new Object[]{getEmail(), f42df.format(begin), f42df.format(end)});
    }

    public static String getSpamLevelValue(String spamLevel) throws Exception {
        String result = (String) levelMap.get(spamLevel);
        if (result == null) {
            throw new Exception("Wrong spam check level parameter " + spamLevel);
        }
        return result;
    }

    public static String getSpamMaxScoreValue(String spamMaxScore) throws Exception {
        String result = (String) maxScoreMap.get(spamMaxScore);
        if (result == null) {
            throw new Exception("Wrong spam maxScore parameter " + spamMaxScore);
        }
        return result;
    }

    public TemplateModel FM_setAntiSpamPreferences(int mdomainSynchronization, String newWhiteString, String newBlackString, String newSpamLevel, String newSpamProcessing, String newMaxScore) throws Exception {
        setAntiSpamPreferences(mdomainSynchronization, newWhiteString, newBlackString, newSpamLevel, newSpamProcessing, newMaxScore);
        return this;
    }

    public void setAntiSpamPreferences(int mdomainSynchronization, String newWhiteString, String newBlackString, String newSpamLevel, String newSpamProcessing, String newMaxScore) throws Exception {
        HashMap spamPrefs = new HashMap();
        for (String key : this.htSpamPreferences.keySet()) {
            spamPrefs.put(key, this.htSpamPreferences.get(key));
        }
        String newWhitelist = "";
        String newBlacklist = "";
        String newWhiteCommaString = "";
        String newBlackCommaString = "";
        StringTokenizer tokenizer = new StringTokenizer(newWhiteString, ",; \n\t\r");
        while (tokenizer.hasMoreTokens()) {
            String value = tokenizer.nextToken();
            if (value != null && value.trim().length() > 0) {
                if (newWhitelist.equalsIgnoreCase("")) {
                    newWhitelist = value;
                } else {
                    newWhitelist = newWhitelist + "\n" + value;
                }
                if (newWhiteCommaString.equalsIgnoreCase("")) {
                    newWhiteCommaString = value;
                } else {
                    newWhiteCommaString = newWhiteCommaString + "," + value;
                }
            }
        }
        StringTokenizer tokenizer2 = new StringTokenizer(newBlackString, ",; \n\t\r");
        while (tokenizer2.hasMoreTokens()) {
            String value2 = tokenizer2.nextToken();
            if (value2 != null && value2.trim().length() > 0) {
                if (newBlacklist.equalsIgnoreCase("")) {
                    newBlacklist = value2;
                } else {
                    newBlacklist = newBlacklist + "\n" + value2;
                }
                if (newBlackCommaString.equalsIgnoreCase("")) {
                    newBlackCommaString = value2;
                } else {
                    newBlackCommaString = newBlackCommaString + "," + value2;
                }
            }
        }
        String whiteValue = spamPrefs.get("whiteList") != null ? spamPrefs.get("whiteList").toString() : "";
        String spamWhitelistScriptValue = ((MailDomain) getParent().get()).getMailPreferevceSaveAction(whiteValue, newWhitelist, "");
        String blackValue = spamPrefs.get("blackList") != null ? spamPrefs.get("blackList").toString() : "";
        String spamBlacklistScriptValue = ((MailDomain) getParent().get()).getMailPreferevceSaveAction(blackValue, newBlacklist, "");
        String levelValue = spamPrefs.get("spamLevel") != null ? spamPrefs.get("spamLevel").toString() : "";
        String spamLevelScriptValue = ((MailDomain) getParent().get()).getMailPreferevceSaveAction(levelValue, newSpamLevel, DEFAULT_LEVEL_VALUE);
        String processingValue = spamPrefs.get("spamProcessing") != null ? spamPrefs.get("spamProcessing").toString() : "";
        String spamProcessingScriptValue = ((MailDomain) getParent().get()).getMailPreferevceSaveAction(processingValue, newSpamProcessing, "remove");
        String maxScoreValue = spamPrefs.get("spamMaxScore") != null ? spamPrefs.get("spamMaxScore").toString() : "";
        String spamMaxScoreScriptValue = ((MailDomain) getParent().get()).getMailPreferevceSaveAction(maxScoreValue, newMaxScore, DEFAULT_MAXSCORE_VALUE);
        if (!"noedit".equals(spamProcessingScriptValue) && "noedit".equals(spamMaxScoreScriptValue)) {
            spamMaxScoreScriptValue = "edit";
        } else if (!"noedit".equals(spamMaxScoreScriptValue) && "noedit".equals(spamProcessingScriptValue)) {
            spamProcessingScriptValue = "edit";
        }
        if (!spamWhitelistScriptValue.equalsIgnoreCase("noedit") || !spamBlacklistScriptValue.equalsIgnoreCase("noedit") || !spamLevelScriptValue.equalsIgnoreCase("noedit") || !spamProcessingScriptValue.equalsIgnoreCase("noedit") || !spamMaxScoreScriptValue.equalsIgnoreCase("noedit")) {
            if (this.useMdomainPrefs == 1) {
                if (mdomainSynchronization == 0) {
                    this.useMdomainPrefs = 0;
                    updateUseMdomainPrefsInDB();
                    ((MailDomain) getParent().get()).insertMailDomainPreferenceAfterUnlink(getId().getId(), "spam_whitelist_from", newWhitelist, newWhiteCommaString, "");
                    ((MailDomain) getParent().get()).insertMailDomainPreferenceAfterUnlink(getId().getId(), "spam_blacklist_from", newBlacklist, newBlackCommaString, "");
                    ((MailDomain) getParent().get()).insertMailDomainPreferenceAfterUnlink(getId().getId(), "spam_level", newSpamLevel, newSpamLevel, DEFAULT_LEVEL_VALUE);
                    ((MailDomain) getParent().get()).insertMailDomainPreferenceAfterUnlink(getId().getId(), "spam_processing", newSpamProcessing, newSpamProcessing, "remove");
                    ((MailDomain) getParent().get()).insertMailDomainPreferenceAfterUnlink(getId().getId(), "spam_max_score", newMaxScore, newMaxScore, DEFAULT_MAXSCORE_VALUE);
                }
            } else {
                ((MailDomain) getParent().get()).saveMailPreference(getId().getId(), "spam_whitelist_from", whiteValue, newWhitelist, newWhiteCommaString, "");
                ((MailDomain) getParent().get()).saveMailPreference(getId().getId(), "spam_blacklist_from", blackValue, newBlacklist, newBlackCommaString, "");
                ((MailDomain) getParent().get()).saveMailPreference(getId().getId(), "spam_level", levelValue, newSpamLevel, newSpamLevel, DEFAULT_LEVEL_VALUE);
                ((MailDomain) getParent().get()).saveMailPreference(getId().getId(), "spam_processing", processingValue, newSpamProcessing, newSpamProcessing, "remove");
                ((MailDomain) getParent().get()).saveMailPreference(getId().getId(), "spam_max_score", maxScoreValue, newMaxScore, newMaxScore, DEFAULT_MAXSCORE_VALUE);
            }
        }
        if (!spamWhitelistScriptValue.equalsIgnoreCase("noedit")) {
            spamPrefs.put("whiteList", newWhitelist);
            if (spamWhitelistScriptValue.equalsIgnoreCase("edit")) {
                spamWhitelistScriptValue = newWhiteCommaString;
            }
        }
        if (!spamBlacklistScriptValue.equalsIgnoreCase("noedit")) {
            spamPrefs.put("blackList", newBlacklist);
            if (spamBlacklistScriptValue.equalsIgnoreCase("edit")) {
                spamBlacklistScriptValue = newBlackCommaString;
            }
        }
        if (!spamLevelScriptValue.equalsIgnoreCase("noedit")) {
            spamPrefs.put("spamLevel", newSpamLevel);
            if (newSpamLevel.equalsIgnoreCase(DEFAULT_LEVEL_VALUE)) {
                spamLevelScriptValue = "delete";
            } else if (spamLevelScriptValue.equalsIgnoreCase("edit")) {
                spamLevelScriptValue = getSpamLevelValue(newSpamLevel);
            }
        }
        if (!spamProcessingScriptValue.equalsIgnoreCase("noedit")) {
            spamPrefs.put("spamProcessing", newSpamProcessing);
            if (spamProcessingScriptValue.equalsIgnoreCase("edit")) {
                spamProcessingScriptValue = newSpamProcessing;
            }
        }
        if (!spamMaxScoreScriptValue.equalsIgnoreCase("noedit")) {
            spamPrefs.put("spamMaxScore", newMaxScore);
            if (newMaxScore.equalsIgnoreCase(DEFAULT_MAXSCORE_VALUE)) {
                spamMaxScoreScriptValue = "delete";
            } else if (spamMaxScoreScriptValue.equalsIgnoreCase("edit")) {
                spamMaxScoreScriptValue = getSpamMaxScoreValue(newMaxScore);
            }
        }
        MailServices.setAntiSpamPreferences(recursiveGet("mail_server"), getEmail(), spamLevelScriptValue, spamWhitelistScriptValue, spamBlacklistScriptValue, spamProcessingScriptValue, spamMaxScoreScriptValue);
        for (String key2 : spamPrefs.keySet()) {
            this.htSpamPreferences.put(key2, spamPrefs.get(key2));
        }
    }

    private void loadMailDomainSpamPrefs() throws Exception {
        if (((MailDomain) getParent().get()).getIsSpamPrefsLoaded()) {
            if (this.htSpamPreferences.size() > 0) {
                this.htSpamPreferences.clear();
            }
            for (String key : ((MailDomain) getParent().get()).getSpamPreferences().keySet()) {
                this.htSpamPreferences.put(key, ((MailDomain) getParent().get()).getSpamPreferences().get(key));
            }
            return;
        }
        this.htSpamPreferences = ((MailDomain) getParent().get()).loadAntiSpamPreferences(getParent().get().getId().getId());
    }

    public int getUseMdomainPrefs() throws Exception {
        return this.useMdomainPrefs;
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
        HashMap ht;
        String spamWhitelistScriptValue = "noedit";
        String spamBlacklistScriptValue = "noedit";
        String spamLevelScriptValue = "noedit";
        String spamProcessingScriptValue = "noedit";
        String spamMaxScoreScriptValue = "noedit";
        new HashMap();
        if (this.useMdomainPrefs == 1) {
            if (!((MailDomain) getParent().get()).getIsSpamPrefsLoaded()) {
                ht = ((MailDomain) getParent().get()).loadAntiSpamPreferences(getParent().get().getId().getId());
            } else {
                ht = ((MailDomain) getParent().get()).getSpamPreferences();
            }
        } else {
            ht = this.htSpamPreferences;
        }
        for (String key : ht.keySet()) {
            if (key.equalsIgnoreCase("whiteList")) {
                StringTokenizer st = new StringTokenizer(ht.get(key).toString());
                while (st.hasMoreTokens()) {
                    String value = st.nextToken();
                    if (spamWhitelistScriptValue.equalsIgnoreCase("noedit")) {
                        spamWhitelistScriptValue = value;
                    } else {
                        spamWhitelistScriptValue = spamWhitelistScriptValue + "," + value;
                    }
                }
            }
            if (key.equalsIgnoreCase("blackList")) {
                StringTokenizer st2 = new StringTokenizer(ht.get(key).toString());
                while (st2.hasMoreTokens()) {
                    String value2 = st2.nextToken();
                    if (spamBlacklistScriptValue.equalsIgnoreCase("noedit")) {
                        spamBlacklistScriptValue = value2;
                    } else {
                        spamBlacklistScriptValue = spamBlacklistScriptValue + "," + value2;
                    }
                }
            }
            if (key.equalsIgnoreCase("spamLevel")) {
                String value3 = ht.get(key).toString();
                if (!value3.equalsIgnoreCase(DEFAULT_LEVEL_VALUE)) {
                    spamLevelScriptValue = getSpamLevelValue(value3);
                }
            }
            if (key.equalsIgnoreCase("spamProcessing")) {
                spamProcessingScriptValue = ht.get(key).toString();
            }
            if (key.equalsIgnoreCase("spamMaxScore")) {
                String value4 = ht.get(key).toString();
                if (!value4.equalsIgnoreCase(DEFAULT_MAXSCORE_VALUE)) {
                    spamMaxScoreScriptValue = getSpamMaxScoreValue(value4);
                }
            }
        }
        MailServices.setAntiSpamPreferences(HostManager.getHost(targetHostId), getEmail(), spamLevelScriptValue, spamWhitelistScriptValue, spamBlacklistScriptValue, spamProcessingScriptValue, spamMaxScoreScriptValue);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        MailServices.setAntiSpamPreferences(HostManager.getHost(targetHostId), getEmail(), "delete", "delete", "delete", "delete", "delete");
        Resource r = getParent().get().getParent().get().getParent().get();
        if (r instanceof Domain) {
            Collection<ResourceId> mdas = r.getId().findChildren("mail_domain_alias");
            for (ResourceId rid : mdas) {
                MailDomainAlias mda = new MailDomainAlias(rid);
                String mdaEmail = geMailDomainAliasParentEmail(mda);
                MailServices.setAntiSpamPreferences(recursiveGet("mail_server"), mdaEmail, "delete", "delete", "delete", "delete", "delete");
            }
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
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

    private String geMailDomainAliasParentEmail(MailDomainAlias mda) throws Exception {
        return getLocal() + "@" + mda.getDomainAlias();
    }

    public String getPreferenceValue(String preferenceName) throws Exception {
        if (this.htSpamPreferences.get(preferenceName) == null && this.useMdomainPrefs == 1) {
            loadMailDomainSpamPrefs();
        }
        if ("spamProcessing".equals(preferenceName) && (this.htSpamPreferences.get("spamProcessing") == null || "".equals(this.htSpamPreferences.get("spamProcessing").toString()))) {
            return "remove";
        }
        return this.htSpamPreferences.get(preferenceName) == null ? "" : this.htSpamPreferences.get(preferenceName).toString();
    }
}
