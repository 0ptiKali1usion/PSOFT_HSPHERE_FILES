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

/* loaded from: hsphere.zip:psoft/yafv/YAFVLexer.class */
public class YAFVLexer implements Scanner {
    public static final int YYEOF = -1;
    public static final int FILTER_REGEXP_CHARCLASS = 5;
    public static final int INFO_MESSAGE = 2;
    public static final int STRING = 1;
    public static final int FILTER_REGEXP_MAIN = 4;
    public static final int START_REGEXP = 3;
    public static final int YYINITIAL = 0;
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
    private static final String yycmap_packed = "\t\u0015\u0001\u0003\u0001\u0002\u0002��\u0001\u0001\u000e\u0015\u0004��\u0001\u0003\u0001/\u0001\b\u0001\u0006\u0001\u0013\u0001:\u00011\u0001��\u00014\u00015\u0001\u0005\u00019\u00017\u0001\u001a\u0001\u0018\u0001\u0004\u0001\u0017\t\u000b\u00018\u0001(\u00012\u0001\u0007\u00013\u0001��\u0001\u0006\u0001\u0014\u0001'\u0002\u0014\u0001\u0019\u0001%\u0002\u0013\u0001 \t\u0013\u0001\u0010\u0007\u0013\u0001\u0011\u0001\t\u0001\u0012\u0001��\u0001\u0013\u0001��\u0001$\u0001&\u0001.\u0001\r\u0001\u001d\u0001!\u0001\u001e\u0001,\u0001\u001b\u0002\u0013\u0001\"\u0001+\u0001\f\u0001#\u0001*\u0001\u0013\u0001\u001f\u0001\u000f\u0001\u001c\u0001\n\u0001-\u0001\u000e\u0001)\u0002\u0013\u00016\u00010\u0001\u0016\u0001\u0006!\u0015\u0002��\u0004\u0013\u0004��\u0001\u0013\u0002��\u0001\u0015\u0007��\u0001\u0013\u0004��\u0001\u0013\u0005��\u0017\u0013\u0001��\u001f\u0013\u0001��Ŀ\u0013\u0019��r\u0013\u0004��\f\u0013\u000e��\u0005\u0013\t��\u0001\u0013\u0011��X\u0015\u0005��\u0013\u0015\n��\u0001\u0013\u000b��\u0001\u0013\u0001��\u0003\u0013\u0001��\u0001\u0013\u0001��\u0014\u0013\u0001��,\u0013\u0001��&\u0013\u0001��\u0005\u0013\u0004��\u0082\u0013\u0001��\u0004\u0015\u0003��E\u0013\u0001��&\u0013\u0002��\u0002\u0013\u0006��\u0010\u0013!��&\u0013\u0002��\u0001\u0013\u0007��'\u0013\t��\u0011\u0015\u0001��\u0017\u0015\u0001��\u0003\u0015\u0001��\u0001\u0015\u0001��\u0002\u0015\u0001��\u0001\u0015\u000b��\u001b\u0013\u0005��\u0003\u0013\r��\u0004\u0015\f��\u0006\u0015\u000b��\u001a\u0013\u0005��\u000b\u0013\u000e\u0015\u0007��\n\u0015\u0004��\u0002\u0013\u0001\u0015c\u0013\u0001��\u0001\u0013\b\u0015\u0001��\u0006\u0015\u0002\u0013\u0002\u0015\u0001��\u0004\u0015\u0002\u0013\n\u0015\u0003\u0013\u0002��\u0001\u0013\u000f��\u0001\u0015\u0001\u0013\u0001\u0015\u001e\u0013\u001b\u0015\u0002��\u0003\u00130��&\u0013\u000b\u0015\u0001\u0013ŏ��\u0003\u00156\u0013\u0002��\u0001\u0015\u0001\u0013\u0010\u0015\u0002��\u0001\u0013\u0004\u0015\u0003��\n\u0013\u0002\u0015\u0002��\n\u0015\u0011��\u0003\u0015\u0001��\b\u0013\u0002��\u0002\u0013\u0002��\u0016\u0013\u0001��\u0007\u0013\u0001��\u0001\u0013\u0003��\u0004\u0013\u0002��\u0001\u0015\u0001\u0013\u0007\u0015\u0002��\u0002\u0015\u0002��\u0003\u0015\t��\u0001\u0015\u0004��\u0002\u0013\u0001��\u0003\u0013\u0002\u0015\u0002��\n\u0015\u0004\u0013\r��\u0003\u0015\u0001��\u0006\u0013\u0004��\u0002\u0013\u0002��\u0016\u0013\u0001��\u0007\u0013\u0001��\u0002\u0013\u0001��\u0002\u0013\u0001��\u0002\u0013\u0002��\u0001\u0015\u0001��\u0005\u0015\u0004��\u0002\u0015\u0002��\u0003\u0015\u000b��\u0004\u0013\u0001��\u0001\u0013\u0007��\f\u0015\u0003\u0013\f��\u0003\u0015\u0001��\t\u0013\u0001��\u0003\u0013\u0001��\u0016\u0013\u0001��\u0007\u0013\u0001��\u0002\u0013\u0001��\u0005\u0013\u0002��\u0001\u0015\u0001\u0013\b\u0015\u0001��\u0003\u0015\u0001��\u0003\u0015\u0002��\u0001\u0013\u000f��\u0002\u0013\u0002\u0015\u0002��\n\u0015\u0001��\u0001\u0013\u000f��\u0003\u0015\u0001��\b\u0013\u0002��\u0002\u0013\u0002��\u0016\u0013\u0001��\u0007\u0013\u0001��\u0002\u0013\u0001��\u0005\u0013\u0002��\u0001\u0015\u0001\u0013\u0006\u0015\u0003��\u0002\u0015\u0002��\u0003\u0015\b��\u0002\u0015\u0004��\u0002\u0013\u0001��\u0003\u0013\u0004��\n\u0015\u0001��\u0001\u0013\u0010��\u0001\u0015\u0001\u0013\u0001��\u0006\u0013\u0003��\u0003\u0013\u0001��\u0004\u0013\u0003��\u0002\u0013\u0001��\u0001\u0013\u0001��\u0002\u0013\u0003��\u0002\u0013\u0003��\u0003\u0013\u0003��\b\u0013\u0001��\u0003\u0013\u0004��\u0005\u0015\u0003��\u0003\u0015\u0001��\u0004\u0015\t��\u0001\u0015\u000f��\t\u0015\t��\u0001\u0013\u0007��\u0003\u0015\u0001��\b\u0013\u0001��\u0003\u0013\u0001��\u0017\u0013\u0001��\n\u0013\u0001��\u0005\u0013\u0004��\u0007\u0015\u0001��\u0003\u0015\u0001��\u0004\u0015\u0007��\u0002\u0015\t��\u0002\u0013\u0004��\n\u0015\u0012��\u0002\u0015\u0001��\b\u0013\u0001��\u0003\u0013\u0001��\u0017\u0013\u0001��\n\u0013\u0001��\u0005\u0013\u0002��\u0001\u0015\u0001\u0013\u0007\u0015\u0001��\u0003\u0015\u0001��\u0004\u0015\u0007��\u0002\u0015\u0007��\u0001\u0013\u0001��\u0002\u0013\u0004��\n\u0015\u0012��\u0002\u0015\u0001��\b\u0013\u0001��\u0003\u0013\u0001��\u0017\u0013\u0001��\u0010\u0013\u0004��\u0006\u0015\u0002��\u0003\u0015\u0001��\u0004\u0015\t��\u0001\u0015\b��\u0002\u0013\u0004��\n\u0015\u0012��\u0002\u0015\u0001��\u0012\u0013\u0003��\u0018\u0013\u0001��\t\u0013\u0001��\u0001\u0013\u0002��\u0007\u0013\u0003��\u0001\u0015\u0004��\u0006\u0015\u0001��\u0001\u0015\u0001��\b\u0015\u0012��\u0002\u0015\r��0\u0013\u0001\u0015\u0002\u0013\u0007\u0015\u0004��\b\u0013\b\u0015\u0001��\n\u0015'��\u0002\u0013\u0001��\u0001\u0013\u0002��\u0002\u0013\u0001��\u0001\u0013\u0002��\u0001\u0013\u0006��\u0004\u0013\u0001��\u0007\u0013\u0001��\u0003\u0013\u0001��\u0001\u0013\u0001��\u0001\u0013\u0002��\u0002\u0013\u0001��\u0004\u0013\u0001\u0015\u0002\u0013\u0006\u0015\u0001��\u0002\u0015\u0001\u0013\u0002��\u0005\u0013\u0001��\u0001\u0013\u0001��\u0006\u0015\u0002��\n\u0015\u0002��\u0002\u0013\"��\u0001\u0013\u0017��\u0002\u0015\u0006��\n\u0015\u000b��\u0001\u0015\u0001��\u0001\u0015\u0001��\u0001\u0015\u0004��\u0002\u0015\b\u0013\u0001��\"\u0013\u0006��\u0014\u0015\u0001��\u0002\u0015\u0004\u0013\u0004��\b\u0015\u0001��$\u0015\t��\u0001\u00159��\"\u0013\u0001��\u0005\u0013\u0001��\u0002\u0013\u0001��\u0007\u0015\u0003��\u0004\u0015\u0006��\n\u0015\u0006��\u0006\u0013\u0004\u0015F��&\u0013\n��)\u0013\u0007��Z\u0013\u0005��D\u0013\u0005��R\u0013\u0006��\u0007\u0013\u0001��?\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0002��\u0007\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0002��'\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0002��\u001f\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0002��\u0007\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0002��\u0007\u0013\u0001��\u0007\u0013\u0001��\u0017\u0013\u0001��\u001f\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0002��\u0007\u0013\u0001��'\u0013\u0001��\u0013\u0013\u000e��\t\u0015.��U\u0013\f��ɬ\u0013\u0002��\b\u0013\n��\u001a\u0013\u0005��K\u0013\u0003��\u0003\u0013\u000f��\r\u0013\u0001��\u0004\u0013\u0003\u0015\u000b��\u0012\u0013\u0003\u0015\u000b��\u0012\u0013\u0002\u0015\f��\r\u0013\u0001��\u0003\u0013\u0001��\u0002\u0015\f��4\u0013 \u0015\u0003��\u0001\u0013\u0003��\u0002\u0013\u0001\u0015\u0002��\n\u0015!��\u0003\u0015\u0002��\n\u0015\u0006��X\u0013\b��)\u0013\u0001\u0015V��\u001d\u0013\u0003��\f\u0015\u0004��\f\u0015\n��\n\u0015\u001e\u0013\u0002��\u0005\u0013\u038b��l\u0013\u0094��\u009c\u0013\u0004��Z\u0013\u0006��\u0016\u0013\u0002��\u0006\u0013\u0002��&\u0013\u0002��\u0006\u0013\u0002��\b\u0013\u0001��\u0001\u0013\u0001��\u0001\u0013\u0001��\u0001\u0013\u0001��\u001f\u0013\u0002��5\u0013\u0001��\u0007\u0013\u0001��\u0001\u0013\u0003��\u0003\u0013\u0001��\u0007\u0013\u0003��\u0004\u0013\u0002��\u0006\u0013\u0004��\r\u0013\u0005��\u0003\u0013\u0001��\u0007\u0013\u000f��\u0004\u0015\u001a��\u0005\u0015\u0010��\u0002\u0013\u0013��\u0001\u0013\u000b��\u0004\u0015\u0006��\u0006\u0015\u0001��\u0001\u0013\r��\u0001\u0013 ��\u0012\u0013\u001e��\r\u0015\u0004��\u0001\u0015\u0003��\u0006\u0015\u0017��\u0001\u0013\u0004��\u0001\u0013\u0002��\n\u0013\u0001��\u0001\u0013\u0003��\u0005\u0013\u0006��\u0001\u0013\u0001��\u0001\u0013\u0001��\u0001\u0013\u0001��\u0004\u0013\u0001��\u0003\u0013\u0001��\u0007\u0013\u0003��\u0003\u0013\u0005��\u0005\u0013\u0016��$\u0013ກ��\u0003\u0013\u0019��\t\u0013\u0006\u0015\u0001��\u0005\u0013\u0002��\u0005\u0013\u0004��V\u0013\u0002��\u0002\u0015\u0002��\u0003\u0013\u0001��_\u0013\u0005��(\u0013\u0004��^\u0013\u0011��\u0018\u00138��\u0010\u0013Ȁ��ᦶ\u0013J��冦\u0013Z��ҍ\u0013ݳ��⮤\u0013⅜��Į\u0013\u0002��;\u0013\u0095��\u0007\u0013\f��\u0005\u0013\u0005��\u0001\u0013\u0001\u0015\n\u0013\u0001��\r\u0013\u0001��\u0005\u0013\u0001��\u0001\u0013\u0001��\u0002\u0013\u0001��\u0002\u0013\u0001��l\u0013!��ū\u0013\u0012��@\u0013\u0002��6\u0013(��\r\u0013\u0003��\u0010\u0015\u0010��\u0004\u0015\u000f��\u0002\u0013\u0018��\u0003\u0013\u0019��\u0001\u0013\u0006��\u0005\u0013\u0001��\u0087\u0013\u0002��\u0001\u0015\u0004��\u0001\u0013\u000b��\n\u0015\u0007��\u001a\u0013\u0004��\u0001\u0013\u0001��\u001a\u0013\n��Z\u0013\u0003��\u0006\u0013\u0002��\u0006\u0013\u0002��\u0006\u0013\u0002��\u0003\u0013\u0003��\u0002\u0013\u0003��\u0002\u0013\u0012��\u0003\u0015\u0004��";
    private static final char[] yycmap = yy_unpack_cmap(yycmap_packed);
    private static final int[] yy_rowMap = {0, 59, 118, 177, 236, 295, 118, 354, 118, 413, 118, 472, 118, 531, 590, 649, 708, 118, 767, 118, 826, 885, 944, 1003, 1062, 1121, 1180, 1239, 1298, 118, 1357, 1416, 1475, 1534, 1593, 1652, 1711, 1770, 118, 118, 118, 118, 118, 118, 118, 1829, 118, 118, 1888, 118, 118, 118, 118, 1947, 118, 2006, 118, 2065, 118, 2124, 118, 2183, 2242, 118, 2301, 2360, 2419, 2478, 767, 2537, 531, 2596, 2655, 2714, 2773, 2832, 2891, 2950, 3009, 3068, 3127, 3186, 3245, 118, 118, 118, 118, 118, 3304, 118, 118, 118, 118, 118, 118, 118, 118, 118, 2183, 3363, 3422, 3481, 3540, 3540, 3599, 3658, 3717, 3776, 3835, 3894, 3953, 4012, 4071, 3717, 4130, 4189, 4248, 4307, 4366, 4425, 4484, 4543, 4602, 4661, 3363, 4720, 4779, 4838, 4897, 531, 531, 4956, 5015, 5074, 5133, 531, 5192, 5251, 5310, 5369, 5428, 5487, 5546, 531, 5605, 5664, 5723, 5782, 5841, 5900, 531, 5959, 6018, 6077, 6136, 531, 6195, 6254, 531, 6313, 6372, 531, 531, 6431, 6490, 6549, 6608, 6667, 6726, 118, 531, 531, 6785, 531, 6844, 6903, 6962, 7021, 7080, 118, 7139, 7198, 7257, 531, 531, 531, 531};
    private static final String yy_packed = "\u0001\u0007\u0001\b\u0002\t\u0001\n\u0001\u000b\u0001\u0007\u0001\f\u0001\r\u0001\u0007\u0001\u000e\u0001\u000f\u0002\u000e\u0001\u0010\u0002\u0011\u0002\u0007\u0002\u000e\u0001\u0007\u0001\u0012\u0001\u0013\u0001\u0007\u0001\u000e\u0001\u0014\u0001\u0015\u0001\u0016\u0001\u0017\u0001\u000e\u0001\u0018\u0001\u0019\u0001\u001a\u0003\u000e\u0001\u001b\u0001\u001c\u0001\u001d\u0001\u001e\u0002\u000e\u0001\u001f\u0001\u000e\u0001 \u0001!\u0001\"\u0001#\u0001$\u0001%\u0001&\u0001'\u0001(\u0001)\u0001*\u0001+\u0001,\u0001-\u0001.\u0002��\u0003.\u0001/\u0001.\u00010\u00011'.\u0003/\u0007.;��\u00012\u0001\b\u0002\t\u00032\u00013\u00014\u00012\u0001\u000e\u00012\u0005\u000e\u00022\u0002\u000e\u00042\u0001\u000e\u00012\r\u000e\u00012\u0006\u000e\f2\u00015\u00016\u00017\u00035\u0001/\u00015\u00010\u00018\u00075\u00019\u001f5\u0003/\b5\u0001:\u0001;\u00035\u0001/\u00015\u0001;\u0001<\b5\u0001=\u001e5\u0003/\u00075\u0002��\u0001\t<��\u0001>\u0001?<��\u0001@=��\u0010\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000f��\u0001\u000f\u000b��\u0001\u000f\u0001A\u0001B\u0003��\u0001B'��\u0010\u000e\u0001��\r\u000e\u0001��\u0003\u000e\u0001C\u0002\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001D\u000b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000f��\u0001E\u000b��\u0001E\u0001A\u0001B\u0003��\u0001B'��\u0002\u000e\u0001F\r\u000e\u0001��\u0006\u000e\u0001G\u0006\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0004\u000e\u0001H\b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001I\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001J\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001K\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001L\u0001\u000e\u0001M\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001L\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\b\u000e\u0001N\u0004\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\b\u000e\u0001O\u0004\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001P\b\u000e\u0001Q\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\t\u000e\u0001R\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\b\u000e\u0001S\u0004\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000b��\u0001Tc��\u0001U;��\u0001V\u0010��\u0001W:��\u0001X3��\u0001.\u0002��\u0003.\u0001��\u0001.\u0002��'.\u0003��\u0007.\n5\u0001Y05\u0002��\u000178��\n5\u0001Y\u00015\u0001Z\u0001[\u0001\\\u0001]\u0001^*5\u0002��\u0001;8��\n5\u0001Y\u00015\u0001_\u0001`\u0001a\u0001b\u0001_*5\u0001>\u0001c\u0001\t8>\u0004?\u0001d\u0001e5?\u000b��\u0001f\u000b��\u0001f.��\u0001g\u000b��\u0001g\u0002��\u0001h\u001e��\u0001h\u000b��\u0010\u000e\u0001��\u0001i\f\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0004\u000e\u0001j\b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001k\u000b\u000e\u0001��\u0005\u000e\u0001l\u0007��\u0001\u000e\u000e��\u0001m\u000f\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0005\u000e\u0001n\n\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001o\u0001\u000e\u0001p\u0002\u000e\u0001q\u0006\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001r\u000b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\b\u000e\u0001s\u0004\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001t\u0006\u000e\u0001u\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\b\u000e\u0001v\u0004\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\b\u000e\u0001w\u0004\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001x\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001��\u0001y\u0005\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001z\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001{\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000f��\u0001|\u0001��\u0001|\u0006��\u0001|\u0002��\u0001|\u0001��\u0001|\u0003��\u0001|\u0003��\u0001|\u0002��\u0004|\u0006��\u0001|\f��\u0005?\u0001��9?\u0001}\u0001e5?\u000b��\u0001f\u000b��\u0001f\u0001��\u0001B\u0003��\u0001B(��\u0001g\u000b��\u0001g-��\u0010\u000e\u0001��\u0007\u000e\u0001~\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u007f\f\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0080\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001\u0081\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0082\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0083\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0001\u0084\u000f\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0085\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0086\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\t\u000e\u0001\u0087\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001\u0088\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0005\u000e\u0001m\n\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001\u0089\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001\u008a\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001\u008b\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0007\u000e\u0001\u008c\u0005\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u008d\f\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0005\u000e\u0001\u008e\n\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000f��\u0001\u008f\u0001��\u0001\u008f\u0006��\u0001\u008f\u0002��\u0001\u008f\u0001��\u0001\u008f\u0003��\u0001\u008f\u0003��\u0001\u008f\u0002��\u0004\u008f\u0006��\u0001\u008f\u0016��\u0010\u000e\u0001��\u0002\u000e\u0001\u0090\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001\u0091\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0003\u000e\u0001\u0092\t\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0001\u0093\u000f\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0004\u000e\u0001\u0094\b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001��\u0001\u0095\u0005\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0004\u000e\u0001\u0096\b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001\u0097\u000b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0098\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u0099\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u009a\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001\u009b\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0003\u000e\u0001\u009c\f\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001\u009d\u000b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000f��\u0001\u009e\u0001��\u0001\u009e\u0006��\u0001\u009e\u0002��\u0001\u009e\u0001��\u0001\u009e\u0003��\u0001\u009e\u0003��\u0001\u009e\u0002��\u0004\u009e\u0006��\u0001\u009e\u0016��\u0010\u000e\u0001��\u0003\u000e\u0001\u009f\t\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001 \n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0003\u000e\u0001¡\f\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001¢\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001��\u0001\u000e\u0001£\u0004\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001¤\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\t\u000e\u0001¥\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\t\u000e\u0001¦\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001§\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001¨\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0004\u000e\u0001©\b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000f��\u0001ª\u0001��\u0001ª\u0006��\u0001ª\u0002��\u0001ª\u0001��\u0001ª\u0003��\u0001ª\u0003��\u0001ª\u0002��\u0004ª\u0006��\u0001ª\u0016��\u0010\u000e\u0001��\u0004\u000e\u0001«\b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001¬\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001\u00ad\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001®\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001¯\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0003\u000e\u0001°\t\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0003\u000e\u0001±\t\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\t\u000e\u0001²\u0003\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001��\u0005\u000e\u0001³\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001´\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001µ\u000b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001\u000e\u0001¶\u000b\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0001·\f\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\u0002\u000e\u0001¸\n\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001��\u0003\u000e\u0001¹\u0002\u000e\u0007��\u0001\u000e\u000e��\u0010\u000e\u0001��\r\u000e\u0001��\u0003\u000e\u0001º\u0002\u000e\u0007��\u0001\u000e\u000e��\u0002\u000e\u0001»\r\u000e\u0001��\r\u000e\u0001��\u0006\u000e\u0007��\u0001\u000e\u0004��";
    private static final int[] yytrans = yy_unpack(yy_packed);
    private static final String[] YY_ERROR_MSG = {"Unkown internal scanner error", "Internal error: unknown state", "Error: could not match input", "Error: pushback value was too large"};
    private static final byte[] YY_ATTRIBUTE = {0, 0, 8, 0, 0, 0, 9, 1, 9, 1, 9, 1, 9, 1, 1, 1, 1, 9, 1, 9, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 1, 9, 9, 0, 9, 9, 9, 9, 1, 9, 1, 9, 1, 9, 1, 9, 0, 0, 9, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 0, 9, 9, 9, 9, 9, 9, 9, 9, 9, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 1, 1, 1, 1, 1, 1, 1};

