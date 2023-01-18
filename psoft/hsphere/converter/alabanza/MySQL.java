package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/MySQL.class */
public class MySQL {
    public static boolean parseMySQLManager(URL url, Document doc, Element mysql, String mysqlcp, List traffic) {
        boolean result;
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            Hashtable pages = MigrationProvider.getMySQLPages(in);
            if (pages.isEmpty()) {
                result = false;
            } else {
                String pageLeft = (String) pages.get("left");
                if (pageLeft != null) {
                    String leftURL = mysqlcp + pageLeft;
                    URL urlLeft = new URL(leftURL);
                    System.out.println("MySQL frame link: " + leftURL);
                    boolean res = parseLeft(urlLeft, doc, mysql, traffic);
                    result = res;
                } else {
                    result = false;
                }
            }
            in.close();
            System.out.println("Finished reading MySQL frameset.");
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MySQL.parseMySQLManager()", e);
            System.out.println("Reading MySQL failed.");
            result = false;
        }
        return result;
    }

    private static boolean parseLeft(URL url, Document doc, Element mySQL, List traffic) {
        boolean result;
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            List mysql = MigrationProvider.getMySQLDBNames(in);
            if (mysql.isEmpty()) {
                result = false;
            } else {
                int size = mysql.size();
                for (int i = 0; i < size; i++) {
                    String dbname = (String) mysql.get(i);
                    if (dbname != null) {
                        Element database = doc.createElement("database");
                        database.setAttribute("name", dbname);
                        database.setAttribute("description", dbname);
                        mySQL.appendChild(database);
                    }
                }
                result = true;
            }
            in.close();
            System.out.println("Finished reading MySQL databases.");
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MySQL.parseLeft()", e);
            System.out.println("Reading database info failed.");
            result = false;
        }
        return result;
    }
}
