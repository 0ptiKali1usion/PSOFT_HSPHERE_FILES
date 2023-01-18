package psoft.hsp.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import org.apache.log4j.Category;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import psoft.hsphere.tools.ExternalCP;
import psoft.hsphere.tools.ToolLogger;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgConfigurator.class */
public class PkgConfigurator {
    private static Category log = Category.getInstance(PkgConfigurator.class.getName());
    private static ToolLogger logger;
    private PkgConfig config;

    public PkgConfigurator(PkgConfig config) throws Exception {
        this.config = config;
        ExternalCP.initCP();
    }

    private void prepareSkel() throws Exception {
        for (String paramName : this.config.getPassedParameters()) {
            if (!"--with-prefix".equals(paramName)) {
                PkgConfigParameter param = this.config.getConfigParameter(paramName);
                logger.outMessage("Preparing source for " + paramName);
                param.prepareSource(this.config.getSrcLocationPrefix());
                logger.outOK();
            }
        }
        saveConfig();
    }

    private void saveConfig() throws IOException {
        PkgConfigSerializer xmlConf = new PkgConfigSerializer(this.config);
        Document cfg = xmlConf.getXMLConfig();
        OutputFormat format = new OutputFormat(cfg);
        format.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(new FileWriter(this.config.getSrcLocationPrefix() + PkgBuilder.PKG_CONFIG_FILENAME), format);
        serializer.serialize(cfg);
    }

    public static void main(String[] argv) throws Exception {
        PkgConfig conf = null;
        logger = new ToolLogger(argv);
        if (argv.length == 0 || Arrays.asList(argv).contains("--help")) {
            printHelp();
            System.exit(0);
        }
        try {
            conf = new PkgConfig(argv);
        } catch (PkgConfigurationException ex) {
            logger.outMessage(ex.getMessage());
            System.exit(-1);
        }
        if (conf.isConfigured("Checking passed parameters", false)) {
            logger.outMessage("Passed parameters checked.\n");
        } else {
            logger.outMessage("Missconfiguration detected. Aborting....\n");
            System.exit(-1);
        }
        PkgConfigurator builder = new PkgConfigurator(conf);
        builder.prepareSkel();
        logger.outMessage("Process finished.\n");
        System.exit(0);
    }

