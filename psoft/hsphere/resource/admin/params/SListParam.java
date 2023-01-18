package psoft.hsphere.resource.admin.params;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/SListParam.class */
public class SListParam extends ListParam {
    public SListParam(BaseParam param) {
        super(param, ",");
    }

    public SListParam(BaseParam param, String delimiter) {
        super(param, delimiter);
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam
    protected boolean isCanSpaces() {
        return true;
    }

    @Override // psoft.hsphere.resource.admin.params.ListParam, psoft.hsphere.resource.admin.params.BaseParamsList, psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new SListParam(this, this.delim);
    }
}
