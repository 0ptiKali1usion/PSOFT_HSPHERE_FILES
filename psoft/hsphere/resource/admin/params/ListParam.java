package psoft.hsphere.resource.admin.params;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/ListParam.class */
public class ListParam extends BaseParamsList {
    protected String oldEditedValue;

    public ListParam(BaseParam param) {
        super(param, ",");
        this.oldEditedValue = null;
    }

    public ListParam(BaseParam param, String delimiter) {
        super(param, delimiter);
        this.oldEditedValue = null;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList
    protected void parseParamsList() {
        parseParamsList(this.delim);
    }

    protected void parseParamsList(String delim) {
        this.params = new ArrayList();
        StringBuffer buff = new StringBuffer();
        StringTokenizer st = new StringTokenizer(this.currParamValue, delim);
        while (st.hasMoreTokens()) {
            String value = st.nextToken().trim();
            if (!value.equals("")) {
                this.params.add(value);
                if (buff.length() == 0) {
                    buff.append(value);
                } else {
                    buff.append(delim + value);
                }
            }
        }
        this.currParamValue = buff.toString();
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValueFlags() {
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("param_list")) {
            return new TemplateList(filter(this.params));
        }
        if (key.equals("param_spaces")) {
            return new TemplateString(isCanSpaces());
        }
        return super.get(key);
    }

    private ArrayList filter(ArrayList list) {
        for (int i = 0; i < list.size(); i++) {
            String tmp = list.get(i).toString();
            list.set(i, replacePattern(tmp, "\"", "&quot;"));
        }
        return list;
    }

    protected boolean isValid(String value) {
        return true;
    }

    public void saveValue(String oldValue, String newValue) {
        try {
            if (!isValueExistInList(newValue) && isValid(newValue)) {
                String allValue = replacePattern(this.currParamValue, oldValue, newValue);
                setFullValue(allValue);
            }
        } catch (Exception e) {
            Session.getLog().info(e);
        }
    }

    public void setFullValue(String value) {
        super.setValue(value);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        try {
            if (!isValueExistInList(value) && isValid(value)) {
                super.setValue(this.currParamValue + this.delim + value);
            }
        } catch (Exception e) {
            Session.getLog().info(e);
        }
    }

    public void removeValue(String value) {
        String removed = removePattern(this.currParamValue, value);
        super.setValue(removed);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public String getValue() {
        return this.currParamValue;
    }

    protected boolean isCanSpaces() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        try {
            List l2 = HTMLEncoder.decode(l);
            if (l2.isEmpty()) {
                return null;
            }
            String key = (String) l2.get(0);
            String value = (String) l2.get(1);
            if (value == null && key.indexOf("_del") < 0) {
                Session.addMessage("Can't set empty value for - " + this.currParamName);
                setChanged(false);
                return null;
            }
            if (value != null) {
                try {
                    value = URLDecoder.decode(value);
                } catch (Exception e) {
                }
                if (value.indexOf(" ") > 0 && value.indexOf(" ") < value.length() - 1 && !isCanSpaces()) {
                    Session.addMessage("Value for - " + this.currParamName + " can't contain spaces");
                    setChanged(false);
                    return null;
                }
            }
            if (key.equalsIgnoreCase(this.currParamName + "_del")) {
                removeValue(value);
                return null;
            } else if (key.equalsIgnoreCase(this.currParamName + "_save")) {
                saveValue(this.oldEditedValue, value);
                return null;
            } else if (key.equalsIgnoreCase(this.currParamName + "_old")) {
                this.oldEditedValue = value;
                return null;
            } else if (key.equalsIgnoreCase(this.currParamName + "_add")) {
                setValue(value);
                return null;
            } else {
                return null;
            }
        } catch (Exception e2) {
            Session.getLog().error("Set physical server params error", e2);
            return new TemplateErrorResult(e2);
        }
    }

    public TemplateModel FM_getParamValue() {
        return new TemplateString(this.currParamValue);
    }

    public String replacePattern(String str, String oldValue, String newValue) {
        if (str == null) {
            return str;
        }
        if (oldValue.equals(this.delim)) {
            return str;
        }
        int s = 0;
        StringBuffer result = new StringBuffer();
        while (true) {
            int e = getValuePositionInList(str, oldValue, s, this.delimChar);
            if (e >= 0) {
                result.append(str.substring(s, e));
                s = e + oldValue.length();
                result.append(newValue);
            } else {
                result.append(str.substring(s, str.length()));
                return result.toString();
            }
        }
    }

    public String removeStr(String str, String pattern) {
        int s = 0;
        StringBuffer result = new StringBuffer();
        String pattern2 = pattern.trim();
        String str2 = str.trim();
        int e = getValuePositionInList(str2, pattern2, 0, this.delimChar);
        if (e >= 0) {
            int pos = e;
            if (e > 0) {
                if (str2.charAt(e - 1) == this.delimChar) {
                    pos = e - 1;
                }
                result.append(str2.substring(0, pos));
                s = e + pattern2.length();
            } else if (e == 0) {
                if (pattern2.length() < str2.length()) {
                    if (str2.charAt(pattern2.length()) == this.delimChar) {
                        s = pattern2.length() + 1;
                    }
                } else {
                    s = pattern2.length();
                }
            }
            result.append(str2.substring(s, str2.length()));
            return result.toString().trim();
        }
        return null;
    }

    public String removePattern(String str, String pattern) {
        String result = removeStr(str, pattern);
        if (result == null) {
            return str;
        }
        return result;
    }

    protected void addParam(String param) {
        this.params.add(param);
    }

    protected boolean isValueExistInList(String value) {
        if (getValuePositionInList(this.currParamValue, value) == -1) {
            return false;
        }
        Session.addMessage("Value - " + value + " exist in list - " + this.currParamName);
        return true;
    }

    protected int getValuePositionInList(String list, String value) {
        return getValuePositionInList(list, value, 0, this.delimChar);
    }

    protected int getValuePositionInList(String list, String value, int fromIndex, char delim) {
        if (value.trim().equals("")) {
            return -1;
        }
        int first = list.indexOf(value.trim(), 0);
        if (first < fromIndex) {
            return -1;
        }
        while (first >= 0) {
            boolean isFistSpace = false;
            boolean isLastSpace = false;
            if (first > 0) {
                if (list.charAt(first - 1) == delim) {
                    isFistSpace = true;
                }
            } else {
                isFistSpace = true;
            }
            int last = (first + value.length()) - 1;
            if (last < list.length() - 1) {
                if (list.charAt(last + 1) == delim) {
                    isLastSpace = true;
                }
            } else {
                isLastSpace = true;
            }
            if (isFistSpace && isLastSpace) {
                for (int i = 0; i < value.length(); i++) {
                    if (list.charAt(first + i) != value.charAt(i)) {
                        return -1;
                    }
                }
                return first;
            }
            first = list.indexOf(value.trim(), last + 1);
        }
        return -1;
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new ListParam(this, this.delim);
    }
}
