package psoft.validators;

import gnu.regexp.REException;
import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/validators/DefaultValidator.class */
public class DefaultValidator {
    private static Hashtable validatormap;
    public static DOSPathValidator dosPath = new DOSPathValidator();
    public static EmailValidator email = new EmailValidator();
    public static DomainNameValidator domainName = new DomainNameValidator();
    public static PhoneNumberValidator phone = new PhoneNumberValidator();
    public static UserValidator user = new UserValidator();
    public static PasswordValidator password = new PasswordValidator();
    public static CopyValidator copy = new CopyValidator();
    public static AliasDomainNameValidator extDomainName = new AliasDomainNameValidator();
    public static ReValidator name;
    public static ReValidator month;
    public static ReValidator year;
    public static ReValidator address;
    public static ReValidator state;
    public static ReValidator zip;
    public static ReValidator countryCode;
    public static ReValidator ccnumber;

    public static Validator getValidator(String name2) {
        Validator v = (Validator) validatormap.get(name2);
        return v != null ? v : copy;
    }

    static {
        try {
            name = new ReValidator("^\\w.*$");
            name.setErrorMessage("No value was set or value is not valid");
            name.setMin(2, "should be 2 characters or longer");
            name.setMax(80, "should be 80 characters or shorter");
            address = new ReValidator("^\\w.*$");
            address.setErrorMessage("No value was set or value is not valid");
            address.setMin(4, "should be 4 characters or longer");
            address.setMax(80, "should be 80 characters or shorter");
            state = new ReValidator("^[A-Za-z][A-Za-z]$");
            state.setErrorMessage("State is not valid");
            zip = new ReValidator("^\\d\\d\\d\\d\\d-?\\d*$");
            zip.setErrorMessage("Zip is not valid");
            zip.setMax(10, "Zip should be shorter than 11 characters");
            year = new ReValidator("[12][0-9][0-9][0-9]");
            year.setErrorMessage("Invalid (or improbable) year");
            month = new ReValidator("(^[1-9]$)|(^1[0-9]$)|(^[0][1-9]$)");
            month.setErrorMessage("Month must be a number from 1 to 12");
            ccnumber = new ReValidator("^\\d+$");
            ccnumber.setErrorMessage("Not a valid number. must be digits only");
            ccnumber.setMin(10, "Number too short");
            ccnumber.setMax(20, "Number too long");
            countryCode = new ReValidator("^[A-Z][A-Z]$");
            countryCode.setErrorMessage("Invalid country code");
            validatormap = new Hashtable();
            validatormap.put("name", name);
            validatormap.put("password", password);
            validatormap.put("login", user);
            validatormap.put("email", email);
            validatormap.put("address", address);
            validatormap.put("street1", address);
            validatormap.put("street2", address);
            validatormap.put("phone", phone);
            validatormap.put("zip", zip);
            validatormap.put("country", countryCode);
            validatormap.put("state", state);
            validatormap.put("city", name);
            validatormap.put("ccnumber", ccnumber);
            validatormap.put("expmonth", month);
            validatormap.put("expyear", year);
        } catch (REException re) {
            re.printStackTrace();
        }
    }
}
