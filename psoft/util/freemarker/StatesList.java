package psoft.util.freemarker;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;

/* loaded from: hsphere.zip:psoft/util/freemarker/StatesList.class */
public class StatesList implements TemplateListModel, TemplateHashModel {
    protected int current = 0;
    public static final StatesList list = new StatesList();
    public static final String[][] statesArray = {new String[]{"AL", "Alabama"}, new String[]{"AK", "Alaska"}, new String[]{"AR", "Arkansas"}, new String[]{"AZ", "Arizona"}, new String[]{"CA", "California"}, new String[]{"CO", "Colorado"}, new String[]{"CT", "Connecticut"}, new String[]{"DE", "Delaware"}, new String[]{"FL", "Florida"}, new String[]{"GA", "Georgia"}, new String[]{"HI", "Hawaii"}, new String[]{"IA", "Iowa"}, new String[]{"ID", "Idaho"}, new String[]{"IL", "Illinois"}, new String[]{"IN", "Indiana"}, new String[]{"KS", "Kansas"}, new String[]{"KY", "Kentucky"}, new String[]{"LA", "Louisiana"}, new String[]{"MA", "Massachusetts"}, new String[]{"MD", "Maryland"}, new String[]{"ME", "Maine"}, new String[]{"MI", "Michigan"}, new String[]{"MN", "Minnesota"}, new String[]{"MS", "Mississippi"}, new String[]{"MO", "Missouri"}, new String[]{"MT", "Montana"}, new String[]{"NC", "North Carolina"}, new String[]{"ND", "North Dakota"}, new String[]{"NE", "Nebraska"}, new String[]{"NH", "New Hampshire"}, new String[]{"NJ", "New Jersey"}, new String[]{"NV", "Nevada"}, new String[]{"NM", "New Mexico"}, new String[]{"NY", "New York"}, new String[]{"OH", "Ohio"}, new String[]{"OK", "Oklahoma"}, new String[]{"OR", "Oregon"}, new String[]{"PA", "Pennsylvania"}, new String[]{"RI", "Rhode Island"}, new String[]{"SC", "South Carolina"}, new String[]{"SD", "South Dakota"}, new String[]{"TN", "Tennessee"}, new String[]{"TX", "Texas"}, new String[]{"UT", "Utah"}, new String[]{"VA", "Virginia"}, new String[]{"VT", "Vermont"}, new String[]{"WA", "Washington"}, new String[]{"WI", "Wisconsin"}, new String[]{"WV", "West Virginia"}, new String[]{"WY", "Wyoming"}, new String[]{"DC", "Washington DC"}};
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
        TemplateModel[] templateModelArr = states;
        int i = this.current;
        this.current = i + 1;
        return templateModelArr[i];
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
