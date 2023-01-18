package psoft.util.freemarker;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;

/* loaded from: hsphere.zip:psoft/util/freemarker/StatesCanadaList.class */
public class StatesCanadaList implements TemplateListModel, TemplateHashModel {
    protected int current = 0;
    public static final StatesCanadaList list = new StatesCanadaList();
    public static final String[][] statesArray = {new String[]{"AB", "Alberta"}, new String[]{"BC", "British Columbia"}, new String[]{"MB", "Manitoba"}, new String[]{"NB", "New Brunswick"}, new String[]{"NF", "Newfoundland"}, new String[]{"NT", "Northwest Territories"}, new String[]{"NS", "Nova Scotia"}, new String[]{"ON", "Ontario"}, new String[]{"PE", "Prince Edward Island"}, new String[]{"QC", "Quebec"}, new String[]{"SK", "Saskatchewan"}, new String[]{"YT", "Yukon"}};
    public static final SimpleHash[] states = new SimpleHash[statesArray.length];

    /* JADX WARN: Type inference failed for: r0v2, types: [java.lang.String[], java.lang.String[][]] */
    static {
        for (int i = 0; i < statesArray.length; i++) {
            SimpleHash hash = new SimpleHash();
            hash.put("name", statesArray[i][1]);
            hash.put("tag", statesArray[i][0]);
            states[i] = hash;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean hasNext() {
        return this.current < states.length;
    }

    public boolean isRewound() {
        return this.current == 0;
    }

    public TemplateModel get(int index) {
        return states[index];
    }

    public TemplateModel next() {
        TemplateModel[] templateModelArr = states;
        int i = this.current;
        this.current = i + 1;
        return templateModelArr[i];
    }

    public void rewind() {
        this.current = 0;
    }

    public TemplateModel get(String key) {
        if (key == null) {
            return null;
        }
        for (int i = 0; i < statesArray.length; i++) {
            if (key.equals(statesArray[i][0])) {
                return new TemplateString(statesArray[i][1]);
            }
        }
        return null;
    }
}
