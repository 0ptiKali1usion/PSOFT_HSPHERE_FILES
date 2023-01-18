package psoft.hsphere.resource.system;

import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/system/WebServices.class */
public class WebServices {
    public static int createUnixUser(HostEntry host, String login, String group, String dir, String password) throws Exception {
        return Integer.parseInt(host.exec("adduser", new String[]{"--user-name=" + login, "--home-dir=" + dir, "--group=" + group}, password).iterator().next().toString());
    }

    public static int createUnixUser(HostEntry host, String login, String group, String dir, String password, int uid) throws Exception {
        return Integer.parseInt(host.exec("adduser", new String[]{"--user-name=" + login, "--home-dir=" + dir, "--group=" + group, "--uid=" + uid}, password).iterator().next().toString());
    }

    public static void createUnixSubUser(HostEntry host, String login, String group, String dir, String password, int uid) throws Exception {
        host.exec("addsubuser", new String[]{"--user-name=" + login, "--home-dir=" + dir, "--group=" + group, "--uid=" + uid}, password);
    }

    public static boolean isUnixUserExists(HostEntry host, String login) throws Exception {
        return "1".equals(host.exec("user-check", new String[]{login}).iterator().next().toString());
    }
}
