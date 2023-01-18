package psoft.spellcheker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/spellcheker/Result.class */
public class Result implements TemplateHashModel {
    private int offset;
    private int type;
    private List suggestions;
    private String original;

    /* renamed from: OK */
    public static final int f247OK = 1;
    public static final int ERROR = 2;
    public static final int NONE = 3;
    public static final int SUGGESTION = 4;

    public Result(String line) {
        if (line == null || line.length() <= 0) {
            processError(line);
        } else if (line.charAt(0) == '*') {
            processOk(line);
        } else if (line.charAt(0) == '&') {
            processSuggestion(line);
        } else if (line.charAt(0) == '#') {
            processNone(line);
        } else {
            processError(line);
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("type".equals(key)) {
            return new TemplateString(this.type);
        }
        if ("original".equals(key)) {
            return new TemplateString(this.original);
        }
        if ("suggestions".equals(key)) {
            return new TemplateList(this.suggestions);
        }
        if ("offset".equals(key)) {
            return new TemplateString(this.offset);
        }
        return null;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getType() {
        return this.type;
    }

    public List getSuggestions() {
        return this.suggestions;
    }

    public String getOriginalWord() {
        return this.original;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[type:");
        buf.append(this.type);
        buf.append(",originalWord:");
        buf.append(this.original);
        buf.append(",offset:");
        buf.append(this.offset);
        buf.append(",suggestions:");
        buf.append(this.suggestions);
        return buf.toString();
    }

    private void processError(String line) {
        this.offset = 0;
        this.type = 2;
        this.suggestions = new ArrayList();
        this.original = "";
    }

    private void processOk(String line) {
        this.offset = 0;
        this.type = 1;
        this.suggestions = new ArrayList();
        this.original = "";
    }

    private void processNone(String line) {
        this.type = 3;
        this.suggestions = new ArrayList();
        StringTokenizer st = new StringTokenizer(line);
        st.nextToken();
        this.original = st.nextToken();
        this.offset = Integer.parseInt(st.nextToken());
    }

    private void processSuggestion(String line) {
        this.type = 4;
        StringTokenizer st = new StringTokenizer(line);
        st.nextToken();
        this.original = st.nextToken();
        int count = Integer.parseInt(st.nextToken().trim());
        if (count > 10) {
            count = 10;
        }
        this.suggestions = new ArrayList(count);
        this.offset = Integer.parseInt(st.nextToken(":").trim());
        StringTokenizer st2 = new StringTokenizer(st.nextToken(":"), ",");
        for (int i = 0; st2.hasMoreTokens() && i < count; i++) {
            this.suggestions.add(st2.nextToken().trim());
        }
    }
}
