package psoft.yafv;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java_cup.runtime.Symbol;
import java_cup.runtime.lr_parser;
import psoft.hsphere.Resource;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.async.AsyncResourceException;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.util.freemarker.TemplateMethodWrapper;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: hsphere.zip:psoft/yafv/CUP$YAFVParser$actions.class */
public class CUP$YAFVParser$actions {
    List params;
    List param_types;
    List includedFunctions;
    String failJs;
    String validJs;
    String validJava;
    String failJava;
    String yafvName;
    String yafvJSIncName;
    String yafvJClassName;
    LinkedList functionList;
    String param;
    String param_type;
    int pNum;
    int maxLength;
    int minLength;

    /* renamed from: fn */
    String f283fn;
    String pack;
    String field;
    protected ExpressionBuffer exprBuffer;
    private final YAFVParser parser;
    final String javaLocalizeMethod = "Localizer.translateLabel";
    final String jsLocalizeLabel = "lang.";
    final String FM_EXT = TemplateMethodWrapper.PREFIX;
    final String FMF_DEFINED = "defined_";
    final String YAFV_PACK_VARIABLE = "FM_defined_yafv_package";
    final String PARAM_PREF = "p_";
    final String REFERENCED_FUNCTION = "referencedFunction";
    Hashtable functionTable = new Hashtable();
    Hashtable referenceTable = new Hashtable();
    Hashtable iYafvTable = new Hashtable();
    String constrainName = null;
    boolean strParamsPresent = false;
    boolean intParamsPresent = false;
    boolean floatParamsPresent = false;
    boolean boolParamsPresent = false;
    boolean strConvPresent = false;
    boolean intConvPresent = false;
    boolean floatConvPresent = false;
    boolean boolConvPresent = false;
    boolean conversionDetected = false;
    boolean hasAuxConvCheck = false;
    private List convVarList = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/yafv/CUP$YAFVParser$actions$ExpressionBuffer.class */
    public class ExpressionBuffer {
        private StringBuffer javaStrBuffer;
        private StringBuffer jsStrBuffer;

        ExpressionBuffer() {
            CUP$YAFVParser$actions.this = r5;
            this.javaStrBuffer = new StringBuffer();
            this.jsStrBuffer = new StringBuffer();
        }

        ExpressionBuffer(ExpressionBuffer eb) {
            CUP$YAFVParser$actions.this = r4;
            this.javaStrBuffer = eb.javaStrBuffer;
            this.jsStrBuffer = eb.jsStrBuffer;
        }

        ExpressionBuffer(String str) {
            CUP$YAFVParser$actions.this = r6;
            this.javaStrBuffer = new StringBuffer(str);
            this.jsStrBuffer = new StringBuffer(str);
        }

        public ExpressionBuffer append(ExpressionBuffer eb) {
            this.javaStrBuffer.append(eb.javaStrBuffer);
            this.jsStrBuffer.append(eb.jsStrBuffer);
            return this;
        }

        public ExpressionBuffer appendStr(String str) {
            this.javaStrBuffer.append(str);
            this.jsStrBuffer.append(str);
            return this;
        }

        public ExpressionBuffer appendJavaStr(String javaStr) {
            this.javaStrBuffer.append(javaStr);
            return this;
        }

        public ExpressionBuffer appendJsStr(String jsStr) {
            this.jsStrBuffer.append(jsStr);
            return this;
        }

        public String getJavaStr() {
            return this.javaStrBuffer.toString();
        }

        public String getJsStr() {
            return this.jsStrBuffer.toString();
        }
    }

    public void updateFunctionTable(String functionName, String packName, int params) {
        if (this.functionTable.get(functionName) == null) {
            this.functionTable.put(functionName, packName);
            this.referenceTable.put(functionName, params <= 1 ? "" : String.valueOf(params));
            return;
        }
        this.parser.exitErrorMessage("The function '" + functionName + "' has been already declared using the package '" + ((String) this.functionTable.get(functionName)) + "' and cannot be defined more than once");
    }

    protected void clearConvData() {
        this.conversionDetected = false;
        this.convVarList.clear();
    }

    protected void setIntConvDetected() {
        this.intConvPresent = true;
        this.conversionDetected = true;
        this.hasAuxConvCheck = true;
    }

    protected void setFloatConvDetected() {
        this.floatConvPresent = true;
        this.conversionDetected = true;
        this.hasAuxConvCheck = true;
    }

    protected void setStrConvDetected() {
        this.strConvPresent = true;
        this.conversionDetected = true;
    }

    protected void setBoolConvDetected() {
        this.boolConvPresent = true;
        this.conversionDetected = true;
    }

    protected void resetConvState() {
        this.hasAuxConvCheck = false;
    }

    protected void appendVarForCheck(String var) {
        if (!this.convVarList.contains(var)) {
            this.convVarList.add(var);
        }
    }

    protected void printJsCheckIfConversion() {
        if (this.conversionDetected && !this.convVarList.isEmpty()) {
            Iterator i = this.convVarList.iterator();
            boolean isFirst = true;
            boolean isOne = this.convVarList.size() == 1;
            this.parser.f284js.print("    if " + (isOne ? "(" : "(("));
            while (i.hasNext()) {
                if (!isFirst) {
                    this.parser.f284js.print(") || (");
                } else {
                    isFirst = false;
                }
                this.parser.f284js.print(((String) i.next()) + " == ''");
            }
            this.parser.f284js.println((isOne ? ")" : "))") + " return V_INCOMPLETE;");
        }
    }

    public CUP$YAFVParser$actions(YAFVParser parser) {
        this.parser = parser;
    }

