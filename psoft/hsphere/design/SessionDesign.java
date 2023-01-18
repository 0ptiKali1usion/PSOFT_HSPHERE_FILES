package psoft.hsphere.design;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Account;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.AccountType;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.global.Globals;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/design/SessionDesign.class */
public class SessionDesign extends DesignProvider {
    protected Account account = null;
    protected AccountType accountType = null;
    protected String designId = null;
    protected String designPrefix = null;
    protected Hashtable designHash = null;
    protected String colorSchemeId = null;
    protected Hashtable colorSchemeHash = null;
    protected String imageSetId = null;
    protected Hashtable imageSetHash = null;
    protected String iconImageSetId = null;
    protected Hashtable iconImageSetHash = null;
    protected String skillIconSetId = null;
    protected Hashtable skillIconSetHash = null;
    protected String designTemplateDir = null;
    protected String replacementTemplateDir = null;
    private boolean isUpdated = false;
    private boolean areIconsUpdated = false;

    public void reset() {
        this.isUpdated = false;
        this.areIconsUpdated = false;
    }

    private synchronized void refresh() {
        this.account = Session.getAccount();
        this.accountType = getAccountType();
        if (!isEmpty()) {
            this.designId = getDesignId();
            this.designPrefix = getDesignPrefix(this.designId);
            this.designHash = getDesignHash(this.designId);
            this.colorSchemeId = getColorSchemeId();
            this.colorSchemeHash = getColorSchemeHash();
            this.imageSetId = getImageSetId();
            this.imageSetHash = getImageSetHash();
        }
        this.designTemplateDir = getDesignTemplateDir();
        this.replacementTemplateDir = getReplacementTemplateDir();
        this.isUpdated = true;
    }

    public synchronized void enforceDesign(String designId) {
        this.account = null;
        this.accountType = getAccountType();
        if (!isEmpty()) {
            this.designId = getDesignId(designId);
            this.designPrefix = getDesignPrefix(this.designId);
            this.designHash = getDesignHash(this.designId);
            this.colorSchemeId = getColorSchemeId();
            this.colorSchemeHash = getColorSchemeHash();
            this.imageSetId = getImageSetId();
            this.imageSetHash = getImageSetHash();
        }
        this.designTemplateDir = getDesignTemplateDir();
        this.replacementTemplateDir = null;
        this.isUpdated = true;
        this.areIconsUpdated = false;
    }

    private synchronized void checkFresh() {
        if (!this.isUpdated) {
            refresh();
        }
    }

    private synchronized void refreshIcons() {
        checkFresh();
        this.iconImageSetId = getIconImageSetId();
        this.iconImageSetHash = getIconImageSetHash(this.iconImageSetId);
        this.skillIconSetId = getSkillIconSetId();
        this.skillIconSetHash = getSkillIconSetHash(this.skillIconSetId, this.accountType);
        this.areIconsUpdated = true;
    }

    private synchronized void checkFreshIcons() {
        checkFresh();
        if (!this.areIconsUpdated) {
            refreshIcons();
        }
    }

