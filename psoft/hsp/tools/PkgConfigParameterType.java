package psoft.hsp.tools;

import java.io.File;
import java.util.List;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgConfigParameterType.class */
public class PkgConfigParameterType {
    public static final int CHCK_AS_NOTEMPTY_DIR = 1;
    public static final int CHCK_AS_EMPTY_DIR = 2;
    public static final int CHCK_AS_EXISTING_FILE = 3;
    private static ToolLogger log = ToolLogger.getDefaultLogger();
    private String dir;
    private int fileType;
    private List advSkel;
    private int checkType;

    public PkgConfigParameterType(String dir, int fileType, List advSkel, int checkType) {
        this.dir = dir;
        this.fileType = fileType;
        this.advSkel = advSkel;
        this.checkType = checkType;
    }

    public int getFileType() {
        return this.fileType;
    }

    public List getAdvSkel() {
        return this.advSkel;
    }

    public boolean check(String path) {
        File p = new File(path);
        switch (this.checkType) {
            case -1:
                if (!p.exists()) {
                    if (!p.mkdirs()) {
                        log.outMessage("Unable to create package source directory. ");
                        return false;
                    }
                    return true;
                } else if (!p.isDirectory() || !p.canRead()) {
                    log.outMessage(p.getAbsolutePath() + " exists but it is not a directory or can not be read. ");
                    return false;
                } else {
                    return true;
                }
            case 0:
            default:
                return false;
            case 1:
                if (!p.exists()) {
                    log.outMessage("The " + p.getAbsolutePath() + " does not exist\n");
                    return false;
                } else if (!p.isDirectory()) {
                    log.outMessage("The " + p.getAbsolutePath() + " isn't a directory\n");
                    return false;
                } else if (p.list().length <= 0 || !p.canRead()) {
                    log.outMessage("The " + p.getAbsolutePath() + " is empty or can not be read\n");
                    return false;
                } else {
                    return true;
                }
            case 2:
                if (!p.exists()) {
                    log.outMessage("The " + p.getAbsolutePath() + " does not exist\n");
                    return false;
                } else if (!p.isDirectory()) {
                    log.outMessage("The " + p.getAbsolutePath() + " isn't a directory\n");
                    return false;
                } else if (!p.canRead() || p.list().length != 0) {
                    log.outMessage("The " + p.getAbsolutePath() + " is not empty or can not be read\n");
                    return false;
                } else {
                    return true;
                }
            case 3:
                if (!p.exists()) {
                    log.outMessage("The " + p.getAbsolutePath() + " does not exist\n");
                    return false;
                } else if (!p.isFile()) {
                    log.outMessage("The " + p.getAbsolutePath() + " isn't a file\n");
                    return false;
                } else if (!p.canRead()) {
                    log.outMessage("The " + p.getAbsolutePath() + " can not be read\n");
                    return false;
                } else {
                    return true;
                }
        }
    }

    public int getCheckType() {
        return this.checkType;
    }

    public String getDir() {
        return this.dir;
    }
}
