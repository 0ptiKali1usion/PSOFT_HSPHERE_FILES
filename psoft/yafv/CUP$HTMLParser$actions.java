package psoft.yafv;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java_cup.runtime.Symbol;
import java_cup.runtime.lr_parser;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.util.freemarker.TemplateMethodWrapper;

/* loaded from: hsphere.zip:psoft/yafv/CUP$HTMLParser$actions.class */
class CUP$HTMLParser$actions {
    protected String label;
    protected String check;
    protected String name;
    protected String yafvName;
    protected boolean FTemplate;
    protected StringBuffer formCheck;
    protected List params;
    protected boolean cform;
    private final HTMLParser parser;
    protected String formName = "";
    protected List forms = new ArrayList();
    protected List images = new ArrayList();
    protected final String FMExt = TemplateMethodWrapper.PREFIX;
    protected final String REFERENCED_FUNCTION = "referencedFunction";
    protected final String defaultFName = "f__";
    protected long formNumber = 1;
    protected boolean isReferencedCheck = false;

    public void printCheck() {
        if (this.check != null) {
            this.parser.html.print(" onFocus=\"showOnFocus(this)\"  onKeyUp=\"showOnKeyUp(this)\" onBlur=\"showOnBlur(this)\"");
        }
        this.parser.html.print(">");
    }

    public void printCheckFinish(String finishtag) {
        String callFunction;
        this.parser.html.println(finishtag);
        if (this.check != null) {
            int n = 1;
            String curImageName = this.formName + "_" + this.name + "_";
            while (this.images.contains(curImageName + n)) {
                n++;
            }
            String curImageName2 = curImageName + String.valueOf(n);
            this.images.add(curImageName2);
            this.parser.html.println("<img " + this.parser.getInitImgAtt() + " name=\"" + curImageName2 + "\" align=\"absmiddle\">");
            StringBuffer checkbuf = new StringBuffer(" if (curForm!=null) {");
            checkbuf.append(" curForm.elements['" + this.name + "'].img_name=\"" + curImageName2 + "\";");
            checkbuf.append(" curForm.elements['" + this.name + "'].label=\"" + this.label + "\";");
            if (this.isReferencedCheck) {
                String valueOf = this.params.size() <= 1 ? "" : String.valueOf(this.params.size());
                callFunction = "FM_referencedFunction(" + this.check + ", \"" + (this.params.size() <= 1 ? "" : String.valueOf(this.params.size())) + "\")";
                checkbuf.append(" curForm.elements['" + this.name + "'].vf=\"_${").append(this.check).append("}(");
            } else {
                callFunction = TemplateMethodWrapper.PREFIX + this.check + "()";
                checkbuf.append(" curForm.elements['" + this.name + "'].vf=\"");
                checkbuf.append("_").append(this.check).append("(");
            }
            if (this.params.isEmpty()) {
                checkbuf.append("ThisField");
            } else {
                boolean fparam = true;
                for (Object obj : this.params) {
                    if (!fparam) {
                        checkbuf.append(", ");
                    } else {
                        fparam = false;
                    }
                    String param = obj.toString();
                    if ("this".equals(param)) {
                        checkbuf.append("ThisField");
                    } else if (param.startsWith("'") && param.endsWith("'")) {
                        checkbuf.append(param);
                    } else {
                        checkbuf.append("ThisField.form['" + param + "']");
                    }
                }
            }
            checkbuf.append(")\"; }");
            this.parser.html.println("<script language=\"javascript\"><!--");
            this.parser.html.println(checkbuf);
            this.parser.html.println(" <call " + callFunction + ">");
            this.parser.html.println("//-->\n</script>");
        }
    }

    public void reset() {
        this.label = "";
        this.check = null;
        this.params = new ArrayList();
    }

    public CUP$HTMLParser$actions(HTMLParser parser) {
        this.parser = parser;
    }

