package psoft.hsp.tools;

import java.sql.SQLException;
import java.util.Arrays;
import psoft.hsp.Package;
import psoft.hsp.PackageNotFoundException;
import psoft.hsphere.tools.ExternalCP;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgUnInstaller.class */
public class PkgUnInstaller {
    boolean force;
    ToolLogger log;
    String pkgName;
    Package pkg = null;

    public PkgUnInstaller(String name, boolean force) throws Exception {
        ExternalCP.initCP();
        this.pkgName = name;
        this.force = force;
        this.log = ToolLogger.getDefaultLogger();
    }

    public void deletePkg() {
        try {
            this.log.outMessage("Loading package " + this.pkgName);
            this.pkg = Package.getPackage(this.pkgName);
            this.log.outOK();
            try {
                this.log.outMessage("Deleting package");
                this.pkg.delete(this.force);
                this.log.outMessage("\n\nThe package has been successfuly uninstalled from your H-Sphere.\nDon't forget to restart your control panel.\n\n");
            } catch (Exception ex) {
                this.log.outFail(ex.getMessage(), ex);
            }
        } catch (SQLException sqlex) {
            this.log.outFail(sqlex.getMessage(), sqlex);
        } catch (PackageNotFoundException ex2) {
            this.log.outFail(ex2.getMessage(), ex2);
        }
    }

    public static void main(String[] argv) throws Exception {
        ToolLogger logger;
        boolean force = false;
        String pkgName = null;
        if (argv.length == 0 || Arrays.asList(argv).contains("--help")) {
            printHelp();
            System.exit(0);
        }
        try {
            logger = new ToolLogger(argv);
        } catch (Exception e) {
            System.out.println("Unable to initialize logger. Using defaults");
            logger = ToolLogger.getDefaultLogger();
            if (logger == null) {
                System.out.println("Unable to initialize logger. Exitting...");
                System.exit(-1);
            }
        }
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].startsWith("--pkg-name=")) {
                pkgName = argv[i].substring(argv[i].indexOf("=") + 1, argv[i].length());
                if (pkgName == null || pkgName.length() == 0) {
                    logger.outMessage("You've passed an empty package file name");
                    break;
                }
            } else if ("--force".equals(argv[i])) {
                force = true;
            }
        }
        PkgUnInstaller unInstaller = new PkgUnInstaller(pkgName, force);
        unInstaller.deletePkg();
        System.exit(0);
    }

    private static void printHelp() {
        System.out.println("\nTool to uninstall unnecessary packages from the system.\n");
        System.out.println("SYNOPSIS:");
        System.out.println("java psoft.hsp.tools.PkgUnInstaller --pkg-name=<name> [--force]\n");
        System.out.println("where:");
        System.out.println("\t--pkg-name=<name> is a package name taken from the control panel.\n");
        System.out.println("\t--force is used to run the package uninstallation even in cases when some conflicts detected.");
        System.out.println("\tUse this option only if you are sure this won't damage any other H-Sphere packages installed.\n");
    }
}
