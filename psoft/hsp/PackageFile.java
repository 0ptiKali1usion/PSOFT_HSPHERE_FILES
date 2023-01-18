package psoft.hsp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import psoft.hsphere.Session;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/PackageFile.class */
public abstract class PackageFile {

    /* renamed from: OK */
    public static final int f17OK = 0;
    public static final int CONFLICT = 1;

    /* renamed from: id */
    protected int f18id;
    protected int type;
    protected int belongsTo;
    protected String installed;
    protected String name;
    protected String path;
    protected Set packages;
    protected PackageFileInfo pkfInfo;
    protected static HashMap files = new HashMap();

    public abstract void installPackageFile() throws Exception;

    public abstract void uninstallPackageFile() throws Exception;

    public abstract boolean check() throws Exception;

    public abstract Collection exec() throws Exception;

    public PackageFileInfo getPkfInfo() {
        return this.pkfInfo;
    }

    public void setPkfInfo(PackageFileInfo pkfInfo) {
        this.pkfInfo = pkfInfo;
    }

    public PackageFile(Integer id, PackageFileInfo pkfInfo) {
        this(id, pkfInfo, "");
    }

    public PackageFile(Integer id, PackageFileInfo pkfInfo, String installed) {
        this(id.intValue(), pkfInfo, installed);
    }

    public PackageFile(int id, PackageFileInfo pkfInfo, String installed) {
        this.pkfInfo = null;
        this.f18id = id;
        this.type = pkfInfo.getType();
        this.name = pkfInfo.getName();
        this.path = pkfInfo.getPath();
        this.installed = installed;
        setPkfInfo(pkfInfo);
    }

    public static PackageFile addFile(String packageName, PackageFileInfo pkfInfo, boolean force) throws SQLException, PackageNotFoundException, ClassNotFoundException {
        PackageFile file = null;
        Package.getPackage(packageName);
        try {
            file = getFile(pkfInfo.getName());
        } catch (IOException e) {
        } catch (ClassNotFoundException e2) {
        }
        if (file == null) {
            int id = Session.getNewId("package_file_id");
            Class fileClass = pkfInfo.getClassByType();
            if (fileClass == null) {
                throw new ClassNotFoundException("Unable to get class to load file:" + pkfInfo.getName());
            }
            try {
                Constructor c = fileClass.getConstructor(Integer.class, PackageFileInfo.class);
                file = (PackageFile) c.newInstance(new Integer(id), pkfInfo);
                file.save();
                file.addFileToPackages(packageName);
                files.put(pkfInfo.getName(), file);
                force = true;
            } catch (Exception e3) {
                ToolLogger.getDefaultLogger().outMessage("The class " + fileClass.getName() + " doesn't have constructor", e3);
            }
        }
        if (force) {
            file.setInstalled(packageName);
            try {
                file.setPkfInfo(pkfInfo);
                file.installPackageFile();
            } catch (Exception e4) {
                file.setInstalled("");
                ToolLogger.getDefaultLogger().outMessage("Unable to physically install the file " + pkfInfo.getName(), e4);
            }
        }
        return file;
    }

