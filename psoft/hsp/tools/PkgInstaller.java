package psoft.hsp.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import psoft.hsp.PHServerUpdater;
import psoft.hsp.Package;
import psoft.hsp.PackageAlreadyExistsException;
import psoft.hsp.PackageFile;
import psoft.hsp.PackageFileInfo;
import psoft.hsp.PackageNotFoundException;
import psoft.hsphere.LangBundlesCompiler;
import psoft.hsphere.Session;
import psoft.hsphere.tools.BatchSQL;
import psoft.hsphere.tools.ExternalCP;
import psoft.hsphere.tools.ToolLogger;
import psoft.hsphere.util.PackageConfigurator;
import psoft.util.IOUtils;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgInstaller.class */
public class PkgInstaller {
    public static String pkgStorage;
    public static ToolLogger logger;
    File pkgFile;
    boolean checkOnly;
    boolean force;
    PkgIntegrityChecker verifier;
    PkgInfo pkgInfo;
    static String HSPHERE_PROPERTIES_FILE = "psoft_config/hsphere.properties";
    static String CUSTOM_TEMPLATE_VAR = "USER_TEMPLATE_PATH";

    public PkgInstaller(File pkg, boolean checkOnly, boolean force) throws Exception {
        this.pkgFile = null;
        this.checkOnly = false;
        this.force = false;
        ExternalCP.initCP();
        pkgStorage = PackageConfigurator.getDefaultValue("PACKAGE_STORE") + "/tmp/";
        this.pkgFile = pkg;
        this.checkOnly = checkOnly;
        this.force = force;
        logger = ToolLogger.getDefaultLogger();
    }

    private int comparePackageVersions(Package pkg1, PkgConfig pkg2) {
        return (!pkg1.getVersion().equals(pkg2.getPkgVersion()) || pkg1.getBuild().equals(pkg2.getPkgBuild())) ? 1 : 1;
    }

    private Package getPackage() throws SQLException, PackageNotFoundException {
        return Package.getPackage(this.verifier.getPkgConfig().getPkgName());
    }

