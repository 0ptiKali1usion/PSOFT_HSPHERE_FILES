package psoft.hsphere.design;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.Session;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.util.XMLManager;

/* loaded from: hsphere.zip:psoft/hsphere/design/DesignSchemeBuilder.class */
public class DesignSchemeBuilder {
    private static int errors;
    static final String className = "Design Scheme Builder ";
    static final String someErrorMessagePrefix = "Design Scheme Builder reported an error.";
    static final String XMLErrorMessagePrefix = "Design Scheme Builder reported an error in the XML file.";
    static final String processFinished = "Design Scheme Builder finished the XML parsing process.";
    private static Hashtable previewDesignImages;
    private static Hashtable previewIconSetImages;

    public static synchronized Hashtable getSchemes(String cfgStr) throws Exception {
        errors = 0;
        previewDesignImages = new Hashtable();
        previewIconSetImages = new Hashtable();
        Hashtable skin = new Hashtable();
        if (!"".equals(cfgStr)) {
            try {
                Document doc = XMLManager.getXML(cfgStr);
                Element root = doc.getDocumentElement();
                skin.put("icons", getIconsSection((Element) root.getElementsByTagName("icons").item(0)));
                skin.put("skill_icon_sets", getSkillIconSetSections(root.getElementsByTagName("skill_icon_sets")));
                skin.put("icon_image_sets", getAllIconImageSetSections(root.getElementsByTagName("icon_image_sets")));
                skin.put("common_img", getCommonImagesSection(root.getElementsByTagName("common_images")));
                skin.put("color_type", getColorTypesSection((Element) root.getElementsByTagName("color_types").item(0)));
                skin.put("design", getDesignSection(root));
                skin.put("design_preview", previewDesignImages);
                skin.put("icon_image_set_preview", previewIconSetImages);
                Session.getLog().debug("Design Scheme Builder finished the XML parsing process. " + (errors != 0 ? new Integer(errors).toString() + " error(s)" : "No errors") + " occured.");
                return skin;
            } catch (Exception ex) {
                throw new Exception(XMLErrorMessagePrefix + ex.getMessage());
            }
        }
        throw new Exception("Design Scheme Builder reported an error. The " + cfgStr + " variable has not been found in the property file.");
    }

    protected static void putByAttribute(String attribute, Element el, Hashtable ht) throws Exception {
        if (el.hasAttribute(attribute)) {
            ht.put(attribute, el.getAttribute(attribute));
            return;
        }
        throw new Exception("Error getting the '" + attribute + "' attribute.");
    }

    protected static void putByAttributeVPrefix(String attribute, String prefix, Element el, Hashtable ht) throws Exception {
        if (el.hasAttribute(attribute)) {
            ht.put(attribute, prefix + el.getAttribute(attribute));
            return;
        }
        throw new Exception("Error getting the '" + attribute + "' attribute.");
    }

    protected static void putByOptAttribute(String attribute, Element el, Hashtable ht) throws Exception {
        if (el.hasAttribute(attribute)) {
            ht.put(attribute, el.getAttribute(attribute));
        }
    }

    protected static void putByOptAttributeVPrefix(String attribute, String prefix, Element el, Hashtable ht) throws Exception {
        if (el.hasAttribute(attribute)) {
            ht.put(attribute, prefix + el.getAttribute(attribute));
        }
    }

    protected static String getReqAttribute(String attribute, Element el) throws Exception {
        if (el.hasAttribute(attribute)) {
            String attrValue = el.getAttribute(attribute);
            if ("put".equals(attrValue) || "get".equals(attrValue) || "size".equals(attrValue) || "remove".equals(attrValue) || "KEYS.".equals(attrValue)) {
                throw new Exception("\"" + attrValue + "\" value for the '" + attribute + "' attribute is not allowed.");
            }
            return el.getAttribute(attribute);
        }
        throw new Exception("Error getting the '" + attribute + "' attribute.");
    }

