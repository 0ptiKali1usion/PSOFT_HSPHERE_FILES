package psoft.hsphere.report;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Config;
import psoft.util.NFUCache;
import psoft.util.Toolbox;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/report/Report.class */
public class Report {
    protected List result = new ArrayList();
    protected List sqlParams = new ArrayList();
    protected String sql;

    /* renamed from: id */
    protected static int f118id;
    protected static long lastMod;
    protected static String oldName;
    protected static NFUCache cache = new NFUCache(20);
    public static HashMap reports = new HashMap();

    public static TemplateModel get(Integer i) {
        Session.getLog().info("Report cached" + i);
        if (cache != null) {
            return (TemplateModel) cache.get(i);
        }
        return null;
    }

    /* JADX WARN: Finally extract failed */
    public TemplateModel get(Connection con, Collection c) throws Exception {
        int sqlType;
        int count = 1;
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(this.sql);
            Session.getLog().info("Report--> SQLParams size=" + this.sqlParams.size());
            for (Object obj : c) {
                try {
                    sqlType = ((SQLParam) this.sqlParams.get(count - 1)).getSQLType();
                } catch (IndexOutOfBoundsException e) {
                    sqlType = 19;
                }
                if ("%".equals(obj.toString())) {
                    int i = count;
                    count++;
                    ps.setString(i, obj.toString());
                } else {
                    switch (sqlType) {
                        case 1:
                            int i2 = count;
                            count++;
                            ps.setObject(i2, obj, 2003);
                            break;
                        case 2:
                            int i3 = count;
                            count++;
                            ps.setObject(i3, obj, 12);
                            break;
                        case 3:
                            int i4 = count;
                            count++;
                            ps.setObject(i4, obj);
                            break;
                        case 4:
                            int i5 = count;
                            count++;
                            ps.setObject(i5, obj, -3);
                            break;
                        case 5:
                            int i6 = count;
                            count++;
                            ps.setObject(i6, obj, 2004);
                            break;
                        case 8:
                            int i7 = count;
                            count++;
                            ps.setObject(i7, obj);
                            break;
                        case 9:
                            int i8 = count;
                            count++;
                            ps.setObject(i8, obj);
                            break;
                        case 10:
                            int i9 = count;
                            count++;
                            ps.setObject(i9, obj, 91);
                            break;
                        case 11:
                            int i10 = count;
                            count++;
                            ps.setDouble(i10, Double.parseDouble(obj.toString()));
                            break;
                        case 12:
                            int i11 = count;
                            count++;
                            ps.setFloat(i11, Float.parseFloat(obj.toString()));
                            break;
                        case 13:
                            int i12 = count;
                            count++;
                            ps.setInt(i12, Integer.parseInt(obj.toString()));
                            break;
                        case 14:
                            int i13 = count;
                            count++;
                            ps.setLong(i13, Long.parseLong(obj.toString()));
                            break;
                        case 15:
                            int i14 = count;
                            count++;
                            ps.setObject(i14, obj);
                            break;
                        case 16:
                            int i15 = count;
                            count++;
                            ps.setObject(i15, obj, 1111);
                            break;
                        case 17:
                            int i16 = count;
                            count++;
                            ps.setObject(i16, obj, 2006);
                            break;
                        case 18:
                            int i17 = count;
                            count++;
                            ps.setShort(i17, Short.parseShort(obj.toString()));
                            break;
                        case 19:
                            int i18 = count;
                            count++;
                            ps.setString(i18, obj.toString());
                            break;
                        case 20:
                            int i19 = count;
                            count++;
                            ps.setObject(i19, obj, 92);
                            break;
                        case 21:
                            int i20 = count;
                            count++;
                            ps.setObject(i20, obj, 93);
                            break;
                        case 22:
                            int i21 = count;
                            count++;
                            ps.setObject(i21, obj);
                            break;
                    }
                }
            }
            Session.getLog().info("Report-->" + ps.toString());
            Session.getLog().info("Report-->" + ps.toString().length());
            Session.getLog().info("Report-->Going to execute");
            ResultSet rs = ps.executeQuery();
            Session.getLog().info("Report-->Executed");
            SimpleHash res = new SimpleHash();
            ArrayList entries = new ArrayList();
            int count2 = 0;
            while (rs.next()) {
                SimpleHash entry = new SimpleHash();
                entries.add(entry);
                int pos = 1;
                for (Result r : this.result) {
                    entry.put(r.getName(), r.getEntry(rs, pos));
                    pos++;
                }
                count2++;
            }
            Session.closeStatement(ps);
            res.put("size", new TemplateString(Integer.toString(count2)));
            Integer id = getId();
            if (cache != null) {
                cache.put(id, res);
            }
            res.put("entries", new TemplateList(entries));
            res.put("entrySet", new EntrySet(entries));
            res.put("next", new PageCalc(entries, 1));
            res.put("prev", new PageCalc(entries, -1));
            res.put("pages", new PageList(entries));
            res.put("id", new TemplateString(id));
            return res;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }

    protected synchronized Integer getId() {
        f118id++;
        return new Integer(f118id);
    }

    protected Report() {
    }

    protected void addSQL(String sql) {
        this.sql = sql;
    }

    public void addResult(String name, String type) {
        this.result.add(new Result(name, type));
    }

    public void addSQLParam(String paramIndex, String type) {
        try {
            this.sqlParams.add(new SQLParam(paramIndex, type));
        } catch (Exception e) {
            Session.getLog().info("Report parsing", e);
        }
    }

    public static Report getReport(String name) {
        try {
            parse();
            return (Report) reports.get(name);
        } catch (Exception e) {
            Session.getLog().info("Report parsing", e);
            return null;
        }
    }

    protected static void parse() throws Exception {
        if (oldName == null) {
            parse(Config.getProperty("CLIENT", "REPORT_FILE"));
        } else {
            parse(oldName);
        }
    }

    protected static void parse(String s) throws Exception {
        Session.getLog().warn("Looking up file: " + s);
        File f = new File(s);
        if (oldName == null || !s.equals(oldName) || lastMod < f.lastModified()) {
            lastMod = f.lastModified();
            oldName = s;
            parse(new InputSource(s));
        }
    }

    public static void parse(InputSource in) throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(in);
        Document doc = parser.getDocument();
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("report");
        System.err.println("elements in file " + list.getLength());
        for (int i = 0; i < list.getLength(); i++) {
            Element r = (Element) list.item(i);
            Report report = new Report();
            reports.put(r.getAttribute("name"), report);
            report.addSQL(r.getElementsByTagName("sql").item(0).getFirstChild().getNodeValue());
            NodeList nodes = r.getElementsByTagName("result");
            for (int j = 0; j < nodes.getLength(); j++) {
                Element result = (Element) nodes.item(j);
                report.addResult(result.getAttribute("name"), result.getAttribute("type"));
            }
            NodeList nodes2 = r.getElementsByTagName("sqlparam");
            for (int j2 = 0; j2 < nodes2.getLength(); j2++) {
                Element result2 = (Element) nodes2.item(j2);
                report.addSQLParam(result2.getAttribute("paramindex"), result2.getAttribute("type"));
            }
        }
    }

    public static void main(String[] args) {
        try {
            ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            Database db = Toolbox.getDB(config);
            parse(args[0]);
            List l = new ArrayList();
            l.add(args[1]);
            Template t = new Template(args[2]);
            PrintWriter p = new PrintWriter(new OutputStreamWriter(System.err));
            t.process(getReport("maki").get(db.getConnection(), l), p);
            p.close();
        } catch (Throwable t2) {
            t2.printStackTrace();
        }
    }
}