    private static void printHelp() {
        System.out.println("Tool to configure directory srtructure to assemble an H-Sphere package");
        System.out.println("Tool is used to prepare directory skeleton andnecessary resources for further building H-Sphere package.");
        System.out.println("SYNOPSIS:");
        System.out.println("java psoft.hsp.tools.PkgConfigurator");
        System.out.println(" --with-prefix[=/path/to/deirectory]");
        System.out.println("\t[   --with-templates[=/path/to/deirectory]   ]");
        System.out.println("\t[   --with-images[=/path/to/deirectory]   ]");
        System.out.println("\t[   --with-properties[=/path/to/deirectory]   ]");
        System.out.println("\t[  --with-xmls[=/path/to/deirectory]  ]");
        System.out.println("\t[   --with-classes[=/path/to/deirectory]   ]");
        System.out.println("\t[   --with-scripts[=/path/to/deirectory] | --with-scripts-advanced[=/path/to/deirectory]   ]");
        System.out.println("\t[   --with-scripts-advanced   ]");
        System.out.println("\t[   --with-lang-bundle[=/path/to/deirectory]   ]");
        System.out.println("\t[   --with-preinstall[=/path/to/file]    ]");
        System.out.println("\t[   --with-postinstall[=/path/to/file]   ]");
        System.out.println("\t[   --with-preupgrade[=/path/to/file]    ]");
        System.out.println("\t[   --with-postupgrade[=/path/to/file]   ]");
        System.out.println("\t[   --with-preuninstall[=/path/to/file]   ]");
        System.out.println("\t[   --with-postuninstall[=/path/to/file]   ]");
        System.out.println("\t[   --with-jars[=/path/to/directory]   ]");
        System.out.println("\t[   --with-rpms   ]");
        System.out.println("\t[   --with-tarballs   ]");
        System.out.println("\t[   --with-sql[=/path/to/file]   ]  ");
        System.out.println("\t[   --with-sql-uninstall[=/path/to/file]   ]  ");
        System.out.println("\t[   --with-sql-upgrade[=/path/to/file]   ]  ");
        System.out.println("\t[   --with-sql-uninstall[=/path/to/file]  ] ");
        System.out.println("Allowed parameters:");
        System.out.println("\n --with-prefix[=/path/to/directory]");
        logger.smartPrintLn("Specifies directory into which the package skeleton will be generated. If the parameter is ommited the current working directory will be used as a destination folder", "\t", 75);
        System.out.println("\n[ --with-templates[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that the package which is going to be built, will contain templates.If the path parameter is ommited, the default dirs skeleton is being generated, otherways templates located by the specified path will be copied to the package which is being prepared to be built", "\t", 75);
        System.out.println("\n[ --with-images[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that the package which is going to be built, will contain inmages.", "\t", 75);
        System.out.println("\n[ --with-properties[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that the package which is going to be built will contain the properties file. If the parameter path is present, the properties file gets copied from the specified path", "\t", 75);
        System.out.println("\n[ --with-xmls[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain XML files such as menu.xml, design_config.xml. This allows to configure menu, hosting plans, CP design.", "\t", 75);
        System.out.println("\n[ --with-classes[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain java classes. This allows to add new clasees and override H-Sphere core classes.", "\t", 75);
        System.out.println("\n[ --with-scripts[=/path/to/directory] | --with-scripts-advanced[=/path/to/deirectory] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain scripts. This allows to add new scripts and override core H-Sphere scripts", "\t", 75);
        System.out.println("\n[ --with-lang-bundle[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will conatin language bundle files. This allows to define new languages and override default H-Sphere language dependet elements", "\t", 75);
        System.out.println("\n[ --with-preinstall[=/path/to/file] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will conatin  preinstall script which will be executed before installation of the package", "\t", 75);
        System.out.println("\n[ --with-postinstall[=/path/to/file] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain postinstall script which will be executed after the package files deployment.", "\t", 75);
        System.out.println("\n[ --with-preupgrade[=/path/to/file] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will conatin  preupgrade script which will be executed before upgrade of the package", "\t", 75);
        System.out.println("\n[ --with-postupgrade[=/path/to/file] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain postupgrade script which will be executed after the package files are upgraded.", "\t", 75);
        System.out.println("\n[ --with-preuninstall[=/path/to/file] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain pre-uninstall script which will be executed before deletion package files.", "\t", 75);
        System.out.println("\n[ --with-postuninstall[=/path/to/file] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain post-uninstall script which will be executed after deletion of the package files", "\t", 75);
        System.out.println("\n[ --with-jars[=/path/to/directory] ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain java libraries.", "\t", 75);
        System.out.println("\n[ --with-rpms ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain third party products supplied as RPM files.", "\t", 75);
        System.out.println("\n[ --with-tarballs ]");
        logger.smartPrintLn("Specifies that package which is going to be built will contain third party products supplied tarball files.", "\t", 75);
        System.out.println("\n[ --with-sql[=/path/to/file] ]  ");
        logger.smartPrintLn("Specifies that package which is going to be built will contain SQL file which will be executed against H-Sphere database after deployment of the package files and before execution the package preinstall script if such is preconfigured.", "\t", 75);
        System.out.println("\n[ --with-sql-uninstall[=/path/to/file] ]  ");
        logger.smartPrintLn("Specifies that package which is going to be built will contain SQL file which will be executed against H-Sphere database after package is uninstalled", "\t", 75);
        System.out.println("\n[ --with-sql-upgrade[=/path/to/file] ]  ");
        logger.smartPrintLn("Specifies that package which is going to be built will contain SQL file which will be executed against H-Sphere database during package upgrade", "\t", 75);
        System.out.println("\n\n");
    }
}
