package psoft.hsp.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.tools.ToolLogger;
import psoft.util.IOUtils;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgConfigParameter.class */
public class PkgConfigParameter {
    String sPath;
    PkgConfigParameterType type;
    ToolLogger log;

    public PkgConfigParameter(String paramString, PkgConfigParameterType type) throws PkgConfigurationException {
        this.sPath = null;
        if (paramString.indexOf("=") > 0) {
            StringTokenizer st = new StringTokenizer(paramString, "=");
            if (st.countTokens() == 2) {
                st.nextToken();
                this.sPath = st.nextToken();
                if ((type.getCheckType() == 1 || type.getCheckType() == 1 || type.getCheckType() == -1) && !this.sPath.endsWith("/")) {
                    this.sPath += "/";
                }
            } else {
                throw new PkgConfigurationException("Unparseable parameter " + paramString);
            }
        }
        this.type = type;
        this.log = ToolLogger.getDefaultLogger();
    }

    public boolean check(String prefix) {
        if (this.sPath != null) {
            return this.type.check(prefix + this.sPath);
        }
        return true;
    }

    public void setPath(String sPath) {
        this.sPath = sPath;
    }

    public String getStrPath() {
        return this.sPath;
    }

    public File getFilePath() {
        return new File(getStrPath());
    }

    public void prepareSource(String pkgPrefix) throws Exception {
        if (getStrPath() == null) {
            makeSkel(pkgPrefix);
        } else {
            copyResources(pkgPrefix);
        }
    }

    private void copyResources(String pkgPrefix) throws Exception {
        IOUtils.copyDirRecursively(this.sPath, pkgPrefix + this.type.getDir(), true);
    }

    private void makeSkel(String prefix) {
        List<String> advSkel = this.type.getAdvSkel();
        if (advSkel != null && advSkel.size() > 0) {
            for (String fName : advSkel) {
                File tmp = new File(prefix + this.type.getDir() + fName);
                if (!tmp.exists()) {
                    try {
                        tmp.mkdirs();
                    } catch (Exception e) {
                        this.log.outMessage("Failed to create " + tmp.getAbsolutePath() + "\n");
                    }
                } else if (tmp.isDirectory()) {
                    this.log.outMessage("Directory " + tmp.getAbsolutePath() + " exists skipping\n");
                }
            }
            return;
        }
        File tmp2 = new File(prefix + this.type.getDir());
        if (this.type.getCheckType() == 3) {
            tmp2.getParentFile().mkdirs();
            try {
                tmp2.createNewFile();
                return;
            } catch (IOException e2) {
                return;
            }
        }
        tmp2.mkdirs();
    }

    public PkgConfigParameterType getType() {
        return this.type;
    }
}
