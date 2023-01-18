package psoft.knowledgebase;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.hsphere.Uploader;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplatePair;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/knowledgebase/KnowledgeBaseManager.class */
public class KnowledgeBaseManager implements TemplateHashModel {
    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateList FM_listKB() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, name FROM knowledgebase WHERE reseller_id = ? ");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                list.add((TemplateModel) new TemplatePair(rs.getString(1), rs.getString(2)));
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

    public TemplateList FM_listReadOnlyKB() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, name FROM knowledgebase WHERE reseller_id = ? OR reseller_id =1");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                list.add((TemplateModel) new TemplatePair(rs.getString(1), rs.getString(2)));
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

    public void FM_changeKBName(int id, String newName) throws Exception {
        KnowledgeBase.changeKBName(id, newName);
    }

    public void FM_deleteKB(int id) throws Exception {
        KnowledgeBase.deleteKB(id);
    }

    public KnowledgeBase FM_getReadOnlyKB(int id) throws Exception {
        return KnowledgeBase.getReadOnlyKB(id);
    }

    public KnowledgeBase FM_getKB(int id) throws Exception {
        return KnowledgeBase.getKB(id);
    }

    public KnowledgeBase FM_createKB(String name) throws Exception {
        return KnowledgeBase.createKB(name);
    }

    public TemplateModel FM_reindex(int id) throws Exception {
        KnowledgeBase.reindexKB(id);
        return null;
    }

    public TemplateModel FM_export(int id) throws Exception {
        return new TemplateString(Session.setDownload("kb-", KnowledgeBase.getKB(id)));
    }

    public TemplateModel FM_import(String key) throws Exception {
        Session.getLog().info("----------KB->:inside FM_import:" + key);
        return KnowledgeBase.importData(Uploader.getFile(key));
    }
}
