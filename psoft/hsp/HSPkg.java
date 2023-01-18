package psoft.hsp;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsp/HSPkg.class */
public class HSPkg implements TemplateHashModel {
    private Package pkg;
    private int pkgFilesReportId = -1;

    public HSPkg(Package p) {
        this.pkg = p;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.pkg.getId());
        }
        if ("name".equals(key)) {
            return new TemplateString(this.pkg.getName());
        }
        if ("version".equals(key)) {
            return new TemplateString(this.pkg.getVersion());
        }
        if ("build".equals(key)) {
            return new TemplateString(this.pkg.getBuild());
        }
        if ("shortDescr".equals(key)) {
            return new TemplateString(this.pkg.getDescrShort());
        }
        if ("description".equals(key)) {
            return new TemplateString(this.pkg.getDescription());
        }
        if ("vendor".equals(key)) {
            return new TemplateString(this.pkg.getVendorInfo());
        }
        if ("files".equals(key)) {
            try {
                return getPkgFiles();
            } catch (Exception ex) {
                Session.getLog().error("Error getting HSPkgFilesReport ", ex);
                return null;
            }
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public static List getPackages() throws Exception {
        ArrayList result = new ArrayList();
        for (Package r3 : Package.getPackages()) {
            result.add(new HSPkg(r3));
        }
        return result;
    }

    public TemplateModel getPkgFiles() throws Exception {
        if (this.pkgFilesReportId != -1) {
            try {
                Session.getLog().debug("Getting HSPkgFilesReport by " + this.pkgFilesReportId + " id");
                return AdvReport.getReport(this.pkgFilesReportId);
            } catch (HSUserException e) {
                return initFilesReport();
            }
        }
        return initFilesReport();
    }

    private AdvReport initFilesReport() throws Exception {
        Session.getLog().debug("Initializing HSPkgFilesReport...");
        List args = new ArrayList();
        args.add(this.pkg);
        AdvReport rep = AdvReport.newInstance("pkg_files");
        rep.init(args);
        this.pkgFilesReportId = rep.getId();
        return rep;
    }
}
