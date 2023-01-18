package psoft.yafv;

import java.io.StringWriter;
import java.util.Stack;
import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;
import java_cup.runtime.lr_parser;

/* loaded from: hsphere.zip:psoft/yafv/DFAParser.class */
public class DFAParser extends lr_parser {
    protected static final short[][] _production_table = unpackFromStrings(new String[]{"��\u0011��\u0002\u0003\u0004��\u0002\u0002\u0004��\u0002\u0004\u0004��\u0002\u0004\u0003��\u0002\u0005\u0004��\u0002\u0005\u0003��\u0002\u0006\u0004��\u0002\u0006\u0003��\u0002\u0007\b��\u0002\u0007\u0003��\u0002\b\u0003��\u0002\b\u0003��\u0002\t\u0004��\u0002\t\u0003��\u0002\n\u0005��\u0002\u000b\u0003��\u0002\u000b\u0005"});
    protected static final short[][] _action_table = unpackFromStrings(new String[]{"��\u001d��\u0006\u0003\u0005\u0004\u0004\u0001\u0002��\u0004\r\u001f\u0001\u0002��\u0006\u0003\ufffe\u0005\ufffe\u0001\u0002��\u0004\u0002\u001e\u0001\u0002��\u0006\u0003\t\u0005\f\u0001\u0002��\u0004\u0002\u0001\u0001\u0002��\n\u0002\ufff8\u0003\ufff8\u0005\ufff8\u000e\ufff8\u0001\u0002��\n\u0002\ufffa\u0003\ufffa\u0005\ufffa\u000e\u0018\u0001\u0002��\b\u0002￼\u0003\t\u0005\f\u0001\u0002��\u0004\r\r\u0001\u0002��\u0004\u0007\u000e\u0001\u0002��\u0006\n\u0010\u000b\u0011\u0001\u0002��\u0004\b\u0012\u0001\u0002��\u0004\b\ufff7\u0001\u0002��\u0004\b\ufff6\u0001\u0002��\u0004\t\u0013\u0001\u0002��\n\u0002\ufff9\u0003\ufff9\u0005\ufff9\u000e\ufff9\u0001\u0002��\u0004\u0002�\u0001\u0002��\b\u0002\ufffb\u0003\ufffb\u0005\ufffb\u0001\u0002��\n\u0002\ufff4\u0003\ufff4\u0005\ufff4\u000e\u0018\u0001\u0002��\u0004\u0006\u001b\u0001\u0002��\u0006\u0006\ufff2\f\u0019\u0001\u0002��\u0004\u000e\u001a\u0001\u0002��\u0004\u0006\ufff1\u0001\u0002��\u0004\r\u001c\u0001\u0002��\n\u0002\ufff3\u0003\ufff3\u0005\ufff3\u000e\ufff3\u0001\u0002��\b\u0002\ufff5\u0003\ufff5\u0005\ufff5\u0001\u0002��\u0004\u0002��\u0001\u0002��\u0006\u0003\uffff\u0005\uffff\u0001\u0002"});
    protected static final short[][] _reduce_table = unpackFromStrings(new String[]{"��\u001d��\u0006\u0003\u0005\u0004\u0006\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\b\u0005\u0007\u0006\n\u0007\t\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\b\t\u0014\n\u0015\u000b\u0016\u0001\u0001��\b\u0005\u0013\u0006\n\u0007\t\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0004\b\u000e\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\b\t\u001c\n\u0015\u000b\u0016\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001��\u0002\u0001\u0001"});
    protected CUP$DFAParser$actions action_obj;
    DFALexer lexer;
    public StringWriter writer;

    public DFAParser() {
    }

    public DFAParser(Scanner s) {
        super(s);
    }

    public short[][] production_table() {
        return _production_table;
    }

    public short[][] action_table() {
        return _action_table;
    }

    public short[][] reduce_table() {
        return _reduce_table;
    }

    protected void init_actions() {
        this.action_obj = new CUP$DFAParser$actions(this);
    }

    public Symbol do_action(int act_num, lr_parser parser, Stack stack, int top) throws Exception {
        return this.action_obj.CUP$DFAParser$do_action(act_num, parser, stack, top);
    }

    public int start_state() {
        return 0;
    }

    public int start_production() {
        return 1;
    }

    public int EOF_sym() {
        return 0;
    }

    public int error_sym() {
        return 1;
    }

    public DFAParser(DFALexer lexer, StringWriter writer) {
        super(lexer);
        this.lexer = lexer;
        this.writer = writer;
    }

    public void report_error(String message) throws Exception {
        throw new Exception("YAFVParser error: " + message);
    }
}
