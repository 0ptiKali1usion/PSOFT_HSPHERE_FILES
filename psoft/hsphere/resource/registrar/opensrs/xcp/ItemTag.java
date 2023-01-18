package psoft.hsphere.resource.registrar.opensrs.xcp;

import psoft.hsphere.resource.epayment.MerchantGatewayManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/ItemTag.class */
public class ItemTag extends Tag {
    public ItemTag(String key, String value) {
        super("item", value);
        add(new Param(MerchantGatewayManager.MG_KEY_PREFIX, key));
    }

    public ItemTag(String key, Tag value) {
        super("item");
        add(new Param(MerchantGatewayManager.MG_KEY_PREFIX, key));
        add(value);
    }
}
