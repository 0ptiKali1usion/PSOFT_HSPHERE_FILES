package psoft.hsphere.resource.admin.params;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/params/TextParam.class */
public class TextParam extends BaseParam {
    public TextParam(BaseParam param) {
        super(param);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public void setValue(String value) {
        if (value == null) {
            value = "";
        }
        super.setValue(value);
    }

    @Override // psoft.hsphere.resource.admin.params.BaseParam
    public BaseParam copy() {
        return new TextParam(this);
    }
}
