package psoft.hsphere.resource.moderate_signup;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;

/* loaded from: hsphere.zip:psoft/hsphere/resource/moderate_signup/OtherParameters.class */
public class OtherParameters extends Resource {
    protected TemplateList list = new TemplateList();
    protected Hashtable fakeRequest = new Hashtable();

    public OtherParameters(long uid, long bid, long cid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, value FROM request, request_record WHERE request_record.user_id = ? AND bid = ? AND cid = ? AND request_record.request_id = request.id ORDER BY name");
            ps.setLong(1, uid);
            ps.setLong(2, bid);
            ps.setLong(3, cid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateModel templateMap = new TemplateMap();
                String tmpName = rs.getString(1);
                String tmpValue = rs.getString(2);
                if (tmpName != null && tmpValue != null) {
                    this.fakeRequest.put(tmpName, new String[]{tmpValue});
                }
                if (!tmpName.startsWith("_bi_") && !tmpName.startsWith("_ci_") && !"_bp".equals(tmpName) && !"plan_id".equals(tmpName) && !"_eul_accept".equals(tmpName) && !"password2".equals(tmpName)) {
                    templateMap.put("name", tmpName);
                    templateMap.put("value", tmpValue);
                    this.list.add(templateMap);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("params_list".equals(key)) {
            return this.list;
        }
        return null;
    }

    public Hashtable getTempFakeRequest() {
        return this.fakeRequest;
    }
}
