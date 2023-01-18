package psoft.hsphere.axis;

import java.util.List;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPEnvelope;

/* loaded from: hsphere.zip:psoft/hsphere/axis/WinResourceService.class */
public class WinResourceService extends WinService {
    protected List resources;

    public WinResourceService(String ip, int port, String login, String password) throws Exception {
        super(ip, port, login, password);
        this.resources = null;
        this.serviceName = "resourcemanager";
    }

    public boolean isResourceAvailable(String resourceName) throws Exception {
        if (this.resources == null) {
            SOAPEnvelope envelope = invokeMethod("getresources", null);
            if (envelope != null) {
                this.resources = getChildElementValues((MessageElement) envelope.getBody(), "resource");
            } else {
                return false;
            }
        }
        for (Object obj : this.resources) {
            if (resourceName.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    @Override // psoft.hsphere.axis.WinService
    public SOAPEnvelope invokeMethod(String method, String[][] params) throws Exception {
        if ((method.equals("get") || method.equals("create") || method.equals("delete")) && !isResourceAvailable(params[0][1])) {
            return null;
        }
        return super.invokeMethod(method, params);
    }
}
