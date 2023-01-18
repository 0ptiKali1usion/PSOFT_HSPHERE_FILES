package psoft.yafv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;
import psoft.hsphere.Resource;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.async.AsyncResourceException;
import psoft.hsphere.resource.mysql.MySQLResource;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: hsphere.zip:psoft/yafv/HTMLLexer.class */
public class HTMLLexer implements Scanner {
    public static final int YYEOF = -1;
    public static final int FORM = 10;
    public static final int INPUT_MESSAGE = 13;
    public static final int TEXTAREA_NAME = 8;
    public static final int INPT_CHECK_METHOD = 5;
    public static final int INP_TEXT_CHECK = 4;
    public static final int TEXTAREA_OTHER = 3;
    public static final int INPUT = 1;
    public static final int FORM_DECL = 12;
    public static final int YYINITIAL = 0;
    public static final int YAFV = 15;
    public static final int BODY_DECL = 9;
    public static final int FORM_CHECK = 6;
    public static final int INPUT_NAME = 7;
    public static final int TEXTAREA = 2;
    public static final int TEXTAREA_MESSAGE = 14;
    public static final int FORM_NAME = 11;
    private static final int YY_UNKNOWN_ERROR = 0;
    private static final int YY_ILLEGAL_STATE = 1;
    private static final int YY_NO_MATCH = 2;
    private static final int YY_PUSHBACK_2BIG = 3;
    private Reader yy_reader;
    private int yy_state;
    private int yy_lexical_state;
    private char[] yy_buffer;
    private int yy_markedPos;
    private int yy_pushbackPos;
    private int yy_currentPos;
    private int yy_startRead;
    private int yy_endRead;
    private int yyline;
    private int yychar;
    private int yycolumn;
    private boolean yy_atBOL;
    private boolean yy_atEOF;
    private boolean yy_eof_done;
    StringBuffer string;
    boolean isStateFORM;
    boolean isTextareaCheckAttr;
    private static final String yycmap_packed = "\t��\u0001(\u0001'\u0002��\u0001&\u0012��\u0001(\u0001\"\u0001\u001c\u0001��\u0001\u0015\u0002��\u0001$\u0001*\u0001%\u0001#\u0001��\u0001+\u0001��\u0001\u0018\u0001\u0014\n\u0013\u0002��\u0001\u0001\u0001\u001b\u0001)\u0001#\u0001��\u0001\u0011\u0001\u0003\u0001\u001e\u0001\u0005\u0001\u000f\u0001\u0007\u0001\u0012\u0001\u001f\u0001\n\u0001\u0012\u0001 \u0001\u001d\u0001\t\u0001\u000b\u0001\u0004\u0001\f\u0001\u0012\u0001\b\u0001\u0002\u0001\u000e\u0001\r\u0001!\u0001\u0012\u0001\u0010\u0001\u0006\u0001\u0012\u0001\u0019\u0001��\u0001\u001a\u0001��\u0001\u0012\u0001��\u0001\u0011\u0001\u0003\u0001\u001e\u0001\u0005\u0001\u000f\u0001\u0007\u0001\u0012\u0001\u001f\u0001\n\u0001\u0012\u0001 \u0001\u001d\u0001\t\u0001\u000b\u0001\u0004\u0001\f\u0001\u0012\u0001\b\u0001\u0002\u0001\u000e\u0001\r\u0001!\u0001\u0012\u0001\u0010\u0001\u0006\u0001\u0012\u0001\u0016\u0001#\u0001\u0017ﾂ��";
    private static final char[] yycmap = yy_unpack_cmap(yycmap_packed);
    private static final int[] yy_rowMap = {0, 44, 88, 132, 176, 220, 264, 308, 352, 396, 440, 484, 528, 572, 616, 660, 264, 704, 748, 792, 836, 880, 924, 968, 1012, 264, 264, 1056, 1100, 1144, 264, 1188, 1232, 264, 1276, 1320, 264, 1364, 264, 264, 1408, 1452, 1496, 264, 264, 264, 1452, 264, 1540, 264, 1452, 264, 1584, 264, 1628, 264, 1452, 264, 1672, 1716, 1760, 264, 1804, 264, 264, 264, 1848, 264, 1892, 1936, 1980, 2024, 2068, 2112, 2156, 2200, 2244, 2288, 2332, 2376, 2420, 2464, 2508, 2552, 1452, 2596, 2640, 2684, 264, 2728, 2772, 2816, 2860, 2904, 2948, 2992, 3036, 3080, 3124, 3168, 3212, 3256, 3300, 3344, 3388, 3432, 3476, 3520, 3564, 3608, 3652, 3696, 3740, 3784, 3828, 3872, 3916, 3960, 4004, 4048, 4092, 4136, 4180, 4224, 4268, 4312, 4356, 4400, 4444, 264, 4488, 4532, 4576, 4620, 4664, 4708, 4752, 264, 264, 264, 4796, 4840, 4884, 4928, 4972, 5016, 5060, 5104, 5148, 5192, 5236, 5280, 5324, 5368, 5412, 5456, 264, 5500, 5544, 5588, 264, 5632, 5676, 264, 5720, 5764, 264, 264, 5808, 5852, 264, 5896, 5940, 5984, 6028, 6072, 6116, 6160, 6204, 264, 6248, 264, 6292, 264, 264, 264, 264, 6336, 6380, 6424, 264, 6468, 6512, 6556, 264, 6600, 6644, 264};
    private static final String yy_packed = "\u0001\u0011\u0001\u0012$\u0011\u0001\u0013\f\u0011\u0001\u0014\u0003\u0011\u0001\u0015\u0011\u0011\u0001\u0016\u0001\u0017\u0002\u0011\u0001\u0018\u0004\u0011\u0001\u0019\u0002\u001a\u0001\u001b\t\u0011\u0001\u0014\u0003\u0011\u0001\u001c\u0011\u0011\u0001\u001d\u0001\u001e\u0002\u0011\u0001\u0018\u0004\u0011\u0001\u0019\u0002\u001a\u0001\u001f\u0003\u0011\u0001 \u0012!\u0001\u0011\u0003!\u0004\u0011\u0006!\u0001\u0011\u0003!\u0001\u0013\u0003\u0011\u0001!\u0001\u0011\u0002\"\u0011#\u0002\"\u0001$\u0006\"\u0001%\u0005#\u0004\"\u0001&\u0002'\u0001\"\u0001(\u0003\"\u0011)\u0002\"\u0001*\u0007\"\u0005)\u0002\"\u0001+\u0001,\u0001&\u0002'\u0002\"\u0001-,��\u0002.\u0011)\u0002.\u0001/\u0006.\u00010\u0005)\u0004.\u00011\u0005.\u00022\u0011)\u00022\u00013\u00062\u00014\u0005)\u00042\u00015\u00052&\u0011\u0001\u0013\u0002\u0011\u00016\u0003\u0011\u00017$\u0011\u0001\u0013\u0005\u0011\u00028\u0011)\u00028\u00019\u00068\u0001:\u0005)\u00048\u0001;\u00058\u000b\u0011\u0001<\u0012\u0011\u0001=\u0007\u0011\u0001\u0013\u0002\u0011\u0001>\u0002\u0011\u001c?\u0001@\t?\u0002�� ?\u0001A\t?\u0002��\u0004?\u0002B\u0013C\u0003B\u0001C\u0004B\u0005C\u0004B\u0001&\u0002'\u0001D\u0002B\u0002��\u0001E\u0001F\u0002��\u0001G\u0001H\u0002��\u0001I\u0003��\u0001JD��\u0001\u0011\u0015��\u0001K+��\u0001L+��\u0001M9��\u0001N\u001d��\u0001OA��\u0001\u001a\u0015��\u0001P+��\u0001Q9��\u0001R ��\u0001S\u0019��\u0012!\u0001��\u0003!\u0004��\u0006!\u0001��\u0003!\u0004��\u0001!\u0003��\u0012#\t��\u0005# ��\u0001T<��\u0001'\u0006��\u0012)\u0001��\u0001U\u0007��\u0005) ��\u0001V\u0017��\u0012W\u0001��\u0001X\u0007��\u0005W\u0001��\u0001W\u0001Y.��\u0001.+��\u00012\u0006��\u0001Z\u0004��\u0001[\u0002��\u0001I\u0003��\u0001J\u0005��\u0001\\>��\u00018\u0015��\u0001]9��\u0001^\f��\u001c?\u0001��\t?\u0002��\u0004?\u0002��\u0013C\u0003��\u0001C\u0004��\u0005C\f��\u0001E\u0001F\u0003��\u0001H\u0006��\u0001J!��\u0001_8��\u0001`\u001e��\u0001a2��\u0001b/��\u0001c&��\u0001d*��\u0001e%��\u0001f7��\u0001g9��\u0001h\u0017��\u0001i%��\u0001j7��\u0001k*��\u0001l\u001f��\u0011m\n��\u0005m\f��\u0011n\n��\u0005n\f��\u0012W\u0001��\u0001W\u0007��\u0005W\u0001��\u0001W\u0001Y\t��\u0012W\u0001��\u0001W\u0001o\u0006��\u0005W\u0001��\u0001W\u0001Y\t��\u0001Z\u0004��\u0001[\u0006��\u0001J!��\u0001p.��\u0001q-��\u0001r1��\u0001s!��\u0001t-��\u0001u,��\u0001v/��\u0001w/��\u0001x8��\u0001y\u001d��\u0001z+��\u0001{:��\u0001|\u0017��\u0001}0��\u0001~+��\u0001\u007f:��\u0001\u0080\u001c��\u0001\u0081\u001e��\u0012m\u0003��\u0001\u0082\u0001T\u0001\u0083\u0003��\u0005m\f��\u0012n\u0003��\u0001)\u0001V\u0001\u0084\u0003��\u0005n\f��\u0011\u0085\n��\u0005\u0085\u0012��\u0001\u0086'��\u0001\u00876��\u0001\u0088:��\u0001\u0089\u0013��\u0001\u008aF��\u0001\u008b\u0013��\u0001\u008c/��\u0001\u008d,��\u0001\u008e8��\u0001\u008f+��\u0001\u0090-��\u0001\u0091.��\u0001\u0092\u0010��\u0001yA��\u0001\u0093-��\u0001\u0094.��\u0001\u0095\u001b��\u0001\u0096\u001d��\u0011\u0097\t��\u0001\u0098\u0005\u0097\f��\u0011\u0099\t��\u0001\u009a\u0005\u0099\f��\u0012\u0085\u0003��\u0001\u009b\u0001o\u0001\u009c\u0003��\u0005\u0085\u0013��\u0001\u009d*��\u0001\u009e>��\u0001\u009f0��\u0001 \u0019��\u0001¡.��\u0001¢6��\u0001£+��\u0001¤*��\u0001¥+��\u0001¦\u0006��\u0001§%��\u0001¨*��\u0001©+��\u0001ª\u0006��\u0001«\u0017��\u0001¬\u001f��\u0012\u0097\u0006��\u0001\u00ad\u0002��\u0005\u0097\f��\u0012\u0098\u0001��\u0001\u0098\u0006��\u0001®\u0005\u0098\u0001��\u0001\u0098\n��\u0012\u0099\u0006��\u0001¯\u0002��\u0005\u0099\f��\u0012\u009a\u0001��\u0001\u009a\u0006��\u0001°\u0005\u009a\u0001��\u0001\u009a,��\u0001Y\t��\u0011±\t��\u0001²\u0005±\u0013��\u0001³>��\u0001´*��\u0001µ\u0006��\u0001¶\u0011��\u0001·#��\u001c£\u0001'\u000f£\u001c��\u0001¸+��\u0001¹+��\u0001º+��\u0001» ��\u0001¼1��\u0001\u0082\u0001T\u0001\u0083,��\u0001\u00ad(��\u0001)\u0001V\u0001\u0084,��\u0001¯\u0013��\u0012±\u0006��\u0001½\u0002��\u0005±\f��\u0012²\u0001��\u0001²\u0006��\u0001¾\u0005²\u0001��\u0001²1��\u0001¿\u001e��\u0001À\u001e��\u0001Á$��\u0001Â:��\u0001\u009b\u0001o\u0001\u009c,��\u0001½\u0011��\u001cÀ\u0001¶\u000fÀ\u0011��\u0001Ã)��\u0001Ä-��\u0001ÅC��\u0001Æ\u0002��";
    private static final int[] yytrans = yy_unpack(yy_packed);
    private static final String[] YY_ERROR_MSG = {"Unkown internal scanner error", "Internal error: unknown state", "Error: could not match input", "Error: pushback value was too large"};
    private static final byte[] YY_ATTRIBUTE = {0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 1, 9, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 1, 1, 1, 9, 1, 1, 9, 1, 1, 9, 1, 9, 9, 1, 1, 1, 9, 9, 9, 1, 9, 1, 9, 1, 9, 1, 9, 1, 9, 1, 9, 1, 1, 1, 9, 1, 9, 9, 9, 1, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 9, 9, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 9, 0, 0, 9, 0, 0, 9, 9, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 9, 0, 9, 9, 9, 9, 0, 0, 0, 9, 0, 0, 0, 9, 0, 0, 9};

