package psoft.knowledgebase;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.index.IndexWriter;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Downloadable;
import psoft.hsphere.Session;
import psoft.util.IOUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.yafv.yafvsym;

/* loaded from: hsphere.zip:psoft/knowledgebase/KnowledgeBase.class */
public class KnowledgeBase implements TemplateHashModel, Downloadable {
    private static Map map = new HashMap();
    private static String root = null;
    KnowledgeBaseIndex index;
    private String name;

    /* renamed from: id */
    private int f243id;
    private long resellerId;
    private Map entries = new WeakHashMap();
    private Map categoryMap = new HashMap();

    public long getResellerId() {
        return this.resellerId;
    }

    private static String getRoot() {
        if (root == null) {
            root = Session.getPropertyString("KB_PATH");
            if (root == null || root.length() == 0) {
                root = "/hsphere/local/home/cpanel/.kb";
            }
        }
        return root;
    }

    private static File getIndexPath(int id) {
        return new File(getRoot(), Integer.toString(id));
    }

    private int getCategoryId(int eid) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT cat_id FROM kb_entry WHERE kb_id = ? and id = ?");
            ps.setInt(1, this.f243id);
            ps.setInt(2, eid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new Exception("Entry #" + eid + " not found");
            }
            int i = rs.getInt(1);
            Session.closeStatement(ps);
            con.close();
            return i;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    protected void reIndex(KBEntry e) throws IOException {
        this.index.remove(e.getId());
        this.index.add(e.getDocument());
    }

