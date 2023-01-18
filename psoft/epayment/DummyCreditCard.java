package psoft.epayment;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.hsphere.resource.epayment.BillingInfo;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/epayment/DummyCreditCard.class */
public class DummyCreditCard implements CreditCard {
    protected BillingInfo billingInfo;
    protected String number;
    protected String cvv;
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

    @Override // psoft.epayment.PaymentInstrument
    public PaymentInstrument copy(BillingInfo bi) throws Exception {
        return new DummyCreditCard(bi, this);
    }

    public DummyCreditCard(BillingInfo billingInfo, String number, String cvv, String name, String type, String expYear, String expMonth, String expDay, String issueNo, String startYear, String startMonth, String startDay) throws Exception {
        this.number = number;
        this.cvv = cvv;
        this.name = name;
        this.type = type;
        this.expYear = expYear;
        this.expMonth = expMonth;
        this.expDay = expDay;
        this.startYear = startYear;
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.created = TimeUtils.getDate();
        this.billingInfo = billingInfo;
    }

    public DummyCreditCard(BillingInfo billingInfo, String number, String cvv, String name, String type, String expYear, String expMonth) throws Exception {
        this(billingInfo, number, cvv, name, type, expYear, expMonth, "");
    }

    public DummyCreditCard(BillingInfo billingInfo, String number, String cvv, String name, String type, String expYear, String expMonth, String expDay) throws Exception {
        this(billingInfo, number, cvv, name, type, expYear, expMonth, expDay, "", "", "", "");
    }

    public DummyCreditCard(BillingInfo bi, DummyCreditCard gc) throws Exception {
        this(bi, gc.number, gc.cvv, gc.name, gc.type, gc.expYear, gc.expMonth, gc.expDay, gc.issueNo, gc.startYear, gc.startMonth, gc.startDay);
    }

    public DummyCreditCard(BillingInfo billingInfo, Iterator i) throws Exception {
        this.billingInfo = billingInfo;
        this.number = (String) i.next();
        this.name = (String) i.next();
        this.cvv = (String) i.next();
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
    }

    @Override // psoft.epayment.CreditCard
    public String getNumber() {
        return this.number;
    }

    @Override // psoft.epayment.CreditCard
    public String getCVV() {
        return this.cvv;
    }

    @Override // psoft.epayment.CreditCard
    public String getExp() {
        if (this.expDay == null || this.expDay.length() == 0) {
            return this.expMonth + '/' + this.expYear;
        }
        return this.expDay + '/' + this.expMonth + '/' + this.expYear;
    }

    @Override // psoft.epayment.CreditCard
    public String getExp(DateFormat df) {
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
        return df.format(cal.getTime());
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
    public String getCompany() {
        return getBillingInfo().getCompany();
    }

    @Override // psoft.epayment.CreditCard
    public String getAddress() {
        return getBillingInfo().getAddress();
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

    @Override // psoft.epayment.CreditCard
    public String getHiddenNumber() {
        int len = this.number.length();
        StringBuffer buf = new StringBuffer();
        if (len > 4) {
            for (int i = 0; i < len - 4; i++) {
                buf.append("x");
            }
            buf.append(this.number.substring(len - 4, len));
        } else {
            buf.append(this.number);
        }
        return buf.toString();
    }

    @Override // psoft.epayment.CreditCard
    public boolean isExpired() {
        return false;
    }

    @Override // psoft.epayment.PaymentInstrument
    public void checkValid() throws CreditCardExpiredException {
        if (isExpired()) {
            throw new CreditCardExpiredException(getExp());
        }
    }

    @Override // psoft.epayment.CreditCard
    public void setCVVChecked(boolean result) {
    }

    @Override // psoft.epayment.CreditCard
    public short isCVVChecked() {
        return (short) 1;
    }
}
