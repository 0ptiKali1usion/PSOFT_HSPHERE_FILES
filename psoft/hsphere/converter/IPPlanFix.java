package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/converter/IPPlanFix.class */
public class IPPlanFix extends C0004CP {
    private static Connection con;

    public IPPlanFix() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            IPPlanFix test = new IPPlanFix();
            test.m29go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Operations finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m29go() throws Exception {
        con = Session.getDb();
        PreparedStatement plansPs = null;
        try {
            plansPs = con.prepareStatement("SELECT id FROM plans WHERE deleted <> 1");
            ResultSet plansRs = plansPs.executeQuery();
            while (plansRs.next()) {
                System.out.println("Processing plan #" + plansRs.getInt(1));
                fixPlan(plansRs.getInt(1));
            }
            Session.closeStatement(plansPs);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(plansPs);
            con.close();
            throw th;
        }
    }

    public void fixPlan(int planId) throws Exception {
        PreparedStatement iModsPs = null;
        try {
            iModsPs = con.prepareStatement("SELECT type_id, mod_id FROM plan_iresource WHERE plan_id=? AND (type_id=2 OR type_id=31 OR type_id=32 OR type_id=34 OR type_id=35) AND new_type_id=8 GROUP BY type_id, mod_id HAVING count(*) > 1");
            iModsPs.setInt(1, planId);
            ResultSet iMods = iModsPs.executeQuery();
            while (iMods.next()) {
                System.out.println("Type " + iMods.getInt("type_id") + " with mod " + iMods.getString("mod_id") + " need to be fixed.");
                fixIMod(planId, iMods.getInt("type_id"), iMods.getString("mod_id"));
            }
            Session.closeStatement(iModsPs);
        } catch (Throwable th) {
            Session.closeStatement(iModsPs);
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    /* JADX WARN: Removed duplicated region for block: B:45:0x005d A[Catch: all -> 0x01b3, TryCatch #0 {all -> 0x01b3, blocks: (B:39:0x000d, B:42:0x0023, B:43:0x0038, B:45:0x005d, B:47:0x0097, B:50:0x00ad, B:51:0x00c2, B:52:0x011e, B:53:0x0150, B:54:0x015c, B:55:0x0168, B:56:0x0174, B:57:0x0180, B:58:0x0189, B:49:0x009e, B:41:0x0014), top: B:66:0x000d }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void fixIMod(int r5, int r6, java.lang.String r7) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 456
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.converter.IPPlanFix.fixIMod(int, int, java.lang.String):void");
    }
}
