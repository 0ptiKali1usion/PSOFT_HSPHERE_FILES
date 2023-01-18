package psoft.yafv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.mysql.MySQLResource;

/* loaded from: hsphere.zip:psoft/yafv/DFALexer.class */
public class DFALexer implements Scanner {
    public static final int YYEOF = -1;
    public static final int STATE_DEF = 1;
    public static final int YYINITIAL = 0;
    public static final int STATE_NUMBER = 4;
    public static final int CHAR_INTERVAL = 3;
    public static final int BRANCH_CHAR = 2;
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
    private static final String yycmap_packed = "\t��\u0001\u0003\u0001\u0002\u0002��\u0001\u0001\u0012��\u0001\u0011\f��\u0001\n\u0002��\n\u000b\u0001\u0007\u0003��\u0001\u0014\u0002��\u0006\u0006\u0014��\u0001\b\u0001\u0004\u0001\t\u0003��\u0001\u000f\u0001\u0006\u0001\u0015\u0001\u0006\u0001\u0013\u0001\u0006\u0002��\u0001\f\u0001\u0018\u0001��\u0001\u0010\u0001��\u0001\r\u0001��\u0001\u0016\u0001��\u0001\u0017\u0001\u0012\u0001\u000e\u0001\u0005ﾊ��";
    private static final char[] yycmap = yy_unpack_cmap(yycmap_packed);
    private static final int[] yy_rowMap = {0, 25, 50, 75, 100, 125, 150, 125, 175, 200, 225, 125, 125, 125, 125, 250, 275, 300, 125, 125, 325, 125, 350, 125, 375, 400, 425, 450, 475, 500, 525, 550, 575, 600, 625, 650, 675, 700, 725, 750, 775, 800, 825, 850, 125, 875, 900, 925, 125, 950, 975, HostEntry.VPS_IP, 1025, 125, 125, 1050, 1075, 1100, 1125, 1150, 1175, 1200, 125};
    private static final String yy_packed = "\u0001\u0006\u0001\u0007\u0002\b\u0001\t\u0007\u0006\u0001\n\u0004\u0006\u0001\b\u0001\u000b\u0006\u0006\u0001\f\u0001\u0007\u0002\b\u0003\f\u0001\r\u0001\u000e\u0001\u000f\u0001\f\u0001\u0010\u0003\f\u0001\u0011\u0001\f\u0001\b\u0005\f\u0001\u0012\u0001\f\u0001\u0013\u0001\u0007\u0002\b\u0006\u0013\u0001\u0014\u0006\u0013\u0001\u0015\u0007\u0013\u0001\u0016\u0001\u0007\u0002\b\u0001\u0017\f\u0016\u0001\b\u0007\u0016\u0001\u0018\u0001\u0007\u0002\b\u0007\u0018\u0001\u0019\u0005\u0018\u0001\b\u0007\u0018\u001b��\u0001\b\u001b��\u0001\u001a ��\u0001\u001b\u0019��\u0001\u001c\u0015��\u0001\u0010\"��\u0001\u001d\u0016��\u0001\u001e\u000f��\u0001\u001f\u0013��\u0001 \u001e��\u0001\u0019\u0013��\u0001!\u0004��\u0001!\u0003��\u0001!\u0003��\u0001!\u0001��\u0001!\u000f��\u0001\"\u001b��\u0001#\u001e��\u0001$\u001b��\u0001%\u0014��\u0001&\n��\u0001'\u0004��\u0001'\u0003��\u0001'\u0003��\u0001'\u0001��\u0001'\t��\u0001(\u0004��\u0001(\u0003��\u0001(\u0003��\u0001(\u0001��\u0001(\u0011��\u0001)\u0018��\u0001*\u001d��\u0001+\u0018��\u0001,\u0016��\u0001-\r��\u0001.\u0004��\u0001.\u0003��\u0001.\u0003��\u0001.\u0001��\u0001.\t��\u0001/\u0004��\u0001/\u0003��\u0001/\u0003��\u0001/\u0001��\u0001/\u000f��\u00010\u001f��\u00011\u001b��\u00012\u0017��\u00013\t��\u00014\u0004��\u00014\u0003��\u00014\u0003��\u00014\u0001��\u00014\t��\u0001\u0006\u0004��\u0001\u0006\u0003��\u0001\u0006\u0003��\u0001\u0006\u0001��\u0001\u0006\u0012��\u00015\u0017��\u00016\u0018��\u00017\u0010��\u0001\u0016\u0004��\u0001\u0016\u0003��\u0001\u0016\u0003��\u0001\u0016\u0001��\u0001\u0016\u0013��\u00018\u0019��\u00019\u0019��\u0001:\u0014��\u0001;\u0019��\u0001<\u0017��\u0001=\u001d��\u0001>\f��\u0001?\u0011��";
    private static final int[] yytrans = yy_unpack(yy_packed);
    private static final String[] YY_ERROR_MSG = {"Unkown internal scanner error", "Internal error: unknown state", "Error: could not match input", "Error: pushback value was too large"};
    private static final byte[] YY_ATTRIBUTE = {0, 0, 0, 0, 0, 9, 1, 9, 1, 1, 1, 9, 9, 9, 9, 1, 1, 1, 9, 9, 1, 9, 1, 9, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 9, 1, 1, 0, 9, 0, 0, 1, 0, 9, 9, 0, 0, 0, 0, 0, 0, 0, 9};

