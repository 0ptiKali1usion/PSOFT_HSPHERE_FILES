package psoft.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* loaded from: hsphere.zip:psoft/util/Mail.class */
public class Mail {
    public static OutputStream send(String to) throws IOException {
        try {
            return Runtime.getRuntime().exec("/usr/sbin/sendmail " + to).getOutputStream();
        } catch (SecurityException se) {
            se.printStackTrace();
            throw new IOException();
        }
    }

    public static OutputStream send(String from, String to) throws IOException {
        try {
            return Runtime.getRuntime().exec("/usr/sbin/sendmail -f" + from + " " + to).getOutputStream();
        } catch (SecurityException se) {
            se.printStackTrace();
            throw new IOException();
        }
    }

    public static boolean send(String from, String to, String subject, String msg) {
        System.err.println(to + "\n" + from + "\n" + subject + "\n" + msg);
        try {
            Process proc = Runtime.getRuntime().exec("/usr/sbin/sendmail " + to);
            DataOutputStream os = new DataOutputStream(proc.getOutputStream());
            os.writeBytes("TO: ");
            os.writeBytes(to);
            os.writeBytes("\nFROM: ");
            os.writeBytes(from);
            os.writeBytes("\nSUBJECT: ");
            os.writeBytes(subject);
            os.writeBytes("\n\n");
            os.writeBytes(msg);
            os.close();
            return true;
        } catch (IOException ie) {
            ie.printStackTrace();
            return false;
        } catch (SecurityException se) {
            se.printStackTrace();
            return false;
        }
    }
}
