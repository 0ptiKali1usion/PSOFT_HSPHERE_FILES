package psoft.hsphere.resource.admin;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.billing.AnonymousBillViewer;
import psoft.hsphere.global.Globals;
import psoft.hsphere.promotion.Promo;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.hsphere.util.XMLManager;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/BillingManager.class */
public class BillingManager extends Resource {
    public BillingManager(int type, Collection init) throws Exception {
        super(type, init);
    }

    public BillingManager(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_addCredit(String str_amount, String description, String text, String id, long accountId) throws Exception {
        return FM_addCredit(str_amount, description, text, id, accountId, null);
    }

    public TemplateModel FM_addCredit(String str_amount, String description, String text, String id, long accountId, String inclTaxes) throws Exception {
        double amount = USFormat.parseDouble(str_amount);
        Session.getLog().debug("Amount ----> " + String.valueOf(amount));
        boolean includeTaxes = "1".equals(inclTaxes);
        Date now = TimeUtils.getDate();
        Session.setBillingNote(description);
        Account.addCredit(accountId, amount, Localizer.translateMessage("bill.b_credit", new Object[]{text, id}), now, includeTaxes);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO payment (account_id, amount, id, description, short_desc, entered) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, accountId);
            ps.setDouble(2, amount);
            if (id == null || "".equals(id)) {
                ps.setNull(3, 12);
            } else {
                if (id.length() > 254) {
                    id = id.substring(0, 254);
                }
                ps.setString(3, id);
            }
            if (description == null || "".equals(description)) {
                ps.setNull(4, 12);
            } else {
                if (description.length() > 254) {
                    description = description.substring(0, 254);
                }
                ps.setString(4, description);
            }
            if (text == null || "".equals(text)) {
                ps.setNull(5, 12);
            } else {
                if (text.length() > 99) {
                    text = text.substring(0, 99);
                }
                ps.setString(5, text);
            }
            ps.setDate(6, new java.sql.Date(now.getTime()));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addDebit(String str_amount, String description, String note, String adminNote, long accountId, boolean isInvoice) throws Exception {
        return FM_addDebit(str_amount, description, note, adminNote, accountId, isInvoice, false);
    }

    public TemplateModel FM_addDebit(String str_amount, String description, String note, String adminNote, long accountId, boolean isInvoice, boolean inclTaxes) throws Exception {
        Session.getLog().debug("INSIDE addDebit inclTaxes = " + inclTaxes);
        double amount = USFormat.parseDouble(str_amount);
        Date now = TimeUtils.getDate();
        Account oldAccount = Session.getAccount();
        Account a = (Account) Account.get(new ResourceId(accountId, 0));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Session.setAccount(a);
            Session.setBillingNote(note);
            BillEntry entry = a.getBill().addEntry(inclTaxes ? 16 : 11, now, -1L, -1, description, now, (Date) null, (String) null, amount);
            ps = con.prepareStatement("INSERT INTO payment (account_id, amount, id, description, short_desc, entered, entry_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, accountId);
            ps.setDouble(2, amount);
            ps.setString(3, adminNote);
            ps.setString(4, description);
            ps.setString(5, note);
            ps.setDate(6, new java.sql.Date(now.getTime()));
            ps.setLong(7, entry.getId());
            ps.executeUpdate();
            Bill bill = a.getBill();
            if (bill.getToPay() > a.getPlan().getCredit()) {
                bill.charge(a.getBillingInfo());
            }
            Session.closeStatement(ps);
            con.close();
            Session.resetBillingNote();
            Session.setAccount(oldAccount);
            if (isInvoice) {
                a.sendInvoice(now);
            }
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            Session.resetBillingNote();
            Session.setAccount(oldAccount);
            if (isInvoice) {
                a.sendInvoice(now);
            }
            throw th;
        }
    }

    public TemplateModel FM_addDebit(String str_amount, String description, String note, String adminNote, long accountId) throws Exception {
        return FM_addDebit(str_amount, description, note, adminNote, accountId, true);
    }

    public TemplateModel FM_getCustomEntries(String key, long accountId) throws Exception {
        Account oldAccount = Session.getAccount();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Account a = (Account) Account.get(new ResourceId(accountId, 0));
            Session.setAccount(a);
            if ("debits".equals(key)) {
                ps = con.prepareStatement("SELECT b.bill_id, b.id FROM bill_entry b, bill WHERE b.bill_id = bill.id AND bill.account_id = ? AND b.type = ?");
                ps.setLong(1, accountId);
                ps.setInt(2, 11);
                ResultSet rs = ps.executeQuery();
                List customDebits = new ArrayList();
                while (rs.next()) {
                    try {
                        Bill bi = a.getBill(rs.getLong(1));
                        if (bi != null) {
                            BillEntry entry = bi.getBillEntry(rs.getLong(2));
                            customDebits.add(entry);
                        }
                    } catch (Exception e) {
                        Session.getLog().error("ERROR getting custom bills:", e);
                    }
                }
                TemplateList templateList = new TemplateList(customDebits);
                Session.setAccount(oldAccount);
                Session.closeStatement(ps);
                con.close();
                return templateList;
            } else if ("services".equals(key)) {
                TemplateList templateList2 = new TemplateList(a.getServices());
                Session.setAccount(oldAccount);
                Session.closeStatement(null);
                con.close();
                return templateList2;
            } else if ("current_bill".equals(key)) {
                TemplateModel templateModel = a.getBill().get("id");
                Session.setAccount(oldAccount);
                Session.closeStatement(null);
                con.close();
                return templateModel;
            } else if (!"custom_resources".equals(key)) {
                Session.setAccount(oldAccount);
                Session.closeStatement(null);
                con.close();
                return null;
            } else {
                new ArrayList();
                PreparedStatement ps2 = con.prepareStatement("SELECT child_id, child_type FROM parent_child  WHERE account_id = ? AND child_type = ?");
                ps2.setLong(1, accountId);
                ps2.setInt(2, Integer.parseInt(TypeRegistry.getTypeId("custom_billing")));
                ResultSet rs2 = ps2.executeQuery();
                List customBills = new ArrayList();
                while (rs2.next()) {
                    ResourceId customResId = new ResourceId(rs2.getLong(1), rs2.getInt(2));
                    TemplateHash hs = new TemplateHash();
                    hs.put("id", customResId.get("id"));
                    hs.put("description", customResId.get("custom_desc"));
                    hs.put("note", customResId.get("custom_note"));
                    hs.put("admin_note", customResId.get("custom_admin_note"));
                    hs.put("price", customResId.get("price"));
                    customBills.add(hs);
                }
                TemplateList templateList3 = new TemplateList(customBills);
                Session.setAccount(oldAccount);
                Session.closeStatement(ps2);
                con.close();
                return templateList3;
            }
        } catch (Throwable th) {
            Session.setAccount(oldAccount);
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_enlargeCredit(long accountId, String str_amount) throws Exception {
        double amount = USFormat.parseDouble(str_amount);
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Account a = (Account) Account.get(new ResourceId(accountId, 0));
            Bill bill = a.getBill();
            synchronized (a) {
                bill.setCredit(amount);
            }
            return this;
        } finally {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
        }
    }

    public TemplateModel FM_getCredit(long accountId) throws Exception {
        Account oldAccount = Session.getAccount();
        try {
            Account a = (Account) Account.get(new ResourceId(accountId, 0));
            Bill bill = a.getBill();
            TemplateModel templateModel = bill.get("customCredit");
            Session.setAccount(oldAccount);
            return templateModel;
        } catch (Throwable th) {
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public TemplateModel FM_getMothlyChurnRate(int year, int month) throws Exception {
        if (year == 0 && month == 0) {
            return new MonthlyChurnRate();
        }
        return new MonthlyChurnRate(year, month);
    }

    public TemplateModel FM_getMothlyRevenu(int year, int month) throws Exception {
        if (year == 0 && month == 0) {
            return new MonthlyRevenue();
        }
        return new MonthlyRevenue(year, month);
    }

    public TemplateModel FM_setNewPeriodBegin(long accountId, String newDate) throws Exception {
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Account a = (Account) Account.get(new ResourceId(accountId, 0));
            Session.setUser(a.getUser());
            Session.setAccount(a);
            try {
                Date newPeriodBegin = DateFormat.getDateInstance(3).parse(newDate);
                synchronized (a) {
                    a.setNewPeriodBegin(newPeriodBegin);
                }
                return this;
            } catch (ParseException e) {
                throw new HSUserException(Localizer.translateMessage("set_periodbegin.dateformat_failed", new Object[]{newDate}));
            }
        } finally {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
        }
    }

    public TemplateModel FM_getPeriodBegin(long accountId) throws Exception {
        Account oldAccount = Session.getAccount();
        try {
            Account a = (Account) Account.get(new ResourceId(accountId, 0));
            TemplateString templateString = new TemplateString(DateFormat.getDateInstance(3).format(a.getPeriodBegin()));
            Session.setAccount(oldAccount);
            return templateString;
        } catch (Throwable th) {
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public TemplateModel FM_addTax(double percent, String description, String country, String state, int outsideCountry, int outsideState) throws Exception {
        Session.getReseller().addTax(percent, description, country, state, outsideCountry, outsideState);
        return this;
    }

    public TemplateModel FM_delTax(long id) throws Exception {
        Session.getReseller().delTax(id);
        return this;
    }

    public TemplateModel FM_addCustomResource(long accountId, String description, String note, String adminNote, String strPrice) throws Exception {
        try {
            double price = USFormat.parseDouble(strPrice);
            User oldUser = Session.getUser();
            Account oldAccount = Session.getAccount();
            Account newAccount = null;
            double credit = 0.0d;
            try {
                newAccount = (Account) Account.get(new ResourceId(accountId, 0));
                Session.setUser(newAccount.getUser());
                Session.setAccount(newAccount);
                credit = newAccount.getBill().getCustomCredit();
                newAccount.getBill().setCredit(100000.0d);
                List values = new ArrayList();
                values.add(description);
                values.add(note);
                values.add(adminNote);
                values.add(Double.toString(price));
                synchronized (Session.getAccount()) {
                    newAccount.addChild("custom_billing", "", values);
                }
                newAccount.getBill().setCredit(credit);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                return this;
            } catch (Throwable th) {
                newAccount.getBill().setCredit(credit);
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                throw th;
            }
        } catch (NumberFormatException e) {
            throw new HSUserException("toolbox.incorrect_number");
        }
    }

    public TemplateModel FM_delCustomResource(long accountId, long resourceId) throws Exception {
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            Account newAccount = (Account) Account.get(new ResourceId(accountId, 0));
            Session.setUser(newAccount.getUser());
            Session.setAccount(newAccount);
            ResourceId resId = new ResourceId(resourceId, Integer.parseInt(TypeRegistry.getTypeId("custom_billing")));
            if (resId == null) {
                throw new HSUserException(Localizer.translateMessage("resource.invalid_resource_id", new Object[]{String.valueOf(resourceId)}));
            }
            Resource res = resId.get();
            synchronized (Session.getAccount()) {
                res.delete(false);
            }
            return this;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    public TemplateModel FM_addService(long accountId, int billingType, int frequency, int duration, String description, String note, String adminNote, String strPrice) throws Exception {
        return FM_addService(accountId, billingType, frequency, duration, description, note, adminNote, strPrice, true);
    }

    public TemplateModel FM_addService(long accountId, int billingType, int frequency, int duration, String description, String note, String adminNote, String strPrice, boolean isInvoice) throws Exception {
        Date now = TimeUtils.getDate();
        try {
            double price = USFormat.parseDouble(strPrice);
            User oldUser = Session.getUser();
            Account oldAccount = Session.getAccount();
            try {
                Account newAccount = (Account) Account.get(new ResourceId(accountId, 0));
                Session.setUser(newAccount.getUser());
                Session.setAccount(newAccount);
                synchronized (Session.getAccount()) {
                    newAccount.getService().addEntry(billingType, frequency, duration, price, description, note, adminNote);
                }
                newAccount.getBill().charge(newAccount.getBillingInfo());
                if (isInvoice) {
                    newAccount.sendInvoice(now);
                }
                return this;
            } finally {
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
            }
        } catch (NumberFormatException e) {
            throw new HSUserException("toolbox.incorrect_number");
        }
    }

    public TemplateModel FM_delService(long accountId, long serviceId) throws Exception {
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            Account newAccount = (Account) Account.get(new ResourceId(accountId, 0));
            Session.setUser(newAccount.getUser());
            Session.setAccount(newAccount);
            Session.getLog().debug("DELETED SERVICE :" + serviceId + " ACCOUNT ID:" + accountId);
            synchronized (Session.getAccount()) {
                newAccount.getService().deleteService(serviceId);
            }
            return this;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        }
    }

    public TemplateModel FM_getDefaultTransferPrice(String tld) throws Exception {
        return new TemplateString(DomainRegistrar.getDomainPriceAsStr(tld, -1, Session.getResellerId()));
    }

    public TemplateModel FM_getTLDPrices(String tld) throws UnknownResellerException, SQLException {
        return new TemplateMap(DomainRegistrar.getTLDPrices(tld));
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "setTLDPrices".equals(key) ? new PricesUpdator() : super.get(key);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/BillingManager$PricesUpdator.class */
    class PricesUpdator implements TemplateMethodModel {
        PricesUpdator() {
            BillingManager.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                Iterator i = HTMLEncoder.decode(l).iterator();
                String tld = (String) i.next();
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("DELETE FROM tld_prices WHERE reseller_id = ? AND tld = ?");
                ps.setLong(1, Session.getResellerId());
                ps.setString(2, tld);
                ps.executeUpdate();
                ps.close();
                int j = 0;
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO tld_prices (reseller_id, duration, tld, price) VALUES (?, ?, ?, ?)");
                ps2.setLong(1, Session.getResellerId());
                ps2.setString(3, tld);
                while (i.hasNext()) {
                    j++;
                    String price = (String) i.next();
                    if (price != null && !"".equals(price)) {
                        if (j == 11) {
                            j = -1;
                        }
                        ps2.setInt(2, j);
                        ps2.setString(4, price);
                        ps2.executeUpdate();
                    }
                }
                Session.closeStatement(ps2);
                con.close();
                return new TemplateOKResult();
            } catch (Exception e) {
                Session.getLog().error("Error settings prices", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    public TemplateModel FM_getResellerTLDPrices(String tld) throws Exception {
        return new TemplateMap(getResellerTLDPrices(tld));
    }

    public String FM_getResellerTransferPrice(String tld) throws Exception {
        double pr = Session.getReseller().getPrices("TRANSFER_" + tld).getSetupPrice();
        if (Double.isNaN(pr)) {
            pr = DomainRegistrar.getDomainTransferPrice(tld, 1L);
        }
        if (Double.isNaN(pr)) {
            return null;
        }
        return Double.toString(pr);
    }

    public HashMap getResellerTLDPrices(String tld) throws Exception {
        HashMap prices = new HashMap();
        for (int j = 1; j < 11; j++) {
            double pr = Session.getReseller().getPrices("TLD_" + tld + "_" + j).getSetupPrice();
            Session.getLog().debug("Getting ResellerTLD prices from plan TLD=" + tld + " price=" + pr);
            if (Double.isNaN(pr)) {
                pr = DomainRegistrar.getDomainPrice(tld, j, 1L);
                Session.getLog().debug("Getting ResellerTLD prices from defaults TLD=" + tld + " price=" + pr);
            }
            if (!Double.isNaN(pr)) {
                prices.put("" + j, Double.toString(pr));
            }
        }
        return prices;
    }

    public void resetFatts(long bid) throws Exception {
        BillingInfoObject bi = new BillingInfoObject(bid);
        if (bi.getPaymentInstrument() instanceof GenericCreditCard) {
            ((GenericCreditCard) bi.getPaymentInstrument()).clearFatts();
        }
    }

    public void resetFatts(long bid, long aid) throws Exception {
        GenericCreditCard cc = null;
        try {
            Account a = (Account) Account.get(new ResourceId(aid, 0));
            BillingInfoObject bi = a.getBillingInfo();
            if (bi.getId() == bid && (bi.getPaymentInstrument() instanceof GenericCreditCard)) {
                cc = (GenericCreditCard) bi.getPaymentInstrument();
            }
        } catch (Exception ex) {
            Session.getLog().warn("Unable to get Billing Info for the account id '" + aid + "'", ex);
        }
        if (cc != null) {
            cc.clearFatts();
        } else {
            resetFatts(bid);
        }
    }

    public TemplateModel FM_resetFatts(long bid, String aid) throws Exception {
        Long acctid = null;
        try {
            acctid = new Long(aid);
        } catch (Exception e) {
            Session.getLog().warn("Problem parsing account id (id=" + aid + ")");
        }
        if (acctid != null) {
            resetFatts(bid, acctid.longValue());
        } else {
            resetFatts(bid);
        }
        return this;
    }

    public TemplateModel FM_resetFatts(long bid) throws Exception {
        resetFatts(bid);
        return this;
    }

    public TemplateModel FM_approveTaxExemption(String exemptionCode, String accountId) throws Exception {
        try {
            Account a = (Account) Account.get(new ResourceId(Long.parseLong(accountId), 0));
            BillingInfoObject bi = a.getBillingInfo();
            bi.approveTaxExemption(exemptionCode);
            a.sendBillingMessage("ACCOUNT_TEXEMPT_APPROVED");
            return this;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get Billing Info for the account id '" + accountId + "'", ex);
            throw new HSUserException("bi.unable_get_info");
        }
    }

    public TemplateModel FM_rejectTaxExemption(String exemptionCode, String accountId) throws Exception {
        try {
            Account a = (Account) Account.get(new ResourceId(Long.parseLong(accountId), 0));
            BillingInfoObject bi = a.getBillingInfo();
            bi.rejectTaxExemption(exemptionCode);
            a.sendBillingMessage("ACCOUNT_TEXEMPT_REJECTED");
            return this;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get Billing Info for the account id '" + accountId + "'", ex);
            throw new HSUserException("bi.unable_get_info");
        }
    }

    public TemplateModel FM_getBillviewer(long id) {
        return new AnonymousBillViewer(id);
    }

    public TemplateModel FM_getAvailablePromotionsConfig() throws Exception {
        return XMLManager.getXMLAsTemplate("PROMO_CONFIG", "config");
    }

    public static TemplateModel FM_createPromo(String promoCode, String promoName, String billDescr, String promoValidatorId, String promoCalcId, String setup, String recurrent, String usage, String codeLess) throws Exception {
        return Promo.createPromo(promoCode, promoName, billDescr, promoValidatorId, promoCalcId, "ON".equals(setup.toUpperCase()), "ON".equals(recurrent.toUpperCase()), "ON".equals(usage.toUpperCase()), "ON".equals(codeLess.toUpperCase()));
    }

    public static TemplateModel FM_getAvailablePromotions() throws Exception {
        ArrayList res = new ArrayList();
        res.addAll(Promo.getPromoHash().values());
        return new TemplateList(res);
    }

    public static TemplateModel FM_getActivePropmotions() throws Exception {
        return new TemplateList(getActivePropmotions());
    }

    public static List getActivePropmotions() throws Exception {
        ArrayList res = new ArrayList();
        Collection<Promo> promos = Promo.getPromoHash().values();
        for (Promo p : promos) {
            if (p.isActive()) {
                res.add(p);
            }
        }
        return res;
    }

    public TemplateModel FM_getPromo(long promoId) throws Exception {
        return Promo.getPromo(promoId);
    }

    public TemplateModel FM_getResellerDSPrices(String dstId, String periodId) throws Exception {
        return new TemplateHash(getResellerDSPrices(dstId, periodId));
    }

    public Hashtable getResellerDSPrices(String dstId, String periodId) throws Exception {
        String dsPrefix = Globals.getAccessor().getSet("ds_templates").getPrefix();
        String prefix = dsPrefix + dstId;
        Hashtable prices = new Hashtable();
        String setup = Double.toString(Session.getReseller().getPrices(prefix).getSetupPrice());
        String recurrent = Double.toString(Session.getReseller().getPrices(prefix).getRecurrentPrice());
        prices.put("setup", setup == null ? "0" : setup);
        prices.put("recurrent", recurrent == null ? "0" : recurrent);
        return prices;
    }
}
