package psoft.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/util/LocalExec.class */
public class LocalExec {
    private static final Category log = Category.getInstance(LocalExec.class.getName());

    public static Collection exec(String[] args, String input) throws Exception {
        String errors = "STDERR: ";
        Process p = Runtime.getRuntime().exec(args);
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
            log.info("LocalExec ERROR: " + exit + "\n");
            BufferedReader er = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String tmp2 = er.readLine();
                if (null == tmp2) {
                    break;
                }
                errors = errors + tmp2 + "\n";
            }
            String output = "";
            Iterator i = col.iterator();
            while (i.hasNext()) {
                output = output + ((String) i.next()) + "\n";
            }
            String command = "";
            for (String arg : args) {
                command = command + " " + arg;
            }
            throw new Exception("command " + command + " failed: \n" + errors + "\n STDIN: " + input + "\nSTDOUT: " + output);
        }
        return col;
    }
}
