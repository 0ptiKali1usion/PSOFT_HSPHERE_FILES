package psoft.hsphere.resource.epayment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import psoft.epayment.MerchantGatewayLog;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/MerchantTrackingLog.class */
public class MerchantTrackingLog implements MerchantGatewayLog {
    protected MerchantGatewayLog log;
    protected Map merchants;

    public MerchantTrackingLog(MerchantGatewayLog log) {
        this.log = log;
        this.merchants = new HashMap();
    }

    public MerchantTrackingLog() {
        this(new DbLog());
    }

    @Override // psoft.epayment.MerchantGatewayLog
    public void transaction(int id, double amount, Date date, int type) {
        this.log.transaction(id, amount, date, type);
        try {
            MerchantSetting ms = MerchantSetting.get(id);
            if (type == 0 || type == 4) {
                ms.add(amount);
            }
        } catch (Exception e) {
            System.err.println("Can't write transaction log");
            e.printStackTrace();
        }
    }

    @Override // psoft.epayment.MerchantGatewayLog
    public long write(long id, long accid, double amount, int trtype, String dataOut, String dataIn, String error, boolean success, int mgid) {
        Session.getLog().info("EEEE--" + this.log);
        return this.log.write(id, accid, amount, trtype, dataOut, dataIn, error, success, mgid);
    }
}
