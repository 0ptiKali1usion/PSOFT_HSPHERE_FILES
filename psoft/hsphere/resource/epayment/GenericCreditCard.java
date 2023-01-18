package psoft.hsphere.resource.epayment;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.encryption.Crypt;
import psoft.epayment.CreditCard;
import psoft.epayment.CreditCardExpiredException;
import psoft.epayment.CreditCardProcessingException;
import psoft.epayment.PaymentInstrument;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.admin.CCEncryption;
import psoft.hsphere.admin.PrivateKeyNotLoadedException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.HostEntry;
import psoft.util.Base64;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/GenericCreditCard.class */
public class GenericCreditCard implements CreditCard, TemplateHashModel {
    protected BillingInfo billingInfo;
    protected String number;
    protected String hiddenNumber;
    protected String cvv;
    protected short cvvChecked;
    protected String name;
    protected String type;
    protected String expYear;
    protected String expMonth;
    protected String expDay;
    protected String issueNo;
    protected String startYear;
    protected String startMonth;
    protected String startDay;
    protected Date created;
    protected int failedChargeAttemptsCounter;
    protected Timestamp lastFatt;
    public static final short CVVNOTCHECKED = 1;
    public static final short CVVOK = 2;
    public static final short CVVFAIL = 3;

    @Override // psoft.epayment.PaymentInstrument
    public PaymentInstrument copy(BillingInfo bi) throws Exception {
        return new GenericCreditCard(bi, this);
    }

    public GenericCreditCard(BillingInfo billingInfo) throws Exception {
        synchronized (CCEncryption.ENCRYPTION_LOCK) {
            this.billingInfo = billingInfo;
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT name, cc_number, hidden_cc_number,  encrypted_cc_number, exp_year, exp_month, exp_day,  issueno, start_year, start_month, start_day, type,  created, fatts,last_fatt, cvv_checked FROM credit_card WHERE id = ?");
            ps.setLong(1, billingInfo.getId());
            ResultSet rs = ps.executeQuery();
            boolean ccencryption = CCEncryption.get().isOn();
            if (rs.next()) {
                this.name = rs.getString("name");
                this.hiddenNumber = rs.getString("hidden_cc_number");
                this.expYear = rs.getString("exp_year");
                this.expMonth = rs.getString("exp_month");
                this.expDay = rs.getString("exp_day");
                this.issueNo = rs.getString("issueno");
                this.startYear = rs.getString("start_year");
                this.startMonth = rs.getString("start_month");
                this.startDay = rs.getString("start_day");
                this.type = rs.getString("type");
                this.created = rs.getDate("created");
                this.failedChargeAttemptsCounter = rs.getInt("fatts");
                this.lastFatt = rs.getTimestamp("last_fatt");
                String ccNumber = rs.getString("cc_number");
                short iscvvChecked = rs.getShort("cvv_checked");
                if (!ccencryption) {
                    this.number = ccNumber;
                }
                this.cvv = "";
                this.cvvChecked = (iscvvChecked == 1 || iscvvChecked == 2 || iscvvChecked == 3) ? iscvvChecked : (short) 1;
                Session.closeStatement(ps);
                con.close();
            } else {
                throw new Exception("Not Found " + billingInfo.getId() + ":" + billingInfo);
            }
        }
    }

    public GenericCreditCard(BillingInfo bi, GenericCreditCard gc) throws Exception {
        this.number = gc.getNumber();
        this.cvv = gc.cvv;
        this.cvvChecked = gc.cvvChecked;
        this.hiddenNumber = gc.hiddenNumber;
        this.cvv = gc.cvv;
        this.name = gc.name;
        this.type = gc.type;
        this.expYear = gc.expYear;
        this.expMonth = gc.expMonth;
        this.expDay = gc.expDay;
        this.issueNo = gc.issueNo;
        this.startYear = gc.startYear;
        this.startMonth = gc.startMonth;
        this.startDay = gc.startDay;
        this.billingInfo = bi;
        this.failedChargeAttemptsCounter = 0;
        save();
    }

