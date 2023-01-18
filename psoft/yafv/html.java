package psoft.yafv;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/yafv/html.class */
public class html {
    protected static String filterPath(String path) {
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

    static String cutDotYafv(String pack) {
        return (pack == null || !pack.endsWith(".yafv")) ? pack : pack.substring(0, pack.length() - 5);
    }

    static String shortName(String pack) {
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
        if (ie >= 6 && "-f".equals(argv[4])) {
            StringBuffer baseHtmlPath = new StringBuffer(filterPath(argv[0]));
            String JSPath = filterPath(argv[1]);
            String jsDir = filterPath(argv[2]);
            String initImgAtt = filterPath(argv[3]);
            for (int i = 5; i < ie; i++) {
                try {
                    String inFile = argv[i];
                    if (!inFile.endsWith(".in")) {
                        inBaseName = inFile;
                        inFile = inFile + ".in";
                    } else {
                        inBaseName = inFile.substring(0, inFile.length() - 3);
                    }
                    StringTokenizer st = new StringTokenizer(inFile, "/");
                    String old = null;
                    while (st.hasMoreTokens()) {
                        if (old != null) {
                            baseHtmlPath.append("/").append(old);
                        }
                        old = st.nextToken();
                    }
                    FileReader in = new FileReader(inFile);
                    HTMLLexer lexer = new HTMLLexer(in);
                    File f = new File(baseHtmlPath.toString());
                    f.mkdirs();
                    String htmloutfile = ((Object) baseHtmlPath) + "/" + inBaseName;
                    PrintWriter html = new PrintWriter(new FileWriter(htmloutfile));
                    System.out.println("HTML file : " + htmloutfile);
                    HTMLParser parserObj = new HTMLParser(lexer, html, JSPath, jsDir, initImgAtt);
                    try {
                        parserObj.parse();
                    } catch (Exception e) {
                        e.printStackTrace();
                        exitCode = 2;
                    }
                    html.close();
                    in.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    exitCode = 3;
                }
            }
        } else {
            System.err.println(" You should call:\njava psoft/yafv/html `$TEMPLATES_PATH' `$JS_PATH' `$JS_DIR' `$INIT_IMG_ATT' -f  `html-templates.in'");
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
