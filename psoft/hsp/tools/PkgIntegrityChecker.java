package psoft.hsp.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.xml.transform.TransformerException;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgIntegrityChecker.class */
public class PkgIntegrityChecker {
    private File srcLocation;
    private String pkgLocation;
    private PkgConfig config;
    private PkgConfigSerializer confSerialiser;
    private ToolLogger logger;
    private static Hashtable properties2CheckOut = new Hashtable();

    static {
        properties2CheckOut.put("MENU_CONFIG", "--with-xmls|menu.xml");
        properties2CheckOut.put("HELP_CONFIG", "--with-xmls|help.xml");
        properties2CheckOut.put("TEMPLATE_BUNDLE", "--with-lang-bundle|hsphere_lang.properties");
        properties2CheckOut.put("MENU_BUNDLE", "--with-lang-bundle|menu.properties");
        properties2CheckOut.put("USER_BUNDLE", "--with-lang-bundle|messages.properties");
        properties2CheckOut.put("DESIGN_SCHEME_CONFIG", "--with-xmls|design_config.xml");
        properties2CheckOut.put("CRON_CONFIG", "--with-xmls|cron_config.xml");
        properties2CheckOut.put("ONLINE_HELP_CONFIG", "--with-xmls|online_help.xml");
    }

    public PkgIntegrityChecker(File pkgLocation) {
        this.logger = null;
        this.pkgLocation = pkgLocation.getAbsolutePath();
        this.srcLocation = pkgLocation;
        this.logger = ToolLogger.getDefaultLogger();
    }

    public PkgIntegrityChecker(String pkgLocation, String srcSubdir) {
        this.logger = null;
        this.pkgLocation = pkgLocation;
        if (srcSubdir != null && srcSubdir.length() > 0) {
            this.srcLocation = new File(pkgLocation + srcSubdir);
        } else {
            this.srcLocation = new File(pkgLocation);
        }
        this.logger = ToolLogger.getDefaultLogger();
    }

    public boolean preInstallCheck() {
        this.logger.outMessage("Checking package integrity\n");
        this.logger.setPrfx("\t");
        File pkgConfig = getConfigFile();
        if (pkgConfig == null) {
            return false;
        }
        this.config = getPkgConfig(pkgConfig);
        if (this.config == null || !checkPkgProperties() || !checkRPMS()) {
            return false;
        }
        this.logger.setPrfx("");
        return true;
    }

    public PkgConfig getPkgConfig() {
        return this.config;
    }

    public boolean preBuildCheck() {
        this.logger.outMessage("Checking package integrity\n");
        this.logger.setPrfx("\t");
        File pkgConfig = getConfigFile();
        if (pkgConfig == null) {
            return false;
        }
        this.config = getPkgConfig(pkgConfig);
        if (this.config == null || !this.config.checkPreparedSource() || !checkPkgProperties() || !checkRPMS()) {
            return false;
        }
        this.logger.setPrfx("");
        return true;
    }

    public File getConfigFile() {
        this.logger.outMessage("Checking source directory");
        if (!this.srcLocation.exists() || !this.srcLocation.isDirectory() || this.srcLocation.list() == null || this.srcLocation.list().length == 0) {
            this.logger.outFail(this.srcLocation.getAbsolutePath() + " source directory does not exists or empty. Exiting");
            return null;
        }
        this.logger.outOK();
        this.logger.outMessage("Checking package config file availability");
        File configFile = new File(this.srcLocation.getAbsolutePath() + PkgBuilder.PKG_CONFIG_FILENAME);
        if (!configFile.exists() || !configFile.isFile()) {
            this.logger.outFail("Package config file:" + configFile.getAbsolutePath() + " not found\n");
            return null;
        }
        if (!configFile.canRead()) {
            this.logger.outFail("Package config file" + configFile.getAbsolutePath() + " is not readable\n");
        }
        this.logger.outOK();
        return configFile;
    }

    private PkgConfig getPkgConfig(File configFile) {
        DOMParser parser = new DOMParser();
        this.logger.outMessage("Loading package config file");
        try {
            InputSource is = new InputSource(new FileInputStream(configFile));
            try {
                parser.parse(is);
            } catch (IOException ioEx) {
                this.logger.outFail("An I/O exception occured while parsing package config file", ioEx);
                return null;
            } catch (SAXException saxEx) {
                this.logger.outFail("Error parsing package config file. Please consider using xml validation software", saxEx);
            }
            this.logger.outOK();
            Document doc = parser.getDocument();
            this.logger.outMessage("Parsing package config file");
            try {
                this.confSerialiser = new PkgConfigSerializer(doc, this.pkgLocation);
                this.logger.outOK();
                this.config = this.confSerialiser.getConfig();
                return this.config;
            } catch (Exception ex) {
                this.logger.outFail("Error serializing package config", ex);
                return null;
            }
        } catch (FileNotFoundException ex2) {
            this.logger.outFail("File " + configFile.getAbsolutePath() + " not found", ex2);
            return null;
        }
    }

