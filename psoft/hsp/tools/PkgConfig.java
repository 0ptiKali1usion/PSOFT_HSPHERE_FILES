package psoft.hsp.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgConfig.class */
public class PkgConfig {
    String pkgName;
    String pkgDescription;
    String pkgInfo;
    String pkgVendor;
    String pkgVersion;
    String pkgBuild;
    ToolLogger log;
    PkgConfigParameter pkgPrefix;
    Hashtable passedParams;
    public static PkgConfigParameterType TEMPLATES = new PkgConfigParameterType("templates/", 1, Arrays.asList("common/control", "common/replacements"), 1);
    public static PkgConfigParameterType PROPERTIES = new PkgConfigParameterType("pkg_config/default.properties", 7, null, 3);
    public static PkgConfigParameterType XML = new PkgConfigParameterType("pkg_xmls/", 9, null, 1);
    public static PkgConfigParameterType SCRIPTS = new PkgConfigParameterType("pkg_scripts/", 4, null, 1);
    private static Hashtable allowedParams = new Hashtable();

    static {
        allowedParams.put("--with-prefix", new PkgConfigParameterType("", -1, null, -1));
        allowedParams.put("--with-templates", TEMPLATES);
        allowedParams.put("--with-properties", PROPERTIES);
        allowedParams.put("--with-xmls", XML);
        allowedParams.put("--with-classes", new PkgConfigParameterType("pkg_classes/", 12, null, 1));
        allowedParams.put("--with-scripts", SCRIPTS);
        allowedParams.put("--with-scripts-advanced", new PkgConfigParameterType("pkg_scripts/", 4, getAdvScriptsSkel(), 1));
        allowedParams.put("--with-lang-bundle", new PkgConfigParameterType("pkg_lang_bundle/", 3, null, 1));
        allowedParams.put("--with-preinstall", new PkgConfigParameterType("_SCRIPTS/_pre-install", 8, null, 3));
        allowedParams.put("--with-postinstall", new PkgConfigParameterType("_SCRIPTS/_post-install", 8, null, 3));
        allowedParams.put("--with-preupgrade", new PkgConfigParameterType("_SCRIPTS/_pre-upgrade", 8, null, 3));
        allowedParams.put("--with-postupgrade", new PkgConfigParameterType("_SCRIPTS/_post-upgrade", 8, null, 3));
        allowedParams.put("--with-preuninstall", new PkgConfigParameterType("_SCRIPTS/_pre-uninstall", 8, null, 3));
        allowedParams.put("--with-postuninstall", new PkgConfigParameterType("_SCRIPTS/_post_uninstall", 8, null, 3));
        allowedParams.put("--with-jars", new PkgConfigParameterType("pkg_jars/", 2, null, 1));
        allowedParams.put("--with-rpms", new PkgConfigParameterType("pkg_rpms/", 6, Arrays.asList("RH72", "RH73", "RHES2.1", "RHAS2.1", "FBSD4"), 1));
        allowedParams.put("--with-tarballs", new PkgConfigParameterType("pkg_tarballs/", 11, null, 1));
        allowedParams.put("--with-sql", new PkgConfigParameterType("_SCRIPTS/_pkg.sql", 8, null, 3));
        allowedParams.put("--with-sql-uninstall", new PkgConfigParameterType("_SCRIPTS/_pkg-uninstall.sql", 8, null, 3));
        allowedParams.put("--with-sql-upgrade", new PkgConfigParameterType("_SCRIPTS/_pkg-upgrade.sql", 8, null, 3));
        allowedParams.put("--with-images", new PkgConfigParameterType("pkg_images/", 10, null, 1));
    }