    protected static String getOptAttribute(String attribute, Element el) throws Exception {
        if (el.hasAttribute(attribute)) {
            String attrValue = el.getAttribute(attribute);
            if ("put".equals(attrValue) || "get".equals(attrValue) || "size".equals(attrValue) || "remove".equals(attrValue) || "KEYS.".equals(attrValue)) {
                throw new Exception("\"" + attrValue + "\" value for the '" + attribute + "' attribute is not allowed.");
            }
            return el.getAttribute(attribute);
        }
        return null;
    }

    protected static String getDirAttribute(String attribute, Element el) throws Exception {
        String res = el.getAttribute(attribute);
        if (!"".equals(res)) {
            if (!res.startsWith("/")) {
                res = "/" + res;
            }
            if (!res.endsWith("/")) {
                res = res + "/";
            }
        }
        return res;
    }

    protected static Hashtable getPreviewImage(Element parentElement) throws Exception {
        Hashtable previewImage = new Hashtable();
        try {
            Element curTopic = (Element) parentElement.getElementsByTagName("preview_image").item(0);
            putByAttribute("file", curTopic, previewImage);
            putByOptAttribute("alt_file", curTopic, previewImage);
            putByOptAttribute("rtl_file", curTopic, previewImage);
            putByOptAttribute("rtl_alt_file", curTopic, previewImage);
            putByAttribute("width", curTopic, previewImage);
            putByAttribute("height", curTopic, previewImage);
            return previewImage;
        } catch (Exception e) {
            throw new Exception(" Error in the <preview_image> section: " + e.toString() + ".");
        }
    }

    protected static Hashtable getImageSetHash(Element parentElement, String fileDir) throws Exception {
        return getImageSetHash(parentElement, fileDir, null);
    }

