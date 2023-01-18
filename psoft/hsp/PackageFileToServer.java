package psoft.hsp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsp.tools.PHServerTypes;

/* loaded from: hsphere.zip:psoft/hsp/PackageFileToServer.class */
public class PackageFileToServer {
    protected PackageFile pkgFile;
    protected File location;
    protected HashMap servers;

    public PackageFileToServer(PackageFile file, File location) {
        this.pkgFile = file;
        this.location = location;
    }

    public PackageFileToServer(PackageFile file) {
        this.pkgFile = file;
    }

    protected String[] getServerType(File file) {
        String platform = "";
        String serverGroup = "";
        File parentFile = file.getParentFile();
        while (true) {
            File tmpFile = parentFile;
            if (tmpFile.getParentFile() == null || tmpFile.getParentFile().equals(this.location)) {
                break;
            }
            if (PHServerTypes.getSupportedOsFamilies().contains(tmpFile.getParent())) {
                platform = tmpFile.getParent();
            }
            if (PHServerTypes.getLServerGroupTypes().contains(tmpFile.getParent())) {
                serverGroup = tmpFile.getParent();
            }
            parentFile = file.getParentFile();
        }
        return new String[]{platform, serverGroup};
    }

    public void getListServers() throws IOException {
        List<File> files = getFilesByLocation();
        for (File file : files) {
        }
    }

    public List getFilesByLocation() throws IOException {
        return getFilesByLocation(new LinkedList(), Arrays.asList(this.location));
    }

    public List getFilesByLocation(List resultFiles, List dirToCheck) throws IOException {
        Iterator i = dirToCheck.iterator();
        while (i.hasNext()) {
            File source = (File) i.next();
            File[] files = source.listFiles(new FileFilter() { // from class: psoft.hsp.PackageFileToServer.1
                @Override // java.io.FileFilter
                public boolean accept(File pathname) {
                    if (pathname.isFile() && pathname.getName().equals(PackageFileToServer.this.pkgFile.getName())) {
                        return true;
                    }
                    return false;
                }
            });
            if (files.length > 0) {
                resultFiles.addAll(Arrays.asList(files));
            }
            File[] dirs = source.listFiles(new FileFilter() { // from class: psoft.hsp.PackageFileToServer.2
                @Override // java.io.FileFilter
                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) {
                        return true;
                    }
                    return false;
                }
            });
            if (dirs.length > 0) {
                return getFilesByLocation(resultFiles, Arrays.asList(dirs));
            }
        }
        return resultFiles;
    }
}
