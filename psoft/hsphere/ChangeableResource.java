package psoft.hsphere;

import java.util.Collection;
import java.util.Date;

/* loaded from: hsphere.zip:psoft/hsphere/ChangeableResource.class */
public interface ChangeableResource {
    double changeResource(Collection collection) throws Exception;

    String getRecurrentChangeDescripton(InitToken initToken, double d) throws Exception;

    String getRecurrentRefundDescription(Date date, Date date2, double d);
}
