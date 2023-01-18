package psoft.epayment.test;

import psoft.hsphere.resource.epayment.BillingInfo;

/* loaded from: hsphere.zip:psoft/epayment/test/TestBillingInfo.class */
class TestBillingInfo implements BillingInfo {
    protected String address;
    protected String city;
    protected String state;
    protected String zip;
    protected String country;
    protected String email;
    protected String phone;
    protected String type;
    protected String company;

    /* renamed from: id */
    protected long f11id;

    public TestBillingInfo() {
        this.address = "123 72nd St";
        this.city = "Brooklyn";
        this.state = "NY";
        this.zip = "11234";
        this.country = "US";
        this.email = "bar@fooware.net";
        this.phone = "555-5555";
        this.type = "CC";
        this.company = "test";
        this.f11id = -100L;
    }

    public TestBillingInfo(String address, String city, String state, String zip, String country, String email, String phone) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.country = country;
        this.email = email;
        this.phone = phone;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getAddress() {
        return this.address;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getCity() {
        return this.city;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getState() {
        return this.state;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getState2() {
        return null;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getZip() {
        return this.zip;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getCountry() {
        return this.country;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getEmail() {
        return this.email;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getPhone() {
        return this.phone;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getType() {
        return this.type;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public String getCompany() {
        return this.company;
    }

    @Override // psoft.hsphere.resource.epayment.BillingInfo
    public long getId() {
        return this.f11id;
    }
}