    public void upgrade(boolean force) throws PkgInstallerException {
        boolean containsTemplates = false;
        boolean compileTemplates = false;
        logger.outMessage((this.checkOnly ? "Checking " : "Updating ") + this.pkgFile.getName() + " package\n");
        File src = unpackPkg();
        if (src == null) {
            throw new PkgInstallerException("Unable to unpack source");
        }
        this.verifier = new PkgIntegrityChecker(src);
        this.verifier.preInstallCheck();
        this.pkgInfo = new PkgInfo(this.verifier.getPkgConfig(), this.verifier.getConfSerialiser().getXMLConfig());
        try {
            this.pkgInfo.loadAvailableFiles();
            try {
                Package pkg = getPackage();
                if (comparePackageVersions(pkg, this.verifier.getPkgConfig()) == 0) {
                    throw new PkgInstallerException("Cannot upgrade to the same version, run installser");
                }
                try {
                    pkg.startUpgrade();
                    logger.outMessage("Upgrading files\n");
                    if (this.pkgInfo.getPreUpgradeScript() != null) {
                        try {
                            logger.outMessage("Executing pre-upgrade script...");
                            pkg.addFile(this.pkgInfo.getPreUpgradeScript(), force).exec();
                            logger.outOK();
                        } catch (Exception ex) {
                            if (!force) {
                                logger.outFail("Error running pre-upgrade script", ex);
                            } else {
                                throw new PkgInstallerException("Error running pre-upgrade script", ex);
                            }
                        }
                    }
                    logger.setPrfx("\t");
                    Iterator i = this.pkgInfo.pkgFilesIterator();
                    while (i.hasNext()) {
                        PackageFileInfo pfi = (PackageFileInfo) i.next();
                        try {
                            if (this.pkgInfo.getPreUpgradeScript() == null || !pfi.getName().equals(this.pkgInfo.getPreUpgradeScript().getName())) {
                                logger.outMessage(pfi.getName());
                                if (pfi.getType() == 1 && !compileTemplates) {
                                    containsTemplates = true;
                                    if (pfi.needsToBeCompiled()) {
                                        compileTemplates = true;
                                    }
                                }
                                pkg.addFiles(pfi.getCompiledFiles(), true);
                                pkg.addFile(pfi, true);
                                logger.outOK();
                            }
                        } catch (Exception ex2) {
                            if (!force) {
                                continue;
                            } else if (force) {
                                logger.outFail("Error installing files", ex2);
                            } else {
                                throw new PkgInstallerException("Error installing files", ex2);
                            }
                        }
                    }
                    if (this.pkgInfo.getUpgradeSQL() != null) {
                        try {
                            logger.outMessage("Executing package SQL script ... \n");
                            BatchSQL.execFile(this.pkgInfo.getUpgradeSQL().getPackageFile().getFullPath());
                            logger.outMessage("\n\n SQL script successfuly executed\n\n");
                        } catch (Exception ex3) {
                            if (force) {
                                logger.outFail("Error executing upgrade sql script", ex3);
                            } else {
                                throw new PkgInstallerException("Error executing upgrade sql script", ex3);
                            }
                        }
                    }
                    if (this.pkgInfo.getPostUpgradeScript() != null) {
                        try {
                            logger.outMessage("Executing post-upgrade script...");
                            PackageFile.getFile(this.pkgInfo.getPostUpgradeScript().getName()).exec();
                            logger.outOK();
                        } catch (Exception ex4) {
                            if (!force) {
                                logger.outFail("Error running post-upgrade script", ex4);
                            } else {
                                throw new PkgInstallerException("Error running post-upgrade script", ex4);
                            }
                        }
                    }
                    try {
                        logger.outMessage("Deleted old files...");
                        if (pkg.finishUpgrade()) {
                            containsTemplates = true;
                            compileTemplates = true;
                        }
                        logger.outOK();
                    } catch (Exception e) {
                        logger.outFail("Error removing old files", e);
                    }
                    logger.setPrfx("");
                    try {
                        logger.outMessage("\n\nRecompiling lang bundles ... \n\n");
                        LangBundlesCompiler.compile();
                        logger.outMessage("\nLanguage bundles got successfuly recompiled.\n\n");
                        if (containsTemplates) {
                            if (compileTemplates) {
                                try {
                                    logger.outMessage("\nRecompiling templates.\nDepending on the system configuration this can take up to 10 mins.\nPlease wait...\n");
                                    PHServerUpdater.recompileCPTemplates();
                                    logger.outOK();
                                } catch (Exception ex5) {
                                    throw new PkgInstallerException("Error compiling templates", ex5);
                                }
                            } else {
                                try {
                                    logger.outMessage("\nConfiguring templates... ");
                                    PHServerUpdater.configureCPTemplates();
                                    logger.outOK();
                                } catch (Exception ex6) {
                                    throw new PkgInstallerException("Error configuring templates", ex6);
                                }
                            }
                        }
                        if (!"/".equals(src.getAbsolutePath())) {
                            logger.outMessage("\n\nRemoving unnecessary temp directories... ");
                            try {
                                IOUtils.rmdir(src);
                                logger.outOK();
                            } catch (IOException ex7) {
                                logger.outMessage("\n Error: ", ex7);
                            }
                        }
                        try {
                            pkg.updatePackage(this.verifier);
                        } catch (SQLException e2) {
                            logger.outMessage("Error updating package signature");
                        }
                        logger.outMessage("\n\nThe package has been successfully installed on your H-Sphere.\nDon't forget to restart your control panel.\n\n");
                    } catch (Exception e3) {
                        throw new PkgInstallerException("Error recompiling lang bundles");
                    }
                } catch (SQLException e4) {
                    throw new PkgInstallerException("Unable to init files", e4);
                }
            } catch (Exception e5) {
                throw new PkgInstallerException("Unable to load package", e5);
            }
        } catch (Exception ex8) {
            throw new PkgInstallerException("Error loading package files", ex8);
        }
    }

