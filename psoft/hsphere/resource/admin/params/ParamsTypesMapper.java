package psoft.hsphere.resource.admin.params;

import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/ParamsTypesMapper.class */
public class ParamsTypesMapper {
    private static Hashtable classes = new Hashtable();
    private static Hashtable templates = new Hashtable();

    static {
        classes.put("edit", EditParam.class);
        templates.put("edit", "pserver_edit_incl.html");
        classes.put("check", CheckParam.class);
        templates.put("check", "pserver_check_incl.html");
        classes.put("radio", RadioList.class);
        templates.put("radio", "pserver_radio_incl.html");
        classes.put("list", ListParam.class);
        templates.put("list", "pserver_list_incl.html");
        classes.put("slist", SListParam.class);
        templates.put("slist", "pserver_list_incl.html");
        classes.put("ipparam", IPParam.class);
        templates.put("ipparam", "pserver_ipparam_incl.html");
        classes.put("iplist", IPList.class);
        templates.put("iplist", "pserver_iplist_incl.html");
        classes.put("label", LabelParam.class);
        templates.put("label", "pserver_label_incl.html");
        classes.put("unixpathparam", UnixPathParam.class);
        templates.put("unixpathparam", "pserver_unixpathparam_incl.html");
        classes.put("winpathparam", WinPathParam.class);
        templates.put("winpathparam", "pserver_winpathparam_incl.html");
        classes.put("labelslist", LabelsListParam.class);
        templates.put("labelslist", "pserver_labelslist_incl.html");
        classes.put("checklist", CheckList.class);
        templates.put("checklist", "pserver_checklist_incl.html");
        classes.put("int", IntParam.class);
        templates.put("int", "pserver_int_incl.html");
        classes.put("emailslist", EmailsList.class);
        templates.put("emailslist", "pserver_emailslist_incl.html");
        classes.put("domainslist", DomainsList.class);
        templates.put("domainslist", "pserver_domainslist_incl.html");
        classes.put("patternslist", PatternsList.class);
        templates.put("patternslist", "pserver_list_incl.html");
        classes.put("radiogroup", RadioGroup.class);
        templates.put("radiogroup", "pserver_radiogroup_incl.html");
        classes.put("text", TextParam.class);
        templates.put("text", "pserver_text_incl.html");
        classes.put("select", SelectParam.class);
        templates.put("select", "pserver_select_incl.html");
    }

    public static Class getClassForType(String type) {
        return (Class) classes.get(type);
    }

    public static String getTemplateForType(String type) {
        return (String) templates.get(type);
    }
}