    public void save() throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO package_files(id, file_type, name, path, groups, installed)  VALUES (?,?,?,?,?,?)");
            ps.setInt(1, getId());
            ps.setInt(2, getType());
            ps.setString(3, getName());
            ps.setString(4, getPath());
            ps.setString(5, getPkfInfo().getServerGroupAsString());
            ps.setString(6, this.installed);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delFile(String packageName) throws Exception {
        synchronized (this) {
            if (getUsedByPackages().contains(packageName)) {
                Package pkg = Package.getPackage(packageName);
                if (getUsedByPackages().size() <= 1) {
                    delete();
                }
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("DELETE FROM package_files_r WHERE  file_id = ? AND package_id = ?");
                ps.setInt(1, getId());
                ps.setInt(2, pkg.getId());
                ps.executeUpdate();
                getUsedByPackages().remove(packageName);
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    protected void delete() throws Exception {
        try {
            check();
            uninstallPackageFile();
            PreparedStatement ps = null;
            Connection con = Session.getDb();
            try {
                ps = con.prepareStatement("DELETE FROM package_files WHERE id = ?");
                ps.setInt(1, getId());
                ps.executeUpdate();
                files.remove(getName());
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        } catch (Exception e) {
            ToolLogger.getDefaultLogger().outMessage("Unable to physicaly install file:" + this.pkfInfo.getName(), e);
            throw e;
        }
    }

    public static PackageFile getFile(String name) throws SQLException, IOException, ClassNotFoundException {
        synchronized (files) {
            PackageFile pf = (PackageFile) files.get(name);
            if (pf != null) {
                return pf;
            }
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, file_type, name, path, groups, installed FROM package_files, package_files_r WHERE package_files.id = file_id AND name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PackageFileInfo pkfInfo = new PackageFileInfo(rs.getString(3), rs.getInt(2), rs.getString(4));
                pkfInfo.setServerGroupFromString(rs.getString(5));
                Class fileClass = pkfInfo.getClassByType();
                if (fileClass == null) {
                    throw new ClassNotFoundException("Unable to get class to load file:" + name);
                }
                try {
                    Constructor c = fileClass.getConstructor(Integer.class, PackageFileInfo.class, String.class);
                    pf = (PackageFile) c.newInstance(new Integer(rs.getInt(1)), pkfInfo, rs.getString(6));
                    files.put(name, pf);
                } catch (Exception e) {
                    ToolLogger.getDefaultLogger().outMessage("The class " + fileClass.getName() + " doesn't have constructor", e);
                }
                Session.closeStatement(ps);
                con.close();
                return pf;
            }
            throw new IOException("File :" + name + "not found:");
        }
    }

    public void addFileToPackages(String packageName) throws SQLException, PackageNotFoundException {
        synchronized (this) {
            Package pkg = Package.getPackage(packageName);
            Connection con = Session.getDb();
            Set p = getUsedByPackages();
            PreparedStatement ps = con.prepareStatement("INSERT INTO package_files_r(file_id, package_id)  VALUES (?,?)");
            ps.setInt(1, getId());
            ps.setInt(2, pkg.getId());
            ps.executeUpdate();
            p.add(packageName);
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void delFileFromPackages(String packageName) throws SQLException, PackageNotFoundException {
        synchronized (this) {
            Package pkg = Package.getPackage(packageName);
            Connection con = Session.getDb();
            Set p = getUsedByPackages();
            PreparedStatement ps = con.prepareStatement("DELETE FROM package_files_r WHERE file_id = ? AND package_id = ?");
            ps.setInt(1, getId());
            ps.setInt(2, pkg.getId());
            ps.executeUpdate();
            p.remove(packageName);
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Set getUsedByPackages() throws SQLException {
        synchronized (this) {
            if (this.packages != null) {
                return this.packages;
            }
            this.packages = new HashSet();
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT packages.name FROM packages, package_files_r  WHERE file_id  = ?  AND packages.id = package_files_r.package_id");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.packages.add(rs.getString(1));
            }
            Session.closeStatement(ps);
            con.close();
            return this.packages;
        }
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.f18id;
    }

    public void setBelongsTo(int belongsTo) {
        this.belongsTo = belongsTo;
    }

    public int getBelongsTo() {
        return this.belongsTo;
    }

    public int getType() {
        return this.type;
    }

    public String getPath() {
        return new File(this.path).getAbsolutePath() + "/";
    }

    public boolean isInstalled() {
        return !"".equals(this.installed);
    }

    public String getInstalled() {
        return this.installed;
    }

    public void setInstalled() throws SQLException {
        setInstalled("");
    }

    public void setInstalled(String installed) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE package_files SET installed = ? WHERE id = ?");
            ps.setString(1, installed);
            ps.setInt(2, getId());
            ps.executeUpdate();
            this.installed = installed;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static PackageFile getConflictFile(String name, String pkgName) throws SQLException, ClassNotFoundException {
        try {
            PackageFile pf = getFile(name);
            if (!pkgName.equals(pf.getInstalled())) {
                return pf;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public String getFullPath() {
        return getPath() + getName();
    }
}