    private Symbol symbol(int type) {
        return new Symbol(type, this.yyline, this.yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, this.yyline, this.yycolumn, value);
    }

    public String invalidLexMsg(String lex) {
        if (lex == null || "".equals(lex)) {
            return "Invalid symbol";
        }
        return lex.length() == 1 ? "Invalid char '" + lex + "'" : "Invalid char sequence `" + lex + "`";
    }

    public void exitErrorMsg(String msg, Symbol info) {
        String addInfo = " (line " + (this.yyline + 1) + ", column " + (this.yycolumn + 1) + " of the input file)";
        String text = yytext();
        System.err.println((text != null ? "\nToken: [" + text + "]" + addInfo + ".\n" + msg : "Error: " + msg + addInfo) + ".");
        System.exit(1);
    }

    public void exitErrorMsg(String msg) {
        String addInfo = " (line " + (this.yyline + 1) + ", column " + (this.yycolumn + 1) + " of the input file)";
        String text = yytext();
        System.err.println((text != null ? "\nToken: [" + text + "]" + addInfo + ".\n" + msg : "Error: " + msg + addInfo) + ".");
        System.exit(1);
    }

    public String stripFMMacro(String expr) {
        return (expr != null && expr.startsWith("${") && expr.endsWith("}")) ? expr.substring(2, expr.length() - 1) : expr;
    }

