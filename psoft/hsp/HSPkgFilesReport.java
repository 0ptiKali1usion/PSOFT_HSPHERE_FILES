package psoft.hsp;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsp/HSPkgFilesReport.class */
public class HSPkgFilesReport extends AdvReport {
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Inside HSPkgFilesReport::init");
        Iterator it = args.iterator();
        Package pkg = (Package) it.next();
        List data = new ArrayList();
        Session.getLog().debug("Found " + pkg.getFiles().size() + " files for package " + pkg.getName());
        for (String str : pkg.getFiles()) {
            PackageFile pf = PackageFile.getFile(str);
            Hashtable h = new Hashtable();
            h.put("name", pf.getName());
            h.put("file_type", new Integer(pf.getType()));
            h.put("installed_by", pf.getInstalled());
            h.put("used_by", new TemplateList(pf.getUsedByPackages()));
            h.put("target_servers", new TemplateList(pf.getPkfInfo().getServerGroup()));
            h.put("target_path", pf.getPath());
            Session.getLog().debug(pf.getName() + " has been added. The file is used by the package(s): " + pf.getUsedByPackages() + ".");
            data.add(h);
        }
        init(new DataContainer(data));
    }
}