    public GenericCreditCard(BillingInfo billingInfo, Accessor a, NameModifier nm) throws Exception {
        this.billingInfo = billingInfo;
        this.number = a.get(nm.getName("cc_number"));
        this.cvv = a.get(nm.getName("cc_cvv"));
        this.hiddenNumber = getHiddenNumber(a.get(nm.getName("cc_number")));
        this.name = a.get(nm.getName("cc_name"));
        this.type = a.get(nm.getName("cc_type"));
        this.expYear = a.get(nm.getName("cc_exp_year"));
        this.expMonth = a.get(nm.getName("cc_exp_month"));
        this.expDay = a.get(nm.getName("cc_exp_day"));
        this.issueNo = a.get(nm.getName("cc_issue_no"));
        this.startYear = a.get(nm.getName("cc_start_year"));
        this.startMonth = a.get(nm.getName("cc_start_month"));
        this.startDay = a.get(nm.getName("cc_start_day"));
        this.failedChargeAttemptsCounter = 0;
        save();
    }

    public GenericCreditCard(BillingInfo billingInfo, Iterator i) throws Exception {
        this.billingInfo = billingInfo;
        this.number = (String) i.next();
        this.cvv = (String) i.next();
        this.hiddenNumber = getHiddenNumber(this.number);
        this.name = (String) i.next();
        this.type = (String) i.next();
        this.expYear = (String) i.next();
        this.expMonth = (String) i.next();
        if (i.hasNext()) {
            this.expDay = (String) i.next();
        }
        if (i.hasNext()) {
            this.issueNo = (String) i.next();
        }
        if (i.hasNext()) {
            this.startYear = (String) i.next();
        }
        if (i.hasNext()) {
            this.startMonth = (String) i.next();
        }
        if (i.hasNext()) {
            this.startDay = (String) i.next();
        }
        this.failedChargeAttemptsCounter = 0;
        save();
    }

