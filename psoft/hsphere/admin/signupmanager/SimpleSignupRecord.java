package psoft.hsphere.admin.signupmanager;

import java.util.Date;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/hsphere/admin/signupmanager/SimpleSignupRecord.class */
public class SimpleSignupRecord implements SignupRecord {

    /* renamed from: id */
    protected long f65id;
    protected Date created;

    /* renamed from: ip */
    protected String f66ip;
    protected Date updated;
    protected int state;
    protected long userId;
    protected long resellerId;
    protected long accountId;
    protected String msg;
    protected String countryCode;
    protected Map values;

    public SimpleSignupRecord(long id, long resellerId) {
        this.f65id = id;
        this.resellerId = resellerId;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void compleate() {
        this.state = 1;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public boolean isCompleated() {
        return this.state == 1;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void error(String errorMessage) {
        this.state = 2;
        this.msg = errorMessage;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public Date getCreated() {
        return this.created;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord, psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.f65id;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setId(long id) {
        this.f65id = id;
        if (getValues() != null) {
            getValues().put("signup_id", String.valueOf(id));
        }
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public String getIp() {
        return this.f66ip;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setIp(String ip) {
        this.f66ip = ip;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public String getMsg() {
        return this.msg;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public String getCountryCode() {
        return this.countryCode;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public long getResellerId() {
        return this.resellerId;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public int getState() {
        return this.state;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setState(int state) {
        this.state = state;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public Date getUpdated() {
        return this.updated;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public long getUserId() {
        return this.userId;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public long getAccountId() {
        return this.accountId;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public Map getValues() {
        return this.values;
    }

    @Override // psoft.hsphere.admin.signupmanager.SignupRecord
    public void setValues(Map values) {
        this.values = values;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SimpleSignupRecord) {
            SimpleSignupRecord simpleSignupRecord = (SimpleSignupRecord) o;
            if (this.f65id == simpleSignupRecord.f65id && this.resellerId == simpleSignupRecord.resellerId && this.userId == simpleSignupRecord.userId && this.created.equals(simpleSignupRecord.created)) {
                if (this.f66ip != null) {
                    if (!this.f66ip.equals(simpleSignupRecord.f66ip)) {
                        return false;
                    }
                } else if (simpleSignupRecord.f66ip != null) {
                    return false;
                }
                if (this.msg != null) {
                    if (!this.msg.equals(simpleSignupRecord.msg)) {
                        return false;
                    }
                } else if (simpleSignupRecord.msg != null) {
                    return false;
                }
                if (this.updated != null) {
                    if (!this.updated.equals(simpleSignupRecord.updated)) {
                        return false;
                    }
                } else if (simpleSignupRecord.updated != null) {
                    return false;
                }
                return this.values.equals(simpleSignupRecord.values);
            }
            return false;
        }
        return false;
    }
}
