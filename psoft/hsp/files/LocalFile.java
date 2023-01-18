package psoft.hsp.files;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import psoft.hsp.PHServerUpdater;
import psoft.hsp.PackageFile;
import psoft.hsp.PackageFileInfo;
import psoft.util.LocalExec;

/* loaded from: hsphere.zip:psoft/hsp/files/LocalFile.class */
public class LocalFile extends PackageFile {
    public LocalFile(Integer id, PackageFileInfo pkfInfo) {
        super(id, pkfInfo);
    }

    public LocalFile(Integer id, PackageFileInfo pkfInfo, String installed) {
        super(id, pkfInfo, installed);
    }

    public LocalFile(int id, PackageFileInfo pkfInfo, String installed) {
        super(id, pkfInfo, installed);
    }

    @Override // psoft.hsp.PackageFile
    public void installPackageFile() throws Exception {
        if (getPkfInfo() != null) {
            File templateFile = (File) getPkfInfo().getPhysicalFile(new HashSet(Arrays.asList("CP")), "ANY", "ANY");
            if (templateFile.exists()) {
                PHServerUpdater phs = PHServerUpdater.getInstance();
                File destFile = new File(getPath() + "/" + getName());
                phs.makePathLocaly(destFile.getParentFile(), getPkfInfo().getDirectoryOwner(), getPkfInfo().getDirectoryGroup(), getPkfInfo().getDirectoryPermission());
                phs.putFileLocaly(templateFile, destFile);
                phs.setPermissionsFileLocaly(destFile, getPkfInfo().getOwner(), getPkfInfo().getGroup(), getPkfInfo().getPermission());
            }
        }
    }

    @Override // psoft.hsp.PackageFile
    public void uninstallPackageFile() throws Exception {
        File file = new File(getFullPath());
        if (file.exists()) {
            PHServerUpdater phs = PHServerUpdater.getInstance();
            phs.removeFileLocaly(file, new File(getPath()));
        }
    }

    @Override // psoft.hsp.PackageFile
    public boolean check() throws Exception {
        File file = new File(getFullPath());
        return PHServerUpdater.getInstance().checkFileLoclay(file);
    }

    @Override // psoft.hsp.PackageFile
    public Collection exec() throws Exception {
        File file = new File(getFullPath());
        return LocalExec.exec(new String[]{file.getAbsolutePath()}, "");
    }
}
