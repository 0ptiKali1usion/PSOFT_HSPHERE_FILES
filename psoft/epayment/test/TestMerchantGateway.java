package psoft.epayment.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import psoft.epayment.CreditCard;
import psoft.epayment.DummyCreditCard;
import psoft.epayment.MerchantGateway;

/* loaded from: hsphere.zip:psoft/epayment/test/TestMerchantGateway.class */
public class TestMerchantGateway {

    /* renamed from: cc */
    private CreditCard f12cc;

    /* renamed from: mg */
    private MerchantGateway f13mg;
    String ttTypes;
    private double amount;
    boolean verbose;

    public TestMerchantGateway(String configFileName, boolean verbose) throws Exception {
        this.verbose = verbose;
        this.ttTypes = "CHARGE AUTH VOID CAPTURE";
        this.amount = 0.01d;
        HashMap map = new HashMap();
        BufferedReader inStream = new BufferedReader(new FileReader(configFileName));
        while (true) {
            try {
                String line = inStream.readLine();
                if (line == null) {
                    break;
                }
                StringTokenizer tkz = new StringTokenizer(line);
                String name = null;
                name = tkz.hasMoreTokens() ? tkz.nextToken() : name;
                String value = "";
                while (tkz.hasMoreTokens()) {
                    if (!"".equals(value)) {
                        value = value + " ";
                    }
                    value = value + tkz.nextToken();
                }
                if (value != null && name != null) {
                    map.put(name, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Usage: java psoft.epayment.test.TestMerchantGateway [-v] <filename.in>\n -v - verbose output");
                throw new Exception(e.getMessage());
            }
        }
        String address = map.containsKey("ADDR") ? (String) map.get("ADDR") : "address";
        String city = map.containsKey("CITY") ? (String) map.get("CITY") : "Cincinnati";
        String state = map.containsKey("STATE") ? (String) map.get("STATE") : "OH";
        String zip = map.containsKey("ZIP") ? (String) map.get("ZIP") : "30329";
        String country = map.containsKey("COUNTRY") ? (String) map.get("COUNTRY") : "country";
        String phone = map.containsKey("PHONE") ? (String) map.get("PHONE") : "098";
        String email = map.containsKey("EMAIL") ? (String) map.get("EMAIL") : "test@test.com";
        String ccNumber = map.containsKey("CCNUM") ? (String) map.get("CCNUM") : "4222222222222";
        String ccCVV = map.containsKey("CCCVV") ? (String) map.get("CCCVV") : "";
        String ccName = map.containsKey("CCNAME") ? (String) map.get("CCNAME") : "Test User";
        String ccType = map.containsKey("CCTYPE") ? (String) map.get("CCTYPE") : "VISA";
        String ccYear = map.containsKey("CCYEAR") ? (String) map.get("CCYEAR") : "2004";
        String ccMonth = map.containsKey("CCMONTH") ? (String) map.get("CCMONTH") : "01";
        if (map.containsKey("TtTYPES")) {
            this.ttTypes = (String) map.get("TtTYPES");
        }
        String strAmount = "";
        if (map.containsKey("AMOUNT")) {
            try {
                strAmount = (String) map.get("AMOUNT");
                this.amount = Double.parseDouble(strAmount);
            } catch (Exception e2) {
                System.out.println("Incorrect Amount value has been entered: " + strAmount);
                this.amount = 0.01d;
            }
        } else {
            System.out.println("Default value for amount is set");
        }
        if (map.containsKey("CLASSNAME")) {
            String className = (String) map.get("CLASSNAME");
            TestBillingInfo bi = new TestBillingInfo(address, city, state, zip, country, email, phone);
            this.f12cc = new DummyCreditCard(bi, ccNumber, ccCVV, ccName, ccType, ccYear, ccMonth);
            Class[] argT = new Class[0];
            Object[] argV = new Object[0];
            this.f13mg = (MerchantGateway) Class.forName(className).getConstructor(argT).newInstance(argV);
            if (verbose) {
                this.f13mg.setLog(new ConsoleLog());
            }
            this.f13mg.init(-100, map);
            System.out.println("Test started");
            System.out.println("\n\n\nThe following parametrs have been passed to gateway*********************\n");
            System.out.println("\nCC Info ---------------------------");
            System.out.println("CC Type: " + ccType);
            System.out.println("CC Number: " + ccNumber);
            System.out.println("CC Name: " + ccName);
            System.out.println("CC Year: " + ccYear);
            System.out.println("CC Month: " + ccMonth);
            System.out.println("\nAddress: " + address);
            System.out.println("City: " + city);
            System.out.println("Country: " + country);
            System.out.println("State: " + state);
            System.out.println("Zip: " + zip);
            System.out.println("Phone: " + phone);
            System.out.println("Email: " + email);
            System.out.println("\nConfig----------------------------");
            for (String key : map.keySet()) {
                if (!"ADDR".equals(key) && !"CITY".equals(key) && !"STATE".equals(key) && !"ZIP".equals(key) && !"COUNTRY".equals(key) && !"PHONE".equals(key) && !"EMAIL".equals(key) && !"CCNUM".equals(key) && !"CCNAME".equals(key) && !"CCTYPE".equals(key) && !"CCYEAR".equals(key) && !"CCMONTH".equals(key) && !"CCCVV".equals(key)) {
                    String val = (String) map.get(key);
                    System.out.println("    " + key + " : " + val);
                }
            }
            System.out.println("\n**********************************************************************");
            System.out.println("\nList of trasactions which are allowed with this gateway: " + this.ttTypes + "\n");
            return;
        }
        System.out.println("CLASSNAME parameter is not specified");
        throw new Exception("CLASSNAME parameter is not specified");
    }

    public void testAuthCapture() throws Exception {
        Random rand = new Random();
        if (this.ttTypes.indexOf("CAPTURE") != -1) {
            System.out.println("Try auth using " + this.f13mg.getDescription() + " ...");
            HashMap res = this.f13mg.authorize(Math.abs(rand.nextLong()), "test transaction", this.amount, this.f12cc);
            System.out.println("Auth ok. ID=" + ((String) res.get("id")) + "\n");
            System.out.println("Try capture using " + this.f13mg.getDescription() + " ...");
            this.f13mg.capture(rand.nextLong(), "test transaction", res, this.f12cc);
            System.out.println("Capture ok.");
            System.out.println("Id=" + ((String) res.get("id")) + "\n");
            return;
        }
        System.out.println("Capture (Post Auth) test has been skipped.");
    }

    public void testAuthVoid() throws Exception {
        Random rand = new Random();
        if (this.ttTypes.indexOf("VOID") != -1) {
            System.out.println("Try auth using " + this.f13mg.getDescription() + " ...");
            HashMap res = this.f13mg.authorize(Math.abs(rand.nextLong()), "test transaction", this.amount, this.f12cc);
            System.out.println("Auth ok. ID=" + ((String) res.get("id")) + "\n");
            System.out.println("Try void ...");
            this.f13mg.voidAuthorize(Math.abs(rand.nextLong()), "test transaction", res, this.f12cc);
            System.out.println("Void ok.");
            System.out.println("id=" + ((String) res.get("id")) + "\n");
            return;
        }
        System.out.println("Void test has been skipped.");
    }

    public void testCharge() throws Exception {
        Random rand = new Random();
        if (this.ttTypes.indexOf("CHARGE") != -1) {
            System.out.println("Try charge using " + this.f13mg.getDescription() + " ...");
            HashMap res = this.f13mg.charge(Math.abs(rand.nextLong()), "test transaction", this.amount, this.f12cc);
            System.out.println("Charge ok.");
            System.out.println("Id=" + ((String) res.get("id")) + "\n");
            return;
        }
        System.out.println("Charge test has been skipped.");
    }

    public void testAuth() throws Exception {
        Random rand = new Random();
        if (this.ttTypes.indexOf("AUTH") != -1) {
            System.out.println("Try auth using " + this.f13mg.getDescription() + " ...");
            HashMap res = this.f13mg.authorize(Math.abs(rand.nextLong()), "test transaction", this.amount, this.f12cc);
            System.out.println("Auth ok. ID=" + ((String) res.get("id")) + "\n");
            return;
        }
        System.out.println("Auth test has been skipped.");
    }

    public void runTests() throws Exception {
        try {
            testAuth();
            testCharge();
            testAuthCapture();
            testAuthVoid();
            System.out.println(this.f13mg.getDescription() + "has been tested :OK");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Tests " + this.f13mg.getDescription() + " failed");
            throw new Exception(e.getMessage());
        }
    }

    public static void main(String[] argv) {
        String fileName;
        try {
            boolean verbose = false;
            if ("-v".equals(argv[0])) {
                verbose = true;
                fileName = argv[1];
            } else {
                fileName = argv[0];
            }
            TestMerchantGateway testMg = new TestMerchantGateway(fileName, verbose);
            testMg.runTests();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Test finished");
        System.exit(0);
    }
}