    private Symbol symbol(int type) {
        return new Symbol(type);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, value);
    }

    private Symbol symbolInt(int type, String value) throws Exception {
        try {
            return new Symbol(type, Integer.valueOf(value));
        } catch (Exception ex) {
            errorMsg("Unable to get a value as Integer: [" + value + "]");
            throw ex;
        }
    }

    public void errorMsg(String msg) throws Exception {
        throw new Exception("DFA Lexer: " + msg + ".");
    }

    public DFALexer(Reader in) {
        this.yy_lexical_state = 0;
        this.yy_buffer = new char[16384];
        this.yy_reader = in;
    }

    public DFALexer(InputStream in) {
        this(new InputStreamReader(in));
    }

    private static int[] yy_unpack(String packed) {
        int[] trans = new int[1225];
        int i = 0;
        int j = 0;
        while (i < 400) {
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
        while (i < 84) {
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

    public Symbol next_token() throws IOException, Exception {
        int yy_input;
        int yy_next;
        while (true) {
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
                case 5:
                case 8:
                case 9:
                case 10:
                case yafvsym.ELSE /* 39 */:
                case 46:
                    yybegin(2);
                    return symbol(12, yytext());
                case 6:
                case 7:
                case 20:
                case SignupGuard.TRIAL_ACCOUNT_FLAG /* 64 */:
                case 65:
                case 66:
                case 67:
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
                    break;
                case 11:
                case 16:
                case 17:
                    errorMsg("Wrong symbol in state definition: [" + yytext() + "]");
                    break;
                case 12:
                    yybegin(0);
                    return symbol(7);
                case 13:
                    return symbol(5);
                case 14:
                    return symbol(6);
                case 15:
                    return symbolInt(11, yytext());
                case 18:
                    errorMsg("Wrong symbol in branch definition: [" + yytext() + "]");
                    break;
                case 19:
                    yybegin(3);
                    return symbol(10);
                case 21:
                case 22:
                case 45:
                case 51:
                    yybegin(2);
                    return symbol(12, yytext());
                case 23:
                    errorMsg("Wrong state number: [" + yytext() + "]");
                    break;
                case 24:
                    yybegin(0);
                    return symbolInt(11, yytext());
                case 25:
                case yafvsym.MOD /* 26 */:
                case yafvsym.AND /* 27 */:
                case yafvsym.f286OR /* 28 */:
                case yafvsym.f285LT /* 29 */:
                case 30:
                case yafvsym.LTEQ /* 31 */:
                case 32:
                case yafvsym.f288EQ /* 33 */:
                case yafvsym.NEQ /* 34 */:
                case yafvsym.NOT /* 35 */:
                case yafvsym.RETURN_CALL /* 36 */:
                case yafvsym.WHILE /* 37 */:
                case yafvsym.f287IF /* 38 */:
                case yafvsym.STRINGLITERAL /* 40 */:
                case yafvsym.INTEGER_CONSTANT /* 41 */:
                case yafvsym.FLOAT_CONSTANT /* 42 */:
                case yafvsym.BOOL_CONSTANT /* 43 */:
                case 47:
                case 49:
                case MySQLResource.MAX_DB_NAME /* 50 */:
                case 52:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 63:
                default:
                    if (yy_input == -1 && this.yy_startRead == this.yy_currentPos) {
                        this.yy_atEOF = true;
                        yy_do_eof();
                        return new Symbol(0);
                    }
                    yy_ScanError(2);
                    break;
                case 44:
                    yybegin(4);
                    return symbol(4);
                case 48:
                    yybegin(1);
                    return symbol(3);
                case 53:
                    return symbol(8);
                case 54:
                    return symbol(9);
                case 62:
                    yybegin(4);
                    return symbol(2);
            }
        }
    }
}
