package psoft.hsphere.design;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.Hashtable;
import psoft.hsphere.AccountType;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.admin.Settings;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/design/DesignProvider.class */
public class DesignProvider implements TemplateHashModel {
    static final String COMMON_DESIGN = "common";
    static final String REPLACEMENT_DIR_PREFIX = "replacements/";
    static final String TEXT_LINK_SET = "text_link_set";
    protected static Hashtable skin = null;
    protected static Hashtable designs = null;
    protected static Hashtable icons = null;
    protected static Hashtable iconImageSets = null;
    protected static Hashtable skillIconSets = null;
    protected static Hashtable colorTypes = null;
    protected static Hashtable commonImages = null;
    protected static Hashtable designPreviewImages = null;
    protected static Hashtable iconImageSetPreviewImages = null;
    protected static String configString = null;

    public boolean isEmpty() {
        return skin == null;
    }

    public static void initialize(String cfgString) throws Exception {
        if (cfgString != null) {
            configString = cfgString;
            skin = DesignSchemeBuilder.getSchemes(cfgString);
            designs = (Hashtable) skin.get("design");
            icons = (Hashtable) skin.get("icons");
            iconImageSets = (Hashtable) skin.get("icon_image_sets");
            skillIconSets = (Hashtable) skin.get("skill_icon_sets");
            colorTypes = (Hashtable) skin.get("color_type");
            commonImages = (Hashtable) skin.get("common_img");
            designPreviewImages = (Hashtable) skin.get("design_preview");
            iconImageSetPreviewImages = (Hashtable) skin.get("icon_image_set_preview");
            return;
        }
        throw new Exception("DesignProvider: a configuration string parameter must be not null!");
    }

    public static String getDesignPrefix(String designId) {
        return COMMON_DESIGN.equals(designId) ? "" : "de_" + designId + ".";
    }

    public static Hashtable getDesignHash(String designId) {
        try {
            return (Hashtable) designs.get(designId);
        } catch (Exception e) {
            return null;
        }
    }

    public static Collection getAllDesignIds() {
        return designs.keySet();
    }

    public static boolean isValidDesignId(String designId) {
        return getDesignHash(designId) != null;
    }

    public static String getDesignDefaultColorScheme(String designId) {
        try {
            return getDesignHash(designId).get("default_color_scheme").toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getIconImageSetPrefix(String setId) {
        return "is_" + setId + ".";
    }

    public static Hashtable getIconImageSetHash(String setId) {
        if (setId == null || "".equals(setId)) {
            return null;
        }
        try {
            Hashtable result = (Hashtable) iconImageSets.get(setId);
            if (result == null) {
                throw new Exception("There is no the predefined '" + setId + "' icon image set.");
            }
            return result;
        } catch (Exception ex) {
            Session.getLog().debug("Getting Icon Image Set Hash", ex);
            return null;
        }
    }

    public static boolean isValidIconImageSet(String setId) {
        return getIconImageSetHash(setId) != null;
    }

    public static Hashtable getIconHash(String iconId) {
        try {
            return (Hashtable) icons.get(iconId);
        } catch (Exception e) {
            return null;
        }
    }

    public static Hashtable getSkillIconSetHash(String setId, AccountType accountType) {
        try {
            Hashtable result = (Hashtable) ((Hashtable) ((Hashtable) skillIconSets.get(accountType.toString())).get("skill_set")).get(setId);
            if (result == null) {
                throw new Exception("There is no the predefined '" + setId + "' skill icon set for the '" + accountType + "' account type.");
            }
            return result;
        } catch (Exception ex) {
            Session.getLog().debug("Getting Skill Icon Hash", ex);
            return null;
        }
    }

    public static boolean isValidSkillIconSet(String setId, AccountType accountType) {
        return getSkillIconSetHash(setId, accountType) == null;
    }

    public Hashtable getCommonImage(String imageId) {
        try {
            return (Hashtable) commonImages.get(imageId);
        } catch (Exception e) {
            return null;
        }
    }

    private String getDesignColorSchemeId(String designId) {
        String schemeId = Settings.get().getValue(getDesignPrefix(designId) + "color_scheme");
        if (schemeId == null || "".equals(schemeId) || (!"custom".equals(schemeId) && getDesignColorSchemeHash(designId, schemeId) == null)) {
            schemeId = getDesignDefaultColorScheme(designId);
            if (schemeId == null) {
                schemeId = "";
            }
        }
        return schemeId;
    }

    public static Hashtable getDesignColorSchemeHash(String designId, String schemeId) {
        try {
            return (Hashtable) ((Hashtable) getDesignHash(designId).get("color_schemes")).get(schemeId);
        } catch (Exception e) {
            return null;
        }
    }

    public String getDesignSchemeColor(String designId, String colorSchemeId, String colorId) {
        String color = Settings.get().getValue(getDesignPrefix(designId) + colorId);
        if (color != null && !"".equals(color)) {
            return color;
        }
        try {
            return ((Hashtable) getDesignColorSchemeHash(designId, colorSchemeId).get("colors")).get(colorId).toString();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isIconDemoAllowed(String iconId) {
        Hashtable icon = getIconHash(iconId);
        return (icon == null || icon.get("demo_allowed") == null) ? false : true;
    }

    public TemplateModel get(String key) {
        if (key.equals("skin_hash")) {
            return new MapAdapter(skin);
        }
        if (key.equals("all_designs")) {
            return new MapAdapter(designs);
        }
        if (key.equals("all_icons")) {
            return new MapAdapter(icons);
        }
        if (key.equals("all_icon_image_sets")) {
            return new MapAdapter(iconImageSets);
        }
        if (key.equals("all_skill_icon_sets")) {
            return new MapAdapter(skillIconSets);
        }
        if (key.equals("all_color_types")) {
            return new MapAdapter(colorTypes);
        }
        if (key.equals("all_common_images")) {
            return new MapAdapter(commonImages);
        }
        if (key.equals("all_design_previews")) {
            return new MapAdapter(designPreviewImages);
        }
        if (key.equals("all_icon_image_set_previews")) {
            return new MapAdapter(iconImageSetPreviewImages);
        }
        try {
            return TemplateMethodWrapper.getMethod(this, key);
        } catch (IllegalArgumentException e) {
            Session.getLog().error("Design Provider: '" + key + "' is unknown key.", e);
            return new TemplateErrorResult(e);
        }
    }

    public TemplateModel FM_icon(String iconId) {
        return new MapAdapter(getIconHash(iconId));
    }

    public TemplateModel FM_get_design_prefix(String designId) {
        return new TemplateString(getDesignPrefix(designId));
    }

    public TemplateModel FM_get_iconset_prefix(String setId) {
        return new TemplateString(getIconImageSetPrefix(setId));
    }

    public TemplateModel FM_get_design_color_scheme(String designId) {
        return new TemplateString(getDesignColorSchemeId(designId));
    }

    public TemplateModel FM_get_design_scheme_color(String designId, String colorSchemeId, String colorId) {
        return new TemplateString(getDesignSchemeColor(designId, colorSchemeId, colorId));
    }

    public TemplateModel FM_isIconDemoAllowed(String iconId) {
        if (isIconDemoAllowed(iconId)) {
            return new TemplateString("1");
        }
        return null;
    }
}
