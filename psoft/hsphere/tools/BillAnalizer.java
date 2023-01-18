package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.Invoice;
import psoft.hsphere.InvoiceEntry;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.User;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/BillAnalizer.class */
public class BillAnalizer {
    long accId;
    boolean processAll;
    Date dateFrom;
    String billingDescription;
    boolean force;
    static final double ANALIZE_INACCURACY_PERCENT = 10.0d;
    static final DateFormat mediumDateFromat = DateFormat.getDateInstance(2);
    static final DateFormat shortDateFromat = DateFormat.getDateInstance(3);

    public BillAnalizer(long accId) throws Exception {
        this(accId, null);
    }

    public BillAnalizer(long accId, Date dateFrom) throws Exception {
        this.processAll = false;
        this.billingDescription = "";
        this.force = false;
        this.accId = accId;
        this.processAll = false;
        this.dateFrom = dateFrom;
        ExternalCP.initCP();
    }

    public BillAnalizer(Date dateFrom) throws Exception {
        this.processAll = false;
        this.billingDescription = "";
        this.force = false;
        this.accId = 0L;
        this.processAll = true;
        this.dateFrom = dateFrom;
        ExternalCP.initCP();
    }

    public static void main(String[] argv) {
        long accId = -1;
        boolean processAll = false;
        Date dateFrom = null;
        int i = 0;
        while (i < argv.length) {
            try {
                if ("-acc".equals(argv[i]) || "--accountId".equals(argv[i])) {
                    try {
                        accId = Long.parseLong(argv[i + 1]);
                        i++;
                    } catch (NumberFormatException e) {
                        System.out.println("Unrecognized account id " + argv[i + 1]);
                    }
                }
                if ("-all".equals(argv[i]) || "--all".equals(argv[i])) {
                    processAll = true;
                }
                if ("-d".equals(argv[i]) || "--date-from".equals(argv[i])) {
                    try {
                        dateFrom = shortDateFromat.parse(argv[i + 1]);
                        i++;
                    } catch (ParseException e2) {
                        System.out.println("Unrecognized date from " + argv[i + 1]);
                    }
                }
                if ("-h".equals(argv[i]) || "--help".equals(argv[i])) {
                    printHelp();
                }
                i++;
            } catch (Throwable th) {
                System.out.println("Finished at " + TimeUtils.getDate());
                throw th;
            }
        }
        BillAnalizer analize = null;
        try {
            if (processAll) {
                analize = new BillAnalizer(dateFrom);
            } else if (accId > 0) {
                analize = new BillAnalizer(accId, dateFrom);
            }
            if (analize != null) {
                analize.m21go();
            }
            System.out.println("Finished at " + TimeUtils.getDate());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
            System.out.println("Finished at " + TimeUtils.getDate());
        }
        System.out.println("Resetting of balance has been finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m21go() throws Exception {
        PreparedStatement ps = null;
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        Connection con = Session.getDb();
        try {
            StringBuffer query = new StringBuffer();
            if (this.processAll) {
                query.append("SELECT a.id FROM accounts a, user_account u, balance_credit b WHERE  a.deleted IS NULL AND u.account_id = a.id AND b.id = a.id ");
            }
            if (this.accId > 0) {
                query.append("SELECT a.id FROM accounts a, user_account u WHERE  a.deleted IS NULL AND u.account_id = a.id AND a.id = ?");
            }
            if (query.length() > 0) {
                ps = con.prepareStatement(query.toString());
                if (this.accId > 0) {
                    ps.setLong(1, this.accId);
                    int i = 1 + 1;
                }
                Session.getLog().info("Starting :");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        Account acc = (Account) Account.get(new ResourceId(rs.getString(1) + "_0"));
                        if (acc != null) {
                            Session.setUser(acc.getUser());
                            Session.setAccount(acc);
                            processAccount(acc);
                        } else {
                            System.out.println("Can not get account #" + rs.getString(1) + " for processing. Please check the account id");
                        }
                        Session.setUser(oldUser);
                        Session.setAccount(oldAcc);
                    } catch (Exception ex) {
                        System.out.println("An error occured while processin account id" + rs.getLong(1) + " see log for more details");
                        Session.getLog().error("An error occured while processin account id" + rs.getLong(1), ex);
                        ex.printStackTrace();
                        Session.setUser(oldUser);
                        Session.setAccount(oldAcc);
                    }
                }
                Session.closeStatement(ps);
                con.close();
                return;
            }
            Session.closeStatement(null);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static boolean checkAmountDiff(double d1, double d2) {
        return checkAmountDiff(d1, d2, ANALIZE_INACCURACY_PERCENT);
    }

    public static boolean checkAmountDiff(double d1, double d2, double inaccuracy) {
        double avg = (Math.abs(d1) + Math.abs(d2)) / 2.0d;
        if (Math.abs(d1 - d2) > (avg * inaccuracy) / 100.0d) {
            return true;
        }
        return false;
    }

    public static String getDescription(ResourceId rid) {
        String description;
        try {
            Resource r = rid.get();
            description = r.getDescription();
        } catch (Exception e) {
            try {
                description = TypeRegistry.getDescription(rid.getType());
            } catch (NoSuchTypeException e2) {
                description = "Unknown type:" + rid.getType();
            }
        }
        return description;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/tools/BillAnalizer$EntriesList.class */
    public class EntriesList {
        protected List messageList = new LinkedList();
        protected Map entries = new Hashtable();

        EntriesList() {
            BillAnalizer.this = r5;
        }

        public List getMessageList() {
            return this.messageList;
        }

        public EntryInfo addEntry(Bill bill, BillEntry entry) {
            if (entry.getResourceId() != null && !entry.getResourceId().isChangeable()) {
                EntryInfo entryInfo = new EntryInfo(entry, bill);
                this.entries.put(entry.getResourceId(), entryInfo);
                return entryInfo;
            }
            return null;
        }

        public void checkInvoice(Invoice invoice) {
            for (InvoiceEntry entry : invoice.getEntries()) {
                if (entry.getType() == 2) {
                    EntryInfo ei = (EntryInfo) this.entries.get(entry.getResourceId());
                    if (ei == null) {
                        this.messageList.add("Estimated fee amount:" + entry.getAmount() + " for " + BillAnalizer.getDescription(entry.getResourceId()) + " is missed in the open bill.");
                    } else {
                        ei.checkInvoiceEntry(entry);
                    }
                }
            }
        }

        public void checkBill(Bill bill) {
            LinkedList processsedSet = new LinkedList();
            for (BillEntry entry : bill.getEntries()) {
                if (entry.getType() == 2) {
                    EntryInfo ei = (EntryInfo) this.entries.get(entry.getResourceId());
                    if (ei == null) {
                        ei = addEntry(bill, entry);
                    } else {
                        ei.checkEntry(entry, bill);
                    }
                    if (ei != null) {
                        processsedSet.add(ei.getResourceId());
                    }
                }
            }
            LinkedList removedEntries = new LinkedList(this.entries.keySet());
            removedEntries.removeAll(processsedSet);
            Iterator i = removedEntries.iterator();
            while (i.hasNext()) {
                ResourceId rid = (ResourceId) i.next();
                ((EntryInfo) this.entries.get(rid)).markAsMissed(bill);
            }
        }

        /* loaded from: hsphere.zip:psoft/hsphere/tools/BillAnalizer$EntriesList$EntryInfo.class */
        public class EntryInfo {
            protected double amount;
            protected double currentAmount;
            protected ResourceId resourceId;
            protected Date created;
            protected int count;
            protected Bill createdBill;
            protected boolean wasReset;
            protected long planId;
            protected long periodId;
            protected Bill missedBill = null;
            protected Bill lastBill = null;
            protected Date deleted = null;
            protected int missedCount = 0;

            public double getAmount() {
                return this.amount;
            }

            public ResourceId getResourceId() {
                return this.resourceId;
            }

            public boolean wasReset() {
                return this.wasReset;
            }

            public String getDescription() {
                return BillAnalizer.getDescription(getResourceId());
            }

            public EntryInfo(BillEntry entry, Bill bill) {
                EntriesList.this = r5;
                this.count = 0;
                this.createdBill = null;
                this.wasReset = false;
                this.amount = entry.getAmount();
                this.created = entry.getOpened();
                this.resourceId = entry.getResourceId();
                this.count = 1;
                this.createdBill = bill;
                this.wasReset = true;
                this.planId = entry.getPlanId();
                this.periodId = entry.getPeriodId();
            }

            public boolean checkEntry(BillEntry entry, Bill bill) {
                if (this.lastBill == null || (this.lastBill != null && this.lastBill.getId() != bill.getId())) {
                    if (this.missedBill != null && this.count != 2) {
                        EntriesList.this.messageList.add("The resource:\"" + getDescription() + "\" disapeared in BILL:[" + this.missedBill.getDescription() + (this.missedBill.opened() != null ? " opened:" + BillAnalizer.mediumDateFromat.format(this.missedBill.opened()) : "") + (this.missedBill.closed() != null ? " closed:" + BillAnalizer.mediumDateFromat.format(this.missedBill.closed()) : "") + "] and was missed in " + this.missedCount + " bill(s).");
                        this.missedBill = null;
                        this.missedCount = 0;
                    }
                    if (this.planId != entry.getPlanId() || this.periodId != entry.getPeriodId()) {
                        this.wasReset = false;
                    } else {
                        long started = entry.getStarted().getTime();
                        long opened = bill.opened().getTime();
                        if (opened - started <= 86400000 && BillAnalizer.checkAmountDiff(entry.getAmount(), this.amount)) {
                            EntriesList.this.messageList.add("The amount " + entry.getAmount() + " on the bill entry \"" + entry.getDescription() + "\" is different than the last charged amount " + this.amount + ".");
                        }
                    }
                }
                this.amount = entry.getAmount();
                this.planId = entry.getPlanId();
                this.periodId = entry.getPeriodId();
                this.count++;
                this.lastBill = bill;
                return true;
            }

            public boolean checkInvoiceEntry(InvoiceEntry entry) {
                if (this.missedBill != null && this.count != 2) {
                    EntriesList.this.messageList.add("The resource:\"" + getDescription() + "\" disapeared in BILL:[" + this.missedBill.getDescription() + (this.missedBill.opened() != null ? " opened:" + BillAnalizer.mediumDateFromat.format(this.missedBill.opened()) : "") + (this.missedBill.closed() != null ? " closed:" + BillAnalizer.mediumDateFromat.format(this.missedBill.closed()) : "") + "] and was missed in " + this.missedCount + " bill(s).");
                    this.missedBill = null;
                    this.missedCount = 0;
                }
                if (BillAnalizer.checkAmountDiff(entry.getAmount(), this.amount)) {
                    EntriesList.this.messageList.add("The estimated amount " + entry.getAmount() + " on the resource \"" + getDescription() + "\" is different than the previosly charged amount " + this.amount + ".");
                }
                this.amount = entry.getAmount();
                this.count++;
                return true;
            }

            public void markAsMissed(Bill bill) {
                this.missedBill = bill;
                this.missedCount++;
            }
        }
    }

    private void processAccount(Account account) {
        try {
            EntriesList entriesList = new EntriesList();
            Iterator i = account.getBills();
            while (i.hasNext()) {
                Bill bill = (Bill) i.next();
                if (this.dateFrom != null && bill.opened().after(this.dateFrom)) {
                    entriesList.checkBill(bill);
                } else if (this.dateFrom == null) {
                    entriesList.checkBill(bill);
                }
            }
            Invoice invoice = account.estimateOpenPeriod(account.getPlan(), account.getPeriodId());
            entriesList.checkInvoice(invoice);
            if (entriesList.getMessageList().size() > 0) {
                System.out.println("ACCOUNT:" + account.getId().getId() + " description:" + account.getDescription());
                for (int i2 = 0; i2 < entriesList.getMessageList().size(); i2++) {
                    String message = (String) entriesList.getMessageList().get(i2);
                    System.out.println("\t" + message);
                }
            }
        } catch (Exception ex) {
            System.err.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void printHelp() {
        System.out.println("NAME:\n\tpsoft.hsphere.tools.BillAnalizer \n\tTo analize account's bills");
        System.out.println("SYNOPSIS:\n\tpsoft.hsphere.tools.BillAnalizer options");
        System.out.println("OPTIONS:");
        System.out.println("\t-h|--help \t\t\t- shows this screen");
        System.out.println("\t-acc|--acountId number\t\t- process only account with given number");
        System.out.println("\t-all|--all \t\t\t- process all account");
        System.out.println("\t-d|--from-date \t\t\t- process all account from date");
        System.exit(0);
    }
}
