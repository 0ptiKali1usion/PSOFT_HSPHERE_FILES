package psoft.hsp.tools;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsp.Package;
import psoft.hsp.PackageFileInfo;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgInfo.class */
public class PkgInfo {
    PkgConfig conf;
    Document xmlConf;
    private DirsFileFilter dFilter = new DirsFileFilter();
    private FilesFilter fFilter = new FilesFilter();
    private PackageFileInfo preInstallScript = null;
    private PackageFileInfo postInstallScript = null;
    private PackageFileInfo preUnInstallScript = null;
    private PackageFileInfo postUnInstallScript = null;
    private PackageFileInfo preUpgradeScript = null;
    private PackageFileInfo postUpgradeScript = null;
    private PackageFileInfo upgradeSQL = null;
    private PackageFileInfo pkgSQL = null;
    Hashtable availableFiles = new Hashtable();
    private Hashtable pkgFiles = new Hashtable();
    ToolLogger log = ToolLogger.getDefaultLogger();

    public PkgInfo(PkgConfig conf, Document xmlConf) {
        this.conf = conf;
        this.xmlConf = xmlConf;
        System.out.println("Building list of available file types");
        for (PkgConfigParameter p : conf.getPassedParamValues()) {
            if (p.getType().getFileType() != -1) {
                File tf = new File(conf.getPkgPrefix() + "/" + p.getStrPath());
                this.availableFiles.put(new Integer(p.getType().getFileType()), tf.isFile() ? tf.getParentFile() : tf);
            }
        }
    }

    public Set availableFileTypes() {
        return this.availableFiles.keySet();
    }

    public void loadAvailableFiles() throws Exception {
        for (Integer num : availableFileTypes()) {
            loadFilesInfoByType(num.intValue());
        }
    }

    public void loadFilesInfoByType(int fileType) throws Exception {
        if (!this.availableFiles.keySet().contains(new Integer(fileType))) {
            return;
        }
        File f = (File) this.availableFiles.get(new Integer(fileType));
        switch (fileType) {
            case 6:
                try {
                    getAllRPMS();
                    return;
                } catch (TransformerException te) {
                    this.log.outMessage("Error loading RPMs info.\n", te);
                    return;
                }
            case 11:
                try {
                    getTarballs();
                    return;
                } catch (TransformerException te2) {
                    this.log.outMessage("Error loading info of tarball files.\n", te2);
                    return;
                }
            default:
                addFileInfo(f, fileType);
                return;
        }
    }

    public void addFileInfo(File f, int fileType) throws Exception {
        if (!f.exists()) {
            this.log.outMessage(f.getAbsolutePath() + " does not exists. Ignoring...\n");
        } else if (!f.isDirectory()) {
            this.log.outMessage("\nAding " + f.getPath() + ":" + f.getAbsolutePath() + "\n");
            String fileName = getFileName(f, fileType);
            PackageFileInfo pfi = getPackageFileInfo(fileName, fileType);
            pfi.addPhysicalFile("CP/ANY/ANY", f);
            this.pkgFiles.put(fileName, pfi);
        } else {
            File[] files = f.listFiles(this.fFilter);
            for (File currFile : files) {
                if (fileType == 4) {
                    addComplexFileInfo(currFile, fileType);
                } else {
                    String fileName2 = getFileName(currFile, fileType);
                    PackageFileInfo pfi2 = getPackageFileInfo(fileName2, fileType);
                    pfi2.addPhysicalFile("CP/ANY/ANY", currFile);
                    this.pkgFiles.put(fileName2, pfi2);
                }
            }
            File[] dirs = f.listFiles(this.dFilter);
            for (File file : dirs) {
                addFileInfo(file, fileType);
            }
        }
    }

    private String getFileName(File f, int fileType) throws Exception {
        if (!f.isFile()) {
            throw new Exception(f.getAbsolutePath() + " is not a regulaf file");
        }
        String absPath = f.getAbsolutePath();
        String pathPref = ((File) this.availableFiles.get(new Integer(fileType))).getAbsolutePath() + "/";
        return absPath.substring(absPath.indexOf(pathPref) + pathPref.length(), absPath.length());
    }

