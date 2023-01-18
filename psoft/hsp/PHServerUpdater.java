package psoft.hsp;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;
import psoft.hsp.tools.PHServerTypes;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.PhysicalServer;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.WinPhysicalServer;
import psoft.hsphere.tools.ExternalCP;
import psoft.hsphere.tools.ToolLogger;
import psoft.util.IOUtils;
import psoft.util.LocalExec;

/* loaded from: hsphere.zip:psoft/hsp/PHServerUpdater.class */
public class PHServerUpdater {
    protected HashMap servers;
    protected HashMap groups;
    protected HashMap platforms;

    /* renamed from: os */
    protected HashMap f15os;
    protected Set unavailableServers;
    private static ToolLogger log;
    static final String MAKEFILE = "Makefile";
    protected static PHServerUpdater phServerUpdater = null;
    protected static String cpTemplatePath = null;

    public Set getServersByGroup(String groupName) {
        synchronized (this) {
            Set groupSet = (Set) this.groups.get(groupName);
            if (groupSet != null) {
                return groupSet;
            }
            return new HashSet();
        }
    }

    public Set getServersByGroups(Set groupsToGet) {
        HashSet tmpSet = new HashSet();
        Iterator iterator = groupsToGet.iterator();
        while (iterator.hasNext()) {
            String str = (String) iterator.next();
            synchronized (this) {
                Set groupSet = (Set) this.groups.get(str);
                if (groupSet != null) {
                    tmpSet.addAll(groupSet);
                }
            }
        }
        return tmpSet;
    }

    public String[] getServerPlatformOs(PhysicalServer ph) {
        String tmpStr = (String) this.servers.get(ph);
        String os = "";
        String platform = "";
        if (tmpStr != null && !"".equals(tmpStr)) {
            StringTokenizer tkz = new StringTokenizer(tmpStr, "-");
            if (tkz.hasMoreTokens()) {
                os = tkz.nextToken();
            }
            if (tkz.hasMoreTokens()) {
                platform = tkz.nextToken();
            }
            return new String[]{os, platform};
        }
        return null;
    }

    public Set getServersByPlatforms(String platformName) {
        Set set;
        synchronized (this) {
            set = (Set) this.platforms.get(platformName);
        }
        return set;
    }

    public Set getServersByOs(String osName) {
        Set set;
        synchronized (this) {
            set = (Set) this.f15os.get(osName);
        }
        return set;
    }

    public Set getUnavailableServers() {
        Set set;
        synchronized (this) {
            set = this.unavailableServers;
        }
        return set;
    }

    public PHServerUpdater() {
        log = ToolLogger.getDefaultLogger();
        init();
    }

    public static synchronized PHServerUpdater getInstance() {
        if (phServerUpdater == null) {
            phServerUpdater = new PHServerUpdater();
        }
        return phServerUpdater;
    }

    public void init() {
        synchronized (this) {
            this.servers = new HashMap();
            this.groups = new HashMap();
            Iterator i = HostManager.getHosts().iterator();
            while (i.hasNext()) {
                HostEntry entry = (HostEntry) i.next();
                if (!(entry instanceof WinHostEntry)) {
                    try {
                        PhysicalServer ph = entry.getPServer();
                        if (!this.servers.keySet().contains(String.valueOf(ph.getId()))) {
                            this.servers.put(ph, "");
                        }
                        Set serv = (Set) this.groups.get("ANY");
                        if (serv == null) {
                            serv = new HashSet();
                        }
                        serv.add(ph);
                        this.groups.put("ANY", serv);
                        String group = null;
                        Iterator gi = HostEntry.getGroupTypeToIdHash().keySet().iterator();
                        while (group == null && gi.hasNext()) {
                            Object key = gi.next();
                            String groupStrId = (String) HostEntry.getGroupTypeToIdHash().get(key);
                            if (Integer.parseInt(groupStrId) == entry.getGroupType()) {
                                group = (String) key;
                            }
                        }
                        if (group != null) {
                            Set serv2 = (Set) this.groups.get(group.toUpperCase());
                            if (serv2 == null) {
                                serv2 = new HashSet();
                            }
                            serv2.add(ph);
                            this.groups.put(group.toUpperCase(), serv2);
                        }
                    } catch (Exception e) {
                        System.out.println("Unable to get Physical server");
                        e.printStackTrace();
                    }
                }
            }
            this.f15os = new HashMap();
            this.platforms = new HashMap();
            this.unavailableServers = new HashSet();
            for (PhysicalServer server : this.servers.keySet()) {
                String serverPlatform = "";
                String serverOs = "";
                try {
                    Collection result = server.exec("get-platform-os", new String[0], "");
                    Iterator iOs = result.iterator();
                    if (iOs.hasNext()) {
                        String tmpPlatform = (String) iOs.next();
                        if (iOs.hasNext()) {
                            String tmpOs = (String) iOs.next();
                            PHServerTypes.registerUnsupportedOsFamilies(tmpOs, tmpPlatform);
                            if (PHServerTypes.getSupportedOs().contains(tmpOs.toUpperCase())) {
                                Set servOs = (Set) this.f15os.get(tmpOs.toUpperCase());
                                if (servOs == null) {
                                    servOs = new HashSet();
                                }
                                servOs.add(server);
                                this.f15os.put(tmpOs.toUpperCase(), servOs);
                                serverOs = tmpOs;
                                if (PHServerTypes.getSupportedOsFamilies().contains(tmpPlatform.toUpperCase())) {
                                    Set servP = (Set) this.platforms.get(tmpPlatform.toUpperCase());
                                    if (servP == null) {
                                        servP = new HashSet();
                                    }
                                    servP.add(server);
                                    this.platforms.put(tmpPlatform.toUpperCase(), servP);
                                    serverPlatform = tmpPlatform;
                                }
                            }
                        }
                    }
                } catch (Exception e2) {
                    log.outFail("Unable to get platform and os for server:", e2);
                }
                if ("".equals(serverOs) || "".equals(serverPlatform)) {
                    this.unavailableServers.add(server);
                } else {
                    Set serv3 = (Set) this.platforms.get("");
                    if (serv3 == null) {
                        serv3 = new HashSet();
                    }
                    serv3.add(server);
                    this.platforms.put("", serv3);
                    Set serv4 = (Set) this.f15os.get("");
                    if (serv4 == null) {
                        serv4 = new HashSet();
                    }
                    serv4.add(server);
                    this.f15os.put("", serv4);
                    this.servers.put(server, serverPlatform + "-" + serverOs);
                }
            }
        }
    }