    public boolean install(boolean force) {
        boolean containsTemplates = false;
        boolean compileTemplates = false;
        logger.outMessage((this.checkOnly ? "Checking " : "Installing ") + this.pkgFile.getName() + " package\n");
        File src = unpackPkg();
        if (src == null) {
            return false;
        }
        this.verifier = new PkgIntegrityChecker(src);
        boolean result = this.verifier.preInstallCheck();
        this.pkgInfo = new PkgInfo(this.verifier.getPkgConfig(), this.verifier.getConfSerialiser().getXMLConfig());
        try {
            this.pkgInfo.loadAvailableFiles();
            if ((!isInstallationPossible() && !force) || this.checkOnly) {
                return false;
            }
            Package pkg = null;
            try {
                pkg = Package.createPackage(this.verifier.getPkgConfig().getPkgName(), this.verifier.getPkgConfig().getPkgDescription(), this.verifier.getPkgConfig().getPkgInfo(), this.verifier.getPkgConfig().getPkgVendor(), this.verifier.getPkgConfig().getPkgVersion(), this.verifier.getPkgConfig().getPkgBuild());
            } catch (SQLException sqlex) {
                logger.outMessage(sqlex.getMessage() + "\n", sqlex);
                return false;
            } catch (PackageAlreadyExistsException ex) {
                if (!force) {
                    logger.outMessage(ex.getMessage() + "\n", ex);
                    return false;
                }
                try {
                    pkg = getPackage();
                } catch (SQLException sqlex2) {
                    logger.outMessage(ex.getMessage() + "\n", sqlex2);
                } catch (PackageNotFoundException ex1) {
                    logger.outMessage(ex.getMessage() + "\n", ex1);
                }
            }
            try {
                savePackageSource(pkg);
            } catch (IOException e) {
                logger.outMessage("Unable to save the package source file:" + this.pkgFile.getAbsolutePath() + " to " + pkg.getPackageDir() + '/' + this.pkgFile.getName());
            }
            logger.outMessage("Installing files \n");
            if (this.pkgInfo.getPreInstallScript() != null) {
                try {
                    logger.outMessage("Executing pre-install script...");
                    PackageFile f = pkg.addFile(this.pkgInfo.getPreInstallScript(), force);
                    f.exec();
                    logger.outOK();
                } catch (Exception ex2) {
                    logger.outFail(ex2.getMessage(), ex2);
                    if (!force) {
                        return false;
                    }
                }
            }
            logger.setPrfx("\t");
            Iterator i = this.pkgInfo.pkgFilesIterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                PackageFileInfo pfi = (PackageFileInfo) i.next();
                try {
                    if (this.pkgInfo.getPreInstallScript() == null || !pfi.getName().equals(this.pkgInfo.getPreInstallScript().getName())) {
                        logger.outMessage(pfi.getName());
                        if (pfi.getType() == 1 && !compileTemplates) {
                            containsTemplates = true;
                            if (pfi.needsToBeCompiled()) {
                                compileTemplates = true;
                            }
                        }
                        pkg.addFiles(pfi.getCompiledFiles(), force);
                        pkg.addFile(pfi, force);
                        logger.outOK();
                    }
                } catch (Exception ex3) {
                    logger.outFail(ex3.getMessage(), ex3);
                    if (!force) {
                        result = false;
                        break;
                    }
                }
            }
            if (this.pkgInfo.getPkgSQL() != null) {
                try {
                    logger.outMessage("Executing package SQL script ... \n");
                    BatchSQL.execFile(this.pkgInfo.getPkgSQL().getPackageFile().getFullPath());
                    logger.outMessage("\n\n SQL script successfuly executed\n\n");
                } catch (Exception ex4) {
                    logger.outFail(ex4.getMessage(), ex4);
                    if (!force) {
                        return false;
                    }
                }
            }
            if (this.pkgInfo.getPostInstallScript() != null) {
                try {
                    logger.outMessage("Executing post-install script...");
                    PackageFile f2 = PackageFile.getFile(this.pkgInfo.getPostInstallScript().getName());
                    f2.exec();
                    logger.outOK();
                } catch (Exception ex5) {
                    logger.outFail(ex5.getMessage(), ex5);
                    if (!force) {
                        return false;
                    }
                }
            }
            logger.setPrfx("");
            try {
                logger.outMessage("\n\nRecompiling lang bundles ... \n\n");
                LangBundlesCompiler.compile();
                logger.outMessage("\nLanguage bundles got successfuly recompiled.\n\n");
            } catch (Exception ex6) {
                logger.outFail();
                logger.outMessage(ex6.getMessage() + "\n", ex6);
                result = false;
            }
            if (containsTemplates) {
                if (compileTemplates) {
                    try {
                        logger.outMessage("\nRecompiling templates.\nDepending on the system configuration this can take up to 10 mins.\nPlease wait...\n");
                        PHServerUpdater.recompileCPTemplates();
                        logger.outOK();
                    } catch (Exception ex7) {
                        logger.outFail();
                        logger.outMessage(ex7.getMessage() + "\n", ex7);
                        result = false;
                    }
                } else {
                    try {
                        logger.outMessage("\nConfiguring templates... ");
                        PHServerUpdater.configureCPTemplates();
                        logger.outOK();
                    } catch (Exception ex8) {
                        logger.outFail();
                        logger.outMessage(ex8.getMessage() + "\n", ex8);
                        result = false;
                    }
                }
            }
            if (!"/".equals(src.getAbsolutePath())) {
                logger.outMessage("\n\nRemoving unnecessary temp directories... ");
                try {
                    IOUtils.rmdir(src);
                    logger.outOK();
                } catch (IOException ex9) {
                    logger.outMessage("\n Error: ", ex9);
                }
            }
            if (force) {
                try {
                    pkg.updatePackage(this.verifier);
                    result = false;
                } catch (SQLException e2) {
                    logger.outMessage("Error updating package signature");
                }
            }
            if (result) {
                logger.outMessage("\n\nThe package has been successfully installed on your H-Sphere.\nDon't forget to restart your control panel.\n\n");
            } else {
                logger.outMessage("\n\nThe package was installed with some errors.Please look through the log file for more details.\n\n");
            }
            return result;
        } catch (Exception ex10) {
            logger.outMessage("Error loading package files\n", ex10);
            return false;
        }
    }

    private void savePackageSource(Package pkg) throws IOException {
        String sourceFileName = pkg.getPackageDir().getPath() + '/' + this.pkgFile.getName();
        IOUtils.removeFilesByMask(pkg.getPackageDir().getPath() + '/', ".hsp");
        File destFile = new File(sourceFileName);
        IOUtils.copyFile(this.pkgFile, destFile);
    }

    private boolean isInstallationPossible() {
        boolean result = true;
        logger.outMessage("Checking package conflicts\n");
        logger.setPrfx("\t");
        Iterator i = this.pkgInfo.pkgFilesIterator();
        while (i.hasNext()) {
            PackageFileInfo pfi = (PackageFileInfo) i.next();
            logger.outMessage("Checking " + pfi.getName());
            try {
                PackageFile pf = PackageFile.getConflictFile(pfi.getName(), this.verifier.getPkgConfig().getPkgName());
                if (pf == null) {
                    logger.outOK();
                } else {
                    result = false;
                    logger.outFail("Already installed by the " + pf.getInstalled() + " package");
                }
            } catch (Exception e) {
                logger.outFail("An error occured: " + e.getMessage(), e);
                result = false;
            }
        }
        logger.setPrfx("");
        return result;
    }

    private File unpackPkg() {
        return unpackPkg(this.pkgFile, pkgStorage);
    }

    public static File unpackPkg(File _pkgFile, String _pkgStorage) {
        try {
            File tempDirectory = IOUtils.makeTempDirectory(new File(_pkgStorage));
            try {
                IOUtils.unpackJar(_pkgFile.getAbsoluteFile(), tempDirectory);
                return tempDirectory;
            } catch (IOException ex) {
                logger.outFail("Unable to extract the package archive into the  temp directory [" + tempDirectory + "]. ", ex);
                return null;
            }
        } catch (IOException ex2) {
            logger.outFail("Unable to create the neccesary temp directory at the path [" + _pkgStorage + "]. ", ex2);
            return null;
        }
    }

    protected static synchronized String updateHSProperties() throws Exception {
        String customTemplatePath = Session.getPropertyString(CUSTOM_TEMPLATE_VAR);
        if (customTemplatePath != null && !"".equals(customTemplatePath)) {
            return null;
        }
        String hsHomeDir = PackageConfigurator.getDefaultValue("HSPHERE_HOME");
        if (hsHomeDir == null) {
            throw new Exception("\nUnable to find 'HSPHERE_HOME' directory.\n");
        }
        String customTemplateDir = PackageConfigurator.getDefaultValue(CUSTOM_TEMPLATE_VAR);
        if (customTemplateDir == null) {
            throw new Exception("\nUnable to find '" + CUSTOM_TEMPLATE_VAR + "' directory.\n");
        }
        String hspFileName = hsHomeDir + "/" + HSPHERE_PROPERTIES_FILE;
        String tmpFileName = hspFileName + ".TMP";
        String newProperty = CUSTOM_TEMPLATE_VAR + " = " + customTemplateDir + "/";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String fext = "." + df.format(new Date());
        IOUtils.copyFile(hspFileName, hspFileName + fext, false);
        File hspFile = new File(hspFileName);
        File tmpFile = new File(tmpFileName);
        if (!hspFile.canRead() || !hspFile.canWrite()) {
            throw new Exception("\nError accessing the '" + hspFileName + "' file. Please check that both the 'read' and 'write' permissions are set for this file.\n");
        }
        BufferedReader hspFileReader = new BufferedReader(new FileReader(hspFile));
        PrintWriter tmpFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(tmpFile)));
        while (true) {
            try {
                String buf = hspFileReader.readLine();
                if (buf != null) {
                    String buf2 = buf.trim();
                    if (buf2.startsWith(CUSTOM_TEMPLATE_VAR)) {
                        buf2 = "#" + buf2;
                    }
                    tmpFileWriter.println(buf2);
                } else {
                    tmpFileWriter.println(newProperty);
                    tmpFileWriter.flush();
                    hspFile.delete();
                    tmpFile.renameTo(hspFile);
                    return hspFileName;
                }
            } catch (Throwable th) {
                tmpFileWriter.flush();
                throw th;
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        Throwable t;
        boolean checkOnly = false;
        boolean force = false;
        boolean upgrade = false;
        File pkg = null;
        try {
            logger = new ToolLogger(argv);
        } catch (Exception e) {
            System.out.println("Unable to initialize logger. Will use default");
            logger = ToolLogger.getDefaultLogger();
        }
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].startsWith("--package=")) {
                String fname = argv[i].substring(argv[i].indexOf("=") + 1, argv[i].length());
                if (fname == null || fname.length() == 0) {
                    logger.outMessage("You've passed an empty package file name\n");
                    break;
                }
                pkg = new File(fname);
            } else if ("--check-only".equals(argv[i])) {
                checkOnly = true;
            } else if ("--force".equals(argv[i])) {
                force = true;
            } else if ("--help".equals(argv[i])) {
                printHelp();
            } else if ("--upgrade".equals(argv[i])) {
                upgrade = true;
            }
        }
        if (pkg == null) {
            printHelp();
        }
        if (pkg.isFile() && pkg.canRead()) {
            PkgInstaller installer = null;
            try {
                installer = new PkgInstaller(pkg, checkOnly, force);
            } catch (Exception ex) {
                logger.outMessage("Unable to initialize PkgInstaller: " + ex.getMessage() + "\n", ex);
                System.exit(-1);
            }
            String updatedPropFile = null;
            try {
                updatedPropFile = updateHSProperties();
            } catch (Exception ex2) {
                logger.outFail(ex2.getMessage());
                System.exit(1);
            }
            if (updatedPropFile != null) {
                logger.outMessage("The '" + updatedPropFile + "' file has been updated with the '" + CUSTOM_TEMPLATE_VAR + "' property. Please check it carefully before H-Sphere restart.\n\n");
            }
            if (!upgrade) {
                installer.install(force);
                return;
            }
            try {
                installer.upgrade(force);
                return;
            } catch (PkgInstallerException e2) {
                if (e2.getCause() == null) {
                    t = e2;
                } else {
                    t = e2.getCause();
                }
                logger.outFail();
                logger.outMessage(e2.getMessage(), t);
                return;
            }
        }
        logger.outMessage("The " + pkg.getAbsolutePath() + " either not a package file or can not be read. Please check\n");
    }

    public static void printHelp() {
        logger.outMessage("\nTool to install an H-Sphere package\n\n");
        logger.outMessage("SYNOPSIS:\n\n");
        logger.outMessage("java psoft.hsp.tools.PkgInstaller --package=/path/to/package/file [--upgrade] [--check-only] [--force]\n\n");
        logger.outMessage("WHERE:\n");
        logger.outMessage("\n--package=/path/to/package/file\n");
        logger.smartPrintLn("Path to the H-Sphere package file you are going to install", "\t", 75);
        logger.outMessage("\n--upgrade\n");
        logger.smartPrintLn("Upgrades existing package", "\t", 75);
        logger.outMessage("\n--check-only\n");
        logger.smartPrintLn("Makes utility not install the package but only perform a check routine if the package can be installed", "\t", 75);
        logger.outMessage("\n--force\n");
        logger.smartPrintLn("Forces the package installation even if conflicts were detected. Use this option only in case you are sure that this will not damage other already installed H-Sphere packages", "\t", 75);
        System.exit(0);
    }
}
