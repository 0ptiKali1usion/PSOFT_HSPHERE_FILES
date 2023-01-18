package psoft.hsphere.migrator.creator;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/UserCreatorConfig.class */
public class UserCreatorConfig {
    private InputSource dataFile;
    private String defPlan;
    private BufferedWriter log;
    private boolean detailedLog;
    private boolean sendMail;
    private String resumeUser;
    private boolean clearUp;
    private boolean clearAll;
    private boolean force;
    private Document doc;
    private boolean resumed;
    private boolean createOnlyUsers;
    private boolean printPlans;

    public UserCreatorConfig(InputSource source, BufferedWriter log, boolean force) throws Exception {
        this.dataFile = null;
        this.log = null;
        this.detailedLog = false;
        this.sendMail = false;
        this.resumeUser = "";
        this.clearUp = false;
        this.clearAll = false;
        this.force = false;
        this.doc = null;
        this.resumed = false;
        this.createOnlyUsers = false;
        this.printPlans = false;
        this.dataFile = source;
        DOMParser parser = new DOMParser();
        parser.parse(this.dataFile);
        this.doc = parser.getDocument();
        this.log = log;
        this.detailedLog = true;
        this.force = force;
    }

    public UserCreatorConfig(InputSource source, BufferedWriter log, boolean force, boolean createOnlyUsers) throws Exception {
        this.dataFile = null;
        this.log = null;
        this.detailedLog = false;
        this.sendMail = false;
        this.resumeUser = "";
        this.clearUp = false;
        this.clearAll = false;
        this.force = false;
        this.doc = null;
        this.resumed = false;
        this.createOnlyUsers = false;
        this.printPlans = false;
        this.dataFile = source;
        DOMParser parser = new DOMParser();
        parser.parse(this.dataFile);
        this.doc = parser.getDocument();
        this.log = log;
        this.detailedLog = true;
        this.force = force;
        this.createOnlyUsers = createOnlyUsers;
    }

