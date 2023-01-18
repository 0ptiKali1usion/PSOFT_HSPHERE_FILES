package psoft.hsphere.notes;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/notes/AccountNotes.class */
public class AccountNotes {
    public static TemplateModel getNotes(long accountId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT note, created, created_by FROM account_notes WHERE account_id = ?");
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("note", Session.getClobValue(rs, 1));
                map.put("created", new TemplateString(rs.getTimestamp(2)));
                map.put("created_by", new TemplateString(rs.getString(3)));
                list.add((TemplateModel) map);
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

    public static void addNote(long accountId, String note) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO account_notes (account_id, note, created, created_by) VALUES (?, ?, ?, ?)");
            ps.setLong(1, accountId);
            Session.setClobValue(ps, 2, note);
            ps.setTimestamp(3, TimeUtils.getSQLTimestamp());
            ps.setString(4, Session.getUser().getLogin() + "/" + Session.getAccount().getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
