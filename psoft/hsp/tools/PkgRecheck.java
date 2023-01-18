package psoft.hsp.tools;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import psoft.hsp.Package;
import psoft.hsp.PackageNotFoundException;
import psoft.hsphere.tools.ExternalCP;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgRecheck.class */
public class PkgRecheck {
    boolean force;
    ToolLogger log;
    String pkgName;
    Package pkg = null;

    public PkgRecheck(String name, boolean force) throws Exception {
        ExternalCP.initCP();
        this.pkgName = name;
        this.force = force;
        this.log = ToolLogger.getDefaultLogger();
    }

    public void recheckPkgs(String pkgName) throws Exception {
        try {
            this.log.outMessage("Loading package " + pkgName);
            this.pkg = Package.getPackage(pkgName);
            this.log.outOK();
            boolean needsToReinstall = false;
            try {
                if (this.pkg.needsToReinstall()) {
                    needsToReinstall = true;
                    this.log.outFail();
                } else {
                    this.log.outOK();
                }
            } catch (Exception ex) {
                this.log.outFail("Unable to check package " + this.pkg.getName() + " integrity ", ex);
            }
            if (needsToReinstall && this.force) {
                this.log.outMessage("Reupdating package " + this.pkg.getName() + " ... ");
                try {
                    this.pkg.reinstallRemoteFiles();
                    this.log.outOK();
                } catch (Exception ex2) {
                    this.log.outFail("Unable to check package " + this.pkg.getName(), ex2);
                }
            }
        } catch (SQLException sqlex) {
            this.log.outFail(sqlex.getMessage(), sqlex);
        } catch (PackageNotFoundException ex3) {
            this.log.outFail(ex3.getMessage(), ex3);
        }
    }

    public void recheckPkgs() throws Exception {
        try {
            LinkedList incompleatedPackages = new LinkedList();
            for (Package pkg : Package.getPackages()) {
                this.log.outMessage("Package integrity checking " + pkg.getName() + " ... ");
                try {
                    if (pkg.needsToReinstall()) {
                        incompleatedPackages.add(pkg);
                        this.log.outFail();
                    } else {
                        this.log.outOK();
                    }
                } catch (Exception ex) {
                    this.log.outFail("Unable to check package " + pkg.getName() + " integrity ", ex);
                }
            }
            if (this.force) {
                if (incompleatedPackages.size() >= 0) {
                    Iterator i = incompleatedPackages.iterator();
                    while (i.hasNext()) {
                        Package pkg2 = (Package) i.next();
                        this.log.outMessage("Reupdating package " + pkg2.getName() + " ... ");
                        try {
                            pkg2.reinstallRemoteFiles();
                            this.log.outOK();
                        } catch (Exception ex2) {
                            this.log.outFail("Unable to check package " + pkg2.getName(), ex2);
                        }
                    }
                } else {
                    this.log.outMessage("The all packages ar compleated");
                    this.log.outOK();
                }
            }
        } catch (SQLException sqlex) {
            this.log.outFail(sqlex.getMessage(), sqlex);
        } catch (PackageNotFoundException ex3) {
            this.log.outFail(ex3.getMessage(), ex3);
        }
    }

    public static void main(String[] argv) throws Exception {
        ToolLogger logger;
        boolean force = false;
        String pkgName = null;
        if (Arrays.asList(argv).contains("--help")) {
            printHelp();
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
        PkgRecheck pkgRecheck = new PkgRecheck(pkgName, force);
        if (pkgName != null) {
            pkgRecheck.recheckPkgs(pkgName);
        } else {
            pkgRecheck.recheckPkgs();
        }
        System.exit(0);
    }

    private static void printHelp() {
        System.out.println("\nTool to check package integrity and reinstall missing package's files on remote physical boxes.");
        System.out.println("It checks if package files are properly installed on physical boxes,");
        System.out.println("and reinstalls the missing files (run with the --force option).");
        System.out.println("SYNOPSIS:");
        System.out.println("java psoft.hsp.tools.PkgRecheck [--pkg-name=<name>] [--force]");
        System.out.println("\twhere:");
        System.out.println("\t --pkg-name=<name> - check the specified package; otherwise, check all installed packages\n");
        System.out.println("\t --force - reinstall missing package files on remote physical boxes.");
    }
}