    public static void changeKBName(int id, String newName) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE knowledgebase SET name = ? WHERE id = ?");
            ps.setString(1, newName);
            ps.setInt(2, id);
            ps.executeQuery();
            Session.closeStatement(ps);
            con.close();
            KnowledgeBase kb = getKB(id);
            kb.name = newName;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void deleteKB(int id) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM kb_entry WHERE kb_id = ?");
            ps2.setInt(1, id);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM kb_category WHERE kb_id = ?");
            ps3.setInt(1, id);
            ps3.executeUpdate();
            Session.closeStatement(ps3);
            ps = con.prepareStatement("DELETE FROM knowledgebase WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            map.remove(new Integer(id));
            IOUtils.rmdir(getIndexPath(id));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized KnowledgeBase getKB(int id) throws Exception {
        KnowledgeBase kb = (KnowledgeBase) map.get(new Integer(id));
        if (kb != null && kb.getResellerId() == Session.getResellerId()) {
            return kb;
        }
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT name, reseller_id FROM knowledgebase WHERE id = ? AND reseller_id = ?");
            ps.setInt(1, id);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                KnowledgeBase kb2 = new KnowledgeBase(id, rs.getString(1), rs.getLong(2));
                map.put(new Integer(id), kb2);
                Session.closeStatement(ps);
                con.close();
                return kb2;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static synchronized KnowledgeBase getReadOnlyKB(int id) throws Exception {
        KnowledgeBase kb = (KnowledgeBase) map.get(new Integer(id));
        if (kb != null && (kb.getResellerId() == Session.getResellerId() || kb.getResellerId() == 1)) {
            return kb;
        }
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT name, reseller_id FROM knowledgebase WHERE id = ? AND (reseller_id = ? OR reseller_id = 1)");
            ps.setInt(1, id);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                KnowledgeBase kb2 = new KnowledgeBase(id, rs.getString(1), rs.getLong(2));
                map.put(new Integer(id), kb2);
                Session.closeStatement(ps);
                con.close();
                return kb2;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void reindexKB(int id) throws Exception {
        File indexPath = getIndexPath(id);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, cat_id, q, a FROM kb_entry WHERE kb_id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            synchronized (KnowledgeBaseIndex.class) {
                IndexWriter writer = null;
                try {
                    writer = KnowledgeBaseIndex.init(indexPath);
                    while (rs.next()) {
                        KBEntry kb = new KBEntry(rs.getInt(1), rs.getInt(2), Session.getClobValue(rs, 3), Session.getClobValue(rs, 4));
                        writer.addDocument(kb.getDocument());
                    }
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Throwable th) {
                    if (writer != null) {
                        writer.close();
                    }
                    throw th;
                }
            }
            map.remove(new Integer(id));
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public static KnowledgeBase createKB(String name) throws Exception {
        int id = Session.getNewId("kb_seq");
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO knowledgebase (id, reseller_id, name) VALUES (?, ?, ?)");
            ps.setInt(1, id);
            ps.setLong(2, Session.getResellerId());
            ps.setString(3, name);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            File indexPath = getIndexPath(id);
            indexPath.mkdirs();
            KnowledgeBaseIndex.init(indexPath).close();
            KnowledgeBase kb = new KnowledgeBase(id, name, Session.getResellerId());
            map.put(new Integer(id), kb);
            return kb;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void loadCategories() throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, name FROM kb_category WHERE kb_id = ?");
            ps.setInt(1, this.f243id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.categoryMap.put(rs.getString(1), rs.getString(2));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public KnowledgeBase(int id, String name, long resellerId) throws IOException, SQLException {
        this.f243id = id;
        this.name = name;
        this.index = new KnowledgeBaseIndex(getIndexPath(id));
        this.resellerId = resellerId;
        loadCategories();
    }

    public void changeCategoryName(int catId, String name) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE kb_category SET name = ? WHERE id = ?");
            ps.setString(1, name);
            ps.setInt(2, this.f243id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int addCategory(String name) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        int catId = Session.getNewId("kb_seq");
        try {
            ps = con.prepareStatement("INSERT INTO kb_category (id, kb_id, name) VALUES (?, ?, ?)");
            ps.setInt(1, catId);
            ps.setInt(2, this.f243id);
            ps.setString(3, name);
            ps.executeUpdate();
            this.categoryMap.put(Integer.toString(catId), name);
            Session.closeStatement(ps);
            con.close();
            return catId;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void removeEntry(int id) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM kb_entry WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.index.remove(id);
            this.entries.remove(new Integer(id));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List search(String query, int max) throws Exception {
        return this.index.search(query, max);
    }

    public List search(String query) throws Exception {
        return this.index.search(query);
    }

    public List getEntriesByCat(int catId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, q, a FROM kb_entry WHERE kb_id = ? AND cat_id = ?");
            ps.setInt(1, this.f243id);
            ps.setInt(2, catId);
            ResultSet rs = ps.executeQuery();
            List list = new ArrayList();
            while (rs.next()) {
                int eid = rs.getInt(1);
                list.add(rs.getString(1));
                this.entries.put(new Integer(eid), new KBEntry(eid, catId, Session.getClobValue(rs, 2), Session.getClobValue(rs, 3)));
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public KBEntry changeCategory(int eid, int newCatId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE kb_entry SET cat_id = ? WHERE id = ? AND kb_id = ?");
            ps.setInt(1, newCatId);
            ps.setInt(2, eid);
            ps.setInt(3, this.f243id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            KBEntry kb = getEntryById(eid);
            kb.setCategoryId(newCatId);
            reIndex(kb);
            this.entries.put(new Integer(eid), kb);
            return kb;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public KBEntry updateEntry(int eid, String question, String answer) throws Exception {
        int catId = getCategoryId(eid);
        KBEntry kb = new KBEntry(eid, catId, question, answer);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE kb_entry SET q = ?, a = ? WHERE id = ? AND kb_id = ?");
            Session.setClobValue(ps, 1, question);
            Session.setClobValue(ps, 2, answer);
            ps.setInt(3, eid);
            ps.setInt(4, this.f243id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            reIndex(kb);
            this.entries.put(new Integer(eid), kb);
            return kb;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public KBEntry getEntryById(int eid) throws Exception {
        KBEntry kb = (KBEntry) this.entries.get(new Integer(eid));
        if (kb != null) {
            return kb;
        }
        PreparedStatement kb2 = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement kb3 = con.prepareStatement("SELECT cat_id, q, a FROM kb_entry WHERE id = ? AND kb_id = ?");
            kb3.setInt(1, eid);
            kb3.setInt(2, this.f243id);
            ResultSet rs = kb3.executeQuery();
            if (rs.next()) {
                kb = new KBEntry(eid, rs.getInt(1), Session.getClobValue(rs, 2), Session.getClobValue(rs, 3));
                this.entries.put(new Integer(eid), kb);
            }
            return kb2;
        } finally {
            Session.closeStatement(kb2);
            con.close();
        }
    }

    public KBEntry addEntry(String question, String answer, int category) throws Exception {
        int eid = Session.getNewId("kb_seq");
        KBEntry e = new KBEntry(eid, category, question, answer);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO kb_entry (id, cat_id, kb_id, q, a) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, eid);
            ps.setInt(2, category);
            ps.setInt(3, this.f243id);
            Session.setClobValue(ps, 4, question);
            Session.setClobValue(ps, 5, answer);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.index.add(e.getDocument());
            this.entries.put(new Integer(eid), e);
            return e;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return "id".equals(key) ? new TemplateString(this.f243id) : "name".equals(key) ? new TemplateString(this.name) : "category".equals(key) ? new TemplateMap(this.categoryMap) : "category_list".equals(key) ? new TemplateList(this.categoryMap.keySet()) : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public KBEntry FM_addEntry(String question, String answer, int category) throws Exception {
        return addEntry(question, answer, category);
    }

    public KBEntry FM_getEntryById(int eid) throws Exception {
        return getEntryById(eid);
    }

    public KBEntry FM_updateEntry(int eid, String question, String answer) throws Exception {
        return updateEntry(eid, question, answer);
    }

    public KBEntry FM_changeCategory(int eid, int newCatId) throws Exception {
        return changeCategory(eid, newCatId);
    }

    public TemplateList FM_getEntriesByCat(int catId) throws Exception {
        return new TemplateList(getEntriesByCat(catId));
    }

    public TemplateList FM_filteredSearch(String query, int max) throws Exception {
        if (query == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < query.length(); i++) {
            char ch = query.charAt(i);
            switch (ch) {
                case yafvsym.f288EQ /* 33 */:
                case yafvsym.NEQ /* 34 */:
                case yafvsym.f287IF /* 38 */:
                case yafvsym.STRINGLITERAL /* 40 */:
                case yafvsym.INTEGER_CONSTANT /* 41 */:
                case yafvsym.FLOAT_CONSTANT /* 42 */:
                case yafvsym.BOOL_CONSTANT /* 43 */:
                case '-':
                case ':':
                case '?':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                case '~':
                    buf.append('\\');
                    break;
            }
            buf.append(ch);
        }
        return FM_search(buf.toString(), max);
    }

    public TemplateList FM_search(String query, int max) throws Exception {
        return FM_search(query, max, 0);
    }

    public TemplateList FM_search(String query, int max, int cat) throws Exception {
        List l = this.index.search(query, max, cat);
        if (l.size() == 0) {
            return null;
        }
        return new TemplateList(l);
    }

    public TemplateList FM_search(String query) throws Exception {
        List l = this.index.search(query);
        if (l.size() == 0) {
            return null;
        }
        return new TemplateList(l);
    }

    public void FM_removeEntry(int id) throws Exception {
        removeEntry(id);
    }

    public TemplateModel FM_delCategory(int category) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM kb_entry WHERE cat_id = ? AND kb_id = ?");
            ps2.setInt(1, category);
            ps2.setInt(2, this.f243id);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("DELETE FROM kb_category WHERE id = ? AND kb_id = ?");
            ps.setInt(1, category);
            ps.setInt(2, this.f243id);
            ps.executeUpdate();
            this.categoryMap.remove(Integer.toString(category));
            this.entries.clear();
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateString FM_addCategory(String name) throws Exception {
        return new TemplateString(addCategory(name));
    }

    public TemplateModel FM_optimize() throws IOException {
        this.index.optimize();
        return null;
    }

    public void FM_changeCategoryName(int catId, String name) throws Exception {
        changeCategoryName(catId, name);
    }

    @Override // psoft.hsphere.Downloadable
    public void process(HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            String fname = this.name != null ? this.name + ".jar" : "unknown_kb.jar";
            response.setHeader("Content-disposition", "attachment; filename=\"" + fname + "\"");
            response.setContentType("appication/x-psoft-knowledgebase");
            response.setHeader("Cache-Control", "no-cache, must-revalidate");
            response.setHeader("Connection", "close");
            response.setHeader("Expires", "0");
            export(outputStream);
            outputStream.flush();
        } catch (SQLException se) {
            response.setContentType("text/plain");
            PrintWriter p = new PrintWriter((OutputStream) outputStream);
            p.println("Unable to export knowledgebase, try again later");
            se.printStackTrace(p);
            p.flush();
            outputStream.flush();
        }
    }

    protected static String readStr(JarInputStream in, ZipEntry e) throws IOException {
        if (e.getSize() > 0) {
            byte[] b = new byte[(int) e.getSize()];
            in.read(b, 0, b.length);
            return new String(b);
        }
        StringBuffer buf = new StringBuffer();
        while (true) {
            int ch = in.read();
            if (ch != -1) {
                buf.append((char) ch);
            } else {
                return buf.toString();
            }
        }
    }

    public static KnowledgeBase importData(InputStream is) throws Exception {
        ZipEntry entry;
        JarInputStream in = new JarInputStream(is);
        KnowledgeBase kb = createKB(readStr(in, in.getNextEntry()));
        HashMap map2 = new HashMap();
        while (true) {
            in.closeEntry();
            entry = in.getNextEntry();
            if (entry != null && entry.getName().startsWith("c.")) {
                map2.put(entry.getName().substring(2), new Integer(kb.addCategory(readStr(in, entry))));
            }
        }
        while (entry != null) {
            String q = readStr(in, entry);
            in.closeEntry();
            int catId = ((Integer) map2.get(readStr(in, in.getNextEntry()))).intValue();
            in.closeEntry();
            String a = readStr(in, in.getNextEntry());
            kb.addEntry(q, a, catId);
            in.closeEntry();
            entry = in.getNextEntry();
        }
        return kb;
    }

    protected void export(OutputStream os) throws IOException, SQLException {
        JarOutputStream out = new JarOutputStream(os);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ZipEntry entry = new ZipEntry("kb-name");
            byte[] buf = this.name.getBytes();
            entry.setSize(buf.length);
            out.putNextEntry(entry);
            out.write(buf);
            out.closeEntry();
            PreparedStatement ps2 = con.prepareStatement("SELECT name, id FROM kb_category WHERE kb_id = ? ORDER BY id");
            ps2.setInt(1, this.f243id);
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                ZipEntry entry2 = new ZipEntry("c." + rs.getString(2));
                byte[] buf2 = rs.getString(1).getBytes();
                entry2.setSize(buf2.length);
                out.putNextEntry(entry2);
                out.write(buf2);
                out.closeEntry();
            }
            ps2.close();
            ps = con.prepareStatement("SELECT cat_id, id, q, a FROM kb_entry WHERE kb_id = ?");
            ps.setInt(1, this.f243id);
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                ZipEntry entry3 = new ZipEntry("q." + rs2.getString(2));
                entry3.setComment(rs2.getString(1));
                byte[] buf3 = Session.getClobValue(rs2, 3).getBytes();
                entry3.setSize(buf3.length);
                out.putNextEntry(entry3);
                out.write(buf3);
                out.closeEntry();
                out.putNextEntry(new ZipEntry("i." + rs2.getString(2)));
                out.write(rs2.getString(1).getBytes());
                out.closeEntry();
                ZipEntry entry4 = new ZipEntry("a." + rs2.getString(2));
                byte[] buf4 = Session.getClobValue(rs2, 4).getBytes();
                entry4.setSize(buf4.length);
                out.putNextEntry(entry4);
                out.write(buf4);
                out.closeEntry();
            }
            Session.closeStatement(ps);
            con.close();
            out.finish();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
