package psoft.hsphere.report;

import java.util.List;
import java.util.Vector;
import psoft.hsphere.report.adv.MailReport;

/* loaded from: hsphere.zip:psoft/hsphere/report/GenericMailReport.class */
public class GenericMailReport extends MailReport {
    public GenericMailReport(long mailObjectId, Vector v, DataComparator dc) {
        init(mailObjectId, new DataContainer(v, dc));
    }

    @Override // psoft.hsphere.report.adv.MailReport, psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        throw new Exception("Not Implemented");
    }
}
