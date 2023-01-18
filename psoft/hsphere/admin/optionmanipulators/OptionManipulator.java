package psoft.hsphere.admin.optionmanipulators;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/* loaded from: hsphere.zip:psoft/hsphere/admin/optionmanipulators/OptionManipulator.class */
public interface OptionManipulator extends TemplateHashModel {
    Object getOption(long j);

    void setOption(long j, Object obj);

    TemplateModel FM_getOption(long j);

    TemplateModel FM_setOption(long j, String str);

    TemplateModel FM_useXML();
}
