package psoft.yafv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java_cup.runtime.Symbol;
import java_cup.runtime.lr_parser;

/* loaded from: hsphere.zip:psoft/yafv/CUP$DFAParser$actions.class */
class CUP$DFAParser$actions {
    String param;
    String param_type;
    int initState = 0;
    final String NEW_ARRAY = "new Array";
    ArrayList stateList = new ArrayList();
    ArrayList stateBranches = null;
    private final DFAParser parser;

    public CUP$DFAParser$actions(DFAParser parser) {
        this.parser = parser;
    }

    /* JADX WARN: Not initialized variable reg: 4, insn: ?: MOVE  (r1 I:??) = (r4 I:??), block:B:111:0x057c */
    public final Symbol CUP$DFAParser$do_action(int CUP$DFAParser$act_num, lr_parser CUP$DFAParser$parser, Stack CUP$DFAParser$stack, int CUP$DFAParser$top) throws Exception {
        long j;
        switch (CUP$DFAParser$act_num) {
            case 0:
                this.parser.writer.write("new Array(");
                long stateNumber = -1;
                Iterator stlIter = this.stateList.iterator();
                while (stlIter.hasNext()) {
                    stateNumber = j;
                    if (stateNumber + 1 > 0) {
                        this.parser.writer.write(",");
                    }
                    boolean wasDrawn = false;
                    this.parser.writer.write("new Array(");
                    Iterator iter = ((ArrayList) stlIter.next()).iterator();
                    while (iter.hasNext()) {
                        if (!wasDrawn) {
                            wasDrawn = true;
                        } else {
                            this.parser.writer.write(",");
                        }
                        String chr = (String) iter.next();
                        if ("'".equals(chr) || "\"".equals(chr)) {
                            chr = "\\" + chr;
                        }
                        this.parser.writer.write("'" + chr + "'");
                    }
                    if (!wasDrawn) {
                        this.parser.writer.write("'0'");
                    }
                    this.parser.writer.write(")");
                }
                if (stateNumber < 0) {
                    this.parser.report_error("Internal error: There are no states defined in the DFA.");
                }
                this.parser.writer.write(",new Array('fa'," + String.valueOf(stateNumber) + "," + this.initState + "))");
                this.parser.writer.close();
                Symbol CUP$DFAParser$result = new Symbol(1, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result;
            case 1:
                int i = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left;
                int i2 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).right;
                Object start_val = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).value;
                Symbol CUP$DFAParser$result2 = new Symbol(0, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, start_val);
                CUP$DFAParser$parser.done_parsing();
                return CUP$DFAParser$result2;
            case 2:
                int i3 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left;
                int i4 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right;
                Integer stateIndex = (Integer) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).value;
                this.initState = stateIndex.intValue();
                Symbol CUP$DFAParser$result3 = new Symbol(2, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result3;
            case 3:
                this.parser.report_error("Unable to get the initial state.");
                Symbol CUP$DFAParser$result4 = new Symbol(2, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result4;
            case 4:
                Symbol CUP$DFAParser$result5 = new Symbol(3, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result5;
            case 5:
                Symbol CUP$DFAParser$result6 = new Symbol(3, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result6;
            case 6:
                Symbol CUP$DFAParser$result7 = new Symbol(4, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result7;
            case 7:
                Symbol CUP$DFAParser$result8 = new Symbol(4, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result8;
            case 8:
                int i5 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 4)).left;
                int i6 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 4)).right;
                Integer stateIndex2 = (Integer) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 4)).value;
                int i7 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).left;
                int i8 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).right;
                String a_r = (String) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).value;
                this.stateBranches = new ArrayList();
                this.stateBranches.add(a_r);
                this.stateList.add(stateIndex2.intValue(), this.stateBranches);
                Symbol CUP$DFAParser$result9 = new Symbol(5, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 5)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result9;
            case 9:
                this.parser.report_error("Unable to parse the STATE block");
                Symbol CUP$DFAParser$result10 = new Symbol(5, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result10;
            case 10:
                Symbol CUP$DFAParser$result11 = new Symbol(6, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, "1");
                return CUP$DFAParser$result11;
            case 11:
                Symbol CUP$DFAParser$result12 = new Symbol(6, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, "0");
                return CUP$DFAParser$result12;
            case 12:
                Symbol CUP$DFAParser$result13 = new Symbol(7, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 1)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result13;
            case 13:
                Symbol CUP$DFAParser$result14 = new Symbol(7, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result14;
            case 14:
                int i9 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left;
                int i10 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right;
                Integer stateIndex3 = (Integer) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).value;
                this.stateBranches.add(stateIndex3.toString());
                Symbol CUP$DFAParser$result15 = new Symbol(8, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result15;
            case 15:
                int i11 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left;
                int i12 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right;
                String ch = (String) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).value;
                this.stateBranches.add(ch);
                this.stateBranches.add("");
                Symbol CUP$DFAParser$result16 = new Symbol(9, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result16;
            case 16:
                int i13 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).left;
                int i14 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).right;
                String ch1 = (String) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).value;
                int i15 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).left;
                int i16 = ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right;
                String ch2 = (String) ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).value;
                this.stateBranches.add(ch1);
                this.stateBranches.add(ch2);
                Symbol CUP$DFAParser$result17 = new Symbol(9, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 2)).left, ((Symbol) CUP$DFAParser$stack.elementAt(CUP$DFAParser$top - 0)).right, (Object) null);
                return CUP$DFAParser$result17;
            default:
                throw new Exception("Invalid action number found in internal parse table");
        }
    }
}
