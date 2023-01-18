package psoft.epayment.test;

import java.util.Date;
import java.util.Random;
import psoft.epayment.GenericMerchantGateway;
import psoft.epayment.MerchantGatewayLog;

/* loaded from: hsphere.zip:psoft/epayment/test/ConsoleLog.class */
public class ConsoleLog implements MerchantGatewayLog {
    @Override // psoft.epayment.MerchantGatewayLog
    public void transaction(int id, double amount, Date date, int type) {
    }

    @Override // psoft.epayment.MerchantGatewayLog
    public long write(long trid, long accid, double amount, int trtype, String dataOut, String dataIn, String error, boolean success, int gatewayId) {
        Random rand = new Random();
        if (trid == 0) {
            return Math.abs(rand.nextLong());
        }
        System.out.println("\n****************************************" + GenericMerchantGateway.getTrDescription(trtype) + "*********************************************\n\nTransaction Type: " + GenericMerchantGateway.getTrDescription(trtype) + "\nAmount: " + Double.toString(amount) + "\naccId: " + Long.toString(accid) + "\n-----------------------------------------------------------------------\nRequest: \n" + dataOut + "\n-----------------------------------------------------------------------\nResponse:\n" + dataIn + "\n-----------------------------------------------------------------------\nError: " + error + "\nResult: " + new Boolean(success).toString() + "\n\n**************************************************************************************\n");
        return Math.abs(rand.nextLong());
    }
}
