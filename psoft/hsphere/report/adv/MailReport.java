package psoft.hsphere.report.adv;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/MailReport.class */
public class MailReport extends AdvReport {
    protected int numOfNewMailObjectId;

    /* renamed from: id */
    protected long f131id;
    protected static NFUCache mail_cache;

    static {
        try {
            mail_cache = new NFUCache(new Integer(Session.getPropertyString("MAIL_CASHE_SIZE")).intValue());
        } catch (Exception e) {
            mail_cache = new NFUCache(50);
        }
    }

    protected static String getCacheKey(long id) {
        try {
            return Long.toString(id) + "|" + Session.getAccountId();
        } catch (Exception e) {
            Session.getLog().error("Unable to retrieve reseller ID:", e);
            return "";
        }
    }

    public static MailReport getMailReport(long id) throws Exception {
        MailReport rep = (MailReport) mail_cache.get(getCacheKey(id));
        return rep;
    }

    public static void cleanMailReport(long id) throws Exception {
        mail_cache.remove(getCacheKey(id));
    }

    public void init(long mailObjectId, DataContainer data) {
        this.f131id = mailObjectId;
        this.data = data;
        this.size = data.size();
        initPageList();
        setPosition(0);
        mail_cache.put(getCacheKey(this.f131id), this);
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        long mailObjectId = new Long((String) i.next()).longValue();
        this.numOfNewMailObjectId = new Integer((String) i.next()).intValue();
        init(mailObjectId, new DataContainer((LinkedList) i.next()));
    }

    public void FM_setPageForNewMailObject() {
        int neededPage = 0;
        do {
            neededPage++;
        } while (this.numOfNewMailObjectId >= this.step * neededPage);
        this.currentPage = neededPage;
        FM_setPage(this.currentPage);
    }
}
