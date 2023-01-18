package psoft.hsphere.migrator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.util.FakeRequest;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/PlanUtils.class */
public class PlanUtils {
    public static AccountManager getAccountManager(String reseller) throws Exception {
        ArrayList res = new ArrayList();
        res.add(reseller);
        ArrayList ids = UsersInfoExtractor.getResellersList(res);
        if (ids.size() <= 0) {
            throw new Exception("Unknown reseller - " + reseller);
        }
        int resellerId = Integer.parseInt((String) ids.iterator().next());
        return getAccountManager(resellerId);
    }

    private static Reseller setResellerAccount(int resellerId) throws Exception {
        Reseller resell = Reseller.getReseller(resellerId);
        Account account = resell.getAccount();
        User usr = User.getUser(resell.getUser());
        Session.setUser(usr);
        Session.setAccount(account);
        return resell;
    }

    private static AccountManager getAccountManager(int resellerId) throws Exception {
        Reseller resell = setResellerAccount(resellerId);
        User admin = User.getUser(resell.getAdmin());
        HashSet hash = admin.getAccountIds();
        Iterator i = hash.iterator();
        while (i.hasNext()) {
            ResourceId accId = (ResourceId) i.next();
            Account acc = (Account) Account.get(accId);
            Session.setUser(admin);
            Session.setAccount(acc);
            if (acc.FM_getChild(FMACLManager.ADMIN) != null) {
                return new AccountManager(acc.FM_getChild(FMACLManager.ADMIN));
            }
            setResellerAccount(resellerId);
        }
        return null;
    }

    private static Account setAdminAccount() throws Exception {
        User reseller = User.getUser(FMACLManager.ADMIN);
        Account adminAccount = reseller.getAccount(new ResourceId(1L, 0));
        Session.setResellerId(1L);
        Session.setUser(reseller);
        Session.setAccount(adminAccount);
        return adminAccount;
    }

    public static AccountManager getAccountManager() throws Exception {
        Account adminAccount = setAdminAccount();
        ResourceId adminId = adminAccount.FM_getChild(FMACLManager.ADMIN);
        if (adminId == null) {
            return null;
        }
        AccountManager manager = (AccountManager) adminId.get();
        return manager;
    }

    public static PrintStream setOutputStream(String errorFile) throws Exception {
        PrintStream newStream = new PrintStream(new FileOutputStream(errorFile));
        PrintStream oldStream = System.out;
        System.setErr(newStream);
        return oldStream;
    }

    public static PrintStream setOutputStream(ByteArrayOutputStream stream) throws Exception {
        PrintStream newStream = new PrintStream(stream);
        PrintStream oldStream = System.out;
        System.setErr(newStream);
        System.setOut(newStream);
        return oldStream;
    }

    public static PrintStream unsetOutputStream(PrintStream oldStream) {
        PrintStream stream = System.out;
        System.setOut(oldStream);
        return stream;
    }

    public static PrintStream setErrorStream(String errorFile) throws Exception {
        PrintStream newStream = new PrintStream(new FileOutputStream(errorFile));
        PrintStream oldStream = System.err;
        System.setErr(newStream);
        return oldStream;
    }

    public static void unsetErrorStream(PrintStream oldStream) {
        System.setErr(oldStream);
    }

    public static void saveInFile(FakeRequest rq, String fileName) throws Exception {
        File file = new File(fileName);
        PrintStream output = new PrintStream(new FileOutputStream(file));
        output.print("\nwizard=" + rq.getParameter("wizard") + "\n\n");
        Enumeration names = rq.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String[] values = rq.getParameterValues(name);
            String value = "";
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    value = value + values[i] + " ";
                }
            }
            output.print(name + "=" + value + "\n");
        }
    }

    private static void print(String msg) {
        System.out.print(msg);
    }

    private static void printErr(String msg) {
        System.err.print(msg);
    }

    private static void printFAILED() {
        print("  [ FAILED ]\n");
    }

    private static void printErrFAILED() {
        printErr("  [ FAILED ]\n");
    }

    public static void message(String message) {
        print(message);
    }

    public static void messageErr(String message) {
        printErr(message);
    }

    public static void outOK() {
        print("  [ OK ]\n");
    }

    public static void outErrOK() {
        printErr("  [ OK ]\n");
    }

    public static void outFail() {
        printFAILED();
    }

    public static void outErrFail() {
        printErrFAILED();
    }

    public static void outFail(String message) {
        printFAILED();
        print(message + "\n");
    }

    public static void outErrFail(String message) {
        printErrFAILED();
        printErr(message + "\n");
    }

    public static void outFail(Exception exc) {
        printFAILED();
        exc.printStackTrace();
    }

    public static void outErrFail(Exception exc) {
        printErrFAILED();
        exc.printStackTrace();
    }

    public static void outFail(String message, Exception exc) {
        print(message + "\n");
        exc.printStackTrace();
    }

    public static void outErrFail(String message, Exception exc) {
        printErr(message + "\n");
        exc.printStackTrace();
    }
}
