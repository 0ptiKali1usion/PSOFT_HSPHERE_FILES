package psoft.util;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/util/PhoneNumberConverter.class */
public class PhoneNumberConverter {
    static List northAmericanCountries = Arrays.asList("US", "CA", "MX", "UM");

    public static String getITUFormated(String phone) throws PhoneNumberConvertionException {
        if (PhoneNumber.isITUFormat(phone)) {
            return phone;
        }
        if (!PhoneNumber.isNPAFormat(phone)) {
            throw new PhoneNumberConvertionException("Unable to detect the source format.");
        }
        return PhoneNumber.getITUFormat(PhoneNumber.parseNPAFormat(phone));
    }

    public static String getNPAFormated(String phone) throws PhoneNumberConvertionException {
        if (PhoneNumber.isNPAFormat(phone)) {
            return phone;
        }
        if (!PhoneNumber.isITUFormat(phone)) {
            throw new PhoneNumberConvertionException("Unable to detect the source format.");
        }
        return PhoneNumber.getNPAFormat(PhoneNumber.parseITUFormat(phone));
    }

    public static String getCountryFormated(String phone, String countryId) throws PhoneNumberConvertionException {
        if (northAmericanCountries.contains(countryId)) {
            return getNPAFormated(phone);
        }
        return getITUFormated(phone);
    }

    /* loaded from: hsphere.zip:psoft/util/PhoneNumberConverter$PhoneNumber.class */
    public static class PhoneNumber {
        String countryCode;
        String simpleNumber;
        String extension;
        static final RunAutomaton vPhoneNPA = new RunAutomaton(new RegExp("[2-9][0-9]{2}-[2-9][0-9]{2}-[0-9]{4}([x\\.\\/][0-9]{1,4})?").toAutomaton());
        static final RunAutomaton vPhoneITU = new RunAutomaton(new RegExp("\\+[0-9]{1,3}\\.([0-9][ \\-]?){3,12}([x\\.\\/][0-9]{1,4})?").toAutomaton());

        PhoneNumber(String countryCode, String simpleNumber, String extension) throws PhoneNumberConvertionException {
            int fLength = countryCode == null ? -1 : countryCode.length();
            if (fLength == 0 || fLength > 3) {
                throw new PhoneNumberConvertionException("The '" + countryCode + "' Country Code is invalid.");
            }
            int fLength2 = simpleNumber == null ? -1 : simpleNumber.length();
            if (fLength2 <= 0) {
                throw new PhoneNumberConvertionException("The phone number is absent.");
            }
            if (fLength2 > 10) {
                throw new PhoneNumberConvertionException("The number '" + simpleNumber + "' is too long.");
            }
            int fLength3 = extension == null ? -1 : extension.length();
            if (fLength3 == 0 || fLength3 > 4) {
                throw new PhoneNumberConvertionException("The '" + extension + "' phone extension is invalid.");
            }
            this.countryCode = countryCode;
            this.simpleNumber = simpleNumber;
            this.extension = extension;
        }

        public static boolean isNPAFormat(String phone) {
            return vPhoneNPA.run(phone);
        }

        public static boolean isITUFormat(String phone) {
            return vPhoneITU.run(phone);
        }

        public static PhoneNumber parseNPAFormat(String phone) throws PhoneNumberConvertionException {
            if (phone == null) {
                return null;
            }
            StringBuffer sNumber = new StringBuffer();
            String extNumber = null;
            StringTokenizer est = new StringTokenizer(phone, "x/");
            if (est.hasMoreTokens()) {
                String part = est.nextToken();
                StringTokenizer st = new StringTokenizer(part, "- ");
                while (st.hasMoreTokens()) {
                    sNumber.append(st.nextToken());
                }
            }
            if (est.hasMoreTokens()) {
                extNumber = est.nextToken();
            }
            return new PhoneNumber("1", sNumber.toString(), extNumber);
        }

        public static PhoneNumber parseITUFormat(String phone) throws PhoneNumberConvertionException {
            if (phone == null) {
                return null;
            }
            String cCode = null;
            StringBuffer sNumber = new StringBuffer();
            String extNumber = null;
            StringTokenizer est = new StringTokenizer(phone, "x/");
            if (est.hasMoreTokens()) {
                String part = est.nextToken();
                int nPos = part.indexOf(46);
                if (nPos >= 0) {
                    cCode = part.substring(0, nPos);
                    if (cCode.startsWith("+")) {
                        cCode = cCode.substring(1);
                    }
                    part = part.substring(nPos + 1);
                }
                StringTokenizer st = new StringTokenizer(part, "- ");
                while (st.hasMoreTokens()) {
                    sNumber.append(st.nextToken());
                }
            }
            if (est.hasMoreTokens()) {
                extNumber = est.nextToken();
            }
            return new PhoneNumber(cCode, sNumber.toString(), extNumber);
        }

        public static String getNPAFormat(PhoneNumber ph) throws PhoneNumberConvertionException {
            if (!"1".equals(ph.countryCode) || ph.simpleNumber.length() != 10) {
                throw new PhoneNumberConvertionException("The phone number cannot be reporesented in the desired format.");
            }
            StringBuffer res = new StringBuffer();
            res.append(ph.simpleNumber.substring(0, 3)).append('-').append(ph.simpleNumber.substring(3, 6)).append('-').append(ph.simpleNumber.substring(6));
            if (ph.extension != null) {
                res.append('/').append(ph.extension);
            }
            return res.toString();
        }

        public static String getITUFormat(PhoneNumber ph) {
            StringBuffer res = new StringBuffer("+");
            res.append(ph.countryCode).append('.').append(ph.simpleNumber);
            if (ph.extension != null) {
                res.append('/').append(ph.extension);
            }
            return res.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        String number = args.length > 0 ? args[0] : "+1.2345678901/12";
        System.out.println("Phone number: [" + number + "]");
        System.out.println("ITU Format: [" + getITUFormated(number) + "]");
        System.out.println("NPA Format: [" + getNPAFormated(number) + "]");
    }
}
