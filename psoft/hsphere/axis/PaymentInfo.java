package psoft.hsphere.axis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/* loaded from: hsphere.zip:psoft/hsphere/axis/PaymentInfo.class */
public class PaymentInfo implements Serializable {
    protected String type;
    protected static final String[] availableTypes = {"Check", "CC", "PayPal", "WorldPay", "2CheckOut"};
    protected String number;
    protected String cvv;
    protected String name;
    protected String cc_type;
    protected String expYear;
    protected String expMonth;
    protected String expDay;
    protected String issueNo;
    protected String startYear;
    protected String startMonth;
    protected String startDay;

    public PaymentInfo(String cc_type, String expDay, String expMonth, String expYear, String issueNo, String name, String number, String startDay, String startMonth, String startYear, String type) {
        this(cc_type, expDay, expMonth, expYear, issueNo, name, number, startDay, startMonth, startYear, type, "");
    }

    public PaymentInfo(String cc_type, String expDay, String expMonth, String expYear, String issueNo, String name, String number, String startDay, String startMonth, String startYear, String type, String cvv) {
        this.cc_type = cc_type;
        this.expDay = expDay;
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.issueNo = issueNo;
        this.name = name;
        this.number = number;
        this.cvv = cvv;
        this.startDay = startDay;
        this.startMonth = startMonth;
        this.startYear = startYear;
        this.type = type;
    }

    public PaymentInfo() {
    }

    public String getCc_type() {
        return this.cc_type;
    }

    public void setCc_type(String cc_type) {
        this.cc_type = cc_type;
    }

    public String getExpDay() {
        return this.expDay;
    }

    public void setExpDay(String expDay) {
        this.expDay = expDay;
    }

    public String getExpMonth() {
        return this.expMonth;
    }

    public void setExpMonth(String expMonth) {
        this.expMonth = expMonth;
    }

    public String getExpYear() {
        return this.expYear;
    }

    public void setExpYear(String expYear) {
        this.expYear = expYear;
    }

    public String getIssueNo() {
        return this.issueNo;
    }

    public void setIssueNo(String issueNo) {
        this.issueNo = issueNo;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getStartDay() {
        return this.startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getStartMonth() {
        return this.startMonth;
    }

    public void setStartMonth(String startMonth) {
        this.startMonth = startMonth;
    }

    public String getStartYear() {
        return this.startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCvv() {
        return this.cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public Iterator iterator() {
        Collection res = new ArrayList();
        if ("CC".equals(this.type)) {
            res.add(getNumber());
            res.add(getCvv());
            res.add(getName());
            res.add(getCc_type());
            res.add(getExpYear());
            res.add(getExpMonth());
            res.add(getExpDay());
            res.add(getIssueNo());
            res.add(getStartYear());
            res.add(getStartMonth());
            res.add(getStartDay());
        }
        return res.iterator();
    }
}