    private StringBuffer cleanStrBuffer(StringBuffer buffer) {
        buffer.setLength(0);
        return buffer;
    }

    private Symbol symbol(int type) {
        return new Symbol(type, this.yyline, this.yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, this.yyline, this.yycolumn, value);
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

    public YAFVLexer(Reader in) {
        this.yy_lexical_state = 0;
        this.yy_buffer = new char[16384];
        this.string = new StringBuffer();
        this.yy_reader = in;
    }

    YAFVLexer(InputStream in) {
        this(new InputStreamReader(in));
    }

    private static int[] yy_unpack(String packed) {
        int[] trans = new int[7316];
        int i = 0;
        int j = 0;
        while (i < 2370) {
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
        while (i < 1772) {
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
                case 6:
                case yafvsym.NEQ /* 34 */:
                case yafvsym.NOT /* 35 */:
                    exitErrorMsg("An unknown lexeme [" + yytext() + "]");
                    break;
                case 7:
                case 8:
                case 98:
                case 124:
                case 188:
                case 189:
                case 190:
                case 191:
                case 192:
                case 193:
                case 194:
                case 195:
                case 196:
                case 197:
                case 198:
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
                case 243:
                case 244:
                case 245:
                case 246:
                case 247:
                case 248:
                case 249:
                case 250:
                case 251:
                    break;
                case 9:
                    return symbol(25);
                case 10:
                    return symbol(24);
                case 11:
                case MySQLResource.MAX_DB_NAME /* 50 */:
                    return symbol(21);
                case 12:
                    cleanStrBuffer(this.string);
                    yybegin(1);
                    break;
                case 13:
                case 15:
                case 16:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case yafvsym.MOD /* 26 */:
                case yafvsym.AND /* 27 */:
                case yafvsym.f286OR /* 28 */:
                case 30:
                case yafvsym.LTEQ /* 31 */:
                case 32:
                case 66:
                case 67:
                case 69:
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
                case Resource.B_REFUND_ALL /* 104 */:
                case 105:
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
                case 125:
                case 126:
                case 127:
                case SignupGuard.MODERATE_EVERYTHING_FLAG /* 128 */:
                case 131:
                case 132:
                case 133:
                case 134:
                case 137:
                case 138:
                case 139:
                case 140:
                case 141:
                case 144:
                case 145:
                case 146:
                case 147:
                case 148:
                case 149:
                case 151:
                case 152:
                case 153:
                case 154:
                case 156:
                case 159:
                case 160:
                case 163:
                case 164:
                case 165:
                case 166:
                case 167:
                case 168:
                case 172:
                case 174:
                case 175:
                case 176:
                case 177:
                case 178:
                case 180:
                case 181:
                case 182:
                    return symbol(16, yytext());
                case 14:
                case 18:
                    return symbol(41, yytext());
                case 17:
                    return symbol(12);
                case 19:
                    return symbol(23);
                case yafvsym.f285LT /* 29 */:
                    return symbol(15);
                case yafvsym.f288EQ /* 33 */:
                    return symbol(35);
                case yafvsym.RETURN_CALL /* 36 */:
                    return symbol(29);
                case yafvsym.WHILE /* 37 */:
                    return symbol(30);
                case yafvsym.f287IF /* 38 */:
                    return symbol(9);
                case yafvsym.ELSE /* 39 */:
                    return symbol(10);
                case yafvsym.STRINGLITERAL /* 40 */:
                    return symbol(11);
                case yafvsym.INTEGER_CONSTANT /* 41 */:
                    return symbol(13);
                case yafvsym.FLOAT_CONSTANT /* 42 */:
                    return symbol(14);
                case yafvsym.BOOL_CONSTANT /* 43 */:
                    return symbol(22);
                case 44:
                    return symbol(26);
                case 45:
                case 52:
                    this.string.append(yytext());
                    break;
                case 46:
                    this.string.append("\\").append(yytext());
                    break;
                case 47:
                    yybegin(0);
                    return symbol(40, this.string.toString());
                case 48:
                case 61:
                case 62:
                case SignupGuard.TRIAL_ACCOUNT_FLAG /* 64 */:
                case 65:
                case 88:
                case 99:
                case AsyncResourceException.UNKNOWN /* 100 */:
                case 103:
                case 187:
                default:
                    if (yy_input == -1 && this.yy_startRead == this.yy_currentPos) {
                        this.yy_atEOF = true;
                        yy_do_eof();
                        return new Symbol(0);
                    }
                    yy_ScanError(2);
                    break;
                case 49:
                    exitErrorMsg("An Illegal symbol '" + yytext() + "'.");
                    break;
                case 51:
                    cleanStrBuffer(this.string);
                    yybegin(4);
                    break;
                case 53:
                case 54:
                case 55:
                    exitErrorMsg("An illegal symbol '" + yytext() + "'");
                    break;
                case 56:
                    this.string.append("[");
                    yybegin(5);
                    break;
                case 57:
                case 58:
                case 59:
                    exitErrorMsg("An illegal symbol '" + yytext() + "'.");
                    break;
                case 60:
                    this.string.append("]");
                    yybegin(4);
                    break;
                case 63:
                    return symbol(33);
                case 68:
                case 101:
                case 102:
                    return symbol(42, yytext());
                case 70:
                    return symbol(38);
                case 83:
                    return symbol(34);
                case 84:
                    return symbol(28);
                case 85:
                    return symbol(27);
                case 86:
                    return symbol(31);
                case 87:
                    return symbol(32);
                case 89:
                    this.string.append("(\r\n|\n)");
                    break;
                case 90:
                    this.string.append("[0-9]");
                    break;
                case 91:
                    this.string.append("[0-9a-zA-Z_]");
                    break;
                case 92:
                    this.string.append("[ \t]");
                    break;
                case 93:
                    this.string.append("[^ \t]");
                    break;
                case 94:
                    exitErrorMsg("You shouldn't use the '" + yytext() + "' symbol inside a char class ([...]).");
                    break;
                case 95:
                    this.string.append("0-9");
                    break;
                case 96:
                    this.string.append("0-9a-zA-Z_");
                    break;
                case 97:
                    this.string.append(" \t");
                    break;
                case 106:
                case 170:
                    return symbol(17);
                case 123:
                case 142:
                case 157:
                case 169:
                    this.string.append(String.valueOf((char) Integer.parseInt(yytext().substring(2), 16)));
                    break;
                case 129:
                    return symbol(43, yytext());
                case 130:
                    return symbol(39);
                case 135:
                    return symbol(8);
                case 136:
                case 173:
                case 179:
                    return symbol(20);
                case 143:
                    return symbol(37);
                case 150:
                    return symbol(18);
                case 155:
                    return symbol(7);
                case 158:
                    return symbol(19);
                case 161:
                    return symbol(36);
                case 162:
                    yybegin(3);
                    return symbol(3);
                case 171:
                    return symbol(2);
                case 183:
                    exitErrorMsg("The syntax with 'reference' is not supported any longer. Instead, \nplease use the following construction: check=\"${validation_var}\"\nin the client-side templates. \nNote, 'validation_var' is a freemarker variable which is evaluated to \nthe real name of a validation function (regexp) during template processing");
                    break;
                case 184:
                    return symbol(6);
                case 185:
                    return symbol(5);
                case 186:
                    return symbol(4);
            }
        }
    }
}
