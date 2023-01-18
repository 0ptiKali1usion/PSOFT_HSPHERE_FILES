package psoft.hsphere.plan;

import freemarker.template.TemplateModel;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.UnknownResellerException;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;

/* loaded from: hsphere.zip:psoft/hsphere/plan/DiscountedPlan.class */
public class DiscountedPlan extends Plan {

    /* renamed from: p */
    Plan f104p;
    Discount discount;

    public DiscountedPlan(Plan p, Discount d) {
        this.f104p = p;
        this.discount = d;
    }

    @Override // psoft.hsphere.Plan
    public int getId() {
        return this.f104p.getId();
    }

    @Override // psoft.hsphere.Plan
    public String getDescription() {
        return this.f104p.getDescription();
    }

    @Override // psoft.hsphere.Plan, java.lang.Comparable
    public int compareTo(Object o) {
        return this.f104p.compareTo(o);
    }

    @Override // psoft.hsphere.Plan
    public boolean isAccessible(String id) {
        return this.f104p.isAccessible(id);
    }

    @Override // psoft.hsphere.Plan
    public boolean isAccessible(int id) {
        return this.f104p.isAccessible(id);
    }

    @Override // psoft.hsphere.Plan
    public ResourceType getResourceType(int id) {
        return this.f104p.getResourceType(id);
    }

    @Override // psoft.hsphere.Plan
    public ResourceType getResourceType(String id) {
        return this.f104p.getResourceType(id);
    }

    @Override // psoft.hsphere.Plan
    public int getBilling() {
        return this.f104p.getBilling();
    }

    @Override // psoft.hsphere.Plan
    public int getCInfo() {
        return this.f104p.getCInfo();
    }

    @Override // psoft.hsphere.Plan
    public double getCredit() {
        return this.f104p.getCredit();
    }

    @Override // psoft.hsphere.Plan
    public double getTrialCredit() {
        return this.f104p.getTrialCredit();
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_accessChange(String str) throws Exception {
        return this.f104p.FM_accessChange(str);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_accessCheck(String id) {
        return this.f104p.FM_accessCheck(id);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_getResourceType(String typeName) throws Exception {
        return this.f104p.FM_getResourceType(typeName);
    }

    @Override // psoft.hsphere.Plan
    public ResourceType isResourceAvailable(String typeName) {
        return this.f104p.isResourceAvailable(typeName);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_isResourceAvailable(String typeName) throws Exception {
        return this.f104p.FM_isResourceAvailable(typeName);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_areResourcesAvailable(String typePattern) throws Exception {
        return this.f104p.FM_areResourcesAvailable(typePattern);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_enable() throws Exception {
        return this.f104p.FM_enable();
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_disable() throws Exception {
        return this.f104p.FM_disable();
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_enableResource(String typeName) throws Exception {
        return this.f104p.FM_enableResource(typeName);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_disableResource(String typeName) throws Exception {
        return this.f104p.FM_disableResource(typeName);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_copy() throws Exception {
        return this.f104p.FM_copy();
    }

    public void addResourceType(String typeId, String className, int disabled) {
        this.f104p.addResourceType(typeId, className, disabled, 1);
    }

    @Override // psoft.hsphere.Plan
    public String getValue(int type, String key) {
        String value = this.discount.getValue(type, key);
        return value != null ? value : this.f104p.getValue(type, key);
    }

    @Override // psoft.hsphere.Plan
    public String getValue(String type, String key) {
        String value = this.discount.getValue(type, key);
        return value != null ? value : this.f104p.getValue(type, key);
    }

    @Override // psoft.hsphere.Plan
    public List getInitResources(int type, String mod) throws Exception {
        return this.f104p.getInitResources(type, mod);
    }

    @Override // psoft.hsphere.Plan
    public TemplateList FM_getInitResources(int type, String mod) throws Exception {
        return this.f104p.FM_getInitResources(type, mod);
    }

    @Override // psoft.hsphere.Plan
    public List getDefaultInitValues(Resource r, int type, String mod) throws Exception {
        return this.f104p.getDefaultInitValues(r, type, mod);
    }

    @Override // psoft.hsphere.Plan
    public Class getResourceClassByName(String type) throws Exception {
        return this.f104p.getResourceClassByName(type);
    }

    @Override // psoft.hsphere.Plan
    public Class getResourceClass(int type) throws Exception {
        return this.f104p.getResourceClass(type);
    }

    @Override // psoft.hsphere.Plan
    public Class getResourceClass(String type) throws Exception {
        return this.f104p.getResourceClass(type);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel exec(List l) {
        return this.f104p.exec(l);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_setDescription(String desc) throws Exception {
        return this.f104p.FM_setDescription(desc);
    }

    @Override // psoft.hsphere.Plan
    public Date getNextPaymentDate(Date start, int periodId) {
        return this.f104p.getNextPaymentDate(start, periodId);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_putValue(String key, String value) throws Exception {
        return this.f104p.FM_putValue(key, value);
    }

    @Override // psoft.hsphere.Plan
    public void setGroup(int groupId) {
        this.f104p.setGroup(groupId);
    }

    @Override // psoft.hsphere.Plan
    public void setGroupName(String groupName) {
        this.f104p.setGroupName(groupName);
    }

    @Override // psoft.hsphere.Plan
    public int getGroup() {
        return this.f104p.getGroup();
    }

    @Override // psoft.hsphere.Plan
    public String getGroupName() {
        return this.f104p.getGroupName();
    }

    @Override // psoft.hsphere.Plan
    public boolean isEmpty() {
        return this.f104p.isEmpty();
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel get(String key) {
        if (key.equals("discount")) {
            return new TemplateMap(this.discount.getDiscounts());
        }
        return this.f104p.get(key);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_setCInfo(int cinfo) throws Exception {
        return this.f104p.FM_setCInfo(cinfo);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_setBInfo(int binfo) throws Exception {
        return this.f104p.FM_setBInfo(binfo);
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_getAccessiblePlanList(int id) throws UnknownResellerException {
        return this.f104p.FM_getAccessiblePlanList(id);
    }

    @Override // psoft.hsphere.Plan
    public long getResellerId() {
        return this.f104p.getResellerId();
    }

    @Override // psoft.hsphere.Plan
    public void delete() throws Exception {
        this.f104p.delete();
    }

    @Override // psoft.hsphere.Plan
    public int qntySignupedUsers() throws Exception {
        return this.f104p.qntySignupedUsers();
    }

    @Override // psoft.hsphere.Plan
    public boolean isDeleted() {
        return this.f104p.isDeleted();
    }

    @Override // psoft.hsphere.Plan
    public HashMap getResources() {
        return this.f104p.getResources();
    }

    @Override // psoft.hsphere.Plan
    public String getPeriodInWords(int pId) {
        return this.f104p.getPeriodInWords(pId);
    }

    @Override // psoft.hsphere.Plan
    public boolean isTotalyEqual(Plan p) {
        return p.isTotalyEqual(p);
    }

    @Override // psoft.hsphere.Plan
    public boolean isDemoPlan() throws Exception {
        return this.f104p.isDemoPlan();
    }

    @Override // psoft.hsphere.Plan
    public TemplateModel FM_isDemoPlan() throws Exception {
        return this.f104p.FM_isDemoPlan();
    }
}