    public HTMLLexer(Reader in) {
        this.yy_lexical_state = 0;
        this.yy_buffer = new char[16384];
        this.string = new StringBuffer();
        this.isStateFORM = false;
        this.isTextareaCheckAttr = false;
        this.yy_reader = in;
    }

    HTMLLexer(InputStream in) {
        this(new InputStreamReader(in));
    }

    private static int[] yy_unpack(String packed) {
        int[] trans = new int[6688];
        int i = 0;
        int j = 0;
        while (i < 1122) {
            int i2 = i;
            int i3 = i + 1;
            int count = packed.charAt(i2);
            i = i3 + 1;
            int value = packed.charAt(i3);
            int value2 = value - 1;
            do {
                int i4 = j;
                j++;
                trans[i4] = value2;
                count--;
            } while (count > 0);
        }
        return trans;
    }

    private static char[] yy_unpack_cmap(String packed) {
        char[] map = new char[65536];
        int i = 0;
        int j = 0;
        while (i < 180) {
            int i2 = i;
            int i3 = i + 1;
            int count = packed.charAt(i2);
            i = i3 + 1;
            char value = packed.charAt(i3);
            do {
                int i4 = j;
                j++;
                map[i4] = value;
                count--;
            } while (count > 0);
        }
        return map;
    }

