package psoft.user;

import freemarker.template.TemplateHashModel;
import java.util.Date;
import psoft.validators.Accessor;

/* loaded from: hsphere.zip:psoft/user/User.class */
public interface User extends TemplateHashModel {
    String getLogin();

    String getPassword();

    String getEmail();

    int getId();

    boolean login(String str, String str2);

    Accessor getAccessor();

    Date getTimeStamp();

    void setTimeStamp(Date date);
}