    public PkgConfig(String[] conf) throws Exception {
        this.pkgName = null;
        this.pkgDescription = null;
        this.pkgInfo = null;
        this.pkgVendor = null;
        this.pkgVersion = null;
        this.pkgBuild = null;
        this.pkgPrefix = null;
        this.passedParams = null;
        this.passedParams = new Hashtable();
        this.log = ToolLogger.getDefaultLogger();
        for (String paramStr : conf) {
            if (isAllowedParameter(paramStr)) {
                int ei = paramStr.indexOf("=");
                String paramName = ei > 0 ? paramStr.substring(0, ei) : paramStr;
                PkgConfigParameter param = new PkgConfigParameter(paramStr, (PkgConfigParameterType) allowedParams.get(paramName));
                if ("--with-prefix".equals(paramName)) {
                    this.pkgPrefix = param;
                    if (param.getStrPath() == null) {
                        param.setPath(PkgBuilder.PKG_PREFIX);
                    }
                }
                this.passedParams.put(paramName, param);
            }
        }
        if (!this.passedParams.keySet().contains("--with-prefix")) {
            PkgConfigParameter param2 = new PkgConfigParameter("--with-prefix=./", (PkgConfigParameterType) allowedParams.get("--with-prefix"));
            this.passedParams.put("--with-prefix", param2);
            this.pkgPrefix = param2;
            this.log.outMessage("Directory for assembling package was not specified. Assuming current directory...\n");
        }
    }

    public PkgConfig(String[] conf, String name, String description, String info, String vendor, String version, String build) throws Exception {
        this(conf);
        this.pkgName = name;
        this.pkgDescription = description;
        this.pkgInfo = info;
        this.pkgVendor = vendor;
        this.pkgVersion = version;
        this.pkgBuild = build;
    }

    public boolean isConfigured(String message, boolean useSrcPrefix) {
        boolean result = true;
        String srcPrefix = useSrcPrefix ? getSrcLocationPrefix() : "";
        this.log.outMessage(message + "\n");
        for (PkgConfigParameter p : this.passedParams.values()) {
            if (!p.check(srcPrefix)) {
                result = false;
            }
        }
        return result;
    }

    public String getPkgPrefix() {
        return ((PkgConfigParameter) this.passedParams.get("--with-prefix")).getStrPath();
    }

    public File getFileParamPath(String key) {
        return ((PkgConfigParameter) this.passedParams.get(key)).getFilePath();
    }

    public String getSrcLocationPrefix() {
        return this.pkgPrefix.getStrPath() + PkgBuilder.PKG_SOURCE_DIRNAME;
    }

    public String getSrcLocationPrefix(String param) {
        if ("--with-prefix".equals(param)) {
            return getSrcLocationPrefix();
        }
        return getSrcLocationPrefix() + ((PkgConfigParameterType) allowedParams.get(param)).getDir();
    }

    private boolean isAllowedParameter(String param) {
        String temp = param;
        if (temp.indexOf("=") > 0) {
            temp = temp.substring(0, temp.indexOf("="));
        }
        return allowedParams.keySet().contains(temp);
    }

    public Set getPassedParameters() {
        return this.passedParams.keySet();
    }

    public Collection getPassedParamValues() {
        return this.passedParams.values();
    }

    public File getPathByParameter(String param) {
        return (File) this.passedParams.get(param);
    }

    public static String getPkgSrcPath(String param) {
        return ((PkgConfigParameterType) allowedParams.get(param)).getDir();
    }

    public boolean checkPreparedSource() {
        return isConfigured("Checking package source directories\n", true);
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public String getPkgDescription() {
        return this.pkgDescription;
    }

    public String getPkgInfo() {
        return this.pkgInfo;
    }

    public String getPkgVendor() {
        return this.pkgVendor;
    }

    public String getPkgVersion() {
        return this.pkgVersion;
    }

    public String getPkgBuild() {
        return this.pkgBuild;
    }

    private static List getAdvScriptsSkel() {
        ArrayList res = new ArrayList();
        res.addAll(PHServerTypes.getSupportedOsFamilies());
        for (String group : PHServerTypes.getLServerGroupTypes()) {
            for (String platform : PHServerTypes.getSupportedOsFamilies()) {
                res.add(group + "/" + platform);
            }
        }
        return res;
    }

    public PkgConfigParameter getConfigParameter(String paramName) {
        return (PkgConfigParameter) this.passedParams.get(paramName);
    }

    public PkgConfigParameterType getFileTypeByParamName(String paramName) {
        return (PkgConfigParameterType) allowedParams.get(paramName);
    }
}