    private int yy_advance() throws IOException {
        if (this.yy_currentPos < this.yy_endRead) {
            char[] cArr = this.yy_buffer;
            int i = this.yy_currentPos;
            this.yy_currentPos = i + 1;
            return cArr[i];
        } else if (this.yy_atEOF) {
            return -1;
        } else {
            if (this.yy_startRead > 0) {
                System.arraycopy(this.yy_buffer, this.yy_startRead, this.yy_buffer, 0, this.yy_endRead - this.yy_startRead);
                this.yy_endRead -= this.yy_startRead;
                this.yy_currentPos -= this.yy_startRead;
                this.yy_markedPos -= this.yy_startRead;
                this.yy_pushbackPos -= this.yy_startRead;
                this.yy_startRead = 0;
            }
            if (this.yy_currentPos >= this.yy_buffer.length) {
                char[] newBuffer = new char[this.yy_currentPos * 2];
                System.arraycopy(this.yy_buffer, 0, newBuffer, 0, this.yy_buffer.length);
                this.yy_buffer = newBuffer;
            }
            int numRead = this.yy_reader.read(this.yy_buffer, this.yy_endRead, this.yy_buffer.length - this.yy_endRead);
            if (numRead == -1) {
                return -1;
            }
            this.yy_endRead += numRead;
            char[] cArr2 = this.yy_buffer;
            int i2 = this.yy_currentPos;
            this.yy_currentPos = i2 + 1;
            return cArr2[i2];
        }
    }

