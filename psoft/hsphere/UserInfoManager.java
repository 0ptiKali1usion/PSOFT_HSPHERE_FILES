package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import psoft.util.freemarker.ResultSetModel;

/* loaded from: hsphere.zip:psoft/hsphere/UserInfoManager.class */
public class UserInfoManager extends SimpleDynamicMethod implements TemplateHashModel {

    /* renamed from: u */
    psoft.user.User f59u;

    public UserInfoManager(psoft.user.User u) {
        super("user.info");
        this.f59u = u;
    }

    @Override // psoft.hsphere.SimpleDynamicMethod
    public TemplateModel get(String key) throws TemplateModelException {
        if ("update".equals(key)) {
            return new UserInfoUpdater(this.f59u);
        }
        TemplateModel tm = this.f59u.get(key);
        return null != tm ? tm : super.get(key);
    }

    @Override // psoft.hsphere.SimpleDynamicMethod
    public boolean isEmpty() {
        return false;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/UserInfoManager$UserInfoUpdater.class */
    class UserInfoUpdater implements TemplateMethodModel {

        /* renamed from: u */
        psoft.user.User f60u;

        public UserInfoUpdater(psoft.user.User u) {
            UserInfoManager.this = r4;
            this.f60u = u;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) {
            PreparedStatement ps = null;
            Connection con = null;
            try {
                try {
                    con = Session.getDb();
                    ps = con.prepareStatement("select * from users where user_id = " + this.f60u.getId());
                    ResultSetModel resultSetModel = new ResultSetModel(ps.executeQuery());
                    Session.closeStatement(ps);
                    if (con != null) {
                        try {
                            con.close();
                        } catch (SQLException e) {
                            Session.getLog().error("Error closing connection", e);
                        }
                    }
                    return resultSetModel;
                } catch (SQLException e2) {
                    e2.printStackTrace();
                    Session.closeStatement(ps);
                    if (con != null) {
                        try {
                            con.close();
                        } catch (SQLException e3) {
                            Session.getLog().error("Error closing connection", e3);
                        }
                    }
                    return null;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException e4) {
                        Session.getLog().error("Error closing connection", e4);
                    }
                }
                throw th;
            }
        }
    }
}
