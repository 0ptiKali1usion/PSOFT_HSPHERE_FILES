package psoft.hsphere.axis;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/axis/ContactInfo.class */
public class ContactInfo implements Serializable {
    protected String first_name;
    protected String last_name;
    protected String org_name;
    protected String address1;
    protected String address2;
    protected String address3;
    protected String city;
    protected String state;
    protected String state2;
    protected String country;
    protected String postal_code;
    protected String phone;
    protected String fax;
    protected String email;

    public String getAddress1() {
        return this.address1;
    }

    public String getAddress2() {
        return this.address2;
    }

    public String getAddress3() {
        return this.address3;
    }

    public String getCity() {
        return this.city;
    }

    public String getCountry() {
        return this.country;
    }

    public String getEmail() {
        return this.email;
    }

    public String getFax() {
        return this.fax;
    }

    public String getFirst_name() {
        return this.first_name;
    }

    public String getLast_name() {
        return this.last_name;
    }

    public String getOrg_name() {
        return this.org_name;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getPostal_code() {
        return this.postal_code;
    }

    public String getState() {
        return this.state;
    }

    public String getState2() {
        return this.state2;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public void setAddress3(String address3) {
        this.address3 = address3;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public void setOrg_name(String org_name) {
        this.org_name = org_name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPostal_code(String postal_code) {
        this.postal_code = postal_code;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setState2(String state2) {
        this.state2 = state2;
    }

    public ContactInfo() {
    }

    public ContactInfo(String first_name, String last_name, String org_name, String address1, String address2, String address3, String city, String state, String state2, String country, String postal_code, String phone, String fax, String email) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.city = city;
        this.country = country;
        this.email = email;
        this.fax = fax;
        this.first_name = first_name;
        this.last_name = last_name;
        this.org_name = org_name;
        this.phone = phone;
        this.postal_code = postal_code;
        this.state = state;
        this.state2 = state2;
    }
}
