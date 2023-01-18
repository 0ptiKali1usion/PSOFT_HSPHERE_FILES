package psoft.hsphere.report;

import java.util.List;
import java.util.Vector;

/* loaded from: hsphere.zip:psoft/hsphere/report/GenericReport.class */
public class GenericReport extends AdvReport {
    public GenericReport(Vector v, DataComparator dc) {
        init(new DataContainer(v, dc));
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        throw new Exception("Not Implemented");
    }
}
