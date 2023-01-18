package psoft.hsphere.migrator;

import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/ResellerPlanHolder.class */
public class ResellerPlanHolder {
    private String resellerName;
    private boolean isUsingPlansIDs = false;
    private ArrayList plans = new ArrayList();
    private String resumePlanStr = "";

    public ResellerPlanHolder(String reseller) {
        this.resellerName = reseller;
    }

    public void setUsingPlansIDs(boolean isUsingPlansIDs) {
        this.isUsingPlansIDs = isUsingPlansIDs;
    }

    public boolean isUsingPlansIDs() {
        return this.isUsingPlansIDs;
    }

    public String getResellerName() {
        return this.resellerName;
    }

    public void addPlan(String plan) {
        this.plans.add(plan);
    }

    public ArrayList getPlans() {
        return this.plans;
    }

    public void setResumePlan(String planStr) {
        this.resumePlanStr = planStr;
    }

    public String getResumePlan() {
        return this.resumePlanStr;
    }

    public void printContents() {
        System.out.println("\n========= " + this.resellerName + " ===========");
        if (this.plans.size() > 0) {
            Iterator iter = this.plans.iterator();
            while (iter.hasNext()) {
                System.out.println(iter.next());
            }
        }
        System.out.println("========== End " + this.resellerName + " ==========\n");
    }
}