    public UserCreatorConfig(String[] argv) throws Exception {
        this.dataFile = null;
        this.log = null;
        this.detailedLog = false;
        this.sendMail = false;
        this.resumeUser = "";
        this.clearUp = false;
        this.clearAll = false;
        this.force = false;
        this.doc = null;
        this.resumed = false;
        this.createOnlyUsers = false;
        this.printPlans = false;
        int i = 0;
        while (i < argv.length) {
            try {
                if ("-pp".equals(argv[i])) {
                    this.printPlans = true;
                }
                if ("-d".equals(argv[i]) || "--datafile".equals(argv[i])) {
                    this.dataFile = new InputSource(new FileInputStream(argv[i + 1]));
                    DOMParser parser = new DOMParser();
                    parser.parse(this.dataFile);
                    this.doc = parser.getDocument();
                    i++;
                }
                if ("-l".equals(argv[i]) || "--log".equals(argv[i])) {
                    this.log = new BufferedWriter(new FileWriter(argv[i + 1]));
                }
                if ("-dp".equals(argv[i]) || "--defplan".equals(argv[i])) {
                    this.defPlan = argv[i + 1];
                }
                if ("-dl".equals(argv[i]) || "--detailedlog".equals(argv[i])) {
                    this.detailedLog = true;
                }
                if ("-sn".equals(argv[i]) || "--sendnotifications".equals(argv[i])) {
                    this.sendMail = true;
                }
                if ("-r".equals(argv[i]) || "--resume".equals(argv[i])) {
                    this.resumeUser = argv[i + 1];
                    this.resumed = true;
                    i++;
                    if ("-c".equals(argv[i + 1]) || "--clearup".equals(argv[i])) {
                        this.clearUp = true;
                    }
                }
                if ("-rc".equals(argv[i])) {
                    this.resumeUser = argv[i + 1];
                    this.resumed = true;
                    this.clearUp = true;
                }
                if ("-f".equals(argv[i]) || "--force".equals(argv[i])) {
                    this.force = true;
                }
                if ("-ca".equals(argv[i]) || "--clearall".equals(argv[i])) {
                    this.clearAll = true;
                }
                if ("-cu".equals(argv[i])) {
                    this.createOnlyUsers = true;
                }
                i++;
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        if (this.doc == null) {
            printHelp();
            throw new Exception("Invalid parameters");
        }
    }

    public UserCreatorConfig(Document doc, String logFileName, String resumedUser, boolean clear, boolean clearAll) throws Exception {
        this.dataFile = null;
        this.log = null;
        this.detailedLog = false;
        this.sendMail = false;
        this.resumeUser = "";
        this.clearUp = false;
        this.clearAll = false;
        this.force = false;
        this.doc = null;
        this.resumed = false;
        this.createOnlyUsers = false;
        this.printPlans = false;
        this.doc = doc;
        this.log = logFileName.length() > 0 ? new BufferedWriter(new FileWriter(logFileName)) : null;
        this.defPlan = "";
        this.detailedLog = true;
        this.sendMail = true;
        this.resumeUser = resumedUser;
        this.clearUp = clear;
        this.force = true;
        this.resumed = false;
        this.clearAll = clearAll;
    }

    public boolean isClearUp() {
        return this.clearUp || this.clearAll;
    }

    public InputSource getDataFile() {
        return this.dataFile;
    }

    public BufferedWriter getLog() {
        return this.log;
    }

    public String getDefaultPlan() {
        return this.defPlan;
    }

    public boolean isLogDetailed() {
        return this.detailedLog;
    }

    public boolean isMailActivated() {
        return this.sendMail;
    }

    public boolean isResumed() {
        return this.resumeUser.length() > 0 && this.resumed;
    }

    public String getResumedUser() {
        return this.resumeUser;
    }

    public void setResumedUser(String user) {
        this.resumeUser = user;
        this.resumed = true;
    }

    public void clearResumedUser() {
        this.resumeUser = "";
        this.resumed = false;
    }

    public boolean isForced() {
        return this.force;
    }

    public Document getDocument() {
        return this.doc;
    }

    public void setClearUp(boolean flag) {
        this.clearUp = flag;
    }

    public void setResumed(boolean flag) {
        this.resumed = flag;
    }

    public boolean isCreateOnlyUsers() {
        return this.createOnlyUsers;
    }

    public boolean isPrintPlans() {
        return this.printPlans;
    }

    public static void printHelp() {
        System.out.println("Usage java psoft.hsphere.converter.ClientMigrator -d datafile.xml [-l logfile] [-dl] \n[-dp defplan] [sn] [-r username] [-f]\n");
        System.out.println("Where ");
        System.out.println("-d datafile.xml or --datafile datafile.xml\n file which contains data for the bulk\n users creation\n");
        System.out.println("-l logfile or --log logfile\n file in which class should log actions and results\n");
        System.out.println("-dl or --detailedlog defines detailed log level.\n In this mode log will contain not just 'failed'\n messages but stack trace as well.\n");
        System.out.println("-dp defplan or --defplan defplan\n plan which will be used as the default if no plan is defined in datafile.\n In case no plan is defined in the datafile and this\n parameter is not defined, plan Unix will be used.\n");
        System.out.println("-sn or --sendnotifications Send welcome letters,\n troubletickets and all other kind of e-mail notifications.\n Please make sure mail settings are configured in the control\n panel before using this option\n");
        System.out.println("-r username --resume username.\n This option allows to resume migration process from certain user.\n All users which preceed this one will be skipped. This\n option is usable only if the previous launch of the program\n had been performed without the -f option.\n");
        System.out.println("-f or --force. Continue the user migration even if there are any errors.\n Do not use this option if you are going to user -r option later\n\n");
        System.out.println("-cu  Create only users - skip resellers creation\nIf not avaliable then create only resellers - skip users creation\n");
        System.out.println("-pp  Print information about all necessary for creations plans for users/resellers.\n");
    }
}