    public final void yyclose() throws IOException {
        this.yy_atEOF = true;
        this.yy_endRead = this.yy_startRead;
        this.yy_reader.close();
    }

    public final int yystate() {
        return this.yy_lexical_state;
    }

    public final void yybegin(int newState) {
        this.yy_lexical_state = newState;
    }

    public final String yytext() {
        return new String(this.yy_buffer, this.yy_startRead, this.yy_markedPos - this.yy_startRead);
    }

    public final int yylength() {
        return this.yy_markedPos - this.yy_startRead;
    }

    private void yy_ScanError(int errorCode) {
        try {
            System.out.println(YY_ERROR_MSG[errorCode]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(YY_ERROR_MSG[0]);
        }
        System.exit(1);
    }

    private void yypushback(int number) {
        if (number > yylength()) {
            yy_ScanError(3);
        }
        this.yy_markedPos -= number;
    }

    private void yy_do_eof() throws IOException {
        if (!this.yy_eof_done) {
            this.yy_eof_done = true;
            yyclose();
        }
    }

    public Symbol next_token() throws IOException {
        int yy_input;
        int yy_next;
        while (true) {
            boolean yy_counted = false;
            this.yy_currentPos = this.yy_startRead;
            while (this.yy_currentPos < this.yy_markedPos) {
                switch (this.yy_buffer[this.yy_currentPos]) {
                    case '\n':
                        if (yy_counted) {
                            yy_counted = false;
                            break;
                        } else {
                            this.yyline++;
                            this.yycolumn = 0;
                            break;
                        }
                    case '\r':
                        this.yyline++;
                        this.yycolumn = 0;
                        yy_counted = true;
                        break;
                    default:
                        yy_counted = false;
                        this.yycolumn++;
                        break;
                }
                this.yy_currentPos++;
            }
            if (yy_counted) {
                if (yy_advance() == 10) {
                    this.yyline--;
                }
                if (!this.yy_atEOF) {
                    this.yy_currentPos--;
                }
            }
            int yy_action = -1;
            int i = this.yy_markedPos;
            this.yy_startRead = i;
            this.yy_currentPos = i;
            this.yy_state = this.yy_lexical_state;
            while (true) {
                yy_input = yy_advance();
                if (yy_input != -1 && (yy_next = yytrans[yy_rowMap[this.yy_state] + yycmap[yy_input]]) != -1) {
                    this.yy_state = yy_next;
                    byte b = YY_ATTRIBUTE[this.yy_state];
                    if ((b & 1) > 0) {
                        yy_action = this.yy_state;
                        this.yy_markedPos = this.yy_currentPos;
                        if ((b & 8) > 0) {
                        }
                    }
                }
            }
            switch (yy_action) {
                case 15:
                case yafvsym.NEQ /* 34 */:
                case yafvsym.STRINGLITERAL /* 40 */:
                case 66:
                    return symbol(8, yytext());
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case yafvsym.AND /* 27 */:
                case yafvsym.f286OR /* 28 */:
                case yafvsym.f285LT /* 29 */:
                case yafvsym.LTEQ /* 31 */:
                case 32:
                case 54:
                case 59:
                case 60:
                    return symbol(3, yytext());
                case 24:
                case 25:
                    return symbol(5);
                case yafvsym.MOD /* 26 */:
                    yybegin(this.isStateFORM ? 10 : 0);
                    return symbol(4);
                case 30:
                    yybegin(3);
                    return symbol(19);
                case yafvsym.f288EQ /* 33 */:
                case yafvsym.NOT /* 35 */:
                case yafvsym.INTEGER_CONSTANT /* 41 */:
                case yafvsym.FLOAT_CONSTANT /* 42 */:
                    exitErrorMsg(invalidLexMsg(yytext()) + " in the 'check' attribute of the <" + (this.isTextareaCheckAttr ? "textarea" : "input") + "> tag");
                    break;
                case yafvsym.RETURN_CALL /* 36 */:
                    yybegin(this.isTextareaCheckAttr ? 2 : 1);
                    break;
                case yafvsym.WHILE /* 37 */:
                case yafvsym.f287IF /* 38 */:
                case 199:
                case 200:
                case 201:
                case 202:
                case 203:
                case 204:
                case 205:
                case 206:
                case 207:
                case 208:
                case 209:
                case 210:
                case 211:
                case 212:
                case 213:
                case 214:
                case 215:
                case 216:
                case 217:
                case 218:
                case 219:
                case 220:
                case 221:
                case 222:
                case 223:
                case 224:
                case 225:
                case 226:
                case 227:
                case 228:
                case 229:
                case 230:
                case 231:
                case 232:
                case 233:
                case 234:
                case 235:
                case 236:
                case 237:
                case 238:
                case 239:
                case 240:
                case 241:
                case 242:
                    break;
                case yafvsym.ELSE /* 39 */:
                    yybegin(5);
                    return symbol(10);
                case yafvsym.BOOL_CONSTANT /* 43 */:
                    yybegin(4);
                    return symbol(11);
                case 44:
                    return symbol(12);
                case 45:
                case 46:
                case 48:
                    exitErrorMsg(invalidLexMsg(yytext()) + " in the 'name' attribute of the <input> tag");
                    break;
                case 47:
                    yybegin(1);
                    break;
                case 49:
                case MySQLResource.MAX_DB_NAME /* 50 */:
                case 52:
                    exitErrorMsg(invalidLexMsg(yytext()) + " in the 'name' attribute of the <textarea> tag");
                    break;
                case 51:
                    yybegin(2);
                    break;
                case 53:
                    yybegin(0);
                    return symbol(23);
                case 55:
                case 56:
                case 58:
                    exitErrorMsg(invalidLexMsg(yytext()) + " in the 'name' attribute of the <form> tag");
                    break;
                case 57:
                    yybegin(12);
                    break;
                case 61:
                    yybegin(10);
                    return symbol(16);
                case 62:
                    this.string.append(yytext());
                    break;
                case 63:
                    yybegin(1);
                    return symbol(25, this.string.toString());
                case SignupGuard.TRIAL_ACCOUNT_FLAG /* 64 */:
                    yybegin(2);
                    return symbol(25, this.string.toString());
                case 65:
                    exitErrorMsg(invalidLexMsg(yytext()) + " in the <yafv> declaration");
                    break;
                case 67:
                    yybegin(0);
                    return symbol(21);
                case 68:
                case 69:
                case 70:
                case 71:
                case 72:
                case 73:
                case 74:
                case 75:
                case 76:
                case 77:
                case 78:
                case 79:
                case 80:
                case 81:
                case 82:
                case 83:
                case 84:
                case 85:
                case 86:
                case 87:
                case 89:
                case 90:
                case 91:
                case 92:
                case 93:
                case 94:
                case 95:
                case 96:
                case 97:
                case 98:
                case 99:
                case AsyncResourceException.UNKNOWN /* 100 */:
                case 101:
                case 102:
                case 103:
                case Resource.B_REFUND_ALL /* 104 */:
                case 105:
                case 106:
                case 107:
                case 108:
                case 109:
                case 110:
                case 111:
                case 112:
                case 113:
                case 114:
                case 115:
                case 116:
                case 117:
                case 118:
                case 119:
                case 120:
                case 121:
                case 122:
                case 123:
                case 124:
                case 125:
                case 126:
                case 127:
                case SignupGuard.MODERATE_EVERYTHING_FLAG /* 128 */:
                case 130:
                case 131:
                case 132:
                case 133:
                case 134:
                case 135:
                case 136:
                case 140:
                case 141:
                case 142:
                case 143:
                case 144:
                case 145:
                case 146:
                case 147:
                case 148:
                case 149:
                case 150:
                case 151:
                case 152:
                case 153:
                case 154:
                case 155:
                case 157:
                case 158:
                case 159:
                case 161:
                case 162:
                case 164:
                case 165:
                case 168:
                case 169:
                case 171:
                case 172:
                case 173:
                case 174:
                case 175:
                case 176:
                case 177:
                case 178:
                case 180:
                case 182:
                case 187:
                case 188:
                case 189:
                case 191:
                case 192:
                case 193:
                case 195:
                case 196:
                case 198:
                default:
                    if (yy_input == -1 && this.yy_startRead == this.yy_currentPos) {
                        this.yy_atEOF = true;
                        yy_do_eof();
                        return new Symbol(0);
                    }
                    yy_ScanError(2);
                    break;
                case 88:
                    return symbol(25, yytext());
                case 129:
                    return symbol(9, stripFMMacro(yytext()));
                case 137:
                    yybegin(9);
                    return symbol(22);
                case 138:
                    yybegin(15);
                    return symbol(20);
                case 139:
                    this.isStateFORM = true;
                    yybegin(12);
                    return symbol(13);
                case 156:
                    exitErrorMsg("A new <form tag has occured. The current form is not closed with the </form> tag");
                    break;
                case 160:
                    yybegin(1);
                    return symbol(2);
                case 163:
                    yybegin(7);
                    return symbol(6);
                case 166:
                    System.err.println("WARNING: The syntax with 'check!' is no longer supported in the <input> tag.\n\tInstead, please use the following construction: check=\"${validation_var}\".\n\tNote: 'validation_var' is a freemarker variable which is evaluated to the\n\treal name of a validation function (regexp) during template processing.\n\nThe construction removed from the output file !\n");
                    break;
                case 167:
                    yybegin(8);
                    return symbol(6);
                case 170:
                    System.err.println("WARNING: The syntax with 'check!' is no longer supported in the <textarea> tag.\n\tInstead, please use the following construction: check=\"${validation_var}\".\n\tNote: 'validation_var' is a freemarker variable which is evaluated to the\n\treal name of a validation function (regexp) during template processing.\n\nThe construction removed from the output file !\n");
                    break;
                case 179:
                    yybegin(11);
                    return symbol(6);
                case 181:
                    return symbol(14);
                case 183:
                    this.string.setLength(0);
                    yybegin(13);
                    return symbol(24);
                case 184:
                    this.isTextareaCheckAttr = false;
                    yybegin(4);
                    return symbol(7);
                case 185:
                    this.string.setLength(0);
                    yybegin(14);
                    return symbol(24);
                case 186:
                    this.isTextareaCheckAttr = true;
                    yybegin(4);
                    return symbol(7);
                case 190:
                    yybegin(0);
                    this.isStateFORM = false;
                    return symbol(15);
                case 194:
                    yybegin(2);
                    return symbol(17);
                case 197:
                    yybegin(this.isStateFORM ? 10 : 0);
                    return symbol(18);
            }
        }
    }
}