    protected static Hashtable getImageSetHash(Element parentElement, String fileDir, Hashtable existingHash) throws Exception {
        Hashtable imageHash = existingHash == null ? new Hashtable() : new Hashtable(existingHash);
        String curId = null;
        try {
            NodeList imageList = parentElement.getElementsByTagName("image");
            for (int i = 0; i < imageList.getLength(); i++) {
                Element curTopic = (Element) imageList.item(i);
                Hashtable params = new Hashtable();
                curId = getReqAttribute("id", curTopic);
                putByAttributeVPrefix("file", fileDir, curTopic, params);
                putByOptAttributeVPrefix("alt_file", fileDir, curTopic, params);
                putByOptAttributeVPrefix("rtl_file", fileDir, curTopic, params);
                putByOptAttributeVPrefix("rtl_alt_file", fileDir, curTopic, params);
                putByAttribute("width", curTopic, params);
                putByAttribute("height", curTopic, params);
                imageHash.put(curId, params);
            }
            return imageHash;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + " Last image id=\"" + curId + "\".");
        }
    }

    protected static Hashtable constructImageSetHash(Element parentElement, String fileDir, Hashtable imageSetHash, Hashtable baseImageHash) throws Exception {
        Hashtable imageHash = new Hashtable(baseImageHash);
        Set<Object> imageSetKeys = new HashSet(imageSetHash.keySet());
        String curId = null;
        try {
            NodeList imageList = parentElement.getElementsByTagName("image");
            for (int i = 0; i < imageList.getLength(); i++) {
                Element curTopic = (Element) imageList.item(i);
                curId = getReqAttribute("id", curTopic);
                Hashtable params = new Hashtable();
                putByAttributeVPrefix("file", fileDir, curTopic, params);
                putByOptAttributeVPrefix("alt_file", fileDir, curTopic, params);
                putByOptAttributeVPrefix("rtl_file", fileDir, curTopic, params);
                putByOptAttributeVPrefix("rtl_alt_file", fileDir, curTopic, params);
                putByAttribute("width", curTopic, params);
                putByAttribute("height", curTopic, params);
                imageHash.put(curId, params);
                imageSetKeys.remove(curId);
            }
            for (Object obj : imageSetKeys) {
                curId = obj.toString();
                Hashtable params2 = (Hashtable) imageSetHash.get(curId);
                Hashtable newParams = new Hashtable();
                newParams.put("file", fileDir + params2.get("file").toString());
                if (params2.get("alt_file") != null) {
                    newParams.put("alt_file", fileDir + params2.get("alt_file").toString());
                }
                if (params2.get("rtl_file") != null) {
                    newParams.put("rtl_file", fileDir + params2.get("rtl_file").toString());
                }
                if (params2.get("rtl_alt_file") != null) {
                    newParams.put("rtl_alt_file", fileDir + params2.get("rtl_alt_file").toString());
                }
                newParams.put("width", params2.get("width").toString());
                newParams.put("height", params2.get("height").toString());
                imageHash.put(curId, newParams);
            }
            return imageHash;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + " Last image id=\"" + curId + "\".");
        }
    }

    protected static Hashtable getDesignColors(Element parentElement) throws Exception {
        Hashtable colorHash = new Hashtable();
        ArrayList arrayList = new ArrayList();
        Hashtable result = new Hashtable();
        String curId = null;
        try {
            NodeList colorList = parentElement.getElementsByTagName("color");
            for (int i = 0; i < colorList.getLength(); i++) {
                Element curTopic = (Element) colorList.item(i);
                String reqAttribute = getReqAttribute("id", curTopic);
                curId = reqAttribute;
                colorHash.put(reqAttribute, getReqAttribute("value", curTopic));
                arrayList.add(curId);
            }
            result.put("hash", colorHash);
            result.put("list", arrayList);
            return result;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + " Last color id=\"" + curId + "\".");
        }
    }

    protected static Hashtable freshColorHash(Element parentElement, Hashtable baseHash) throws Exception {
        Hashtable colorHash = new Hashtable(baseHash);
        String curId = null;
        try {
            NodeList colorList = parentElement.getElementsByTagName("color");
            for (int i = 0; i < colorList.getLength(); i++) {
                Element curTopic = (Element) colorList.item(i);
                String reqAttribute = getReqAttribute("id", curTopic);
                curId = reqAttribute;
                colorHash.put(reqAttribute, getReqAttribute("value", curTopic));
            }
            return colorHash;
        } catch (Exception e) {
            throw new Exception(e.getMessage() + " Last color id=\"" + curId + "\".");
        }
    }

    protected static Hashtable getIconsSection(Element parentElement) throws Exception {
        String curId = null;
        Hashtable icons = new Hashtable();
        try {
            NodeList iconList = parentElement.getElementsByTagName("icon");
            for (int i = 0; i < iconList.getLength(); i++) {
                Element curTopic = (Element) iconList.item(i);
                Hashtable params = new Hashtable();
                curId = getReqAttribute("id", curTopic);
                putByAttribute("url_param", curTopic, params);
                putByAttribute("rtype", curTopic, params);
                putByAttribute("platform", curTopic, params);
                putByAttribute("label", curTopic, params);
                putByAttribute("tip", curTopic, params);
                putByAttribute("help", curTopic, params);
                if (getOptAttribute("demo_available", curTopic) != null) {
                    params.put("demo_available", "1");
                }
                if (getOptAttribute("new_window", curTopic) != null) {
                    params.put("new_window", "1");
                }
                icons.put(curId, params);
            }
            return icons;
        } catch (Exception e) {
            throw new Exception(" Section <icons>, last icon id=\"" + curId + "\"." + e.getMessage());
        }
    }

    protected static Hashtable getIconGroupsSubsection(Element parentElement) throws Exception {
        String curId = null;
        Hashtable icon_groups = new Hashtable();
        Hashtable groups = new Hashtable();
        ArrayList arrayList = new ArrayList();
        try {
            NodeList iconGroupList = parentElement.getElementsByTagName("icon_group");
            for (int i = 0; i < iconGroupList.getLength(); i++) {
                Element curTopic = (Element) iconGroupList.item(i);
                curId = getReqAttribute("id", curTopic);
                NodeList iconIds = curTopic.getElementsByTagName("icon");
                ArrayList arrayList2 = new ArrayList();
                for (int j = 0; j < iconIds.getLength(); j++) {
                    arrayList2.add(getReqAttribute("id", (Element) iconIds.item(j)));
                }
                Hashtable ht = new Hashtable();
                ht.put("group", arrayList2);
                ht.put("label", getReqAttribute("label", curTopic));
                groups.put(curId, ht);
                arrayList.add(curId);
            }
            icon_groups.put("icon_group_list", arrayList);
            icon_groups.put("icon_group_hash", groups);
            return icon_groups;
        } catch (Exception e) {
            throw new Exception(" Section <icon_groups>, last group id=\"" + curId + "\"." + e.getMessage());
        }
    }

    protected static Hashtable getSkillIconSetSections(NodeList accountSetList) throws Exception {
        String accountType = null;
        String setId = null;
        Hashtable accountSetHash = new Hashtable();
        for (int asItem = 0; asItem < accountSetList.getLength(); asItem++) {
            try {
                Hashtable skillSetHash = new Hashtable();
                Element accountSet = (Element) accountSetList.item(asItem);
                accountType = getReqAttribute("account_type", accountSet);
                try {
                    NodeList skillSetList = accountSet.getElementsByTagName("skill_set");
                    for (int i = 0; i < skillSetList.getLength(); i++) {
                        Element setTopic = (Element) skillSetList.item(i);
                        setId = getReqAttribute("id", setTopic);
                        Hashtable setHash = getIconGroupsSubsection(setTopic);
                        setHash.put("label", getReqAttribute("label", setTopic));
                        skillSetHash.put(setId, setHash);
                    }
                    Hashtable ht = new Hashtable();
                    ht.put("skill_set", skillSetHash);
                    accountSetHash.put(accountType, ht);
                } catch (Exception es) {
                    throw new Exception(" Subsection <skill_set>, last set id=\"" + setId + "\"." + es.getMessage());
                }
            } catch (Exception ea) {
                throw new Exception(ea.getMessage() + " Section <skill_icon_sets>, account_type=\"" + accountType + "\".");
            }
        }
        return accountSetHash;
    }

    protected static Hashtable getAllIconImageSetSections(NodeList nl) throws Exception {
        String setId = null;
        Hashtable icon_image_sets = new Hashtable();
        for (int issIndex = 0; issIndex < nl.getLength(); issIndex++) {
            try {
                Element parentElement = (Element) nl.item(issIndex);
                String baseDir = getDirAttribute("base_dir", parentElement);
                NodeList iconSetList = parentElement.getElementsByTagName(AccountPreferences.ICON_IMAGE_SET);
                for (int i = 0; i < iconSetList.getLength(); i++) {
                    Element setTopic = (Element) iconSetList.item(i);
                    setId = getReqAttribute("id", setTopic);
                    String setDir = baseDir + getDirAttribute("dir", setTopic);
                    try {
                        previewIconSetImages.put(setId, getPreviewImage(setTopic));
                    } catch (Exception ex) {
                        if (previewIconSetImages.get(setId) == null) {
                            Session.getLog().error(ex.getMessage() + " In section <icon_image_set>, the id of the processed set is '" + setId + "'.", ex);
                            errors++;
                        }
                    }
                    Hashtable ht = new Hashtable();
                    Hashtable existingHash = (Hashtable) icon_image_sets.get(setId);
                    if (existingHash != null) {
                        existingHash = (Hashtable) existingHash.get("image");
                    }
                    ht.put("image", new Hashtable(getImageSetHash(setTopic, setDir, existingHash)));
                    ht.put("label", getReqAttribute("label", setTopic));
                    icon_image_sets.put(setId, ht);
                }
            } catch (Exception e) {
                throw new Exception(" In section <icon_image_set>, the id of the processed set is '" + setId + "'." + e.getMessage());
            }
        }
        return icon_image_sets;
    }

    protected static Hashtable getCommonImagesSection(NodeList l) throws Exception {
        Hashtable hash = new Hashtable();
        for (int i = 0; i < l.getLength(); i++) {
            Element parentElement = (Element) l.item(i);
            try {
                String baseDir = getDirAttribute("base_dir", parentElement);
                hash = getImageSetHash(parentElement, baseDir, hash);
            } catch (Exception e) {
                throw new Exception(" Error in the <common_images> section : " + e.toString() + ".");
            }
        }
        return hash;
    }

    protected static Hashtable getColorTypesSection(Element parentElement) throws Exception {
        String curId = null;
        Hashtable color_type = new Hashtable();
        try {
            NodeList colorTypeList = parentElement.getElementsByTagName("color_type");
            for (int i = 0; i < colorTypeList.getLength(); i++) {
                Element curTopic = (Element) colorTypeList.item(i);
                Hashtable params = new Hashtable();
                curId = getReqAttribute("id", curTopic);
                putByAttribute("label", curTopic, params);
                color_type.put(curId, params);
            }
            return color_type;
        } catch (Exception e) {
            throw new Exception(" In section <colors>, the last type id is '" + curId + "'." + e.getMessage());
        }
    }

    protected static Hashtable getAllowedIconSets(Element parentElement, String tagName) throws Exception {
        String accountType = null;
        Hashtable allowedSetHash = new Hashtable();
        try {
            NodeList tagList = parentElement.getElementsByTagName(tagName);
            for (int asItem = 0; asItem < tagList.getLength(); asItem++) {
                Hashtable accountSetHash = new Hashtable();
                Element accountSet = (Element) tagList.item(asItem);
                accountType = getReqAttribute("account_type", accountSet);
                String setId = null;
                String firstSetId = null;
                try {
                    NodeList setList = accountSet.getElementsByTagName("set");
                    for (int i = 0; i < setList.getLength(); i++) {
                        setId = getReqAttribute("id", (Element) setList.item(i));
                        if (firstSetId == null) {
                            firstSetId = setId;
                        }
                        accountSetHash.put(setId, "1");
                    }
                    Hashtable ht = new Hashtable();
                    String defaultSetId = getOptAttribute(AntiSpam.DEFAULT_LEVEL_VALUE, accountSet);
                    if (firstSetId != null) {
                        if (defaultSetId != null && accountSetHash.get(defaultSetId) == null) {
                            Session.getLog().error("Design Scheme Builder reported an error in the XML file. Section <" + tagName + ">, account_type=\"" + accountType + "\". The default value could not be set to \"" + defaultSetId + "\", because this id is not defined in this section. Instead, \"" + firstSetId + "\" was set as the default.");
                            errors++;
                            defaultSetId = firstSetId;
                        } else if (defaultSetId == null) {
                            defaultSetId = firstSetId;
                        }
                        ht.put(AntiSpam.DEFAULT_LEVEL_VALUE, defaultSetId);
                        ht.put("set", accountSetHash);
                        allowedSetHash.put(accountType, ht);
                    }
                } catch (Exception es) {
                    throw new Exception(" Error in subsection <set> (the id is '" + setId + "') : " + es.toString());
                }
            }
            return allowedSetHash;
        } catch (Exception ea) {
            throw new Exception(" Section <" + tagName + ">, account_type=\"" + accountType + "\"." + ea.getMessage());
        }
    }

    protected static Hashtable getDesignSection(Element parentElement) throws Exception {
        Hashtable baseImageHash;
        String designId = null;
        Hashtable design = new Hashtable();
        NodeList designTagList = parentElement.getElementsByTagName("design");
        for (int i = 0; i < designTagList.getLength(); i++) {
            try {
                Hashtable curDesignHash = new Hashtable();
                new Hashtable();
                Element designTopic = (Element) designTagList.item(i);
                designId = getReqAttribute("id", designTopic);
                putByAttribute("label", designTopic, curDesignHash);
                putByAttribute("template_dir", designTopic, curDesignHash);
                putByAttribute("default_color_scheme", designTopic, curDesignHash);
                try {
                    previewDesignImages.put(designId, getPreviewImage(designTopic));
                } catch (Exception ex) {
                    Session.getLog().error(" Section <design>, last design id=\"" + designId + "\"." + ex.getMessage(), ex);
                    errors++;
                }
                try {
                    Element curTopic = (Element) designTopic.getElementsByTagName("colors").item(0);
                    Hashtable designColors = getDesignColors(curTopic);
                    Hashtable designColorHash = (Hashtable) designColors.get("hash");
                    curDesignHash.put("color_list", (Collection) designColors.get("list"));
                    Element baseImagesTag = (Element) designTopic.getElementsByTagName("base_images").item(0);
                    if (baseImagesTag != null) {
                        try {
                            String baseDir = getDirAttribute("base_dir", baseImagesTag);
                            baseImageHash = getImageSetHash(baseImagesTag, baseDir);
                        } catch (Exception e) {
                            throw new Exception(" Subsection <base_images>." + e.getMessage());
                        }
                    } else {
                        baseImageHash = new Hashtable();
                    }
                    String setId = null;
                    try {
                        Element imageSetsTag = (Element) designTopic.getElementsByTagName("image_sets").item(0);
                        String setBaseDir = getDirAttribute("base_dir", imageSetsTag);
                        Element setImagesTag = (Element) imageSetsTag.getElementsByTagName("set_images").item(0);
                        Hashtable setImageHash = getImageSetHash(setImagesTag, "");
                        Hashtable image_sets = new Hashtable();
                        NodeList imageSetList = imageSetsTag.getElementsByTagName("image_set");
                        for (int j = 0; j < imageSetList.getLength(); j++) {
                            Element setTopic = (Element) imageSetList.item(j);
                            setId = getReqAttribute("id", setTopic);
                            String setDir = setBaseDir + getDirAttribute("dir", setTopic);
                            Hashtable ht = new Hashtable();
                            ht.put("image", constructImageSetHash(setTopic, setDir, setImageHash, baseImageHash));
                            ht.put("label", getReqAttribute("label", setTopic));
                            image_sets.put(setId, ht);
                        }
                        curDesignHash.put("image_sets", image_sets);
                        curDesignHash.put("allowed_skill_icon_sets", getAllowedIconSets(designTopic, "allowed_skill_icon_sets"));
                        curDesignHash.put("allowed_icon_image_sets", getAllowedIconSets(designTopic, "allowed_icon_image_sets"));
                        String schemeId = null;
                        try {
                            Hashtable color_schemes = new Hashtable();
                            NodeList colorSchemeList = designTopic.getElementsByTagName("color_scheme");
                            for (int j2 = 0; j2 < colorSchemeList.getLength(); j2++) {
                                Element schemeTopic = (Element) colorSchemeList.item(j2);
                                schemeId = getReqAttribute("id", schemeTopic);
                                Hashtable params = new Hashtable();
                                putByAttribute("image_set", schemeTopic, params);
                                putByAttribute(AccountPreferences.ICON_IMAGE_SET, schemeTopic, params);
                                putByAttribute("label", schemeTopic, params);
                                params.put("colors", freshColorHash(schemeTopic, designColorHash));
                                color_schemes.put(schemeId, params);
                            }
                            curDesignHash.put("color_schemes", color_schemes);
                            design.put(designId, curDesignHash);
                        } catch (Exception e2) {
                            throw new Exception(" Subsection <color_scheme>, last scheme id=\"" + schemeId + "\"." + e2.getMessage());
                        }
                    } catch (Exception e3) {
                        throw new Exception(" Subsection <image_sets>, last set id=\"" + setId + "\"." + e3.getMessage());
                    }
                } catch (Exception e4) {
                    throw new Exception("Subsection <design_colors>." + e4.getMessage());
                }
            } catch (Exception e5) {
                Session.getLog().error("Design Scheme Builder reported an error in the XML file. Section <design>, last design id=\"" + designId + "\"." + e5.getMessage(), e5);
                errors++;
            }
        }
        return design;
    }
}