    public void update(String number, String name, String expMonth, String expYear, String type, String issueNo, String startMonth, String startYear) throws Exception {
        this.number = number;
        this.hiddenNumber = getHiddenNumber(number);
        this.name = name;
        this.type = type;
        this.expYear = expYear;
        this.expMonth = expMonth;
        this.issueNo = issueNo;
        this.startYear = startYear;
        this.startMonth = startMonth;
        this.failedChargeAttemptsCounter = 0;
        Connection con = Session.getTransConnection();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM credit_card WHERE id = ?");
                ps.setLong(1, this.billingInfo.getId());
                ps.executeUpdate();
                ps.close();
                save();
                Session.commitTransConnection(con);
            } catch (Exception e) {
                con.rollback();
                Session.commitTransConnection(con);
            }
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            throw th;
        }
    }

    protected void save() throws Exception {
        synchronized (CCEncryption.ENCRYPTION_LOCK) {
            Connection con = Session.getDb();
            boolean encriptionOn = CCEncryption.get().isOn();
            String encodedNumber = "";
            if (encriptionOn && this.number != null && !"".equals(this.number)) {
                byte[] encryptedNumber = Crypt.encrypt(CCEncryption.get().getPublicEncryptionKey(), this.number.getBytes());
                encodedNumber = Base64.encode(encryptedNumber);
            }
            if (this.cvvChecked != 1 && this.cvvChecked != 2) {
                this.cvvChecked = (short) 1;
            }
            PreparedStatement ps = con.prepareStatement("INSERT INTO credit_card (id, cc_number, hidden_cc_number, encrypted_cc_number, name, exp_year, exp_month, exp_day, issueno, start_year, start_month, start_day, created, type, fatts, last_fatt, cvv_checked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, this.billingInfo.getId());
            if (encriptionOn) {
                ps.setNull(2, 12);
                ps.setString(4, encodedNumber);
            } else {
                ps.setString(2, this.number);
                ps.setString(4, encodedNumber);
            }
            ps.setString(3, this.hiddenNumber);
            ps.setString(5, this.name);
            ps.setString(6, this.expYear);
            ps.setString(7, this.expMonth);
            ps.setString(8, this.expDay);
            ps.setString(9, this.issueNo);
            ps.setString(10, this.startYear);
            ps.setString(11, this.startMonth);
            ps.setString(12, this.startDay);
            ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
            ps.setString(14, this.type);
            ps.setInt(15, this.failedChargeAttemptsCounter);
            ps.setNull(16, 93);
            ps.setShort(17, this.cvvChecked);
            ps.executeUpdate();
            Session.closeStatement(ps);
            if (!Session.isTransConnection()) {
                con.close();
            }
        }
    }

    @Override // psoft.epayment.CreditCard
    public String getCVV() throws Exception {
        return this.cvv;
    }

    @Override // psoft.epayment.CreditCard
    public String getNumber() throws Exception {
        if (this.number == null) {
            synchronized (CCEncryption.ENCRYPTION_LOCK) {
                Connection con = Session.getDb();
                boolean ccencryption = CCEncryption.get().isOn();
                PreparedStatement ps = con.prepareStatement("SELECT cc_number, encrypted_cc_number FROM credit_card WHERE id = ?");
                ps.setLong(1, this.billingInfo.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String ccNumber = rs.getString("cc_number");
                    String ccEncrNum = rs.getString("encrypted_cc_number");
                    Session.closeStatement(ps);
                    con.close();
                    if (ccencryption) {
                        if (!CCEncryption.get().isPrivateKeyLoaded()) {
                            throw new PrivateKeyNotLoadedException("admin.CCEncryption.privateKeyNotLoadedUserError");
                        }
                        if (ccEncrNum != null && !"".equals(ccEncrNum)) {
                            byte[] encryptedCCNumber = Base64.decode(ccEncrNum);
                            byte[] decrypted = Crypt.decrypt(CCEncryption.get().getPrivateEncryptionKey(), encryptedCCNumber);
                            this.number = new String(decrypted);
                        }
                    } else {
                        this.number = ccNumber;
                    }
                } else {
                    throw new Exception("Not Found " + this.billingInfo.getId() + ":" + this.billingInfo);
                }
            }
        }
        return this.number;
    }

    @Override // psoft.epayment.CreditCard
    public String getExp() {
        if (this.expDay == null || this.expDay.length() == 0) {
            return this.expMonth + '/' + this.expYear;
        }
        return this.expDay + '/' + this.expMonth + '/' + this.expYear;
    }

    protected Date getExpAsDate() {
        Calendar cal = TimeUtils.getCalendar();
        int year = 0;
        int month = 0;
        int day = 0;
        try {
            year = Integer.parseInt(this.expYear);
        } catch (Exception e) {
        }
        try {
            month = Integer.parseInt(this.expMonth);
        } catch (Exception e2) {
        }
        try {
            day = Integer.parseInt(this.expDay);
        } catch (Exception e3) {
        }
        cal.set(year > 99 ? year : year + 2000, month, day);
        return cal.getTime();
    }

    @Override // psoft.epayment.CreditCard
    public String getExp(DateFormat df) {
        return df.format(getExpAsDate());
    }

    @Override // psoft.epayment.CreditCard
    public String getName() {
        return this.name;
    }

    @Override // psoft.epayment.CreditCard
    public String getFirstName() {
        StringTokenizer st = new StringTokenizer(getName());
        return st.hasMoreTokens() ? st.nextToken() : "";
    }

    @Override // psoft.epayment.CreditCard
    public String getLastName() {
        StringTokenizer st = new StringTokenizer(getName());
        if (st.hasMoreTokens()) {
            st.nextToken();
            return st.hasMoreTokens() ? st.nextToken() : "";
        }
        return "";
    }

    @Override // psoft.epayment.CreditCard
    public String getAddress() {
        return getBillingInfo().getAddress();
    }

    @Override // psoft.epayment.CreditCard
    public String getCompany() {
        return getBillingInfo().getCompany();
    }

    @Override // psoft.epayment.CreditCard
    public String getCity() {
        return getBillingInfo().getCity();
    }

    @Override // psoft.epayment.CreditCard
    public String getState() {
        return getBillingInfo().getState();
    }

    @Override // psoft.epayment.CreditCard
    public String getZip() {
        return getBillingInfo().getZip();
    }

    @Override // psoft.epayment.CreditCard
    public String getCountry() {
        return getBillingInfo().getCountry();
    }

    @Override // psoft.epayment.CreditCard
    public String getPhone() {
        return getBillingInfo().getPhone();
    }

    @Override // psoft.epayment.CreditCard
    public String getEmail() {
        return getBillingInfo().getEmail();
    }

    @Override // psoft.epayment.CreditCard, psoft.epayment.PaymentInstrument
    public String getType() {
        return this.type;
    }

    @Override // psoft.epayment.CreditCard
    public String getIssueNo() {
        return this.issueNo == null ? "" : this.issueNo;
    }

    @Override // psoft.epayment.CreditCard
    public String getStartDate() {
        if (this.startYear == null || this.startYear.length() == 0 || this.startMonth == null || this.startMonth.length() == 0) {
            return "";
        }
        if (this.startDay == null || this.startDay.length() == 0) {
            return this.startMonth + '/' + this.startYear;
        }
        return this.startDay + '/' + this.startMonth + '/' + this.startYear;
    }

    @Override // psoft.epayment.CreditCard
    public String getStartDate(DateFormat df) {
        Calendar cal = TimeUtils.getCalendar();
        int year = 0;
        int month = 0;
        int day = 0;
        try {
            year = Integer.parseInt(this.startYear);
        } catch (Exception e) {
        }
        try {
            month = Integer.parseInt(this.startMonth);
        } catch (Exception e2) {
        }
        try {
            day = Integer.parseInt(this.startDay);
        } catch (Exception e3) {
        }
        if (year == 0 || month == 0) {
            return "";
        }
        cal.set(year > 99 ? year : year + 2000, month, day);
        return df.format(cal.getTime());
    }

    public BillingInfo getBillingInfo() {
        return this.billingInfo;
    }

    @Override // psoft.epayment.PaymentInstrument
    public int getBillingType() {
        return 1;
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.epayment.CreditCard
    public boolean isExpired() {
        return getExpAsDate().before(TimeUtils.getDate());
    }

    @Override // psoft.epayment.PaymentInstrument
    public void checkValid() throws Exception {
        if (isExpired()) {
            throw new CreditCardExpiredException(getExp());
        }
        if (CCEncryption.get().isOn() && !CCEncryption.get().isPrivateKeyLoaded()) {
            throw new PrivateKeyNotLoadedException("admin.CCEncryption.privateKeyNotLoadedUserError");
        }
        String smaxFatts = Settings.get().getValue("max_fatts");
        String sFattRI = Settings.get().getValue("fatt_ri");
        int maxFatts = 0;
        int fattRI = 0;
        try {
            maxFatts = Integer.parseInt(smaxFatts);
        } catch (Exception ex) {
            Session.getLog().debug("An incorrect value was set in the merchant gateway properties. Please check Maximum allowed failed charge attempts for credit cards parameter" + ex.getMessage());
        }
        try {
            fattRI = Integer.parseInt(sFattRI);
        } catch (Exception ex2) {
            Session.getLog().debug("An incorrect value was set in the merchant gateway properties. Please check Retry interval after failed attempt to process CC (min)" + ex2.getMessage());
        }
        Session.getLog().debug("GenericCreditCard.checkValid() maxFatts=" + maxFatts + " fatts=" + this.failedChargeAttemptsCounter);
        if (this.failedChargeAttemptsCounter > 0 && maxFatts > 0 && this.failedChargeAttemptsCounter >= maxFatts) {
            throw new CreditCardProcessingException("Charge failure more than " + maxFatts + " times");
        }
        if (fattRI > 0 && getLastFatt() != null) {
            Timestamp unlockTime = new Timestamp(getLastFatt().getTime() + (fattRI * 60 * HostEntry.VPS_IP));
            Timestamp now = TimeUtils.getSQLTimestamp();
            Session.getLog().debug("unlockTime=" + unlockTime + " now=" + now + " sFattsRI=" + sFattRI + " fattRI=" + fattRI);
            if (unlockTime.after(now)) {
                long timeToUnlockLeft = (unlockTime.getTime() - now.getTime()) / 60000;
                if (timeToUnlockLeft >= 1) {
                    String sTimeToUnlockLeft = "" + (timeToUnlockLeft / 60) + " hours " + (timeToUnlockLeft - ((timeToUnlockLeft / 60) * 60)) + " minutes";
                    Session.getLog().debug("sTimeToUnlockLeft=" + sTimeToUnlockLeft);
                    throw new CreditCardProcessingException(Localizer.translateMessage("billing.cc_fatt_detected_est", new String[]{sTimeToUnlockLeft}));
                }
                throw new CreditCardProcessingException(Localizer.translateMessage("billing.cc_fatt_detected"));
            }
        }
    }

    protected boolean isFattsControlActivated() {
        String smaxFatts = Settings.get().getValue("max_fatts");
        int maxFatts = 0;
        try {
            maxFatts = Integer.parseInt(smaxFatts);
        } catch (Exception e) {
            Session.getLog().debug("An incorrect value was set in the merchant gateway properties. Please check Maximum allowed failed charge attempts for credit cards parameter");
        }
        return maxFatts > 0;
    }

    protected String getFattsInfo() {
        String smaxFatts = Settings.get().getValue("max_fatts");
        String sFattRI = Settings.get().getValue("fatt_ri");
        int maxFatts = 0;
        int fattRI = 0;
        try {
            maxFatts = Integer.parseInt(smaxFatts);
        } catch (Exception e) {
            Session.getLog().debug("An incorrect value was set in the merchant gateway properties. Please check Maximum allowed failed charge attempts for credit cards parameter");
        }
        try {
            fattRI = Integer.parseInt(sFattRI);
        } catch (Exception e2) {
            Session.getLog().debug("An incorrect value was set in the merchant gateway properties. Please check Retry interval after failed attempt to process CC (min)");
        }
        if (this.failedChargeAttemptsCounter > 0 && maxFatts > 0) {
            if (this.failedChargeAttemptsCounter >= maxFatts) {
                return "Charge failure more than " + maxFatts + " times";
            }
            Timestamp unlockTime = new Timestamp(getLastFatt().getTime() + (fattRI * 60 * HostEntry.VPS_IP));
            Timestamp now = TimeUtils.getSQLTimestamp();
            if (unlockTime.after(now)) {
                long timeToUnlockLeft = (unlockTime.getTime() - now.getTime()) / 60000;
                if (timeToUnlockLeft >= 1) {
                    String sTimeToUnlockLeft = "" + (timeToUnlockLeft / 60) + " hours " + (timeToUnlockLeft - ((timeToUnlockLeft / 60) * 60)) + " minutes";
                    Session.getLog().debug("sTimeToUnlockLeft=" + sTimeToUnlockLeft);
                    return new String(Localizer.translateMessage("billing.cc_fatt_detected_est_admin", new String[]{sTimeToUnlockLeft}));
                }
                return new String(Localizer.translateMessage("billing.cc_fatt_detected"));
            }
            return "";
        }
        return new String(Localizer.translateMessage("billing.cc_fatts_control_off"));
    }

    protected TemplateModel getInfo() {
        return new TemplateString(this.name + ", " + getHiddenNumber() + ", " + getExp());
    }

    public static String getHiddenNumber(String number) {
        int len = number.length();
        StringBuffer buf = new StringBuffer();
        if (len > 4) {
            for (int i = 0; i < len - 4; i++) {
                buf.append("x");
            }
            buf.append(number.substring(len - 4, len));
        } else {
            buf.append(number);
        }
        return buf.toString();
    }

    @Override // psoft.epayment.CreditCard
    public String getHiddenNumber() {
        return this.hiddenNumber;
    }

    public static String getHiddenCVV(String vcvv) {
        return (vcvv.equals("") || vcvv == null) ? "" : "****";
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("number")) {
                return new TemplateString(getNumber());
            }
            if (key.equals("hNumber")) {
                return new TemplateString(getHiddenNumber());
            }
            if (key.equals("cvv_checked")) {
                return new TemplateString(this.cvvChecked == 2 ? "checked" : "");
            } else if (key.equals("exp")) {
                return new TemplateString(getExp());
            } else {
                if (key.equals("exp_year")) {
                    return new TemplateString(this.expYear);
                }
                if (key.equals("exp_month")) {
                    return new TemplateString(this.expMonth);
                }
                if (key.equals("exp_day")) {
                    return new TemplateString(this.expDay);
                }
                if (key.equals("start_date")) {
                    return new TemplateString(getStartDate());
                }
                if (key.equals("start_year")) {
                    return new TemplateString(this.startYear);
                }
                if (key.equals("start_month")) {
                    return new TemplateString(this.startMonth);
                }
                if (key.equals("start_day")) {
                    return new TemplateString(this.startDay);
                }
                if (key.equals("issue_no")) {
                    return new TemplateString(this.issueNo);
                }
                if (key.equals("name")) {
                    return new TemplateString(this.name);
                }
                if (key.equals("expired")) {
                    return new TemplateString(isExpired());
                }
                if (key.equals("info")) {
                    return getInfo();
                }
                if (key.equals("type")) {
                    return new TemplateString(this.type);
                }
                if (key.equals("fatts")) {
                    return new TemplateString(this.failedChargeAttemptsCounter);
                }
                if (key.equals("fatts_info")) {
                    return new TemplateString(getFattsInfo());
                }
                if (key.equals("is_fatts_checked")) {
                    return new TemplateString(isFattsControlActivated());
                }
                return null;
            }
        } catch (Exception e) {
            throw new TemplateModelException(e.getMessage());
        }
    }

    public int getFailedChargeAttemptsCounter() {
        return this.failedChargeAttemptsCounter;
    }

    public void incFatts() throws Exception {
        Session.getLog().debug("Incrementing FATTS to " + (this.failedChargeAttemptsCounter + 1));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            this.failedChargeAttemptsCounter++;
            this.lastFatt = TimeUtils.getSQLTimestamp();
            ps = con.prepareStatement("UPDATE credit_card SET fatts = ?,last_fatt = ? WHERE id=?");
            ps.setInt(1, this.failedChargeAttemptsCounter);
            ps.setTimestamp(2, this.lastFatt);
            ps.setLong(3, this.billingInfo.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Timestamp getLastFatt() {
        return this.lastFatt;
    }

    public void clearFatts() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE credit_card SET fatts = ?, last_fatt = ? WHERE id = ?");
            ps.setInt(1, 0);
            ps.setNull(2, 93);
            ps.setLong(3, this.billingInfo.getId());
            ps.executeUpdate();
            this.failedChargeAttemptsCounter = 0;
            this.lastFatt = null;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.epayment.CreditCard
    public short isCVVChecked() {
        return this.cvvChecked;
    }

    @Override // psoft.epayment.CreditCard
    public void setCVVChecked(boolean result) {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE credit_card SET cvv_checked = ? WHERE id = ?");
            ps.setShort(1, result ? (short) 2 : (short) 3);
            ps.setLong(2, this.billingInfo.getId());
            ps.executeUpdate();
            this.cvvChecked = result ? (short) 2 : (short) 3;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable tr) {
            Session.getLog().error("Error updating cvv_checked field in the credit_card table ", tr);
        }
    }
}
