package psoft.hsphere.resource.admin;

import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/LogicalServerInfo.class */
public abstract class LogicalServerInfo {
    public abstract List getInfo(long j) throws Exception;

    public abstract String getUsed(long j) throws Exception;

    public abstract List getIPTypes() throws Exception;

    public String getFixed() throws Exception {
        return null;
    }
}
