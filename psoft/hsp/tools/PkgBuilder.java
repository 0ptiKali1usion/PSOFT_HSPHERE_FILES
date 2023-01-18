package psoft.hsp.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import psoft.hsphere.tools.ToolLogger;
import psoft.util.IOUtils;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgBuilder.class */
public class PkgBuilder {
    public static final String PKG_PREFIX = "./";
    public static final String PKG_CONFIG_FILENAME = "/_pkg.xml";
    public static final String PKG_SOURCE_DIRNAME = "src/";
    PkgIntegrityChecker checker;
    private File srcLocation;
    private String pkgLocation;
    private static ToolLogger logger;

    public PkgBuilder(String srcLocation) {
        this.srcLocation = new File(srcLocation + PKG_SOURCE_DIRNAME);
        this.pkgLocation = srcLocation;
        this.checker = new PkgIntegrityChecker(srcLocation, PKG_SOURCE_DIRNAME);
    }

    public boolean checkSource() {
        return this.checker.preBuildCheck();
    }

    public boolean assemble() throws Exception {
        PkgConfig pkgconfig = this.checker.getPkgConfig();
        logger.outMessage("Assembling package " + pkgconfig.getPkgName() + "-" + pkgconfig.getPkgVersion() + "-" + pkgconfig.getPkgBuild() + "\n");
        try {
            _assemble(pkgconfig.getPkgName(), this.srcLocation, this.pkgLocation, pkgconfig.getPkgVersion(), pkgconfig.getPkgBuild());
            return true;
        } catch (Exception exception) {
            logger.outFail(exception.getMessage());
            return false;
        }
    }

    private static void _assemble(String name, File dir, String pkgLocation, String version, String build) throws IOException {
        File outputDir = new File(dir, "pkg_classes");
        String jarName = name + ".jar";
        File packageFile = new File(pkgLocation, name + "-" + version + "-" + build + ".hsp");
        if (outputDir.exists()) {
            IOUtils.jarDirectory(outputDir, new File(outputDir, jarName), null);
        }
        IOUtils.jarDirectory(dir, packageFile, new AssemberFilenameFilter(jarName));
        logger.outMessage("Package is assembled: " + packageFile.getName() + "\n");
    }

    /* loaded from: hsphere.zip:psoft/hsp/tools/PkgBuilder$AssemberFilenameFilter.class */
    public static class AssemberFilenameFilter implements FilenameFilter {
        String jarName;

        @Override // java.io.FilenameFilter
        public boolean accept(File file, String fileName) {
            return !file.getName().equals("pkg_classes") || fileName.equals(this.jarName);
        }

        public AssemberFilenameFilter(String jarName) {
            this.jarName = jarName;
        }
    }

    public static void printHelp() {
        logger.outMessage("\nTool to assemble preconfigured H-Sphere package.\n");
        logger.smartPrintLn("At the moment of the tool usage all resources should be copied to their respective locations and package configuration file should contain all data needed for correct package assemble", "", 75);
        logger.outMessage("\nSYNOPSIS:\n");
        logger.outMessage("java psoft.hsp.tools.PkgBuilder --with-source=/path/to/package/source");
        logger.outMessage("\nWHERE:\n");
        logger.outMessage("--with-source=/path/to/package/source");
        logger.smartPrintLn("The path supplied to the parameter must point to the directory with preconfigured package source. This is the same directory which was supplied as --with-prefix parameter when configuring package source.", "\t", 75);
        System.exit(0);
    }

    public static void main(String[] argv) throws Exception {
        String prefix = null;
        logger = new ToolLogger(argv);
        if (argv.length == 0) {
            printHelp();
        }
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("--help")) {
                printHelp();
            } else if (argv[i].startsWith("--with-source") && argv[i].indexOf("=") > 0) {
                prefix = argv[i].substring(argv[i].indexOf("=") + 1, argv[i].length());
                if (prefix == null || prefix.length() == 0) {
                    prefix = PKG_PREFIX;
                }
                if (prefix != null && !prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }
                logger.outMessage("Package source is set to " + prefix + "\n");
            }
        }
        if (prefix == null) {
            prefix = PKG_PREFIX;
            logger.outMessage("Accepting current directory as the default package source location.\n");
        }
        PkgBuilder builder = new PkgBuilder(prefix);
        logger.outMessage("Checking package source....\n");
        if (builder.checkSource()) {
            logger.outOK();
        } else {
            logger.outFail("A misconfiguration detected. Aborting package build...\n");
            System.exit(-1);
        }
        if (builder.assemble()) {
            logger.outMessage("Package is assembled and ready to install\n");
            System.exit(0);
        } else {
            logger.outFail("An error occured when assembling package. Package was not assembled");
            System.exit(-1);
        }
        System.exit(0);
    }

    public static boolean build(String[] argv, String srcLocation) throws Exception {
        logger = new ToolLogger(argv);
        PkgBuilder pkgbuilder = new PkgBuilder(srcLocation);
        if (pkgbuilder.checkSource()) {
            logger.outOK();
            if (pkgbuilder.assemble()) {
                logger.outOK();
                return true;
            }
            logger.outFail("Error assmebling the package\n");
            return false;
        }
        logger.outFail("Configuration error. Aborting package build...\n");
        return false;
    }
}
