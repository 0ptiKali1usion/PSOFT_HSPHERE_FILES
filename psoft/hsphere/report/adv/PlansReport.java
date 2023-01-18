package psoft.hsphere.report.adv;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/PlansReport.class */
public class PlansReport extends AdvReport {
    protected boolean isShowDeleted = false;

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        this.isShowDeleted = !isEmpty((String) args.iterator().next());
        Vector data = new Vector();
        TreeSet planList = getGroupedPlanTree(new ArrayList(Plan.getPlanList()));
        Iterator i = planList.iterator();
        while (i.hasNext()) {
            new Hashtable();
            Hashtable hash = (Hashtable) i.next();
            data.add(hash);
        }
        init(new DataContainer(data));
    }

    /* JADX WARN: Removed duplicated region for block: B:46:0x0079  */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0081  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public java.util.TreeSet getGroupedPlanTree(java.util.List r7) {
        /*
            r6 = this;
            java.util.Hashtable r0 = new java.util.Hashtable
            r1 = r0
            r1.<init>()
            r8 = r0
            r0 = r7
            java.util.Iterator r0 = r0.iterator()
            r11 = r0
        L10:
            r0 = r11
            boolean r0 = r0.hasNext()
            if (r0 == 0) goto Lcb
            r0 = r11
            java.lang.Object r0 = r0.next()
            psoft.hsphere.Plan r0 = (psoft.hsphere.Plan) r0
            r10 = r0
            r0 = r6
            boolean r0 = r0.isShowDeleted
            if (r0 != 0) goto L3d
            r0 = r10
            boolean r0 = r0.isDeleted()
            if (r0 == 0) goto L4d
            java.lang.String r0 = "plan.isDeleted()"
            m22d(r0)
            goto L10
        L3d:
            r0 = r10
            boolean r0 = r0.isDeleted()
            if (r0 != 0) goto L4d
            java.lang.String r0 = "!plan.isDeleted()"
            m22d(r0)
            goto L10
        L4d:
            java.util.Hashtable r0 = new java.util.Hashtable
            r1 = r0
            r1.<init>()
            r9 = r0
            r0 = r9
            java.lang.String r1 = "plan"
            r2 = r10
            java.lang.Object r0 = r0.put(r1, r2)
            r0 = r9
            java.lang.String r1 = "plan_id"
            java.lang.Integer r2 = new java.lang.Integer
            r3 = r2
            r4 = r10
            int r4 = r4.getId()
            r3.<init>(r4)
            java.lang.Object r0 = r0.put(r1, r2)
            r0 = r10
            java.lang.String r0 = r0.getGroupName()
            if (r0 == 0) goto L81
            r0 = r10
            java.lang.String r0 = r0.getGroupName()
            goto L83
        L81:
            java.lang.String r0 = ""
        L83:
            r12 = r0
            r0 = r9
            java.lang.String r1 = "group"
            r2 = r12
            java.lang.Object r0 = r0.put(r1, r2)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r1 = r0
            r1.<init>()
            r1 = r10
            int r1 = r1.getGroup()
            java.lang.StringBuilder r0 = r0.append(r1)
            r1 = r10
            java.lang.String r1 = r1.getDescription()
            java.lang.StringBuilder r0 = r0.append(r1)
            r1 = r10
            int r1 = r1.getId()
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r0 = r0.toString()
            r13 = r0
            r0 = r9
            java.lang.String r1 = "description"
            r2 = r13
            java.lang.Object r0 = r0.put(r1, r2)
            r0 = r8
            r1 = r13
            r2 = r9
            java.lang.Object r0 = r0.put(r1, r2)
            r0 = r13
            m22d(r0)
            goto L10
        Lcb:
            java.util.TreeSet r0 = new java.util.TreeSet
            r1 = r0
            psoft.hsphere.report.adv.PlansReport$1 r2 = new psoft.hsphere.report.adv.PlansReport$1
            r3 = r2
            r4 = r6
            r3.<init>()
            r1.<init>(r2)
            r11 = r0
            r0 = r11
            r1 = r8
            java.util.Collection r1 = r1.values()
            boolean r0 = r0.addAll(r1)
            r0 = r11
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.report.adv.PlansReport.getGroupedPlanTree(java.util.List):java.util.TreeSet");
    }

    /* renamed from: d */
    public static void m23d(int n) {
        Session.getLog().debug("LINE " + n);
    }

    /* renamed from: d */
    public static void m22d(String s) {
        Session.getLog().debug(s);
    }

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
