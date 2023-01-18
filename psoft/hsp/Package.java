package psoft.hsp;

import freemarker.template.TemplateModelException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Category;
import psoft.hsp.files.RemoteFile;
import psoft.hsp.tools.PkgInfo;
import psoft.hsp.tools.PkgInstaller;
import psoft.hsp.tools.PkgInstallerException;
import psoft.hsp.tools.PkgIntegrityChecker;
import psoft.hsphere.Session;
import psoft.hsphere.tools.BatchSQL;
import psoft.hsphere.tools.ToolLogger;
import psoft.hsphere.util.PackageConfigurator;

/* loaded from: hsphere.zip:psoft/hsp/Package.class */
public class Package {

    /* renamed from: id */
    protected int f16id;
    protected String name;
    protected String description;
    protected String descrShort;
    protected String info;
    protected String vendorInfo;
    protected String version;
    protected String build;
    protected String other;
    protected String confFile;
    private static Category log = Category.getInstance(Package.class.getName());
    protected static Map packages = new HashMap();
    protected static boolean packagesLoaded = false;
    protected boolean filesLoaded = false;
    protected HashMap pkgFiles = new HashMap();
    Map oldFiles = null;

    private Set _getFileSet(int fileType) {
        Integer _fileType = new Integer(fileType);
        Set s = (Set) this.pkgFiles.get(_fileType);
        if (s == null) {
            HashMap hashMap = this.pkgFiles;
            HashSet hashSet = new HashSet();
            s = hashSet;
            hashMap.put(_fileType, hashSet);
        }
        return s;
    }

    private Set getFileSet(int fileType) {
        try {
            initFiles();
        } catch (SQLException ex) {
            ToolLogger.getDefaultLogger().outMessage(ex.getMessage(), ex);
        }
        return _getFileSet(fileType);
    }

    public Package(int id, String name, String description, String info, String manufacturer, String version, String other, String confFile) {
        this.f16id = id;
        this.name = name;
        this.description = description;
        this.info = info;
        this.vendorInfo = manufacturer;
        this.version = version;
        this.other = other;
        this.build = other;
        this.confFile = confFile;
    }

