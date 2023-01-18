package psoft.hsphere.tools;

import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.IPDependentResource;

/* loaded from: hsphere.zip:psoft/hsphere/tools/RepostAction.class */
public class RepostAction implements Comparable {
    ResourceId accountId;
    Resource res;

    public RepostAction(ResourceId accountId, Resource res) {
        this.accountId = accountId;
        this.res = res;
    }

    public void repost() throws Exception {
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            try {
                Account newAccount = (Account) Account.get(this.accountId);
                Session.setAccount(newAccount);
                Session.setUser(newAccount.getUser());
                try {
                    if (this.res instanceof IPDependentResource) {
                        IPDependentResource ipdr = (IPDependentResource) this.res;
                        ipdr.ipRestart();
                    }
                } catch (Exception ex) {
                    System.err.println("Error was occurede during recreation:" + this.res.textDump());
                    Session.getLog().error("Error was occurede during recreation:" + this.res.textDump(), ex);
                }
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            } catch (Exception ex2) {
                System.err.println("Unable to repost account:" + this.accountId + "\t[FAILED]");
                Session.getLog().error("Unable to repost account:" + this.accountId, ex2);
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            }
        } catch (Throwable th) {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
            throw th;
        }
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        RepostAction act = (RepostAction) o;
        return hashCode() - act.hashCode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof RepostAction) {
            RepostAction repostAction = (RepostAction) o;
            return this.accountId.equals(repostAction.accountId) && this.res.equals(repostAction.res);
        }
        return false;
    }

    public int hashCode() {
        int result = new Long(this.accountId.getId()).hashCode();
        return (29 * result) + new Long(this.res.getId().getId()).hashCode();
    }
}
