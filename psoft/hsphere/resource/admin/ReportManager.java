package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.TemplateCacheReport;
import psoft.hsphere.report.TemplateReport;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ReportManager.class */
public class ReportManager extends Resource {
    public ReportManager(int type, Collection init) throws Exception {
        super(type, init);
    }

    public ReportManager(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("getReport") ? TemplateReport.report : key.equals("getCachedReport") ? TemplateCacheReport.report : super.get(key);
    }

    public TemplateModel FM_getAdvReport(String name) {
        return AdvReport.newInstance(name);
    }

    public TemplateModel FM_getAdvReportById(int id) throws Exception {
        return AdvReport.getReport(id);
    }

    public TemplateModel FM_massMail(int id, String from, String subject, String message, String tmplt, String useSettings, int oepa, String contentType) throws Exception {
        AdvReport report = AdvReport.getReport(id);
        return report.massMail(from, subject, message, tmplt, useSettings, oepa == 1, contentType);
    }
}