    /* JADX WARN: Type inference failed for: r4v153, types: [long, java.lang.Long] */
    public final Symbol CUP$HTMLParser$do_action(int CUP$HTMLParser$act_num, lr_parser CUP$HTMLParser$parser, Stack CUP$HTMLParser$stack, int CUP$HTMLParser$top) throws Exception {
        switch (CUP$HTMLParser$act_num) {
            case 0:
                Symbol CUP$HTMLParser$result = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result;
            case 1:
                int i = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left;
                int i2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).right;
                Object start_val = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value;
                Symbol CUP$HTMLParser$result2 = new Symbol(0, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, start_val);
                CUP$HTMLParser$parser.done_parsing();
                return CUP$HTMLParser$result2;
            case 2:
                Symbol CUP$HTMLParser$result3 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result3;
            case 3:
                Symbol CUP$HTMLParser$result4 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result4;
            case 4:
                Symbol CUP$HTMLParser$result5 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result5;
            case 5:
                Symbol CUP$HTMLParser$result6 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result6;
            case 6:
                Symbol CUP$HTMLParser$result7 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result7;
            case 7:
                Symbol CUP$HTMLParser$result8 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result8;
            case 8:
                Symbol CUP$HTMLParser$result9 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result9;
            case 9:
                Symbol CUP$HTMLParser$result10 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result10;
            case 10:
                Symbol CUP$HTMLParser$result11 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result11;
            case 11:
                Symbol CUP$HTMLParser$result12 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result12;
            case 12:
                Symbol CUP$HTMLParser$result13 = new Symbol(1, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result13;
            case 13:
                this.parser.html.println("\n<script language=\"javascript\"><!--\n var hasCF=false;\n//-->\n</script> ");
                this.parser.html.print("<body ");
                Symbol CUP$HTMLParser$result14 = new Symbol(19, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result14;
            case 14:
                this.parser.html.print(" onLoad=\"if (hasCF) checkAllForms();\" onResize=\"if (hasCF) checkAllForms();\"");
                Symbol CUP$HTMLParser$result15 = new Symbol(20, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result15;
            case 15:
                Object RESULT = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value != null) {
                    RESULT = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value;
                }
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value != null) {
                    RESULT = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value;
                }
                this.parser.html.println(">");
                Symbol CUP$HTMLParser$result16 = new Symbol(17, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT);
                return CUP$HTMLParser$result16;
            case 16:
                this.yafvName = null;
                Symbol CUP$HTMLParser$result17 = new Symbol(21, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result17;
            case 17:
                Object RESULT2 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value != null) {
                    RESULT2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                }
                int i3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left;
                int i4 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).right;
                Object id = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value;
                this.yafvName = id.toString();
                if (this.yafvName.startsWith("/")) {
                    this.yafvName = this.yafvName.substring(1);
                }
                if (this.yafvName.endsWith(".yafv")) {
                    this.yafvName = this.yafvName.substring(0, this.yafvName.length() - 6);
                }
                this.parser.html.println("<script language=\"javascript\"><!--");
                this.parser.html.println(" <include \"" + this.parser.getJsDir() + this.yafvName + ".js\">");
                this.parser.html.println("//-->\n</script>");
                Symbol CUP$HTMLParser$result18 = new Symbol(16, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT2);
                return CUP$HTMLParser$result18;
            case 18:
                this.parser.html.println("<script language=\"javascript\" src=\"" + this.parser.getJsPath() + "yafv.js\"></script>");
                this.parser.html.println("<assign brDetectionIncluded=\"1\">");
                Symbol CUP$HTMLParser$result19 = new Symbol(16, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result19;
            case 19:
                this.parser.html.print("<form");
                this.formName = null;
                this.formCheck = new StringBuffer();
                this.params = new ArrayList();
                this.cform = false;
                this.FTemplate = false;
                Symbol CUP$HTMLParser$result20 = new Symbol(22, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result20;
            case 20:
                if (this.cform) {
                    if (this.formName == null) {
                        do {
                            StringBuilder append = new StringBuilder().append("f__");
                            long j = this.formNumber;
                            this.formNumber = j + 1;
                            this.formName = append.append(new Long(j).toString()).toString();
                        } while (this.forms.contains(this.formName));
                        this.forms.add(this.formName);
                        this.parser.html.print("name=\"" + this.formName + "\"");
                    }
                    this.parser.html.print(" onSubmit=\"return checkForm(this,true);\"");
                }
                this.parser.html.print(">");
                Symbol CUP$HTMLParser$result21 = new Symbol(23, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result21;
            case 21:
                if (this.cform) {
                    this.parser.html.println("\n<script language=\"javascript\"><!--\n hasCF=true; (curForm=document.forms['" + this.formName + "']).wbv=true;\n//-->\n</script>");
                }
                Symbol CUP$HTMLParser$result22 = new Symbol(24, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result22;
            case 22:
                Object RESULT3 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 6)).value != null) {
                    RESULT3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 6)).value;
                }
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).value != null) {
                    RESULT3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).value;
                }
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value != null) {
                    RESULT3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                }
                this.parser.html.print("</form>");
                Symbol CUP$HTMLParser$result23 = new Symbol(6, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 7)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT3);
                return CUP$HTMLParser$result23;
            case 23:
                Symbol CUP$HTMLParser$result24 = new Symbol(14, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result24;
            case 24:
                int i5 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i6 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object id2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.formName = id2.toString();
                if (this.forms.contains(this.formName)) {
                    long nn = 1;
                    do {
                        StringBuilder append2 = new StringBuilder().append(this.formName).append("_");
                        ?? r4 = nn;
                        nn = r4 + 1;
                        new Long((long) r4);
                        this.formName = append2.append(r4.toString()).toString();
                    } while (this.forms.contains(this.formName));
                    this.forms.add(this.formName);
                    this.parser.html.print("name=\"" + this.formName + "\"");
                    Symbol CUP$HTMLParser$result25 = new Symbol(25, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                    return CUP$HTMLParser$result25;
                }
                this.forms.add(this.formName);
                this.parser.html.print("name=\"" + this.formName + "\"");
                Symbol CUP$HTMLParser$result252 = new Symbol(25, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result252;
            case 25:
                Object RESULT4 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value != null) {
                    RESULT4 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value;
                }
                int i7 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).left;
                int i8 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).right;
                Object obj = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                Symbol CUP$HTMLParser$result26 = new Symbol(7, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT4);
                return CUP$HTMLParser$result26;
            case yafvsym.MOD /* 26 */:
                this.cform = true;
                Symbol CUP$HTMLParser$result27 = new Symbol(26, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result27;
            case yafvsym.AND /* 27 */:
                Object RESULT5 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value != null) {
                    RESULT5 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value;
                }
                Symbol CUP$HTMLParser$result28 = new Symbol(7, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT5);
                return CUP$HTMLParser$result28;
            case yafvsym.f286OR /* 28 */:
                Symbol CUP$HTMLParser$result29 = new Symbol(7, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result29;
            case yafvsym.f285LT /* 29 */:
                Symbol CUP$HTMLParser$result30 = new Symbol(7, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result30;
            case 30:
                Symbol CUP$HTMLParser$result31 = new Symbol(8, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result31;
            case yafvsym.LTEQ /* 31 */:
                Symbol CUP$HTMLParser$result32 = new Symbol(8, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result32;
            case 32:
                Symbol CUP$HTMLParser$result33 = new Symbol(8, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result33;
            case yafvsym.f288EQ /* 33 */:
                Symbol CUP$HTMLParser$result34 = new Symbol(8, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result34;
            case yafvsym.NEQ /* 34 */:
                int i9 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i10 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object str = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.parser.html.print(str);
                Symbol CUP$HTMLParser$result35 = new Symbol(13, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result35;
            case yafvsym.NOT /* 35 */:
                Symbol CUP$HTMLParser$result36 = new Symbol(15, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result36;
            case yafvsym.RETURN_CALL /* 36 */:
                Symbol CUP$HTMLParser$result37 = new Symbol(15, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result37;
            case yafvsym.WHILE /* 37 */:
                Symbol CUP$HTMLParser$result38 = new Symbol(5, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result38;
            case yafvsym.f287IF /* 38 */:
                Symbol CUP$HTMLParser$result39 = new Symbol(5, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result39;
            case yafvsym.ELSE /* 39 */:
                this.parser.html.print("<textarea");
                reset();
                Symbol CUP$HTMLParser$result40 = new Symbol(27, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result40;
            case yafvsym.STRINGLITERAL /* 40 */:
                printCheck();
                Symbol CUP$HTMLParser$result41 = new Symbol(28, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result41;
            case yafvsym.INTEGER_CONSTANT /* 41 */:
                Object RESULT6 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 5)).value != null) {
                    RESULT6 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 5)).value;
                }
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value != null) {
                    RESULT6 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                }
                printCheckFinish("</textarea>");
                Symbol CUP$HTMLParser$result42 = new Symbol(4, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 6)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT6);
                return CUP$HTMLParser$result42;
            case yafvsym.FLOAT_CONSTANT /* 42 */:
                this.parser.html.print("<input");
                reset();
                Symbol CUP$HTMLParser$result43 = new Symbol(29, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result43;
            case yafvsym.BOOL_CONSTANT /* 43 */:
                Object RESULT7 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value != null) {
                    RESULT7 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                }
                printCheck();
                printCheckFinish("");
                Symbol CUP$HTMLParser$result44 = new Symbol(2, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT7);
                return CUP$HTMLParser$result44;
            case 44:
                Symbol CUP$HTMLParser$result45 = new Symbol(3, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result45;
            case 45:
                Symbol CUP$HTMLParser$result46 = new Symbol(3, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result46;
            case 46:
                this.parser.html.print(" ");
                Symbol CUP$HTMLParser$result47 = new Symbol(30, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result47;
            case 47:
                Object RESULT8 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value != null) {
                    RESULT8 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).value;
                }
                Symbol CUP$HTMLParser$result48 = new Symbol(3, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT8);
                return CUP$HTMLParser$result48;
            case 48:
                this.parser.html.print(" ");
                Symbol CUP$HTMLParser$result49 = new Symbol(3, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result49;
            case 49:
                int i11 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i12 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object str2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.parser.html.print(str2);
                Symbol CUP$HTMLParser$result50 = new Symbol(9, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result50;
            case MySQLResource.MAX_DB_NAME /* 50 */:
                Symbol CUP$HTMLParser$result51 = new Symbol(9, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result51;
            case 51:
                Symbol CUP$HTMLParser$result52 = new Symbol(9, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result52;
            case 52:
                Symbol CUP$HTMLParser$result53 = new Symbol(9, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result53;
            case 53:
                int i13 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i14 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object id3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.name = id3.toString();
                this.parser.html.print("name=\"" + id3 + "\"");
                if (this.name.equals("ftemplate")) {
                    this.FTemplate = true;
                }
                Symbol CUP$HTMLParser$result54 = new Symbol(10, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result54;
            case 54:
                int i15 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i16 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object str3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.label = str3.toString();
                Symbol CUP$HTMLParser$result55 = new Symbol(18, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result55;
            case 55:
                int i17 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i18 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object id4 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.check = id4.toString();
                this.isReferencedCheck = false;
                Symbol CUP$HTMLParser$result56 = new Symbol(31, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result56;
            case 56:
                Object RESULT9 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value != null) {
                    RESULT9 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value;
                }
                int i19 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).left;
                int i20 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).right;
                Object obj2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).value;
                Symbol CUP$HTMLParser$result57 = new Symbol(11, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 5)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT9);
                return CUP$HTMLParser$result57;
            case 57:
                int i21 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i22 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object id5 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.check = id5.toString();
                this.isReferencedCheck = false;
                this.params.add("this");
                Symbol CUP$HTMLParser$result58 = new Symbol(11, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result58;
            case 58:
                int i23 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i24 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object val = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.check = val.toString();
                this.isReferencedCheck = true;
                Symbol CUP$HTMLParser$result59 = new Symbol(32, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result59;
            case 59:
                Object RESULT10 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value != null) {
                    RESULT10 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value;
                }
                int i25 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).left;
                int i26 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).right;
                Object obj3 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 4)).value;
                Symbol CUP$HTMLParser$result60 = new Symbol(11, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 5)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT10);
                return CUP$HTMLParser$result60;
            case 60:
                int i27 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i28 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object val2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.check = val2.toString();
                this.isReferencedCheck = true;
                this.params.add("this");
                Symbol CUP$HTMLParser$result61 = new Symbol(11, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 1)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result61;
            case 61:
                int i29 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i30 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object id6 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.params.add(id6);
                Symbol CUP$HTMLParser$result62 = new Symbol(33, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result62;
            case 62:
                Object RESULT11 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value != null) {
                    RESULT11 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                }
                int i31 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left;
                int i32 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).right;
                Object obj4 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value;
                Symbol CUP$HTMLParser$result63 = new Symbol(12, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT11);
                return CUP$HTMLParser$result63;
            case 63:
                int i33 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i34 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object strconst = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.params.add(strconst);
                Symbol CUP$HTMLParser$result64 = new Symbol(34, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result64;
            case SignupGuard.TRIAL_ACCOUNT_FLAG /* 64 */:
                Object RESULT12 = null;
                if (((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value != null) {
                    RESULT12 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 2)).value;
                }
                int i35 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left;
                int i36 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).right;
                Object obj5 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).value;
                Symbol CUP$HTMLParser$result65 = new Symbol(12, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 3)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, RESULT12);
                return CUP$HTMLParser$result65;
            case 65:
                int i37 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i38 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object id7 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.params.add(id7);
                Symbol CUP$HTMLParser$result66 = new Symbol(12, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result66;
            case 66:
                int i39 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left;
                int i40 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right;
                Object strconst2 = ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).value;
                this.params.add(strconst2);
                Symbol CUP$HTMLParser$result67 = new Symbol(12, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).left, ((Symbol) CUP$HTMLParser$stack.elementAt(CUP$HTMLParser$top - 0)).right, (Object) null);
                return CUP$HTMLParser$result67;
            default:
                throw new Exception("Invalid action number found in internal parse table");
        }
    }
}
