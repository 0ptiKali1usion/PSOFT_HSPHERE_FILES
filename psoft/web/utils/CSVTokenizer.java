package psoft.web.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: hsphere.zip:psoft/web/utils/CSVTokenizer.class */
public class CSVTokenizer {
    List values;
    Iterator iter;

    public CSVTokenizer(String src) {
        this(src, ',');
    }

    public CSVTokenizer(String src, char delimeter) {
        this.values = null;
        this.iter = null;
        this.values = new ArrayList();
        if (src == null) {
            this.iter = this.values.iterator();
            return;
        }
        boolean wasQuote = false;
        StringBuffer value = new StringBuffer("");
        int i = 0;
        while (i < src.length()) {
            if (src.charAt(i) == '\"') {
                try {
                    if (src.charAt(i + 1) == '\"') {
                        value.append('\"');
                        i++;
                    }
                } catch (IndexOutOfBoundsException e) {
                }
                if (wasQuote) {
                    wasQuote = false;
                } else {
                    wasQuote = true;
                }
            } else if (src.charAt(i) == delimeter && !wasQuote) {
                this.values.add(value.toString());
                value = new StringBuffer("");
            } else {
                value.append(src.charAt(i));
            }
            i++;
        }
        if (value.length() > 0) {
            this.values.add(value.toString());
        }
        this.iter = this.values.iterator();
    }

    public List values() {
        return this.values;
    }

    public String nextToken() {
        return (String) this.iter.next();
    }

    public boolean hasNextToken() {
        return this.iter.hasNext();
    }

    public static void main(String[] argv) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(argv[0]));
        while (in.ready()) {
            CSVTokenizer csv = new CSVTokenizer(in.readLine(), '\t');
            while (csv.hasNextToken()) {
                System.out.print(csv.nextToken() + "\t");
            }
            System.out.println("");
        }
    }
}