    public final Symbol CUP$YAFVParser$do_action(int CUP$YAFVParser$act_num, lr_parser CUP$YAFVParser$parser, Stack CUP$YAFVParser$stack, int CUP$YAFVParser$top) throws Exception {
        ExpressionBuffer RESULT;
        ExpressionBuffer RESULT2;
        ExpressionBuffer RESULT3;
        String exprJavaStr2;
        switch (CUP$YAFVParser$act_num) {
            case 0:
                this.parser.f284js.println("<if !FM_defined_yafv_package><assign FM_defined_yafv_package = \"" + this.parser.jsPackage() + "\">");
                this.parser.f284js.println("var ThisField;\nvar checkFN = new Array();");
                this.parser.f284js.println("var existingVF = new Array();");
                this.parser.f284js.println("window.document.VFScriptExist = true;");
                this.parser.f284js.println("var status_images = new Array('" + this.parser.img1() + "','" + this.parser.img2() + "','" + this.parser.img3() + "');");
                this.parser.f284js.println("</if>");
                this.parser.f284js.println("<compress>\n");
                Symbol CUP$YAFVParser$result = new Symbol(41, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result;
            case 1:
                Object RESULT4 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    RESULT4 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                Symbol CUP$YAFVParser$result2 = new Symbol(1, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT4);
                return CUP$YAFVParser$result2;
            case 2:
                int i = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i2 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                Object start_val = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                Symbol CUP$YAFVParser$result3 = new Symbol(0, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, start_val);
                CUP$YAFVParser$parser.done_parsing();
                return CUP$YAFVParser$result3;
            case 3:
                Symbol CUP$YAFVParser$result4 = new Symbol(2, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result4;
            case 4:
                Symbol CUP$YAFVParser$result5 = new Symbol(2, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result5;
            case 5:
                Symbol CUP$YAFVParser$result6 = new Symbol(2, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result6;
            case 6:
                Symbol CUP$YAFVParser$result7 = new Symbol(2, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result7;
            case 7:
                Symbol CUP$YAFVParser$result8 = new Symbol(2, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result8;
            case 8:
                Symbol CUP$YAFVParser$result9 = new Symbol(2, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result9;
            case 9:
                this.parser.f284js.println("<if FM_defined_yafv_package == \"" + this.parser.jsPackage() + "\">");
                if (!this.referenceTable.isEmpty()) {
                    this.parser.f284js.println("  <if ! defined_referencedFunction>\n    <function FM_" + this.parser.jsPackagePrefix() + "referencedFunction(__vfn, __ps)>\n\t<if __ps == \"\"><assign __vfs = __vfn><else><assign __vfs = __vfn + \":\" + __ps></if>\n\t<switch __vfs>");
                    Enumeration rtf = this.referenceTable.keys();
                    while (rtf.hasMoreElements()) {
                        this.f283fn = (String) rtf.nextElement();
                        String ps = (String) this.referenceTable.get(this.f283fn);
                        if (!"".equals(ps)) {
                            ps = ":" + ps;
                        }
                        this.pack = (String) this.functionTable.get(this.f283fn);
                        this.parser.f284js.println("\t    <case \"" + this.f283fn + ps + "\"><call " + TemplateMethodWrapper.PREFIX + C0026yafv.shortName(this.pack) + "_" + this.f283fn + "()><break>");
                    }
                    this.parser.f284js.println("\t    <default><if __ps == \"\"><assign __ps = \"1\"></if>\n\t\t// VALIDATION: No function \"${__vfn}\" (with ${__ps} parameter[s]) has been defined in the package.");
                    this.parser.f284js.println("\t</switch>");
                    this.parser.f284js.println("\t<assign defined_referencedFunction = \"1\">");
                    this.parser.f284js.println("    </function>");
                    this.parser.f284js.println("  </if>");
                    this.parser.f284js.println();
                    this.parser.f284js.println("  <assign FM_referencedFunction = FM_" + this.parser.jsPackagePrefix() + "referencedFunction>");
                    if (this.strParamsPresent || this.strConvPresent) {
                        this.parser.java.println("    static String getAsString(String value) {\n\treturn value != null ? value : \"\";\n    }\n");
                    }
                    if (this.strConvPresent) {
                        this.parser.java.println("    static String getAsString(int value) {\n\treturn String.valueOf(value);\n    }\n\n    static String getAsString(float value) {\n\treturn String.valueOf(value);\n    }\n\n    static String getAsString(boolean value) {\n\treturn String.valueOf(value);\n    }\n");
                    }
                    if (this.intParamsPresent || this.intConvPresent) {
                        this.parser.java.println("    static int getAsInt(String value) throws NumberFormatException {\n\treturn Integer.parseInt(value);\n    }\n");
                    }
                    if (this.intConvPresent) {
                        this.parser.java.println("    static int getAsInt(int value) {\n\treturn value;\n    }\n\n    static int getAsInt(float value) throws NumberFormatException {\n\treturn Integer.parseInt(String.valueOf(value));\n    }\n\n    static int getAsInt(boolean value) throws NumberFormatException {\n\tthrow new NumberFormatException(\"Cannot convert a boolean into 'int'.\");\n    }\n");
                    }
                    if (this.floatParamsPresent || this.floatConvPresent) {
                        this.parser.java.println("    static float getAsFloat(String value) throws NumberFormatException {\n\treturn Float.parseFloat(value);\n    }\n");
                    }
                    if (this.floatConvPresent) {
                        this.parser.java.println("    static float getAsFloat(int value) {\n\treturn (float)value;\n    }\n\n    static float getAsFloat(float value) {\n\treturn value;\n    }\n\n    static float getAsFloat(boolean value) throws NumberFormatException {\n\tthrow new NumberFormatException(\"Cannot convert a boolean into 'float'.\");\n    }\n");
                    }
                    if (this.boolParamsPresent || this.floatConvPresent) {
                        this.parser.java.println("    static boolean getAsBoolean(String value) {\n\treturn (value != null) && !\"\".equals(value);\n    }\n");
                    }
                    if (this.boolConvPresent) {
                        this.parser.java.println("    static boolean getAsBoolean(int value) {\n\treturn value != 0;\n    }\n\n    static boolean getAsBoolean(float value) {\n\treturn value != 0;\n    }\n\n    static boolean getAsBoolean(boolean value) {\n\treturn value;\n    }\n");
                    }
                }
                Enumeration e = this.functionTable.keys();
                while (e.hasMoreElements()) {
                    this.f283fn = (String) e.nextElement();
                    this.pack = (String) this.functionTable.get(this.f283fn);
                    if (this.pack != null) {
                        this.parser.f284js.println("  <assign FM_" + this.f283fn + " = " + TemplateMethodWrapper.PREFIX + C0026yafv.shortName(this.pack) + "_" + this.f283fn + ">");
                    } else {
                        this.parser.exitErrorMessage("Unable to determine a package where the '" + this.f283fn + "' function is defined");
                    }
                }
                this.parser.f284js.println("</if>");
                this.parser.f284js.println("</compress>");
                Symbol CUP$YAFVParser$result10 = new Symbol(3, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result10;
            case 10:
                int i3 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i4 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String baseYafv = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                this.functionList = new LinkedList();
                this.yafvName = C0026yafv.cutDotYafv(baseYafv.toString());
                Symbol CUP$YAFVParser$result11 = new Symbol(42, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result11;
            case 11:
                Object RESULT5 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    RESULT5 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                int i5 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left;
                int i6 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).right;
                String str = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).value;
                StringBuffer yafvJSIncName = new StringBuffer();
                StringBuffer yafvJClassName = new StringBuffer();
                StringTokenizer st = new StringTokenizer(this.yafvName, "/.");
                String str2 = null;
                while (true) {
                    String shortName = str2;
                    if (st.hasMoreTokens()) {
                        if (shortName != null) {
                            yafvJClassName.append(shortName).append(".");
                        }
                        str2 = st.nextToken();
                    } else {
                        yafvJClassName.append(shortName);
                        yafvJSIncName.append(this.parser.jsDir()).append(shortName);
                        if (this.iYafvTable.get(yafvJSIncName) == null) {
                            this.parser.f284js.println("<include \"" + ((Object) yafvJSIncName) + ".js\">");
                            this.iYafvTable.put(yafvJSIncName, "1");
                        }
                        Iterator iter = this.functionList.iterator();
                        while (iter.hasNext()) {
                            Hashtable fn = (Hashtable) iter.next();
                            String name = fn.get("name").toString();
                            List<Object> pTypes = (List) fn.get("ptypes");
                            this.parser.f284js.println("<function FM_" + this.parser.jsPackagePrefix() + name + "() >");
                            this.parser.f284js.println("  <call FM_" + shortName + "_" + name + "()>");
                            this.parser.f284js.println("</function>\n");
                            this.parser.java.print("    protected static boolean " + name + "(");
                            this.pNum = 0;
                            StringBuffer paramDeclaration2 = new StringBuffer();
                            StringBuffer paramPassing12 = new StringBuffer();
                            if (pTypes == null) {
                                this.parser.java.print("String p_" + String.valueOf(this.pNum));
                                paramDeclaration2.append("String p_" + String.valueOf(this.pNum));
                                paramPassing12.append("p_" + String.valueOf(this.pNum));
                            } else {
                                for (Object obj : pTypes) {
                                    String pType = obj.toString();
                                    if (this.pNum > 0) {
                                        this.parser.java.print(", ");
                                        paramDeclaration2.append(", ");
                                        paramPassing12.append(", ");
                                    }
                                    this.parser.java.print(pType + " p_" + String.valueOf(this.pNum));
                                    paramDeclaration2.append("String p_" + String.valueOf(this.pNum));
                                    paramPassing12.append("p_" + String.valueOf(this.pNum));
                                    this.pNum++;
                                }
                            }
                            this.parser.java.println(") {");
                            this.parser.java.println("\treturn " + this.parser.javaPackagePrefix() + this.yafvName + "." + name + "(" + paramPassing12.toString() + ");");
                            this.parser.java.println("    }\n");
                            this.parser.java.println("    public static TemplateModel " + name + "(String label, " + paramDeclaration2.toString() + ") {\n\treturn " + this.parser.javaPackagePrefix() + this.yafvName + "." + name + "(label, " + paramPassing12.toString() + ");\n    }\n");
                        }
                        this.parser.f284js.println();
                        Symbol CUP$YAFVParser$result12 = new Symbol(4, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT5);
                        return CUP$YAFVParser$result12;
                    }
                }
            case 12:
                int i7 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i8 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String fName = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                int i9 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i10 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Object pTypes2 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                Hashtable fn2 = new Hashtable();
                fn2.put("name", fName);
                int params = 1;
                if (pTypes2 != null) {
                    fn2.put("ptypes", pTypes2);
                    params = ((List) pTypes2).size();
                }
                updateFunctionTable(fName, this.yafvName, params);
                this.functionList.add(fn2);
                Symbol CUP$YAFVParser$result13 = new Symbol(39, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result13;
            case 13:
                Symbol CUP$YAFVParser$result14 = new Symbol(40, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result14;
            case 14:
                Symbol CUP$YAFVParser$result15 = new Symbol(40, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result15;
            case 15:
                this.param_types = new ArrayList();
                Symbol CUP$YAFVParser$result16 = new Symbol(43, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result16;
            case 16:
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    Object obj2 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                Object RESULT6 = this.param_types;
                Symbol CUP$YAFVParser$result17 = new Symbol(12, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT6);
                return CUP$YAFVParser$result17;
            case 17:
                Symbol CUP$YAFVParser$result18 = new Symbol(12, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result18;
            case 18:
                int i11 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).left;
                int i12 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).right;
                String id = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value;
                int i13 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i14 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                String regexpstr = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                updateFunctionTable(id, this.parser.jsPackage(), 1);
                StringWriter jsRegExp = new StringWriter();
                String dfaRegExp = null;
                try {
                    dfaRegExp = new RunAutomaton(new RegExp(regexpstr).toAutomaton()).toString();
                } catch (Exception ex) {
                    this.parser.exitErrorMessage("Unable to get the DFA representation for the '" + id + "' regular expression:\n" + regexpstr + "\n", ex);
                }
                DFALexer dfaLexer = new DFALexer(new StringReader(dfaRegExp));
                DFAParser dfaParser = new DFAParser(dfaLexer, jsRegExp);
                try {
                    dfaParser.parse();
                } catch (Exception ex2) {
                    this.parser.exitErrorMessage("DFA: \n---\n" + dfaRegExp + "\n---\nUnable to parse the DFA representation for the " + id + " = \"" + regexpstr + "\" regular expression\n", ex2);
                }
                this.parser.f284js.println("<function FM_" + this.parser.jsPackagePrefix() + id + "() >");
                this.parser.f284js.println("<if ! defined_" + id + " >");
                this.parser.f284js.print("var __" + id + "=");
                this.parser.f284js.println(jsRegExp.toString() + ";\n");
                this.parser.f284js.println("function " + id + "(fieldvalue) {");
                String jsRStatement = "validate(__" + id + ", fieldvalue)";
                if (this.maxLength >= 0) {
                    jsRStatement = "AND_(LE_(fieldvalue.length, " + String.valueOf(this.maxLength) + "), " + jsRStatement + ")";
                }
                if (this.minLength >= 0) {
                    jsRStatement = "AND_(GE_(fieldvalue.length, " + String.valueOf(this.minLength) + "), " + jsRStatement + ")";
                }
                this.parser.f284js.println("    return " + jsRStatement + ";");
                this.parser.f284js.println("}\n");
                this.parser.f284js.println("existingVF['_" + id + "']=true;\n");
                this.parser.f284js.println("function _" + id + "(field) {");
                this.parser.f284js.println("    setStates(" + this.validJs + "," + this.failJs + ");");
                this.parser.f284js.println("    if (field == null) return V_INCOMPLETE;");
                this.parser.f284js.println("    return " + id + "(field.value);\n}");
                this.parser.f284js.println("<assign defined_" + id + " = \"1\">");
                this.parser.f284js.println("</if>");
                this.parser.f284js.println("</function>\n");
                StringBuffer javaRegexp = new StringBuffer();
                char[] reChars = regexpstr.toCharArray();
                for (char reChar : reChars) {
                    if (reChar == '\\') {
                        javaRegexp.append("\\\\");
                    } else if (reChar == '\"') {
                        javaRegexp.append("\\\"");
                    } else if (reChar == '\r') {
                        javaRegexp.append("\\r");
                    } else if (reChar == '\n') {
                        javaRegexp.append("\\n");
                    } else if (reChar >= ' ' && reChar <= '~') {
                        javaRegexp.append(reChar);
                    } else {
                        javaRegexp.append("\\u");
                        String uChar = Integer.toHexString(reChar);
                        switch (uChar.length()) {
                            case 1:
                                javaRegexp.append("000");
                                break;
                            case 2:
                                javaRegexp.append("00");
                                break;
                            case 3:
                                javaRegexp.append("0");
                                break;
                        }
                        javaRegexp.append(uChar);
                    }
                }
                this.parser.java.println("    static RunAutomaton __" + id + ";\n    static {\n\ttry {\n\t    __" + id + "= new RunAutomaton((new RegExp(\n\t\t\"" + javaRegexp.toString() + "\")).toAutomaton());\n\t} catch (Exception rae) {\n\t    throw new ExceptionInInitializerError(rae);\n\t}\n    }\n\n    protected static boolean " + id + "(String str) {\n\tif (str == null) str = \"\";");
                String javaRStatement = "__" + id + ".run(str)";
                if (this.maxLength >= 0) {
                    javaRStatement = "(str.length() <= " + String.valueOf(this.maxLength) + ") && (" + javaRStatement + ")";
                }
                if (this.minLength >= 0) {
                    javaRStatement = "(str.length() >= " + String.valueOf(this.minLength) + ") && (" + javaRStatement + ")";
                }
                this.parser.java.println("\treturn  " + javaRStatement + ";\n    }\n\n    public static TemplateModel " + id + "(String label, String str) {\n\treturn " + id + "(str) ? null : new TemplateString(label + \" : \" + " + this.failJava + ");\n    }\n");
                Symbol CUP$YAFVParser$result19 = new Symbol(5, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result19;
            case 19:
                this.maxLength = -1;
                this.minLength = -1;
                this.validJs = "\"\"";
                this.validJava = "\"\"";
                this.failJs = "\"\"";
                this.failJava = "\"\"";
                Symbol CUP$YAFVParser$result20 = new Symbol(44, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result20;
            case 20:
                Object RESULT7 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT7 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result21 = new Symbol(33, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT7);
                return CUP$YAFVParser$result21;
            case 21:
                Symbol CUP$YAFVParser$result22 = new Symbol(34, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result22;
            case 22:
                Symbol CUP$YAFVParser$result23 = new Symbol(34, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result23;
            case 23:
                int i15 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i16 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String str3 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                try {
                    this.maxLength = Integer.valueOf(str3).intValue();
                } catch (NumberFormatException e2) {
                    this.parser.exitErrorMessage("Unable to parse the 'maxlength' value: [" + str3 + "]");
                }
                Symbol CUP$YAFVParser$result24 = new Symbol(35, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result24;
            case 24:
                int i17 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i18 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String str4 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                try {
                    this.minLength = Integer.valueOf(str4).intValue();
                } catch (NumberFormatException e3) {
                    this.parser.exitErrorMessage("Unable to parse the 'minlength' value: [" + str4 + "]");
                }
                Symbol CUP$YAFVParser$result25 = new Symbol(35, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result25;
            case 25:
                int i19 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i20 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                String id2 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                this.constrainName = id2;
                this.parser.f284js.println("<function FM_" + this.parser.jsPackagePrefix() + id2 + "() >");
                this.parser.f284js.println("<if ! defined_" + id2 + " >");
                this.parser.f284js.print("function " + id2 + "(");
                this.parser.java.print("    protected static boolean " + id2 + "(");
                this.params = new ArrayList();
                this.param_types = new ArrayList();
                Symbol CUP$YAFVParser$result26 = new Symbol(45, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result26;
            case yafvsym.MOD /* 26 */:
                int i21 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).left;
                int i22 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).right;
                String str5 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).value;
                updateFunctionTable(this.constrainName, this.parser.jsPackage(), this.params.size());
                this.parser.f284js.print(") ");
                this.parser.java.print(") ");
                Symbol CUP$YAFVParser$result27 = new Symbol(46, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result27;
            case yafvsym.AND /* 27 */:
                int i23 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 7)).left;
                int i24 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 7)).right;
                String str6 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 7)).value;
                this.parser.f284js.println("{");
                this.parser.java.println("{");
                this.includedFunctions = new ArrayList();
                Symbol CUP$YAFVParser$result28 = new Symbol(47, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result28;
            case yafvsym.f286OR /* 28 */:
                Object RESULT8 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 7)).value != null) {
                    RESULT8 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 7)).value;
                }
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value != null) {
                    RESULT8 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value;
                }
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    RESULT8 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                int i25 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 10)).left;
                int i26 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 10)).right;
                String id3 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 10)).value;
                this.parser.f284js.println("}\n");
                this.parser.java.println("    }\n");
                boolean fparam = true;
                StringBuffer jsFnCode = new StringBuffer();
                ExpressionBuffer callParams = new ExpressionBuffer();
                ExpressionBuffer declParams = new ExpressionBuffer();
                Iterator j = this.param_types.iterator();
                for (Object obj3 : this.params) {
                    this.param = obj3.toString();
                    this.param_type = j.next().toString();
                    if (!fparam) {
                        callParams.appendStr(", ");
                        declParams.appendJsStr(", ");
                    } else {
                        fparam = false;
                    }
                    declParams.appendJsStr(this.param);
                    declParams.appendJavaStr(", String " + this.param);
                    if (this.param_type.equals("int")) {
                        jsFnCode.append("    var _" + this.param + " = getStringValue(" + this.param + ");\n    __spCheck = checkInteger_(_" + this.param + ");\n    if (__spCheck != V_ACCEPT) return __spCheck;\n");
                        callParams.appendJsStr("parseInt(_" + this.param + ")");
                        callParams.appendJavaStr("getAsInt(" + this.param + ")");
                        this.intParamsPresent = true;
                    } else if (this.param_type.equals("float")) {
                        jsFnCode.append("    var _" + this.param + " = getStringValue(" + this.param + ");\n    __spCheck = checkFloat_(_" + this.param + ");\n    if (__spCheck != V_ACCEPT) return __spCheck;\n");
                        callParams.appendJsStr("parseFloat(_" + this.param + ")");
                        callParams.appendJavaStr("getAsFloat(" + this.param + ")");
                        this.floatParamsPresent = true;
                    } else if (this.param_type.equals("boolean")) {
                        callParams.appendJsStr("getBooleanValue(" + this.param + ")");
                        callParams.appendJavaStr("getAsBoolean(" + this.param + ")");
                        this.boolParamsPresent = true;
                    } else {
                        callParams.appendJsStr("getStringValue(" + this.param + ")");
                        callParams.appendJavaStr("getAsString(" + this.param + ")");
                        this.strParamsPresent = true;
                    }
                }
                this.parser.f284js.println("existingVF['_" + id3 + "']=true;\n\nfunction _" + id3 + "(" + declParams.getJsStr() + ") {\n    setStates(" + this.validJs + "," + this.failJs + ");\n" + jsFnCode.toString() + "    return " + id3 + "(" + callParams.getJsStr() + ");\n}\n<assign defined_" + id3 + " = \"1\">");
                if (!this.includedFunctions.isEmpty()) {
                    Iterator ifi = this.includedFunctions.iterator();
                    while (ifi.hasNext()) {
                        this.parser.f284js.println("<call FM_" + this.parser.jsPackagePrefix() + ifi.next().toString() + "()>");
                    }
                }
                this.parser.f284js.println("</if>\n</function>\n");
                this.parser.java.println("    public static TemplateModel " + id3 + "(String label" + declParams.getJavaStr() + ") {\n\tTemplateString validres = null;\n\tTemplateString failres = new TemplateString(label + \" : \" + " + this.failJava + ");\n\ttry {\n\t    return " + id3 + "(" + callParams.getJavaStr() + ")\n\t\t? validres : failres;\n\t} catch (NumberFormatException nfe) {\n\t    return failres;\n\t}\n    }\n");
                Symbol CUP$YAFVParser$result29 = new Symbol(6, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 11)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT8);
                return CUP$YAFVParser$result29;
            case yafvsym.f285LT /* 29 */:
                Symbol CUP$YAFVParser$result30 = new Symbol(9, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result30;
            case 30:
                this.parser.f284js.print("{\n    ");
                this.parser.java.print("{\n\t");
                Symbol CUP$YAFVParser$result31 = new Symbol(48, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result31;
            case yafvsym.LTEQ /* 31 */:
                Object RESULT9 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    RESULT9 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                this.parser.f284js.println("    }");
                this.parser.java.println("\t}");
                Symbol CUP$YAFVParser$result32 = new Symbol(17, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT9);
                return CUP$YAFVParser$result32;
            case 32:
                Symbol CUP$YAFVParser$result33 = new Symbol(15, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result33;
            case yafvsym.f288EQ /* 33 */:
                Symbol CUP$YAFVParser$result34 = new Symbol(15, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result34;
            case yafvsym.NEQ /* 34 */:
                Symbol CUP$YAFVParser$result35 = new Symbol(16, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result35;
            case yafvsym.NOT /* 35 */:
                Symbol CUP$YAFVParser$result36 = new Symbol(16, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result36;
            case yafvsym.RETURN_CALL /* 36 */:
                Symbol CUP$YAFVParser$result37 = new Symbol(16, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result37;
            case yafvsym.WHILE /* 37 */:
                Symbol CUP$YAFVParser$result38 = new Symbol(16, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result38;
            case yafvsym.f287IF /* 38 */:
                Symbol CUP$YAFVParser$result39 = new Symbol(16, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result39;
            case yafvsym.ELSE /* 39 */:
                int i27 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i28 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                Object typeName = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                int i29 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i30 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String id4 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                this.parser.f284js.println("    var " + id4 + ";");
                this.parser.java.println("\t" + typeName + " " + id4 + ";");
                Symbol CUP$YAFVParser$result40 = new Symbol(18, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result40;
            case yafvsym.STRINGLITERAL /* 40 */:
                int i31 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i32 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                Object obj4 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                int i33 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i34 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String str7 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                this.exprBuffer = new ExpressionBuffer();
                clearConvData();
                Symbol CUP$YAFVParser$result41 = new Symbol(49, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result41;
            case yafvsym.INTEGER_CONSTANT /* 41 */:
                Object RESULT10 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    RESULT10 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                int i35 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).left;
                int i36 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).right;
                Object typeName2 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).value;
                int i37 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).left;
                int i38 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).right;
                String id5 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value;
                int i39 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i40 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                ExpressionBuffer expr_str = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                printJsCheckIfConversion();
                this.parser.f284js.print("    var " + id5 + " = ");
                this.parser.java.print("\t" + typeName2 + " " + id5 + " = ");
                this.parser.f284js.print(expr_str.getJsStr());
                this.parser.java.print(expr_str.getJavaStr());
                this.parser.f284js.println(";");
                this.parser.java.println(";");
                Symbol CUP$YAFVParser$result42 = new Symbol(18, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 5)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT10);
                return CUP$YAFVParser$result42;
            case yafvsym.FLOAT_CONSTANT /* 42 */:
                int i41 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i42 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String str8 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                clearConvData();
                Symbol CUP$YAFVParser$result43 = new Symbol(50, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result43;
            case yafvsym.BOOL_CONSTANT /* 43 */:
                Object RESULT11 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value != null) {
                    RESULT11 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                }
                int i43 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).left;
                int i44 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).right;
                String id6 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value;
                int i45 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i46 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                ExpressionBuffer exprBuffer = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                printJsCheckIfConversion();
                this.parser.f284js.print(id6 + " = ");
                this.parser.java.print(id6 + " = ");
                this.parser.f284js.print(exprBuffer.getJsStr());
                this.parser.java.print(exprBuffer.getJavaStr());
                this.parser.f284js.println(";");
                this.parser.java.println(";");
                Symbol CUP$YAFVParser$result44 = new Symbol(19, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT11);
                return CUP$YAFVParser$result44;
            case 44:
                clearConvData();
                Symbol CUP$YAFVParser$result45 = new Symbol(51, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result45;
            case 45:
                int i47 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i48 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer exprBuffer2 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                printJsCheckIfConversion();
                this.parser.f284js.print("    return ");
                this.parser.java.print("\treturn ");
                this.parser.f284js.print(exprBuffer2.getJsStr());
                this.parser.java.print(exprBuffer2.getJavaStr());
                Symbol CUP$YAFVParser$result46 = new Symbol(52, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result46;
            case 46:
                Object RESULT12 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).value != null) {
                    RESULT12 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).value;
                }
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT12 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                int i49 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i50 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                ExpressionBuffer expressionBuffer = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                this.parser.f284js.println(";");
                this.parser.java.println(";");
                Symbol CUP$YAFVParser$result47 = new Symbol(20, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT12);
                return CUP$YAFVParser$result47;
            case 47:
                clearConvData();
                Symbol CUP$YAFVParser$result48 = new Symbol(53, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result48;
            case 48:
                int i51 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i52 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                ExpressionBuffer exprBuffer3 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                printJsCheckIfConversion();
                this.parser.f284js.print("    while (");
                this.parser.java.print("\twhile (");
                this.parser.f284js.print(exprBuffer3.getJsStr());
                this.parser.java.print(exprBuffer3.getJavaStr());
                this.parser.f284js.println(")");
                this.parser.java.println(")");
                Symbol CUP$YAFVParser$result49 = new Symbol(54, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result49;
            case 49:
                Object RESULT13 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value != null) {
                    RESULT13 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value;
                }
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT13 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                int i53 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left;
                int i54 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).right;
                ExpressionBuffer expressionBuffer2 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).value;
                Symbol CUP$YAFVParser$result50 = new Symbol(22, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 6)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT13);
                return CUP$YAFVParser$result50;
            case MySQLResource.MAX_DB_NAME /* 50 */:
                clearConvData();
                Symbol CUP$YAFVParser$result51 = new Symbol(55, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result51;
            case 51:
                int i55 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i56 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                ExpressionBuffer exprBuffer4 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                printJsCheckIfConversion();
                this.parser.f284js.print("    if (");
                this.parser.java.print("\tif (");
                this.parser.f284js.print(exprBuffer4.getJsStr());
                this.parser.java.print(exprBuffer4.getJavaStr());
                this.parser.f284js.print(") ");
                this.parser.java.print(") ");
                Symbol CUP$YAFVParser$result52 = new Symbol(56, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result52;
            case 52:
                Object RESULT14 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value != null) {
                    RESULT14 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 4)).value;
                }
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT14 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                int i57 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left;
                int i58 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).right;
                ExpressionBuffer expressionBuffer3 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).value;
                Symbol CUP$YAFVParser$result53 = new Symbol(21, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 6)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT14);
                return CUP$YAFVParser$result53;
            case 53:
                Symbol CUP$YAFVParser$result54 = new Symbol(23, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result54;
            case 54:
                this.parser.f284js.print("    else ");
                this.parser.java.print("\telse ");
                Symbol CUP$YAFVParser$result55 = new Symbol(57, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result55;
            case 55:
                Object RESULT15 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT15 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result56 = new Symbol(23, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT15);
                return CUP$YAFVParser$result56;
            case 56:
                int i59 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i60 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                ExpressionBuffer exprBuffer1 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                int i61 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i62 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String op = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                int i63 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i64 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer exprBuffer22 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                if ("||".equals(op)) {
                    ExpressionBuffer exprBuffer5 = new ExpressionBuffer();
                    exprBuffer5.appendJavaStr(exprBuffer1.getJavaStr()).appendJavaStr(" || ").appendJavaStr(exprBuffer22.getJavaStr());
                    exprBuffer5.appendJsStr("OR_(").appendJsStr(exprBuffer1.getJsStr()).appendJsStr(",").appendJsStr(exprBuffer22.getJsStr()).appendJsStr(")");
                    RESULT = exprBuffer5;
                } else {
                    RESULT = exprBuffer1.appendStr(op).append(exprBuffer22);
                }
                Symbol CUP$YAFVParser$result57 = new Symbol(24, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT);
                return CUP$YAFVParser$result57;
            case 57:
                int i65 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i66 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Symbol CUP$YAFVParser$result58 = new Symbol(24, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                return CUP$YAFVParser$result58;
            case 58:
                int i67 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i68 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer RESULT16 = new ExpressionBuffer("-").append((ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                Symbol CUP$YAFVParser$result59 = new Symbol(24, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT16);
                return CUP$YAFVParser$result59;
            case 59:
                int i69 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i70 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                Object tcBuffer = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                int i71 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i72 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer RESULT17 = ((ExpressionBuffer) tcBuffer).appendStr("(").append((ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value).appendStr(")");
                resetConvState();
                Symbol CUP$YAFVParser$result60 = new Symbol(24, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT17);
                return CUP$YAFVParser$result60;
            case 60:
                int i73 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i74 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Symbol CUP$YAFVParser$result61 = new Symbol(25, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                return CUP$YAFVParser$result61;
            case 61:
                int i75 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i76 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                ExpressionBuffer exprBuffer12 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                int i77 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i78 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String op2 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                int i79 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i80 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer exprBuffer23 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                if ("&&".equals(op2)) {
                    ExpressionBuffer exprBuffer6 = new ExpressionBuffer();
                    exprBuffer6.appendJavaStr(exprBuffer12.getJavaStr()).appendJavaStr(" && ").appendJavaStr(exprBuffer23.getJavaStr());
                    exprBuffer6.appendJsStr("AND_(").appendJsStr(exprBuffer12.getJsStr()).appendJsStr(",").appendJsStr(exprBuffer23.getJsStr()).appendJsStr(")");
                    RESULT2 = exprBuffer6;
                } else {
                    RESULT2 = exprBuffer12.appendStr(op2).append(exprBuffer23);
                }
                Symbol CUP$YAFVParser$result62 = new Symbol(25, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT2);
                return CUP$YAFVParser$result62;
            case 62:
                int i81 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i82 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                ExpressionBuffer RESULT18 = new ExpressionBuffer("(").append((ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value).appendStr(")");
                Symbol CUP$YAFVParser$result63 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT18);
                return CUP$YAFVParser$result63;
            case 63:
                int i83 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i84 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String id7 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                if (this.hasAuxConvCheck) {
                    appendVarForCheck(id7);
                }
                ExpressionBuffer RESULT19 = new ExpressionBuffer(id7);
                Symbol CUP$YAFVParser$result64 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT19);
                return CUP$YAFVParser$result64;
            case SignupGuard.TRIAL_ACCOUNT_FLAG /* 64 */:
                int i85 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i86 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer RESULT20 = new ExpressionBuffer("\"" + ((String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value) + "\"");
                Symbol CUP$YAFVParser$result65 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT20);
                return CUP$YAFVParser$result65;
            case 65:
                int i87 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i88 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer RESULT21 = new ExpressionBuffer((String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                Symbol CUP$YAFVParser$result66 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT21);
                return CUP$YAFVParser$result66;
            case 66:
                int i89 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i90 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer RESULT22 = new ExpressionBuffer((String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                Symbol CUP$YAFVParser$result67 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT22);
                return CUP$YAFVParser$result67;
            case 67:
                int i91 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i92 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer exprBuffer13 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                ExpressionBuffer exprBuffer7 = new ExpressionBuffer();
                exprBuffer7.appendJsStr("NOT_(").appendJsStr(exprBuffer13.getJsStr()).appendJsStr(")");
                exprBuffer7.appendJavaStr(" !").appendJavaStr(exprBuffer13.getJavaStr());
                Symbol CUP$YAFVParser$result68 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, exprBuffer7);
                return CUP$YAFVParser$result68;
            case 68:
                int i93 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i94 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String str9 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                ExpressionBuffer RESULT23 = new ExpressionBuffer().appendJavaStr(str9).appendJsStr("true".equals(str9) ? "V_ACCEPT" : "V_REJECT");
                Symbol CUP$YAFVParser$result69 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT23);
                return CUP$YAFVParser$result69;
            case 69:
                int i95 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i96 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Symbol CUP$YAFVParser$result70 = new Symbol(26, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                return CUP$YAFVParser$result70;
            case 70:
                int i97 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left;
                int i98 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).right;
                String id8 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).value;
                int i99 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i100 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                ExpressionBuffer exprBuffer8 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                this.includedFunctions.add(id8);
                ExpressionBuffer RESULT24 = new ExpressionBuffer(id8).appendStr("(").append(exprBuffer8).appendStr(")");
                Symbol CUP$YAFVParser$result71 = new Symbol(28, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT24);
                return CUP$YAFVParser$result71;
            case 71:
                int i101 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i102 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Symbol CUP$YAFVParser$result72 = new Symbol(29, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                return CUP$YAFVParser$result72;
            case 72:
                int i103 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i104 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                ExpressionBuffer exprBuffer14 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                int i105 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i106 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer RESULT25 = exprBuffer14.appendStr(", ").append((ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                Symbol CUP$YAFVParser$result73 = new Symbol(29, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT25);
                return CUP$YAFVParser$result73;
            case 73:
                int i107 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left;
                int i108 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).right;
                ExpressionBuffer exprBuffer15 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).value;
                int i109 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i110 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                String op3 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                int i111 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i112 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                ExpressionBuffer exprBuffer24 = (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                if ("==".equals(op3)) {
                    ExpressionBuffer exprBuffer9 = new ExpressionBuffer();
                    String exprJsStr1 = exprBuffer15.getJsStr();
                    String exprJsStr2 = exprBuffer24.getJsStr();
                    if ("\"\"".equals(exprJsStr1) || "\"\"".equals(exprJsStr2)) {
                        if ("\"\"".equals(exprJsStr1)) {
                            exprJavaStr2 = exprBuffer24.getJavaStr();
                        } else {
                            exprJsStr2 = exprJsStr1;
                            exprJavaStr2 = exprBuffer15.getJavaStr();
                        }
                        exprBuffer9.appendJsStr("\"\" == ").appendJsStr(exprJsStr2).appendJsStr(" ? V_ACCEPT : V_REJECT");
                        exprBuffer9.appendJavaStr("\"\".equals(String.valueOf(").appendJavaStr(exprJavaStr2).appendJavaStr("))");
                    } else {
                        exprBuffer9.appendJsStr("EQ_(").appendJsStr(exprJsStr1).appendJsStr(",").appendJsStr(exprJsStr2).appendJsStr(")");
                        exprBuffer9.appendJavaStr("(String.valueOf(").appendJavaStr(exprBuffer15.getJavaStr()).appendJavaStr(")).equals(").appendJavaStr("String.valueOf(").appendJavaStr(exprBuffer24.getJavaStr()).appendJavaStr("))");
                    }
                    RESULT3 = exprBuffer9;
                } else if ("!=".equals(op3)) {
                    ExpressionBuffer exprBuffer10 = new ExpressionBuffer();
                    exprBuffer10.appendJsStr(exprBuffer15.getJsStr()).appendJsStr(op3).appendJsStr(exprBuffer24.getJsStr());
                    exprBuffer10.appendJavaStr("!(String.valueOf(").appendJavaStr(exprBuffer15.getJavaStr()).appendJavaStr(")).equals(").appendJavaStr("String.valueOf(").appendJavaStr(exprBuffer24.getJavaStr()).appendJavaStr("))");
                    RESULT3 = exprBuffer10;
                } else if ("<=".equals(op3)) {
                    ExpressionBuffer exprBuffer11 = new ExpressionBuffer();
                    exprBuffer11.appendJsStr("LE_(").appendJsStr(exprBuffer15.getJsStr()).appendJsStr(",").appendJsStr(exprBuffer24.getJsStr()).appendJsStr(")");
                    exprBuffer11.appendJavaStr(exprBuffer15.getJavaStr()).appendJavaStr(op3).appendJavaStr(exprBuffer24.getJavaStr());
                    RESULT3 = exprBuffer11;
                } else if (">=".equals(op3)) {
                    ExpressionBuffer exprBuffer16 = new ExpressionBuffer();
                    exprBuffer16.appendJsStr("GE_(").appendJsStr(exprBuffer15.getJsStr()).appendJsStr(",").appendJsStr(exprBuffer24.getJsStr()).appendJsStr(")");
                    exprBuffer16.appendJavaStr(exprBuffer15.getJavaStr()).appendJavaStr(op3).appendJavaStr(exprBuffer24.getJavaStr());
                    RESULT3 = exprBuffer16;
                } else if ("<".equals(op3)) {
                    ExpressionBuffer exprBuffer17 = new ExpressionBuffer();
                    exprBuffer17.appendJsStr("LT_(").appendJsStr(exprBuffer15.getJsStr()).appendJsStr(",").appendJsStr(exprBuffer24.getJsStr()).appendJsStr(")");
                    exprBuffer17.appendJavaStr(exprBuffer15.getJavaStr()).appendJavaStr(op3).appendJavaStr(exprBuffer24.getJavaStr());
                    RESULT3 = exprBuffer17;
                } else if (">".equals(op3)) {
                    ExpressionBuffer exprBuffer18 = new ExpressionBuffer();
                    exprBuffer18.appendJsStr("GT_(").appendJsStr(exprBuffer15.getJsStr()).appendJsStr(",").appendJsStr(exprBuffer24.getJsStr()).appendJsStr(")");
                    exprBuffer18.appendJavaStr(exprBuffer15.getJavaStr()).appendJavaStr(op3).appendJavaStr(exprBuffer24.getJavaStr());
                    RESULT3 = exprBuffer18;
                } else {
                    RESULT3 = exprBuffer15.appendStr(op3).append(exprBuffer24);
                }
                Symbol CUP$YAFVParser$result74 = new Symbol(27, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT3);
                return CUP$YAFVParser$result74;
            case 74:
                int i113 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i114 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Symbol CUP$YAFVParser$result75 = new Symbol(27, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (ExpressionBuffer) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value);
                return CUP$YAFVParser$result75;
            case 75:
                Symbol CUP$YAFVParser$result76 = new Symbol(31, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "+");
                return CUP$YAFVParser$result76;
            case 76:
                Symbol CUP$YAFVParser$result77 = new Symbol(31, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "-");
                return CUP$YAFVParser$result77;
            case 77:
                Symbol CUP$YAFVParser$result78 = new Symbol(31, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "||");
                return CUP$YAFVParser$result78;
            case 78:
                Symbol CUP$YAFVParser$result79 = new Symbol(32, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "*");
                return CUP$YAFVParser$result79;
            case 79:
                Symbol CUP$YAFVParser$result80 = new Symbol(32, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "/");
                return CUP$YAFVParser$result80;
            case 80:
                Symbol CUP$YAFVParser$result81 = new Symbol(32, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "%");
                return CUP$YAFVParser$result81;
            case 81:
                Symbol CUP$YAFVParser$result82 = new Symbol(32, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "&&");
                return CUP$YAFVParser$result82;
            case 82:
                Symbol CUP$YAFVParser$result83 = new Symbol(30, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "<");
                return CUP$YAFVParser$result83;
            case 83:
                Symbol CUP$YAFVParser$result84 = new Symbol(30, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "<=");
                return CUP$YAFVParser$result84;
            case 84:
                Symbol CUP$YAFVParser$result85 = new Symbol(30, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ">");
                return CUP$YAFVParser$result85;
            case 85:
                Symbol CUP$YAFVParser$result86 = new Symbol(30, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ">=");
                return CUP$YAFVParser$result86;
            case 86:
                Symbol CUP$YAFVParser$result87 = new Symbol(30, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "==");
                return CUP$YAFVParser$result87;
            case 87:
                Symbol CUP$YAFVParser$result88 = new Symbol(30, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "!=");
                return CUP$YAFVParser$result88;
            case 88:
                this.validJs = "\"\"";
                this.validJava = "\"\"";
                this.failJs = "\"\"";
                this.failJava = "\"\"";
                Symbol CUP$YAFVParser$result89 = new Symbol(58, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result89;
            case 89:
                Object RESULT26 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT26 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result90 = new Symbol(36, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT26);
                return CUP$YAFVParser$result90;
            case 90:
                Symbol CUP$YAFVParser$result91 = new Symbol(37, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result91;
            case 91:
                Symbol CUP$YAFVParser$result92 = new Symbol(37, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result92;
            case 92:
                int i115 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i116 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String str10 = "\"" + ((String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value) + "\"";
                this.validJs = str10;
                this.validJava = str10;
                Symbol CUP$YAFVParser$result93 = new Symbol(38, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result93;
            case 93:
                int i117 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i118 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String ident = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                this.validJs = "\"${lang." + ident + "}\"";
                this.validJava = "Localizer.translateLabel(\"" + ident + "\")";
                Symbol CUP$YAFVParser$result94 = new Symbol(38, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result94;
            case 94:
                int i119 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i120 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String str11 = "\"" + ((String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value) + "\"";
                this.failJs = str11;
                this.failJava = str11;
                Symbol CUP$YAFVParser$result95 = new Symbol(38, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result95;
            case 95:
                int i121 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i122 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String ident2 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                this.failJs = "\"${lang." + ident2 + "}\"";
                this.failJava = "Localizer.translateLabel(\"" + ident2 + "\")";
                Symbol CUP$YAFVParser$result96 = new Symbol(38, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result96;
            case 96:
                this.parser.f284js.print(", ");
                this.parser.java.print(", ");
                Symbol CUP$YAFVParser$result97 = new Symbol(59, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result97;
            case 97:
                Object RESULT27 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT27 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result98 = new Symbol(7, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT27);
                return CUP$YAFVParser$result98;
            case 98:
                Symbol CUP$YAFVParser$result99 = new Symbol(7, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result99;
            case 99:
                Symbol CUP$YAFVParser$result100 = new Symbol(7, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result100;
            case AsyncResourceException.UNKNOWN /* 100 */:
                int i123 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left;
                int i124 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).right;
                Object typeName3 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                int i125 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i126 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                String id9 = (String) ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                this.params.add(id9);
                this.param_types.add(typeName3);
                this.parser.f284js.print(id9);
                this.parser.java.print(typeName3 + " " + id9);
                Symbol CUP$YAFVParser$result101 = new Symbol(8, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result101;
            case 101:
                Symbol CUP$YAFVParser$result102 = new Symbol(13, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 2)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result102;
            case 102:
                Symbol CUP$YAFVParser$result103 = new Symbol(13, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result103;
            case 103:
                int i127 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left;
                int i128 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right;
                Object typeName4 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).value;
                this.param_types.add(typeName4);
                Symbol CUP$YAFVParser$result104 = new Symbol(14, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, (Object) null);
                return CUP$YAFVParser$result104;
            case Resource.B_REFUND_ALL /* 104 */:
                Symbol CUP$YAFVParser$result105 = new Symbol(10, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "int");
                return CUP$YAFVParser$result105;
            case 105:
                Symbol CUP$YAFVParser$result106 = new Symbol(10, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "float");
                return CUP$YAFVParser$result106;
            case 106:
                Symbol CUP$YAFVParser$result107 = new Symbol(10, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "String");
                return CUP$YAFVParser$result107;
            case 107:
                Symbol CUP$YAFVParser$result108 = new Symbol(10, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, "boolean");
                return CUP$YAFVParser$result108;
            case 108:
                setIntConvDetected();
                Object RESULT28 = new ExpressionBuffer("getAsInt");
                Symbol CUP$YAFVParser$result109 = new Symbol(60, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT28);
                return CUP$YAFVParser$result109;
            case 109:
                Object RESULT29 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT29 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result110 = new Symbol(11, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT29);
                return CUP$YAFVParser$result110;
            case 110:
                setFloatConvDetected();
                Object RESULT30 = new ExpressionBuffer("getAsFloat");
                Symbol CUP$YAFVParser$result111 = new Symbol(61, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT30);
                return CUP$YAFVParser$result111;
            case 111:
                Object RESULT31 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT31 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result112 = new Symbol(11, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT31);
                return CUP$YAFVParser$result112;
            case 112:
                setStrConvDetected();
                Object RESULT32 = new ExpressionBuffer("getAsString");
                Symbol CUP$YAFVParser$result113 = new Symbol(62, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT32);
                return CUP$YAFVParser$result113;
            case 113:
                Object RESULT33 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT33 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result114 = new Symbol(11, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT33);
                return CUP$YAFVParser$result114;
            case 114:
                setBoolConvDetected();
                Object RESULT34 = new ExpressionBuffer("getAsBoolean");
                Symbol CUP$YAFVParser$result115 = new Symbol(63, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT34);
                return CUP$YAFVParser$result115;
            case 115:
                Object RESULT35 = null;
                if (((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value != null) {
                    RESULT35 = ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 1)).value;
                }
                Symbol CUP$YAFVParser$result116 = new Symbol(11, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 3)).left, ((Symbol) CUP$YAFVParser$stack.elementAt(CUP$YAFVParser$top - 0)).right, RESULT35);
                return CUP$YAFVParser$result116;
            default:
                throw new Exception("Invalid action number found in internal parse table");
        }
    }
}