    private AccountType getAccountType() {
        try {
            AccountType at = AccountType.getType(this.account);
            return at.isAdmin() ? at : AccountType.USER;
        } catch (Exception e) {
            return AccountType.UNKNOWN;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:52:0x0074, code lost:
        if (isDesignAvailable(r5) == false) goto L23;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public java.lang.String getDesignId() {
        /*
            r4 = this;
            r0 = 0
            r5 = r0
            r0 = r4
            psoft.hsphere.Account r0 = r0.account
            if (r0 == 0) goto L3a
            r0 = r4
            psoft.hsphere.Account r0 = r0.account     // Catch: java.lang.Exception -> L19
            psoft.hsphere.AccountPreferences r0 = r0.getPreferences()     // Catch: java.lang.Exception -> L19
            java.lang.String r1 = "design_id"
            java.lang.String r0 = r0.getValueSafe(r1)     // Catch: java.lang.Exception -> L19
            r5 = r0
            goto L3a
        L19:
            r6 = move-exception
            org.apache.log4j.Category r0 = psoft.hsphere.Session.getLog()
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r2 = r1
            r2.<init>()
            java.lang.String r2 = "Unable to get preferences for account #"
            java.lang.StringBuilder r1 = r1.append(r2)
            r2 = r4
            psoft.hsphere.Account r2 = r2.account
            psoft.hsphere.ResourceId r2 = r2.getId()
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r1 = r1.toString()
            r2 = r6
            r0.error(r1, r2)
        L3a:
            r0 = r5
            if (r0 == 0) goto L4f
            java.lang.String r0 = ""
            r1 = r5
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L4f
            r0 = r4
            r1 = r5
            boolean r0 = r0.isDesignAvailable(r1)
            if (r0 != 0) goto L87
        L4f:
            r0 = r4
            psoft.hsphere.AccountType r0 = r0.accountType     // Catch: java.lang.Exception -> L83
            boolean r0 = r0.isAdmin()     // Catch: java.lang.Exception -> L83
            if (r0 != 0) goto L7d
            psoft.hsphere.admin.Settings r0 = psoft.hsphere.admin.Settings.get()     // Catch: java.lang.Exception -> L83
            java.lang.String r1 = "default_design"
            java.lang.String r0 = r0.getValue(r1)     // Catch: java.lang.Exception -> L83
            r5 = r0
            r0 = r5
            if (r0 == 0) goto L77
            java.lang.String r0 = ""
            r1 = r5
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Exception -> L83
            if (r0 != 0) goto L77
            r0 = r4
            r1 = r5
            boolean r0 = r0.isDesignAvailable(r1)     // Catch: java.lang.Exception -> L83
            if (r0 != 0) goto L80
        L77:
            java.lang.String r0 = "common"
            r5 = r0
            goto L80
        L7d:
            java.lang.String r0 = "common"
            r5 = r0
        L80:
            goto L87
        L83:
            r6 = move-exception
            java.lang.String r0 = "common"
            r5 = r0
        L87:
            r0 = r5
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.design.SessionDesign.getDesignId():java.lang.String");
    }

    private String getDesignId(String did) {
        did = (did == null || "".equals(did) || !isValidDesignId(did)) ? "common" : "common";
        return did;
    }

    private boolean isValidColorScheme(String schemeId) {
        try {
            return ((Hashtable) this.designHash.get("color_schemes")).get(schemeId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getColorSchemeId() {
        String schemeId = Settings.get().getValue(this.designPrefix + "color_scheme");
        if (schemeId == null || "".equals(schemeId) || !isValidColorScheme(schemeId)) {
            schemeId = getDesignDefaultColorScheme(this.designId);
            if (schemeId == null) {
                schemeId = "";
            }
        }
        return schemeId;
    }

    private Hashtable getColorSchemeHash() {
        try {
            Hashtable result = (Hashtable) ((Hashtable) this.designHash.get("color_schemes")).get(this.colorSchemeId);
            if (result == null && !isEmpty()) {
                throw new Exception("There is no the " + this.colorSchemeId + " Color Scheme in the " + this.designId + " design");
            }
            return result;
        } catch (Exception ex) {
            Session.getLog().debug(ex);
            return null;
        }
    }

    private Hashtable getImageSetHash() {
        try {
            Hashtable result = (Hashtable) ((Hashtable) this.designHash.get("image_sets")).get(this.imageSetId);
            if (result == null && !isEmpty()) {
                throw new Exception("There is no the " + this.imageSetId + " Image Set in the " + this.colorSchemeId + " Color Scheme");
            }
            return result;
        } catch (Exception ex) {
            Session.getLog().debug(ex);
            return null;
        }
    }

    private boolean isValidImageSet(String setId) {
        try {
            return ((Hashtable) this.designHash.get("image_sets")).get(setId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getImageSetId() {
        String setId = Settings.get().getValue(this.designPrefix + "image_set");
        if (setId == null || "".equals(setId) || !isValidImageSet(setId)) {
            try {
                setId = this.colorSchemeHash.get("image_set").toString();
            } catch (Exception ex) {
                if (!isEmpty()) {
                    Session.getLog().debug("There is no the default Image Set in the " + this.colorSchemeId + "Color Scheme", ex);
                }
                setId = "";
            }
        }
        return setId;
    }

    private String getIconImageSetId() {
        String setId = null;
        if (this.account != null) {
            setId = this.account.getPreferences().getValueSafe(AccountPreferences.ICON_IMAGE_SET);
        }
        if (setId == null || "".equals(setId) || !isValidIconImageSet(setId) || !isIconImageSetAllowed(setId)) {
            try {
                try {
                    setId = Settings.get().getValue(this.designPrefix + AccountPreferences.ICON_IMAGE_SET);
                } catch (Exception e) {
                    setId = null;
                }
                if (setId == null || "".equals(setId) || !isValidIconImageSet(setId) || !isIconImageSetAllowed(setId)) {
                    setId = this.colorSchemeHash.get(AccountPreferences.ICON_IMAGE_SET).toString();
                    if (!isIconImageSetAllowed(setId)) {
                        setId = getAllowedIconImageSetHash().get(AntiSpam.DEFAULT_LEVEL_VALUE).toString();
                    }
                }
            } catch (Exception e2) {
                setId = "";
            }
        }
        return setId;
    }

    private Hashtable getAllowedSkillIconSetHash() {
        try {
            return (Hashtable) ((Hashtable) this.designHash.get("allowed_skill_icon_sets")).get(this.accountType.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private ListAdapter getAllowedSetList(Hashtable allowedSetHash) {
        try {
            return new ListAdapter(((Hashtable) allowedSetHash.get("set")).keySet());
        } catch (Exception e) {
            return null;
        }
    }

    private String getDefaultSkillIconSetId() {
        try {
            return getAllowedSkillIconSetHash().get(AntiSpam.DEFAULT_LEVEL_VALUE).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String getSkillIconSetId() {
        String setId = null;
        if (this.account != null) {
            setId = this.account.getPreferences().getValueSafe(AccountPreferences.SKILL_ICON_SET);
        }
        if (setId == null || "".equals(setId) || !isValidSkillIconSet(setId, this.accountType) || !isSkillIconSetAllowed(setId)) {
            setId = getDefaultSkillIconSetId();
        }
        return setId;
    }

    private boolean isSkillIconSetAllowed(String setId) {
        try {
            return "1".equals(((Hashtable) getAllowedSkillIconSetHash().get("set")).get(setId).toString());
        } catch (Exception e) {
            return false;
        }
    }

    private Hashtable getAllowedIconImageSetHash() {
        try {
            return (Hashtable) ((Hashtable) this.designHash.get("allowed_icon_image_sets")).get(this.accountType.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isIconImageSetAllowed(String iconImageSetId) {
        try {
            return "1".equals(((Hashtable) getAllowedIconImageSetHash().get("set")).get(iconImageSetId).toString());
        } catch (Exception e) {
            return false;
        }
    }

    private String getDesignTemplateDir() {
        try {
            String tempDir = this.designHash.get("template_dir").toString();
            if (!"".equals(tempDir) && !tempDir.endsWith("/")) {
                tempDir = tempDir + "/";
            }
            return tempDir;
        } catch (Exception e) {
            return null;
        }
    }

    private String getReplacementTemplateDir() {
        String tempDir = null;
        Plan pl = null;
        if (Session.getRequest() != null && Session.getRequest().getParameter("plan_id") != null && Session.getRequest().getParameter("signup") != null) {
            try {
                pl = Plan.getPlan(Session.getRequest().getParameter("plan_id"));
            } catch (Exception ex) {
                Session.getLog().error("Error getting Plan", ex);
            }
        } else if (this.account != null) {
            pl = this.account.getPlan();
        }
        if (pl != null) {
            tempDir = pl.getValue("_TEMPLATES_DIR");
            if (tempDir != null && !"".equals(tempDir)) {
                tempDir = "replacements/" + tempDir.trim();
                if (!tempDir.endsWith("/")) {
                    tempDir = tempDir + "/";
                }
            }
        }
        return tempDir;
    }

    public String designTemplateDir() {
        checkFresh();
        return this.designTemplateDir;
    }

    public String replacementTemplateDir() {
        checkFresh();
        return this.replacementTemplateDir;
    }

    public String getSessionColor(String colorId) {
        checkFresh();
        String color = Settings.get().getValue(this.designPrefix + colorId);
        if (color != null && !"".equals(color)) {
            return color;
        }
        try {
            return ((Hashtable) this.colorSchemeHash.get("colors")).get(colorId).toString();
        } catch (Exception e) {
            return "";
        }
    }

    public Hashtable getSessionImage(String imageId) {
        checkFresh();
        try {
            return (Hashtable) ((Hashtable) this.imageSetHash.get("image")).get(imageId);
        } catch (Exception e) {
            return null;
        }
    }

    public Hashtable getSessionIconImage(String imageId) {
        checkFreshIcons();
        try {
            return (Hashtable) ((Hashtable) this.iconImageSetHash.get("image")).get(imageId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override // psoft.hsphere.design.DesignProvider
    public TemplateModel get(String key) {
        if (key.equals(AccountPreferences.DESIGN_ID)) {
            checkFresh();
            return new TemplateString(this.designId);
        } else if (key.equals("design_hash")) {
            checkFresh();
            return new MapAdapter(this.designHash);
        } else if (key.equals("design_prefix")) {
            checkFresh();
            return new TemplateString(this.designPrefix);
        } else if (key.equals("color_scheme_id")) {
            checkFresh();
            return new TemplateString(this.colorSchemeId);
        } else if (key.equals("image_set_id")) {
            checkFresh();
            return new TemplateString(this.imageSetId);
        } else if (key.equals("icon_image_set_id")) {
            checkFreshIcons();
            return new TemplateString(this.iconImageSetId);
        } else if (key.equals("skill_set")) {
            checkFreshIcons();
            return new MapAdapter(this.skillIconSetHash);
        } else if (key.equals("icon_group_hash")) {
            checkFreshIcons();
            return new MapAdapter((Hashtable) this.skillIconSetHash.get("icon_group_hash"));
        } else if (key.equals("icon_group_list")) {
            checkFreshIcons();
            return new ListAdapter((Collection) this.skillIconSetHash.get("icon_group_list"));
        } else if (key.equals("allowed_icon_image_set_list")) {
            checkFreshIcons();
            return getAllowedSetList(getAllowedIconImageSetHash());
        } else if (key.equals("allowed_skill_icon_set_list")) {
            checkFreshIcons();
            return getAllowedSetList(getAllowedSkillIconSetHash());
        } else if (key.equals("available_design_ids")) {
            checkFreshIcons();
            return new TemplateList(getAvailableDesignIds());
        } else if (key.equals("is_text_link_set")) {
            checkFreshIcons();
            if (isTextLinkSet()) {
                return new TemplateString("1");
            }
            return null;
        } else {
            return super.get(key);
        }
    }

    public TemplateModel FM_color(String colorId) {
        return new TemplateString(getSessionColor(colorId));
    }

    public TemplateModel FM_image(String imageId) {
        return new MapAdapter(getImage(imageId));
    }

    public Hashtable getImage(String imageId) {
        Hashtable image = getSessionImage(imageId);
        if (image != null) {
            return image;
        }
        return getCommonImage(imageId);
    }

    public boolean isIconDrawable(String iconId) throws Exception {
        Hashtable curIcon;
        if (iconId == null || (curIcon = getIconHash(iconId)) == null) {
            return false;
        }
        String resources = (String) curIcon.get("rtype");
        if (resources == null) {
            resources = "";
        }
        String platforms = (String) curIcon.get("platform");
        if (platforms == null) {
            platforms = "";
        }
        return (this.account == null || this.account.getPlan() == null) ? "".equals(resources) && "".equals(platforms) : ("".equals(resources) || this.account.getPlan().areResourcesAvailable(resources)) && isPlatformAcceptable(this.account.getPlatformType(), platforms);
    }

    private boolean isPlatformAcceptable(String curPlatform, String allowedPlatforms) throws Exception {
        if (allowedPlatforms == null || curPlatform == null) {
            return false;
        }
        if ("".equals(allowedPlatforms)) {
            return true;
        }
        boolean res = false;
        StringTokenizer orGroups = new StringTokenizer(allowedPlatforms, ";");
        while (orGroups.hasMoreTokens() && !res) {
            StringTokenizer andElements = new StringTokenizer(orGroups.nextToken(), ",");
            while (true) {
                if (!andElements.hasMoreTokens()) {
                    break;
                } else if (curPlatform.equalsIgnoreCase(andElements.nextToken().trim())) {
                    res = true;
                } else {
                    res = false;
                    break;
                }
            }
        }
        return res;
    }

    public TemplateModel FM_isIconDrawable(String iconId) throws Exception {
        try {
            if (isIconDrawable(iconId)) {
                return new TemplateString("1");
            }
            return null;
        } catch (Exception ex) {
            Session.getLog().debug("is ICON: ", ex);
            return null;
        }
    }

    public TemplateModel FM_icon_image(String iconId) {
        return new MapAdapter(getSessionIconImage(iconId));
    }

    public TemplateModel FM_refreshCurrent() {
        refresh();
        return null;
    }

    public TemplateModel FM_enforceCurrent(String dId) {
        enforceDesign(dId);
        return null;
    }

    public TemplateModel FM_isDesignAvailable(String designId) {
        if (isDesignAvailable(designId)) {
            return new TemplateString("1");
        }
        return null;
    }

    public boolean isDesignAvailable(String designId) {
        if (designId == null || "".equals(designId) || !isResellerDesignAvailable(designId)) {
            return false;
        }
        if (this.accountType.isAdmin()) {
            return true;
        }
        String res = Settings.get().getValue(getDesignPrefix(designId) + "available");
        return "1".equals(res);
    }

    protected boolean isResellerDesignAvailable(String designId) {
        try {
            return Globals.isSetKeyDisabled("designs", designId) == 0;
        } catch (Exception ex) {
            try {
                Session.getLog().debug("Unable to figure out whether design '" + designId + "' is disabled for reseller '" + Session.getResellerId() + "' or not.", ex);
                return false;
            } catch (UnknownResellerException e) {
                Session.getLog().error(ex);
                return false;
            }
        }
    }

    public boolean isTextLinkSet() {
        return "text_link_set".equals(this.iconImageSetId);
    }

    public List getAvailableDesignIds() {
        List res = new ArrayList();
        for (String did : getAllDesignIds()) {
            if (isDesignAvailable(did)) {
                res.add(did);
            }
        }
        return res;
    }
}
