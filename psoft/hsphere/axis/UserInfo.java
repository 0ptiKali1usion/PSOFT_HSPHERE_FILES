package psoft.hsphere.axis;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/axis/UserInfo.class */
public class UserInfo implements Serializable {
    protected String username;
    protected String accountId;
    protected String resellerId;
    protected String created;
    protected String plan;
    protected String email;
    protected String billingType;
    protected String balance;
    protected String credit;
    protected String pEnd;
    protected String accountDescription;
    protected String suspended;
    protected String deleted;
    protected String domain;
    protected String domainType;

    public UserInfo() {
    }

    public UserInfo(String username, String accountId, String created, String plan, String email, String billingType, String balance, String credit, String pEnd, String accountDescription, String suspended, String deleted, String resellerId, String domain, String domainType) {
        this.username = username;
        this.accountId = accountId;
        this.created = created;
        this.plan = plan;
        this.email = email;
        this.billingType = billingType;
        this.balance = balance;
        this.credit = credit;
        this.pEnd = pEnd;
        this.accountDescription = accountDescription;
        this.suspended = suspended;
        this.deleted = deleted;
        this.resellerId = resellerId;
        this.domain = domain;
        this.domainType = domainType;
    }

    public String getUserName() {
        return this.username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResellerId() {
        return this.resellerId;
    }

    public void setResellerId(String resellerId) {
        this.resellerId = resellerId;
    }

    public String getCreated() {
        return this.created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getPlan() {
        return this.plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBillingType() {
        return this.billingType;
    }

    public void setBillingType(String billingType) {
        this.billingType = billingType;
    }

    public String getBalance() {
        return this.balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getCredit() {
        return this.credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getPEnd() {
        return this.pEnd;
    }

    public void setPEnd(String pEnd) {
        this.pEnd = pEnd;
    }

    public String getAccountDescription() {
        return this.accountDescription;
    }

    public void setAccountDescription(String accountDescription) {
        this.accountDescription = accountDescription;
    }

    public String getSuspended() {
        return this.suspended;
    }

    public void setSuspended(String suspended) {
        this.suspended = suspended;
    }

    public String getDeleted() {
        return this.deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomainType() {
        return this.domainType;
    }

    public void setDomainType(String domainType) {
        this.domainType = domainType;
    }
}