    public static void main(String[] argvs) throws Exception {
        ExternalCP.initCP();
        PHServerUpdater phs = getInstance();
        for (String group : PHServerTypes.getLServerGroupTypes()) {
            Set<PhysicalServer> servers = phs.getServersByGroup(group);
            if (servers != null && servers.size() > 0) {
                System.out.println("Group:" + group);
                for (PhysicalServer server : servers) {
                    System.out.println("Server:" + server.getPFirstIP());
                }
            }
        }
        Set<PhysicalServer> servers2 = phs.getServersByPlatforms("");
        if (servers2 != null && servers2.size() > 0) {
            for (PhysicalServer server2 : servers2) {
                System.out.println("Server:" + server2.getPFirstIP());
            }
        }
        for (String os : PHServerTypes.getSupportedOs()) {
            Set<PhysicalServer> servers3 = phs.getServersByOs(os);
            if (servers3 != null && servers3.size() > 0) {
                System.out.println("OS:" + os);
                for (PhysicalServer server3 : servers3) {
                    System.out.println("Server:" + server3.getPFirstIP());
                }
            }
        }
        System.out.println("OS:ALL");
        Set<PhysicalServer> servers4 = phs.getServersByPlatforms("");
        if (servers4 != null && servers4.size() > 0) {
            for (PhysicalServer server4 : servers4) {
                System.out.println("Server:" + server4.getPFirstIP());
            }
        }
        for (String platforms : PHServerTypes.getSupportedOsFamilies()) {
            Set<PhysicalServer> servers5 = phs.getServersByPlatforms(platforms);
            if (servers5 != null && servers5.size() > 0) {
                System.out.println("Platforms:" + platforms);
                for (PhysicalServer server5 : servers5) {
                    System.out.println("Server:" + server5.getPFirstIP());
                }
            }
        }
        System.out.println("Platforms:ALL");
        Set<PhysicalServer> servers6 = phs.getServersByPlatforms("");
        if (servers6 != null && servers6.size() > 0) {
            for (PhysicalServer server6 : servers6) {
                System.out.println("Server:" + server6.getPFirstIP());
            }
        }
        Set<PhysicalServer> servers7 = phs.getUnavailableServers();
        if (servers7 != null && servers7.size() > 0) {
            System.out.println("UNAVAILABLE SERVERS");
            for (PhysicalServer server7 : servers7) {
                System.out.println("Server:" + server7.getPFirstIP());
            }
        }
    }

    public boolean checkFileOnServer(PhysicalServer ph, File file) throws Exception {
        Collection result = ph.exec("pkg-check-file", new String[]{file.getAbsolutePath()}, "");
        Iterator i = result.iterator();
        if (i.hasNext()) {
            String value = (String) i.next();
            if ("1".equals(value)) {
                return true;
            }
            return false;
        }
        throw new Exception("Unable to get return parameters for check-file");
    }

    public void putFileToServer(PhysicalServer ph, File file, File destFile) throws Exception {
        if (!(ph instanceof WinPhysicalServer)) {
            try {
                ph.exec("pkg-prepare-dir", new String[]{destFile.getParentFile().getAbsolutePath()}, "");
                LocalExec.exec(new String[]{"scp", file.getAbsolutePath(), ph.getLogin() + "@" + ph.getPFirstIP() + ":" + destFile.getAbsolutePath()}, "");
                return;
            } catch (IOException e) {
                log.outMessage("Unable to put file " + file.getName() + " to server" + ph.getPFirstIP());
                return;
            } catch (InterruptedException e2) {
                log.outMessage("Unable to put file " + file.getName() + " to server" + ph.getPFirstIP());
                return;
            }
        }
        throw new Exception("Not compaible platform");
    }