    public int getId() {
        return this.f16id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getInfo() {
        return this.info;
    }

    public String getVendorInfo() {
        return this.vendorInfo;
    }

    public String getVersion() {
        return this.version;
    }

    public String getOther() {
        return this.other;
    }

    public String getConfFile() {
        return this.confFile;
    }

    public String getConfDirectory() {
        File dir = new File(PackageConfigurator.getDefaultValue("PACKAGE_STORE") + "/" + getName() + "/");
        return dir.getAbsolutePath() + "/";
    }

    public void setConfFile(String confFile) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE packages SET conf_file = ? WHERE id = ?");
            ps.setString(1, confFile);
            ps.setInt(2, getId());
            ps.executeUpdate();
            this.confFile = confFile;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getPackages() throws Exception {
        Session.getLog().debug("Inside Package.getPackages()");
        loadPackagesIfNeeded();
        return packages.size() > 0 ? new LinkedList(packages.values()) : new LinkedList();
    }

    protected static synchronized void loadPackagesIfNeeded() throws SQLException {
        if (packagesLoaded) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, name, description, info, manufacturer, version, other, conf_file FROM packages");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString(2);
                Package pdb = new Package(rs.getInt(1), name, rs.getString(3), Session.getClobValue(rs, 4), rs.getString(5), rs.getString(6), Session.getClobValue(rs, 7), rs.getString(8));
                packages.put(name, pdb);
            }
            packagesLoaded = true;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected synchronized void initFiles() throws SQLException {
        if (this.filesLoaded) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT package_files.name FROM package_files, package_files_r  WHERE package_files.id = package_files_r.file_id AND package_id = ?");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            cleanFiles();
            while (rs.next()) {
                try {
                    PackageFile pf = PackageFile.getFile(rs.getString(1));
                    _getFileSet(pf.getType()).add(pf.getName());
                } catch (IOException e) {
                    ToolLogger.getDefaultLogger().outMessage("Unable to load file: " + rs.getString(1), e);
                } catch (ClassNotFoundException e2) {
                    ToolLogger.getDefaultLogger().outMessage("Unable to load file: " + rs.getString(1), e2);
                } catch (NullPointerException e3) {
                    ToolLogger.getDefaultLogger().outMessage("Unable to load file: " + rs.getString(1), e3);
                }
            }
            this.filesLoaded = true;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void cleanFiles() {
        this.pkgFiles.clear();
    }

    public static synchronized Package getPackage(String name) throws SQLException, PackageNotFoundException {
        loadPackagesIfNeeded();
        Package pdb = (Package) packages.get(name);
        if (pdb == null) {
            throw new PackageNotFoundException(name);
        }
        return pdb;
    }

    public void updatePackage(PkgIntegrityChecker verifier) throws SQLException {
        updatePackage(verifier.getPkgConfig().getPkgDescription(), verifier.getPkgConfig().getPkgInfo(), verifier.getPkgConfig().getPkgVendor(), verifier.getPkgConfig().getPkgVersion(), verifier.getPkgConfig().getPkgBuild());
    }

    public synchronized void updatePackage(String description, String info, String manufacturer, String version, String other) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE packages SET description = ?, info = ?, manufacturer = ?, version = ?, other = ? WHERE id = ?");
            ps.setString(1, description);
            ps.setString(2, info);
            ps.setString(3, manufacturer);
            ps.setString(4, version);
            ps.setString(5, other);
            ps.setInt(6, this.f16id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.description = description;
            this.info = info;
            this.vendorInfo = manufacturer;
            this.version = version;
            this.other = other;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized Package createPackage(String name, String description, String info, String manufacturer, String version, String other) throws SQLException, PackageAlreadyExistsException {
        loadPackagesIfNeeded();
        if (getPackage(name) != null) {
            throw new PackageAlreadyExistsException(name);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            int id = Session.getNewId("package_id");
            ps = con.prepareStatement("INSERT INTO packages (id, name, description, info, manufacturer, version, other) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, info);
            ps.setString(5, manufacturer);
            ps.setString(6, version);
            ps.setString(7, other);
            ps.executeUpdate();
            Package pkg = new Package(id, name, description, info, manufacturer, version, other, "");
            packages.put(name, pkg);
            Session.closeStatement(ps);
            con.close();
            return pkg;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void addFiles(Collection col, boolean force) throws Exception {
        if (col == null) {
            return;
        }
        Iterator i = col.iterator();
        while (i.hasNext()) {
            addFile((PackageFileInfo) i.next(), force);
        }
    }

    public PackageFile addFile(PackageFileInfo pkif, boolean force) throws Exception {
        switch (pkif.getType()) {
            case 3:
            case 7:
            case 8:
            case 9:
                pkif.setName(getName() + "/" + pkif.getName());
                break;
        }
        trackUpgrade(pkif.getType(), pkif.getName());
        PackageFile pf = PackageFile.getConflictFile(pkif.getName(), getName());
        if (pf != null && !force) {
            throw new PackageConflictException(pkif.getType(), pf.getInstalled());
        }
        PackageFile pf2 = PackageFile.addFile(getName(), pkif, force);
        getFileSet(pf2.getType()).add(String.valueOf(pf2.getName()));
        if (pkif.getType() == 7) {
            String file = new File(pf2.getFullPath()).getAbsolutePath();
            String home = PackageConfigurator.getDefaultValue("HSPHERE_HOME");
            int disp = home.length();
            if (!home.endsWith("/")) {
                disp++;
            }
            if (file.startsWith(home)) {
                String bundleName = file.substring(disp);
                if (bundleName.endsWith(".properties")) {
                    String bundleName2 = bundleName.substring(0, bundleName.indexOf(".properties")).replace('/', '.');
                    ToolLogger.getDefaultLogger().outMessage("lang bundle:" + bundleName2);
                    setConfFile(bundleName2);
                }
            }
        }
        return pf2;
    }

    public void deleteFile(String fileName) throws Exception {
        PackageFile pkf = PackageFile.getFile(fileName);
        if (pkf != null) {
            pkf.delFile(getName());
            getFileSet(pkf.getType()).remove(pkf.getName());
            if (pkf.getType() == 7) {
                setConfFile("");
            }
        }
    }

    public Collection getFiles(int type) {
        return getFileSet(type);
    }

    public Set getOldFileSet(int type) {
        return (Set) this.oldFiles.get(new Integer(type));
    }

    public void startUpgrade() throws SQLException {
        this.oldFiles = new HashMap();
        initFiles();
        for (Object key : this.pkgFiles.keySet()) {
            HashSet hashSet = new HashSet();
            hashSet.addAll((Set) this.pkgFiles.get(key));
            this.oldFiles.put(key, hashSet);
        }
    }

    protected void trackUpgrade(int type, String name) {
        if (this.oldFiles != null) {
            if (type == 13 || type == 1) {
                _trackUpgrade(13, name);
                _trackUpgrade(1, name);
                return;
            }
            _trackUpgrade(type, name);
        }
    }

    private void _trackUpgrade(int type, String name) {
        Set s = getOldFileSet(type);
        if (s != null) {
            s.remove(name);
        }
    }

    public boolean finishUpgrade() throws Exception {
        deleteOnUpgrade(4);
        deleteOnUpgrade(6);
        boolean recompile = deleteOnUpgrade(1);
        deleteOnUpgrade(14);
        deleteOnUpgrade(15);
        deleteOnUpgrade(13);
        deleteOnUpgrade(2);
        deleteOnUpgrade(12);
        deleteOnUpgrade(3);
        deleteOnUpgrade(7);
        deleteOnUpgrade(9);
        deleteOnUpgrade(10);
        deleteOnUpgrade(11);
        deleteOnUpgrade(8);
        this.oldFiles = null;
        return recompile;
    }

    public List getFiles() throws SQLException {
        initFiles();
        List result = new ArrayList();
        for (Object obj : this.pkgFiles.keySet()) {
            result.addAll((Set) this.pkgFiles.get(obj));
        }
        return result;
    }

    protected void execScript(String fileName) {
        String _fileName = getName() + '/' + fileName;
        if (getFileSet(8).contains(_fileName)) {
            try {
                PackageFile.getFile(_fileName).exec();
            } catch (Exception e) {
                ToolLogger.getDefaultLogger().outMessage("Error running script: " + fileName, e);
            }
        }
    }

    protected void execSQL(String fileName) {
        String _fileName = getName() + '/' + fileName;
        if (getFileSet(8).contains(_fileName)) {
            try {
                BatchSQL.execFile(PackageFile.getFile(_fileName).getFullPath());
            } catch (Exception e) {
                ToolLogger.getDefaultLogger().outMessage("Error running sql script: " + fileName, e);
            }
        }
    }

    protected boolean deleteOnUpgrade(int type) throws Exception {
        return deleteAllFilesByType(getOldFileSet(type), type, true);
    }

    protected void deleteAllFilesByType(int type, boolean force) throws Exception {
        if (deleteAllFilesByType(getFileSet(type), type, force)) {
            ToolLogger.getDefaultLogger().outMessage("\nRecompiling all H-Sphere templates.\nDepending on the system configuration this can take up to 10 mins.\nPlease wait...\n");
            try {
                PHServerUpdater.recompileCPTemplates();
            } catch (Exception e) {
                ToolLogger.getDefaultLogger().outMessage("Unable to recompile H-Sphere templates. ", e);
                ToolLogger.getDefaultLogger().outMessage("Try to compile templates manually !");
            }
        }
    }

    protected boolean deleteAllFilesByType(Set set, int type, boolean force) throws Exception {
        if (set == null || set.size() == 0) {
            return false;
        }
        boolean isTemplateType = type == 1;
        boolean templatesCleanedUp = false;
        String[] array = (String[]) set.toArray(new String[0]);
        for (String fileName : array) {
            try {
                PackageFile file = PackageFile.getFile(fileName);
                if (file != null) {
                    if (!templatesCleanedUp && isTemplateType && !templatesCleanedUp && file.getPkfInfo().needsToBeCompiled()) {
                        templatesCleanedUp = true;
                        PHServerUpdater.cleanCPTemplates();
                    }
                    file.delFile(getName());
                    getFileSet(type).remove(getName());
                }
            } catch (Exception ex) {
                ToolLogger.getDefaultLogger().outMessage("Unable to delete file:" + fileName, ex);
                if (!force) {
                    throw ex;
                }
            }
        }
        return templatesCleanedUp;
    }

    public void delete() throws Exception {
        delete(false);
    }

    public synchronized void delete(boolean force) throws Exception {
        loadPackagesIfNeeded();
        execScript("_pre-uninstall");
        execSQL("_pkg-uninstall.sql");
        deleteAllFilesByType(4, force);
        deleteAllFilesByType(6, force);
        deleteAllFilesByType(1, force);
        deleteAllFilesByType(14, force);
        deleteAllFilesByType(15, force);
        deleteAllFilesByType(13, force);
        deleteAllFilesByType(2, force);
        deleteAllFilesByType(12, force);
        deleteAllFilesByType(3, force);
        deleteAllFilesByType(7, force);
        deleteAllFilesByType(9, force);
        deleteAllFilesByType(10, force);
        deleteAllFilesByType(11, force);
        execScript("_post-uninstall");
        deleteAllFilesByType(8, force);
        File pkgFile = getHspBundle();
        if (pkgFile != null) {
            try {
                if (pkgFile.exists()) {
                    pkgFile.delete();
                }
            } catch (Exception ex) {
                ToolLogger.getDefaultLogger().outMessage("Unable to delete the bundle for package :" + getName(), ex);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM packages WHERE id = ?");
            ps.setInt(1, this.f16id);
            ps.executeUpdate();
            packages.remove(getName());
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static String getFileDestByFileType(int fileType) {
        String path = "";
        switch (fileType) {
            case 1:
            case 13:
                path = PackageConfigurator.getDefaultValue("USER_TEMPLATE_PATH") + "/";
                break;
            case 2:
            case 12:
                path = PackageConfigurator.getJavaRtHome() + "/";
                break;
            case 3:
            case 7:
            case 8:
            case 9:
                path = PackageConfigurator.getDefaultValue("PACKAGE_STORE") + "/";
                break;
            case 4:
                path = "/hsphere/shared/scripts/pkg_scripts/";
                break;
            case 5:
                path = "";
                break;
            case 6:
                path = "/hsphere/shared/scripts/pkg_rpms/";
                break;
            case 10:
                path = PackageConfigurator.getDefaultValue("USER_IMAGE_PATH") + "/";
                break;
            case 11:
                path = "/hsphere/shared/scripts/3rd_party/";
                break;
            case 14:
                path = PackageConfigurator.getDefaultValue("USER_TEMPLATE_PATH") + "/common/JS/";
                break;
            case 15:
                path = PackageConfigurator.getDefaultValue(null) + "/yafv_html/hsphere/common/";
                break;
        }
        return new File(path).getAbsolutePath();
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String getBuild() {
        return this.build;
    }

    public String getDescrShort() {
        return this.descrShort;
    }

    public void reinstallRemoteFiles() throws Exception {
        PkgInfo pkgInfo = getPkgInfo();
        Iterator i = pkgInfo.pkgFilesIterator();
        while (i.hasNext()) {
            PackageFileInfo pfi = (PackageFileInfo) i.next();
            switch (pfi.getType()) {
                case 4:
                case 11:
                    try {
                        reinstallFile(pfi);
                        break;
                    } catch (Exception ex) {
                        ToolLogger.getDefaultLogger().outMessage("Unable to reinstall the file:" + pfi.getName(), ex);
                        break;
                    }
            }
        }
    }

    protected void reinstallFile(PackageFileInfo pfi) throws SQLException, IOException, ClassNotFoundException {
        PackageFile pf = PackageFile.getFile(pfi.getName());
        if (!pf.getInstalled().equals(getName())) {
            return;
        }
        pf.setPkfInfo(pfi);
        try {
            pf.installPackageFile();
        } catch (Exception e) {
            ToolLogger.getDefaultLogger().outMessage("Unable to reinstall package remote file", e);
        }
    }

    public File getHspBundle() {
        File packageDir = getPackageDir();
        if (packageDir.exists() && packageDir.isDirectory()) {
            File[] listHspBundles = packageDir.listFiles(new FileFilter() { // from class: psoft.hsp.Package.1
                @Override // java.io.FileFilter
                public boolean accept(File pathname) {
                    if (pathname.getName().startsWith(Package.this.getName()) && pathname.getName().endsWith(".hsp")) {
                        return true;
                    }
                    return false;
                }
            });
            if (listHspBundles.length > 0) {
                return listHspBundles[0];
            }
            return null;
        }
        return null;
    }

    public File getPackageDir() {
        String pkgStorage = PackageConfigurator.getDefaultValue("PACKAGE_STORE");
        File packageDir = new File(pkgStorage + "/" + getName());
        return packageDir;
    }

    protected PkgInfo getPkgInfo() throws Exception {
        String pkgStorage = PackageConfigurator.getDefaultValue("PACKAGE_STORE");
        File hspBundle = getHspBundle();
        if (hspBundle == null) {
            throw new Exception("Unable to get source of 'hsp' package");
        }
        File src = PkgInstaller.unpackPkg(hspBundle, pkgStorage);
        PkgIntegrityChecker verifier = new PkgIntegrityChecker(src);
        verifier.preInstallCheck();
        PkgInfo pkgInfo = new PkgInfo(verifier.getPkgConfig(), verifier.getConfSerialiser().getXMLConfig());
        try {
            pkgInfo.loadAvailableFiles();
            return pkgInfo;
        } catch (Exception ex) {
            throw new PkgInstallerException("Error loading package files", ex);
        }
    }

    public boolean needsToReinstall() {
        Set<String> scripts = new HashSet();
        scripts.addAll(getFileSet(4));
        scripts.addAll(getFileSet(11));
        boolean needsToReinstall = false;
        for (String pfName : scripts) {
            try {
                PackageFile pf = PackageFile.getFile(pfName);
                if (pf instanceof RemoteFile) {
                    needsToReinstall = needsToReinstall || !pf.check();
                }
            } catch (Exception e) {
                ToolLogger.getDefaultLogger().outMessage("Unable to check file :" + pfName);
            }
        }
        return needsToReinstall;
    }

    public static void resinstallFiles() throws Exception {
        for (Package aPackage : getPackages()) {
            if (aPackage.needsToReinstall()) {
                try {
                    aPackage.reinstallRemoteFiles();
                } catch (Exception ex) {
                    ToolLogger.getDefaultLogger().outMessage("Unable to reinstall files in :" + aPackage.getName(), ex);
                }
            }
        }
    }
}
