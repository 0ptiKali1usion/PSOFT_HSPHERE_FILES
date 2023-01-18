package psoft.hsp.files;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import psoft.hsp.PHServerUpdater;
import psoft.hsp.PackageFile;
import psoft.hsp.PackageFileInfo;
import psoft.hsphere.Session;
import psoft.hsphere.resource.PhysicalServer;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/files/RemoteFile.class */
public class RemoteFile extends PackageFile {
    protected HashMap installedServers;
    protected HashMap addedServers;
    protected HashMap neededToInstall;

    public boolean isInstalled(PhysicalServer ph) throws SQLException {
        getInstalledServers();
        return this.installedServers.containsKey(ph);
    }

    public Set getInstalledServersForPackage() throws SQLException {
        getInstalledServers();
        HashSet serv = new HashSet();
        for (PhysicalServer ph : this.installedServers.keySet()) {
            String pckgName = (String) this.installedServers.get(ph);
            if (pckgName != null && pckgName.equals(getInstalled())) {
                serv.add(ph);
            }
        }
        return serv;
    }

    public Set getInstalledServers() throws SQLException {
        if (this.installedServers == null) {
            this.installedServers = new HashMap();
            synchronized (this.installedServers) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT server_id, installed_by FROM server_files WHERE file_id = ?");
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    PhysicalServer ph = PhysicalServer.getPServer(rs.getLong(1));
                    if (ph != null) {
                        this.installedServers.put(ph, rs.getString(2));
                    } else {
                        ToolLogger.getDefaultLogger().outMessage("Unable to get Physical server ID:" + ph.getId());
                    }
                }
                Session.closeStatement(ps);
                con.close();
            }
        }
        return new HashSet(this.installedServers.keySet());
    }

    public void addServer(PhysicalServer ph) throws SQLException {
        getInstalledServers();
        synchronized (this.installedServers) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO server_files (server_id, file_id, installed_by) VALUES( ?, ?, ?)");
            ps.setLong(1, ph.getId());
            ps.setInt(2, getId());
            ps.setString(3, getInstalled());
            ps.executeUpdate();
            this.installedServers.put(ph, getInstalled());
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void deleteServer(PhysicalServer ph) throws SQLException {
        getInstalledServers();
        synchronized (this.installedServers) {
            if (this.installedServers.containsKey(ph)) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("DELETE FROM server_files WHERE server_id = ? AND file_id = ?");
                ps.setLong(1, ph.getId());
                ps.setInt(2, getId());
                ps.executeUpdate();
                this.installedServers.remove(ph);
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    public RemoteFile(int id, PackageFileInfo pkfInfo, String installed) {
        super(id, pkfInfo, installed);
    }

    public RemoteFile(Integer id, PackageFileInfo pkfInfo) {
        super(id, pkfInfo);
    }

    public RemoteFile(Integer id, PackageFileInfo pkfInfo, String installed) {
        super(id, pkfInfo, installed);
    }

    @Override // psoft.hsp.PackageFile
    public void installPackageFile() throws Exception {
        PHServerUpdater phsu = PHServerUpdater.getInstance();
        Set servers = phsu.getServersByGroups(getPkfInfo().getServerGroup());
        HashSet serversToInstall = new HashSet(servers);
        serversToInstall.removeAll(phsu.getUnavailableServers());
        Iterator iterator = serversToInstall.iterator();
        while (iterator.hasNext()) {
            PhysicalServer phs = (PhysicalServer) iterator.next();
            installFile(phs);
            deleteServer(phs);
            addServer(phs);
        }
    }

    protected void installFile(PhysicalServer ph) throws Exception {
        PHServerUpdater phsu = PHServerUpdater.getInstance();
        String[] osPlatform = phsu.getServerPlatformOs(ph);
        File inFile = (File) getPkfInfo().getPhysicalFile(phsu.getGroupListForServer(ph), osPlatform[0], osPlatform[1]);
        File destFile = new File(getFullPath());
        phsu.putFileToServer(ph, inFile, destFile);
        phsu.setPermissionsFileOnSerever(ph, destFile, this.pkfInfo.getOwner(), this.pkfInfo.getGroup(), this.pkfInfo.getPermission());
    }

    @Override // psoft.hsp.PackageFile
    public void uninstallPackageFile() throws Exception {
        Set<PhysicalServer> servers = getInstalledServers();
        for (PhysicalServer ph : servers) {
            uninstallFile(ph);
            deleteServer(ph);
        }
    }

    protected void uninstallFile(PhysicalServer ph) throws Exception {
        PHServerUpdater phsu = PHServerUpdater.getInstance();
        File file = new File(getPath() + "/" + getName());
        phsu.removeFileFromServer(ph, file, new File(getPath()));
    }

    @Override // psoft.hsp.PackageFile
    public synchronized boolean check() throws Exception {
        PHServerUpdater phsu = PHServerUpdater.getInstance();
        HashSet serversToInstall = new HashSet(getInstalledServers());
        File file = new File(getPath() + "/" + getName());
        this.neededToInstall = new HashMap();
        this.addedServers = new HashMap();
        Iterator i = serversToInstall.iterator();
        while (i.hasNext()) {
            PhysicalServer ph = (PhysicalServer) i.next();
            if (!phsu.checkFileOnServer(ph, file)) {
                deleteServer(ph);
                this.neededToInstall.put(ph, "The file disapeared from the server");
            }
        }
        HashSet serversToInstall2 = new HashSet(phsu.getServersByGroups(getPkfInfo().getServerGroup()));
        serversToInstall2.removeAll(getInstalledServers());
        Iterator i2 = serversToInstall2.iterator();
        while (i2.hasNext()) {
            PhysicalServer ph2 = (PhysicalServer) i2.next();
            if (phsu.checkFileOnServer(ph2, file)) {
                addServer(ph2);
                this.addedServers.put(ph2, "The file was apeared on the server");
            } else {
                this.neededToInstall.put(ph2, "The file needed to install on the server");
            }
        }
        if (this.neededToInstall.size() > 0) {
            return false;
        }
        return true;
    }

    public HashMap getAddedServers() {
        return this.addedServers;
    }

    public HashMap getNeededToInstall() {
        return this.neededToInstall;
    }

    @Override // psoft.hsp.PackageFile
    public Collection exec() throws Exception {
        Set<PhysicalServer> servers = getInstalledServers();
        Collection col = new ArrayList();
        for (PhysicalServer ph : servers) {
            Collection res = ph.exec(getFullPath(), new String[0], "");
            if (res != null) {
                col.addAll(res);
            }
        }
        return col;
    }
}