    public void putFileToServers(Set servers, File file, File destFile) throws Exception {
        Iterator i = servers.iterator();
        while (i.hasNext()) {
            PhysicalServer ph = (PhysicalServer) i.next();
            putFileToServer(ph, file, destFile);
        }
    }

    public void makePathLocaly(File destFile, String owner, String group, String permissions) throws Exception {
        if (!destFile.getParentFile().exists()) {
            makePathLocaly(destFile.getParentFile(), owner, group, permissions);
        }
        if (destFile.mkdir()) {
            log.outMessage("Creating direcroty: " + destFile.getAbsolutePath());
            LocalExec.exec(new String[]{"chmod", permissions, destFile.getAbsolutePath()}, "");
            LocalExec.exec(new String[]{"chown", owner + ":" + group, destFile.getAbsolutePath()}, "");
        }
    }

    public void putFileLocaly(File file, File destFile) throws Exception {
        IOUtils.copyFile(file, destFile);
    }

    public void setPermissionsFileLocaly(File file, String owner, String group, String permission) throws Exception {
        if (file.exists()) {
            LocalExec.exec(new String[]{"chown", owner + ":" + group, file.getAbsolutePath()}, "");
            LocalExec.exec(new String[]{"chmod", permission, file.getAbsolutePath()}, "");
        }
    }

    public void setPermissionsFileOnSerever(PhysicalServer ph, File file, String owner, String group, String permission) throws Exception {
        if (!(ph instanceof WinPhysicalServer)) {
            try {
                ph.exec("pkg-set-perm", new String[]{file.getAbsolutePath(), owner, group, permission}, "");
                return;
            } catch (IOException e) {
                log.outMessage("Unable to put file " + file.getName() + " to server" + ph.getPFirstIP());
                return;
            } catch (InterruptedException e2) {
                log.outMessage("Unable to put file " + file.getName() + " to server" + ph.getPFirstIP());
                return;
            }
        }
        throw new Exception("Not compaible platform");
    }

    protected void removerParentDirs(File dir, File baseDir) {
        if (!dir.equals(baseDir) && dir.isDirectory()) {
            String[] entries = dir.list();
            switch (entries.length) {
                case 0:
                    break;
                default:
                    return;
                case 1:
                    if (MAKEFILE.equals(entries[0])) {
                        File mkf = new File(dir, MAKEFILE);
                        if (!mkf.isFile() || !mkf.delete()) {
                            return;
                        }
                    } else {
                        return;
                    }
                    break;
            }
            if (dir.delete()) {
                removerParentDirs(dir.getParentFile(), baseDir);
            }
        }
    }

    protected LinkedList generateParentDirs(File dir, File baseDir) {
        return generateParentDirs(dir, baseDir, new LinkedList());
    }

    protected LinkedList generateParentDirs(File dir, File baseDir, LinkedList list) {
        if (dir.equals(baseDir)) {
            return list;
        }
        list.add(dir.getAbsolutePath());
        return generateParentDirs(dir.getParentFile(), baseDir, list);
    }

    public void removeFileLocaly(File file, File baseDir) throws Exception {
        file.delete();
        removerParentDirs(file.getParentFile(), baseDir);
    }

    public boolean checkFileLoclay(File file) throws Exception {
        return file.exists();
    }

    public void removeFileFromServer(PhysicalServer ph, File file, File basePath) throws Exception {
        if (!(ph instanceof WinPhysicalServer)) {
            try {
                LinkedList parentList = generateParentDirs(file.getParentFile(), basePath);
                parentList.addFirst(file.getAbsolutePath());
                ph.exec("pkg-delete-file", (String[]) parentList.toArray(new String[0]), "");
                return;
            } catch (IOException e) {
                log.outMessage("Unable to remove file " + file.getName() + " from server" + ph.getPFirstIP());
                return;
            } catch (InterruptedException e2) {
                log.outMessage("Unable to remove file " + file.getName() + " from server" + ph.getPFirstIP());
                return;
            }
        }
        throw new Exception("Not compaible platform");
    }

    public Set getGroupListForServer(PhysicalServer ph) {
        HashSet tmpSet = new HashSet();
        for (String groupName : this.groups.keySet()) {
            Set pServers = (Set) this.groups.get(groupName);
            if (pServers.contains(ph)) {
                tmpSet.add(groupName);
            }
        }
        return tmpSet;
    }

    public static synchronized String getCPTemplatePath() {
        if (cpTemplatePath == null) {
            cpTemplatePath = Session.getPropertyString("TEMPLATE_PATH");
        }
        return cpTemplatePath;
    }

    public static synchronized void recompileCPTemplates() throws Exception {
        LocalExec.exec(new String[]{getCPTemplatePath() + "configure", "make recompile"}, "");
    }

    public static synchronized void configureCPTemplates() throws Exception {
        LocalExec.exec(new String[]{getCPTemplatePath() + "configure"}, "");
    }

    public static synchronized void cleanCPTemplates() throws Exception {
        LocalExec.exec(new String[]{getCPTemplatePath() + "configure", "clean"}, "");
    }
}
