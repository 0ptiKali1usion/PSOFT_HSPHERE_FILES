package psoft.hsp.files;

import psoft.hsp.PackageFileInfo;

/* loaded from: hsphere.zip:psoft/hsp/files/GeneratedFile.class */
public class GeneratedFile extends LocalFile {
    public GeneratedFile(Integer id, PackageFileInfo pkfInfo) {
        super(id, pkfInfo);
    }

    public GeneratedFile(Integer id, PackageFileInfo pkfInfo, String installed) {
        super(id, pkfInfo, installed);
    }

    public GeneratedFile(int id, PackageFileInfo pkfInfo, String installed) {
        super(id, pkfInfo, installed);
    }

    @Override // psoft.hsp.files.LocalFile, psoft.hsp.PackageFile
    public void installPackageFile() {
    }
}
