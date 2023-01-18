package psoft.hsp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import psoft.hsp.files.GeneratedFile;
import psoft.hsp.files.LocalFile;
import psoft.hsp.files.RemoteFile;
import psoft.hsp.tools.PHServerTypes;
import psoft.hsp.tools.PhysicalFileKey;
import psoft.hsp.tools.PhysicalFilesStore;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/PackageFileInfo.class */
public class PackageFileInfo {
    private static Category log = Category.getInstance(PackageFileInfo.class.getName());
    protected String name;
    protected HashSet groups;
    protected PhysicalFilesStore fileStore;
    protected String destinationPath;
    protected String sourcePath;
    protected int type;

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getSourcePath() {
        return this.sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PackageFileInfo) {
            PackageFileInfo packageFileInfo = (PackageFileInfo) o;
            return this.type == packageFileInfo.type && this.name.equals(packageFileInfo.name);
        }
        return false;
    }

    public int hashCode() {
        int result = this.name.hashCode();
        return (29 * result) + this.type;
    }

    public PackageFileInfo(String name, int type) {
        this(name, type, Package.getFileDestByFileType(type));
    }

    public PackageFileInfo(String name, int type, String destination) {
        this.name = name;
        this.type = type;
        this.groups = new HashSet();
        this.fileStore = new PhysicalFilesStore();
        this.destinationPath = destination;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return this.type;
    }

    public Class getClassByType() {
        switch (this.type) {
            case 1:
            case 2:
            case 3:
            case 7:
            case 8:
            case 9:
            case 10:
            case 12:
                return LocalFile.class;
            case 4:
            case 6:
            case 11:
                return RemoteFile.class;
            case 5:
            default:
                return LocalFile.class;
            case 13:
            case 14:
            case 15:
                return GeneratedFile.class;
        }
    }

    public Set getServerGroup() {
        HashSet hashSet;
        synchronized (this.groups) {
            hashSet = this.groups;
        }
        return hashSet;
    }

    public String getServerGroupAsString() {
        String stringBuffer;
        synchronized (this.groups) {
            StringBuffer result = new StringBuffer("");
            Iterator iterator = this.groups.iterator();
            while (iterator.hasNext()) {
                String group = (String) iterator.next();
                result.append(group);
                if (iterator.hasNext()) {
                    result.append(',');
                }
            }
            stringBuffer = result.toString();
        }
        return stringBuffer;
    }

    public void setServerGroupFromString(String sGroups) {
        StringTokenizer tkz = new StringTokenizer(sGroups, ",");
        this.groups = new HashSet();
        synchronized (this.groups) {
            while (tkz.hasMoreTokens()) {
                String group = tkz.nextToken();
                if (PHServerTypes.getLServerGroupTypes().contains(group) || "ANY".equals(group)) {
                    this.groups.add(group);
                } else {
                    ToolLogger.getDefaultLogger().outMessage("Unable to identify group:" + group);
                }
            }
        }
    }

    public Object getPhysicalFile() {
        return getPhysicalFile(new HashSet(Arrays.asList("CP")), "ANY", "ANY");
    }

    public Object getPhysicalFile(Set srvGroupTypes, String osFamily, String osName) {
        return getPhysicalFile(srvGroupTypes, osFamily, osName, false);
    }

    public Object getPhysicalFile(Set srvGroupTypes, String osFamily, String osName, boolean strict) {
        Iterator i = srvGroupTypes.iterator();
        while (i.hasNext()) {
            String srvGrType = (String) i.next();
            PhysicalFileKey key = new PhysicalFileKey(srvGrType, osFamily, osName);
            Object item = this.fileStore.get(key);
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    public Object getPhysicalFile(PhysicalFileKey key) {
        return this.fileStore.get(key);
    }

    public String getPath() {
        return new File(this.destinationPath).getAbsolutePath() + "/";
    }

    public String getOwner() {
        switch (this.type) {
            case 1:
            case 2:
            case 3:
            case 7:
            case 8:
            case 9:
            case 10:
            case 12:
            case 13:
            case 14:
            case 15:
                return "cpanel";
            case 4:
            case 6:
            case 11:
                return "root";
            case 5:
            default:
                return "root";
        }
    }

    public String getGroup() {
        switch (this.type) {
            case 1:
            case 10:
            case 14:
                return "httpdcp";
            default:
                return getOwner();
        }
    }

    public String getDirectoryPermission() {
        switch (this.type) {
            case 1:
            case 10:
            case 14:
                return "750";
            case 2:
            case 3:
            case 7:
            case 12:
            case 13:
            case 15:
                return "700";
            case 4:
            case 8:
                return "700";
            case 5:
            case 9:
            default:
                return "700";
            case 6:
            case 11:
                return "755";
        }
    }

    public String getDirectoryOwner() {
        return getOwner();
    }

    public String getDirectoryGroup() {
        return getGroup();
    }

    public String getDestinationPath() {
        return this.destinationPath;
    }

    public String getPermission() {
        switch (this.type) {
            case 1:
            case 10:
            case 14:
                return "640";
            case 2:
            case 3:
            case 7:
            case 12:
            case 13:
            case 15:
                return "600";
            case 4:
            case 8:
                return "700";
            case 5:
            case 9:
            default:
                return "600";
            case 6:
            case 11:
                return "644";
        }
    }

    public void addTargerServerGroup(String group) {
        synchronized (this.groups) {
            this.groups.add(group);
        }
    }

    public void addPhysicalFile(PhysicalFileKey key, Object item) {
        if ("ANY".equals(key.getOsFamily()) && !"ANY".equals(key.getOsName())) {
            key.setOsFamily(PHServerTypes.getOsFamilyByOsName(key.getOsName()));
        }
        if (!this.groups.contains(key.getSrvGroupType()) && !this.groups.contains("ANY")) {
            if ("ANY".equals(key.getSrvGroupType())) {
                this.groups.clear();
            }
            this.groups.add(key.getSrvGroupType());
        }
        this.fileStore.put(key, item);
    }

    public void addPhysicalFile(String prefix, Object item) throws Exception {
        PhysicalFileKey key = new PhysicalFileKey(prefix);
        if ("ANY".equals(key.getOsFamily()) && !"ANY".equals(key.getOsName())) {
            key.setOsFamily(PHServerTypes.getOsFamilyByOsName(key.getOsName()));
        }
        if (!this.groups.contains(key.getSrvGroupType()) && !this.groups.contains("ANY")) {
            if ("ANY".equals(key.getSrvGroupType())) {
                this.groups.clear();
            }
            this.groups.add(key.getSrvGroupType());
        }
        this.fileStore.put(key, item);
    }

    public Iterator physicalFilesIterator() {
        return this.fileStore.keySet().iterator();
    }

    public PackageFile getPackageFile() {
        try {
            return PackageFile.getFile(getName());
        } catch (Exception e) {
            return null;
        }
    }

    public Collection getCompiledFiles() {
        if (needsToBeCompiled()) {
            List l = new ArrayList();
            if (this.name.endsWith(".in")) {
                PackageFileInfo pfi = new PackageFileInfo(this.name.substring(0, this.name.length() - 3), 13);
                l.add(pfi);
                pfi.addTargerServerGroup("CP");
            } else {
                int pos = this.name.lastIndexOf("/") + 1;
                String newName = this.name.substring(pos, this.name.length() - 5);
                PackageFileInfo pfi2 = new PackageFileInfo(newName + ".js", 14);
                l.add(pfi2);
                pfi2.addTargerServerGroup("CP");
                PackageFileInfo pfi3 = new PackageFileInfo(newName + ".java", 15);
                l.add(pfi3);
                pfi3.addTargerServerGroup("CP");
                PackageFileInfo pfi4 = new PackageFileInfo(newName + ".class", 15);
                l.add(pfi4);
                pfi4.addTargerServerGroup("CP");
            }
            return l;
        }
        return null;
    }

    public boolean needsToBeCompiled() {
        if (this.type != 1 || this.name == null) {
            return false;
        }
        return this.name.endsWith(".in") || this.name.endsWith(".yafv");
    }
}
