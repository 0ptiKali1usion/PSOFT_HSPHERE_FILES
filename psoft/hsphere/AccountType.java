package psoft.hsphere;

import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/AccountType.class */
public class AccountType {
    private static final int _UNKNOWN = 0;
    private static final int _ADMIN = 1;
    private static final int _RESELLER = 2;
    private static final int _USER = 3;
    public static final AccountType UNKNOWN = new AccountType(0);
    public static final AccountType ADMIN = new AccountType(1);
    public static final AccountType RESELLER = new AccountType(2);
    public static final AccountType USER = new AccountType(3);
    private int type;

    protected AccountType(int type) {
        this.type = 0;
        this.type = type;
    }

    public static AccountType getType(Account account) throws Exception {
        if (account == null) {
            return UNKNOWN;
        }
        try {
            if (account.FM_getChild(FMACLManager.ADMIN) != null) {
                return ADMIN;
            }
            if (account.FM_getChild(FMACLManager.RESELLER) != null) {
                return RESELLER;
            }
            String createdBy = account.getPlan().getValue("_CREATED_BY_");
            if (createdBy == null || "".equals(createdBy) || FMACLManager.ADMIN.equals(createdBy) || "7".equals(createdBy)) {
                return ADMIN;
            }
            return USER;
        } catch (Exception e) {
            return UNKNOWN;
        }
    }

    public boolean isAdmin() {
        return this.type == 1;
    }

    public boolean isReseller() {
        return this.type == 2;
    }

    public boolean isUser() {
        return this.type == 3;
    }

    public boolean equals(AccountType at) {
        return at != null && this.type == at.type;
    }

    public String toString() {
        switch (this.type) {
            case 1:
                return FMACLManager.ADMIN;
            case 2:
                return FMACLManager.RESELLER;
            case 3:
                return FMACLManager.USER;
            default:
                return "unknown";
        }
    }
}
