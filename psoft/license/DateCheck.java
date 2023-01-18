package psoft.license;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

/* loaded from: hsphere.zip:psoft/license/DateCheck.class */
public class DateCheck {
    public static void main(String[] args) throws IOException {
        DataInputStream in = new DataInputStream(System.in);
        System.out.println("Input Date");
        String line = in.readLine();
        System.out.println("Date: " + new Date(Long.parseLong(line)));
    }
}