    private boolean checkPkgProperties() {
        if (this.config.getPassedParameters().contains("--with-properties")) {
            this.logger.outMessage("Loading package properties");
            try {
                Hashtable properties = getPkgProperties(new File(this.srcLocation.getAbsolutePath() + "/" + this.config.getFileParamPath("--with-properties").getPath()));
                this.logger.outOK();
                if (!checkProperties(properties)) {
                    return false;
                }
                return true;
            } catch (IOException ex) {
                this.logger.outFail("An error occured while loading package properties : " + ex.getMessage(), ex);
                return false;
            }
        }
        return true;
    }

    private boolean checkRPMS() {
        boolean result = true;
        try {
            this.logger.outMessage("Checking RPMs\n");
            NodeList rpms = XPathAPI.selectNodeList(this.confSerialiser.getXMLConfig().getDocumentElement(), "rpms/rpm");
            this.logger.outMessage(rpms.getLength() + " rpm description has been found\n");
            for (int i = 0; i < rpms.getLength(); i++) {
                Node rpm = rpms.item(i);
                String rpmName = rpm.getAttributes().getNamedItem("name").getNodeValue();
                this.logger.outMessage("Checking " + rpmName + " package\n");
                NodeList platforms = XPathAPI.selectNodeList(rpm, "platform");
                for (int j = 0; j < platforms.getLength(); j++) {
                    Node platform = platforms.item(j);
                    String platformName = platform.getAttributes().getNamedItem("name").getNodeValue();
                    String location = platform.getAttributes().getNamedItem("location").getNodeValue();
                    this.logger.outMessage("Checking " + platformName + " platform");
                    if (PHServerTypes.getSupportedOs().contains(platformName)) {
                        String extention = "FBSD4".equals(platformName) ? ".tgz" : ".rpm";
                        if ("BUILT-IN".equals(location)) {
                            File f = new File(this.srcLocation.getAbsolutePath() + "/" + this.config.getFileParamPath("--with-rpms").getPath() + "/" + platformName + "/" + rpmName + extention);
                            if (!f.exists() || !f.isFile() || !f.canRead()) {
                                this.logger.outFail("Can not find " + f.getAbsolutePath() + "\n");
                                result = false;
                            }
                        } else if (location.startsWith("ftp") || location.startsWith("http")) {
                            HttpURLConnection con = null;
                            try {
                                URL url = new URL(location);
                                con = (HttpURLConnection) url.openConnection();
                                con.connect();
                                String r = con.getResponseMessage();
                                if (!"OK".equals(r)) {
                                    this.logger.outFail("WARNING:" + location + " not found\n");
                                    if (con != null) {
                                        con.disconnect();
                                    }
                                } else if (con != null) {
                                    con.disconnect();
                                }
                            } catch (MalformedURLException e) {
                                this.logger.outFail("Malformed URL " + location + "\n");
                                result = false;
                                if (con != null) {
                                    con.disconnect();
                                }
                            } catch (IOException e2) {
                                this.logger.outFail("An I/O error occured while opening connection to " + location + "\n");
                                result = false;
                                if (con != null) {
                                    con.disconnect();
                                }
                            }
                        } else {
                            this.logger.outFail("Location parameter can be either BUILT-IN which means that a RPM file should be located in the rpms folder or properly configured URL\n");
                            result = false;
                        }
                        this.logger.outOK();
                    } else {
                        this.logger.outFail("provided platform does not supported\n");
                        result = false;
                    }
                }
            }
        } catch (TransformerException te) {
            this.logger.outFail("An error detected " + te.getMessage());
            result = false;
        }
        return result;
    }

    private Hashtable getPkgProperties(File propFile) throws IOException {
        Hashtable result = new Hashtable();
        FileReader file = new FileReader(propFile);
        BufferedReader in = new BufferedReader(file);
        while (true) {
            String tmp = in.readLine();
            if (tmp != null) {
                if (!tmp.startsWith("#") && tmp.indexOf("=") > 0) {
                    result.put(tmp.substring(0, tmp.indexOf("=")).trim(), tmp.substring(tmp.indexOf("="), tmp.length()).trim());
                }
            } else {
                in.close();
                file.close();
                return result;
            }
        }
    }

    private boolean checkProperties(Hashtable properties) {
        boolean result = true;
        this.logger.outMessage("Checking package properties file\n");
        for (String prop : properties.keySet()) {
            if (properties2CheckOut.keySet().contains(prop)) {
                StringTokenizer st = new StringTokenizer((String) properties2CheckOut.get(prop), "|");
                String pkgProp = st.nextToken();
                if (st.hasMoreTokens()) {
                    String name = st.nextToken();
                    File toCheck = new File(this.srcLocation.getAbsolutePath() + "/" + this.config.getFileParamPath(pkgProp).getPath() + "/" + name);
                    this.logger.outMessage("Checking " + prop);
                    if (toCheck.exists() && toCheck.isFile() && toCheck.canRead()) {
                        this.logger.outOK();
                    } else {
                        this.logger.outFail("File " + toCheck.getAbsolutePath() + " does not exist or can not be read");
                        result = false;
                    }
                } else {
                    this.logger.outFail("Data of the '" + prop + "' property should be specified in the following format: <type>|<file>.\n");
                    return false;
                }
            }
        }
        return result;
    }

    public PkgConfigSerializer getConfSerialiser() {
        return this.confSerialiser;
    }
}
