package psoft.yafv.widgets;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import psoft.yafv.DFALexer;
import psoft.yafv.DFAParser;

/* loaded from: hsphere.zip:psoft/yafv/widgets/YafvJsCheckNumberUpdate.class */
public class YafvJsCheckNumberUpdate {
    private static final String CHECK_INTEGER_PATTERN = "[+\\-]?[0-9]+";
    private static final String CHECK_FLOAT_PATTERN = "[+\\-]?[0-9]+([.,][0-9]+)?([eE][+\\-]?[0-9]+)?";
    private static final String INTEGER_PATTERN_VAR = "var __INTEGER_PATTERN = ";
    private static final String FLOAT_PATTERN_VAR = "var __FLOAT_PATTERN = ";
    private static final String YafvJsFile = "./yafv.js";
    private static final String NewYafvJsFile = "./yafv.js.new";

    public static void exitErrorMessage(String message) {
        System.err.println("Error: " + message + ".");
        System.exit(1);
    }

    public static void exitErrorMessage(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
        System.exit(1);
    }

    public static void main(String[] argv) throws Exception {
        String dfaRegExp = null;
        try {
            dfaRegExp = new RunAutomaton(new RegExp(CHECK_INTEGER_PATTERN).toAutomaton()).toString();
        } catch (Exception e) {
            exitErrorMessage("Unable to get the DFA representation for 'CHECK_INTEGER_PATTERN'");
        }
        DFALexer dfaLexer = new DFALexer(new StringReader(dfaRegExp));
        StringWriter jsRegExp = new StringWriter();
        DFAParser dfaParser = new DFAParser(dfaLexer, jsRegExp);
        try {
            dfaParser.parse();
        } catch (Exception ex) {
            exitErrorMessage("DFA: \n---\n" + dfaRegExp + "\n---\nUnable to parse the DFA representation for 'CHECK_INTEGER_PATTERN'.\n", ex);
        }
        String checkIntegerLine = INTEGER_PATTERN_VAR + jsRegExp.toString() + ";";
        try {
            dfaRegExp = new RunAutomaton(new RegExp(CHECK_FLOAT_PATTERN).toAutomaton()).toString();
        } catch (Exception e2) {
            exitErrorMessage("Unable to get the DFA representation for 'CHECK_FLOAT_PATTERN'");
        }
        DFALexer dfaLexer2 = new DFALexer(new StringReader(dfaRegExp));
        StringWriter jsRegExp2 = new StringWriter();
        DFAParser dfaParser2 = new DFAParser(dfaLexer2, jsRegExp2);
        try {
            dfaParser2.parse();
        } catch (Exception ex2) {
            exitErrorMessage("DFA: \n---\n" + dfaRegExp + "\n---\nUnable to parse the DFA representation for 'CHECK_FLOAT_PATTERN'.\n", ex2);
        }
        String checkFloatLine = FLOAT_PATTERN_VAR + jsRegExp2.toString() + ";";
        BufferedReader in = new BufferedReader(new FileReader(YafvJsFile));
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(NewYafvJsFile)));
        boolean wasIntegerWritten = false;
        boolean wasFloatWritten = false;
        while (true) {
            try {
                try {
                    String readStr = in.readLine();
                    if (readStr == null) {
                        in.close();
                        out.close();
                        break;
                    } else if (readStr.startsWith(INTEGER_PATTERN_VAR)) {
                        if (wasIntegerWritten) {
                            throw new Exception("Double definition for 'var __INTEGER_PATTERN = '.");
                        }
                        out.println(checkIntegerLine);
                        wasIntegerWritten = true;
                    } else if (!readStr.startsWith(FLOAT_PATTERN_VAR)) {
                        out.println(readStr);
                    } else if (wasFloatWritten) {
                        throw new Exception("Double definition for 'var __FLOAT_PATTERN = '.");
                    } else {
                        out.println(checkFloatLine);
                        wasFloatWritten = true;
                    }
                } catch (EOFException e3) {
                    in.close();
                    out.close();
                } catch (Exception ex3) {
                    exitErrorMessage("Unable to read the input './yafv.js' file.", ex3);
                    in.close();
                    out.close();
                }
            } catch (Throwable th) {
                in.close();
                out.close();
                throw th;
            }
        }
        if (!wasIntegerWritten) {
            exitErrorMessage("var __INTEGER_PATTERN = hasn't been written");
        }
        if (!wasFloatWritten) {
            exitErrorMessage("var __FLOAT_PATTERN = hasn't been written");
        }
        System.out.println("The result file has been written successfully. Check it out.");
    }
}
