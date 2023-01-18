package psoft.hsphere.admin.signupmanager;

import java.util.Date;
import java.util.Map;
import psoft.hsphere.cache.CacheableObject;

/* loaded from: hsphere.zip:psoft/hsphere/admin/signupmanager/SignupRecord.class */
public interface SignupRecord extends CacheableObject {
    public static final int INCOMPLETE = 0;
    public static final int DONE = 1;
    public static final int ERROR = 2;
    public static final int INPROCESS = 3;

    void compleate();

    boolean isCompleated();

    void error(String str);

    Date getCreated();

    void setCreated(Date date);

    @Override // psoft.hsphere.cache.CacheableObject
    long getId();

    void setId(long j);

    String getIp();

    void setIp(String str);

    String getMsg();

    void setMsg(String str);

    String getCountryCode();

    void setCountryCode(String str);

    long getResellerId();

    int getState();

    void setState(int i);

    Date getUpdated();

    void setUpdated(Date date);

    long getUserId();

    void setUserId(long j);

    long getAccountId();

    void setAccountId(long j);

    Map getValues();

    void setValues(Map map);
}