    private void addComplexFileInfo(File f, int fileType) throws Exception {
        String name;
        String prefix = "";
        String path = getFileName(f, fileType);
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if (!PHServerTypes.getLServerGroupTypes().contains(t) && !PHServerTypes.getSupportedOs().contains(t) && !PHServerTypes.getSupportedOsFamilies().contains(t)) {
                break;
            }
            prefix = prefix + t + "/";
        }
        if (prefix == null) {
            name = path;
            prefix = AntiSpam.DEFAULT_LEVEL_VALUE;
        } else {
            name = path.substring(path.indexOf(prefix) + prefix.length(), path.length());
        }
        PackageFileInfo pfi = getPackageFileInfo(name, fileType);
        pfi.addPhysicalFile(normalizePrefix(prefix), f);
        this.pkgFiles.put(name, pfi);
    }

    public void getAllRPMS() throws TransformerException {
        String extention;
        this.log.outMessage("Loading RPMs info\n");
        NodeList rpms = XPathAPI.selectNodeList(this.xmlConf.getDocumentElement(), "rpms/rpm");
        this.log.outMessage(rpms.getLength() + " rpm description has been found\n");
        for (int i = 0; i < rpms.getLength(); i++) {
            Node rpm = rpms.item(i);
            String rpmName = rpm.getAttributes().getNamedItem("name").getNodeValue();
            String filePrefix = rpm.getAttributes().getNamedItem("server_group").getNodeValue() + "/";
            NodeList platforms = XPathAPI.selectNodeList(rpm, "platform");
            for (int j = 0; j < platforms.getLength(); j++) {
                Node platform = platforms.item(j);
                String osName = platform.getAttributes().getNamedItem("name").getNodeValue();
                String location = platform.getAttributes().getNamedItem("location").getNodeValue();
                if (osName.startsWith("FBSD")) {
                    extention = ".tgz";
                } else {
                    extention = ".rpm";
                }
                Object item = null;
                this.log.outMessage("Location = " + location + "\n");
                if ("BUILT-IN".equals(location)) {
                    item = new File(this.conf.getPkgPrefix() + "/" + this.conf.getFileParamPath("--with-rpms").getPath() + "/" + rpmName + extention);
                } else if (location.startsWith("ftp") || location.startsWith("http")) {
                    try {
                        item = new URL(location);
                    } catch (MalformedURLException e) {
                        this.log.outMessage("The string specified is not an URL: " + location);
                    }
                }
                PackageFileInfo pfi = getPackageFileInfo(rpmName, 6);
                pfi.addPhysicalFile(new PhysicalFileKey(filePrefix, PHServerTypes.getOsFamilyByOsName(osName), osName), item);
                this.pkgFiles.put(rpmName, pfi);
            }
        }
    }

    public void getTarballs() throws TransformerException {
        this.log.outMessage("Loading info of attached tarballs.\n");
        NodeList tarballs = XPathAPI.selectNodeList(this.xmlConf.getDocumentElement(), "tarballs/tarball");
        this.log.outMessage(tarballs.getLength() + " tarball description has been found\n");
        for (int i = 0; i < tarballs.getLength(); i++) {
            Node tarball = tarballs.item(i);
            String tarballName = tarball.getAttributes().getNamedItem("name").getNodeValue();
            String filePrefix = tarball.getAttributes().getNamedItem("server_group").getNodeValue() + "/";
            NodeList platforms = XPathAPI.selectNodeList(tarball, "platform");
            for (int j = 0; j < platforms.getLength(); j++) {
                Node platform = platforms.item(j);
                String osName = platform.getAttributes().getNamedItem("name").getNodeValue();
                String location = platform.getAttributes().getNamedItem("location").getNodeValue();
                Object item = null;
                this.log.outMessage("Location = " + location + "\n");
                if ("BUILT-IN".equals(location)) {
                    item = new File(this.conf.getPkgPrefix() + "/" + this.conf.getFileParamPath("--with-tarballs").getPath() + "/" + tarballName);
                } else if (location.startsWith("ftp") || location.startsWith("http")) {
                    try {
                        item = new URL(location);
                    } catch (MalformedURLException e) {
                        this.log.outMessage("The string specified is not an URL: " + location);
                    }
                }
                PackageFileInfo pfi = getPackageFileInfo(tarballName, 11);
                pfi.addPhysicalFile(new PhysicalFileKey(filePrefix, PHServerTypes.getOsFamilyByOsName(osName), osName), item);
                this.pkgFiles.put(tarballName, pfi);
            }
        }
    }

    public void printInfo() {
        String obj;
        this.log.setPrfx("");
        this.log.outMessage("\n\nPackage contents:\n");
        for (String name : this.pkgFiles.keySet()) {
            this.log.setPrfx("\t");
            this.log.outMessage("File:" + name + "\n");
            PackageFileInfo lfile = (PackageFileInfo) this.pkgFiles.get(name);
            Iterator phIter = lfile.physicalFilesIterator();
            this.log.setPrfx("\t\t");
            while (phIter.hasNext()) {
                PhysicalFileKey key = (PhysicalFileKey) phIter.next();
                Object item = lfile.getPhysicalFile(key);
                if (item instanceof File) {
                    obj = ((File) item).getAbsolutePath();
                } else if (item instanceof URL) {
                    obj = ((URL) item).toExternalForm();
                } else {
                    obj = item.toString();
                }
                String fileName = obj;
                this.log.outMessage(key.toString() + " : " + fileName + "\n");
            }
            this.log.setPrfx("\t");
        }
        this.log.outMessage("\n\n");
        this.log.setPrfx("");
    }

    private String normalizePrefix(String pref) {
        String srvGrType = "ANY";
        String osFamily = "ANY";
        String osName = "ANY";
        StringTokenizer st = new StringTokenizer(pref, "/");
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if (PHServerTypes.getLServerGroupTypes().contains(t)) {
                srvGrType = t;
            } else if (PHServerTypes.getSupportedOsFamilies().contains(t)) {
                osFamily = t;
            } else if (PHServerTypes.getSupportedOs().contains(t)) {
                osName = t;
            }
            if (!"ANY".equals(osName)) {
                osFamily = PHServerTypes.getOsFamilyByOsName(osName);
            }
        }
        return srvGrType + "/" + osFamily + "/" + osName;
    }

    private PackageFileInfo getPackageFileInfo(String fileName, int fileType) {
        PackageFileInfo pfi;
        if (this.pkgFiles.get(fileName) != null) {
            pfi = (PackageFileInfo) this.pkgFiles.get(fileName);
        } else {
            switch (fileType) {
                case 3:
                case 7:
                case 9:
                    pfi = new PackageFileInfo(fileName, fileType, Package.getFileDestByFileType(fileType) + "/");
                    break;
                case 4:
                case 5:
                case 6:
                default:
                    pfi = new PackageFileInfo(fileName, fileType);
                    break;
                case 8:
                    pfi = new PackageFileInfo(fileName, fileType, Package.getFileDestByFileType(fileType));
                    if (!"_pre-install".equals(fileName)) {
                        if (!"_post-install".equals(fileName)) {
                            if (!"_pre-uninstall".equals(fileName)) {
                                if (!"_post_uninstall".equals(fileName)) {
                                    if (!"_pkg.sql".equals(fileName)) {
                                        if (!"_pkg-uninstall.sql".equals(fileName)) {
                                            if (!"_pkg-upgrade.sql".equals(fileName)) {
                                                if (!"_pre-upgrade".equals(fileName)) {
                                                    if ("_post-upgrade".equals(fileName)) {
                                                        this.postUpgradeScript = pfi;
                                                        break;
                                                    }
                                                } else {
                                                    this.preUpgradeScript = pfi;
                                                    break;
                                                }
                                            } else {
                                                this.upgradeSQL = pfi;
                                                break;
                                            }
                                        }
                                    } else {
                                        this.pkgSQL = pfi;
                                        break;
                                    }
                                } else {
                                    this.postUnInstallScript = pfi;
                                    break;
                                }
                            } else {
                                this.preUnInstallScript = pfi;
                                break;
                            }
                        } else {
                            this.postInstallScript = pfi;
                            break;
                        }
                    } else {
                        this.preInstallScript = pfi;
                        break;
                    }
                    break;
            }
        }
        return pfi;
    }

    public PackageFileInfo getPreInstallScript() {
        return this.preInstallScript;
    }

    public PackageFileInfo getPostInstallScript() {
        return this.postInstallScript;
    }

    public PackageFileInfo getPreUnInstallScript() {
        return this.preUnInstallScript;
    }

    public PackageFileInfo getPostUnInstallScript() {
        return this.postUnInstallScript;
    }

    public PackageFileInfo getPkgSQL() {
        return this.pkgSQL;
    }

    public PackageFileInfo getPreUpgradeScript() {
        return this.preUpgradeScript;
    }

    public PackageFileInfo getPostUpgradeScript() {
        return this.postUpgradeScript;
    }

    public PackageFileInfo getUpgradeSQL() {
        return this.upgradeSQL;
    }

    public Iterator pkgFilesIterator() {
        return this.pkgFiles.values().iterator();
    }

    /* loaded from: hsphere.zip:psoft/hsp/tools/PkgInfo$DirsFileFilter.class */
    public class DirsFileFilter implements FileFilter {
        DirsFileFilter() {
            PkgInfo.this = r4;
        }

        @Override // java.io.FileFilter
        public boolean accept(File f) {
            return f.isDirectory() && f.list() != null && f.list().length > 0;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsp/tools/PkgInfo$FilesFilter.class */
    public class FilesFilter implements FileFilter {
        FilesFilter() {
            PkgInfo.this = r4;
        }

        @Override // java.io.FileFilter
        public boolean accept(File f) {
            return f.isFile();
        }
    }
}
