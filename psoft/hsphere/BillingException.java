package psoft.hsphere;

/* loaded from: hsphere.zip:psoft/hsphere/BillingException.class */
public class BillingException extends HSUserException {
    protected double amount;
    protected double balance;
    protected double credit;

    public double getAmount() {
        return this.amount;
    }

    public double getBalance() {
        return this.balance;
    }

    public double getCredit() {
        return this.credit;
    }

    public BillingException(String msg, double amount, double balance, double credit) {
        super(msg);
        this.amount = amount;
        this.balance = balance;
        this.credit = credit;
    }
}
