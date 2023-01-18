package psoft.hsphere.migrator;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/LinuxPhysicalSource.class */
public class LinuxPhysicalSource implements PhysicalSource {
    String login;

    /* renamed from: ip */
    String f98ip;
    String password;

    public LinuxPhysicalSource(String ip, String login, String password) {
        this.login = "";
        this.f98ip = "";
        this.password = "";
        this.login = login;
        this.password = password;
        this.f98ip = ip;
    }

    public LinuxPhysicalSource(Hashtable params) {
        this((String) params.get("ipaddr"), "root", (String) params.get("passwd"));
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.migrator.PhysicalSource
    public Collection exec(String command, String[] args, String input) throws Exception {
        return exec("root", "", command, Arrays.asList(args), input);
    }

    protected Collection exec(String login, String prefix, String command, Collection args, String input) throws Exception {
        StringBuffer cmd = new StringBuffer("ssh -x -a ");
        cmd.append(login + "@" + this.f98ip + " ");
        cmd.append(prefix + command + " ");
        for (Object obj : args) {
            cmd.append(obj.toString()).append(" ");
        }
        StringBuffer errors = new StringBuffer("ERROR: ");
        Process p = Runtime.getRuntime().exec(cmd.toString());
        if (input != null) {
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes(input);
            out.close();
        }
        Collection col = new LinkedList();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while (true) {
            String tmp = br.readLine();
            if (null == tmp) {
                break;
            }
            col.add(tmp);
        }
        int exit = p.waitFor();
        if (0 != exit) {
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String tmp2 = er.readLine();
                if (null == tmp2) {
                    break;
                }
                errors.append(tmp2).append("\n");
            }
            String errMessage = errors.toString() + "Command: " + cmd.toString();
            if (input != null) {
                errMessage = errMessage + "\nSTDIN: " + input;
            }
            throw new Exception(errMessage);
        }
        return col;
    }

    @Override // psoft.hsphere.migrator.PhysicalSource
    public Reader exec2(String command, String[] args, String input) throws Exception {
        return exec2("root", "", command, Arrays.asList(args), input);
    }

    protected Reader exec2(String login, String prefix, String command, Collection args, String input) throws Exception {
        StringBuffer cmd = new StringBuffer("ssh -x -a ");
        cmd.append(login + "@" + this.f98ip + " ");
        cmd.append(prefix + command + " ");
        for (Object obj : args) {
            cmd.append(obj.toString()).append(" ");
        }
        StringBuffer errors = new StringBuffer("ERROR: ");
        Process p = Runtime.getRuntime().exec(cmd.toString());
        if (input != null) {
            DataOutputStream out = new DataOutputStream(p.getOutputStream());
            out.writeBytes(input);
            out.close();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        int exit = p.waitFor();
        if (0 != exit) {
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String tmp = er.readLine();
                if (null == tmp) {
                    break;
                }
                errors.append(tmp).append("\n");
            }
            String errMessage = errors.toString() + "Command: " + cmd.toString();
            if (input != null) {
                errMessage = errMessage + "\nSTDIN: " + input;
            }
            throw new Exception(errMessage);
        }
        return br;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return null;
    }
}
