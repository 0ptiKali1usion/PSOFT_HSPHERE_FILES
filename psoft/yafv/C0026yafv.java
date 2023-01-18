package psoft.yafv;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;

/* renamed from: psoft.yafv.yafv */
/* loaded from: hsphere.zip:psoft/yafv/yafv.class */
public class C0026yafv {
    static String filterPath(String path) {
        StringBuffer resultPath = new StringBuffer();
        boolean firstToken = true;
        if (path.startsWith("/")) {
            firstToken = false;
        }
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            String shortName = st.nextToken();
            if (firstToken) {
                firstToken = false;
                resultPath.append(shortName);
            } else if (!".".equals(shortName)) {
                resultPath.append("/").append(shortName);
            }
        }
        return resultPath.toString();
    }

    static String filterPackage(String pack) {
        StringBuffer resultPackage = new StringBuffer();
        boolean firstToken = true;
        StringTokenizer st = new StringTokenizer(pack, ".");
        while (st.hasMoreTokens()) {
            String shortName = st.nextToken();
            if (!"".equals(shortName)) {
                if (firstToken) {
                    firstToken = false;
                } else {
                    resultPackage.append('.');
                }
                resultPackage.append(shortName);
            }
        }
        return resultPackage.toString();
    }

    public static String cutDotYafv(String pack) {
        return (pack == null || !pack.endsWith(".yafv")) ? pack : pack.substring(0, pack.length() - 5);
    }

    public static String shortName(String pack) {
        if (pack == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(cutDotYafv(pack), "/.");
        String str = "";
        while (true) {
            String result = str;
            if (st.hasMoreTokens()) {
                str = st.nextToken();
            } else {
                return result;
            }
        }
    }

    public static void main(String[] argv) {
        String inBaseName;
        int ie = argv.length;
        int exitCode = 0;
        if (ie > 9 && "-f".equals(argv[8])) {
            Hashtable processedFiles = new Hashtable();
            String valid = filterPath(argv[0]);
            String fail = filterPath(argv[1]);
            String attention = filterPath(argv[2]);
            String jsPath = filterPath(argv[3]);
            String javaPath = filterPath(argv[4]);
            String baseJavaPackage = filterPackage(argv[5]);
            String designDir = argv[6];
            String jsDir = filterPath(argv[7]);
            File f = new File(jsPath);
            f.mkdirs();
            File f2 = new File(javaPath);
            f2.mkdirs();
            String javaPackage = baseJavaPackage + "." + designDir;
            for (int i = 9; i < ie; i++) {
                try {
                    String inFile = argv[i];
                    if (inFile.endsWith(".yafv")) {
                        inBaseName = inFile.substring(0, inFile.length() - 5);
                    } else {
                        inBaseName = inFile;
                        inFile = inFile + ".yafv";
                    }
                    int shNameIndex = inBaseName.lastIndexOf(47);
                    String shortName = shNameIndex >= 0 ? inBaseName.substring(shNameIndex + 1) : inBaseName;
                    if (processedFiles.get(shortName) != null) {
                        System.err.println("Error: Unable to parse the '" + inFile + "' file.\nA file with the same name have been already processed in this session.\nCheck all your *.yafv files to eleminate the uncertainty in declarations.");
                        exitCode = 4;
                        System.exit(4);
                    } else {
                        processedFiles.put(shortName, "1");
                    }
                    System.out.println("YAFV file : " + inFile);
                    FileReader in = new FileReader(inFile);
                    YAFVLexer lexer = new YAFVLexer(in);
                    PrintWriter js = new PrintWriter(new FileWriter(jsPath + "/" + shortName + ".js"));
                    PrintWriter java = new PrintWriter(new FileWriter(javaPath + "/" + shortName + ".java"));
                    java.println("package " + javaPackage + ";\n");
                    java.println("import java.util.*;");
                    java.println("import freemarker.template.*;");
                    java.println("import psoft.util.freemarker.*;");
                    java.println("import psoft.hsphere.*;\n");
                    java.println("import dk.brics.automaton.*;\n");
                    java.println("public class " + shortName + " {");
                    YAFVParser parserObj = new YAFVParser(lexer, valid, fail, attention, js, java, baseJavaPackage, jsDir, shortName);
                    try {
                        parserObj.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                        exitCode = 2;
                    }
                    java.println("}");
                    java.close();
                    js.println();
                    js.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    exitCode = 3;
                }
            }
        } else {
            System.err.println(" You should call:\njava psoft/yafv/yafv `$VALID' `$FAIL' `$ATTENTION' `$JS_OUT' `$JAVA_OUT' `$JAVA-PACKAGE' `$DESIGN_DIR' -f `yafv-file(s)'");
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
