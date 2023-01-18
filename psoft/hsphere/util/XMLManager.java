package psoft.hsphere.util;

import freemarker.template.TemplateModel;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateXML;
import psoft.util.xml.DOMLoader;

/* loaded from: hsphere.zip:psoft/hsphere/util/XMLManager.class */
public class XMLManager extends Thread {
    public static HashMap cache = new HashMap();
    protected static HashMap fileDates = new HashMap();
    protected long lastLaunch;
    protected long sleepingTime = 600000;
    protected String dbMark = "XML_MANAGER";

    public XMLManager() throws Exception {
        this.lastLaunch = 0L;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT value FROM last_start WHERE name = ?");
                ps.setString(1, this.dbMark);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getTimestamp(1) != null) {
                    this.lastLaunch = rs.getTimestamp(1).getTime();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error getting LAST_LAUNCH", ex);
                Session.closeStatement(ps);
                con.close();
            }
            setDaemon(true);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static Document getXML(String key) throws SAXException, IOException, TransformerException {
        Document document;
        synchronized (cache) {
            Document doc = (Document) cache.get(key);
            if (doc == null) {
                doc = DOMLoader.getXML(PackageConfigurator.getXMLFilesList(key));
                if (doc != null) {
                    cache.put(key, doc);
                    fileDates.put(key, new Long(PackageConfigurator.getSummaryTime(key)));
                } else {
                    throw new IOException("Document not found for key: " + key);
                }
            }
            document = doc;
        }
        return document;
    }

    public static Document getXML(String key, boolean validate) throws SAXException, IOException, TransformerException {
        Document document;
        synchronized (cache) {
            Document doc = (Document) cache.get(key);
            if (doc == null) {
                doc = DOMLoader.getXML(PackageConfigurator.getXMLFilesList(key), validate);
                if (doc != null) {
                    cache.put(key, doc);
                    fileDates.put(key, new Long(PackageConfigurator.getSummaryTime(key)));
                } else {
                    throw new IOException("Document not found for key: " + key);
                }
            }
            document = doc;
        }
        return document;
    }

    public static Document getXML(String key, String fileName) throws SAXException, IOException, TransformerException {
        Document document;
        String newKey = key + "|" + fileName;
        synchronized (cache) {
            Document doc = (Document) cache.get(newKey);
            if (doc == null) {
                doc = DOMLoader.getXML(PackageConfigurator.getXMLFileList(key, fileName));
                if (doc != null) {
                    cache.put(newKey, doc);
                    fileDates.put(newKey, new Long(PackageConfigurator.getSummaryTime(key, fileName)));
                } else {
                    throw new IOException("Document not found: " + fileName);
                }
            }
            document = doc;
        }
        return document;
    }

    public static TemplateModel getXMLAsTemplate(String key, String tag) throws Exception {
        return new TemplateXML(getXML(key), tag);
    }

    public static TemplateModel getXMLAsTemplate(String key, String fileName, String tag) throws Exception {
        return new TemplateXML(getXML(key, fileName), tag);
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        long j = this.sleepingTime;
        setName(this.dbMark);
        while (!isInterrupted()) {
            long begin = TimeUtils.currentTimeMillis();
            if (this.lastLaunch == 0 || this.lastLaunch < begin - this.sleepingTime) {
                long lastLaunchOld = 0;
                try {
                    lastLaunchOld = this.lastLaunch;
                    if (this.lastLaunch == 0) {
                        this.lastLaunch = TimeUtils.currentTimeMillis();
                    } else {
                        long multiplier = 1;
                        if (this.lastLaunch + this.sleepingTime < TimeUtils.currentTimeMillis()) {
                            long tVal = TimeUtils.currentTimeMillis() - this.lastLaunch;
                            multiplier = (long) (tVal / this.sleepingTime);
                        }
                        Session.getLog().debug("multiplier=" + multiplier);
                        this.lastLaunch += this.sleepingTime * multiplier;
                    }
                    Session.getLog().debug("Next launch :" + new Date(this.lastLaunch));
                    try {
                        List<String> badKeys = new ArrayList();
                        for (String key : fileDates.keySet()) {
                            long newSummaryDate = PackageConfigurator.getSummaryTime(key);
                            long oldSummaryDate = ((Long) fileDates.get(key)).longValue();
                            if (newSummaryDate != oldSummaryDate) {
                                badKeys.add(key);
                            }
                        }
                        for (String key2 : badKeys) {
                            Session.getLog().debug("XML for \"" + key2 + "\" has been changed! Remove document from cache");
                            fileDates.remove(key2);
                            cache.remove(key2);
                        }
                    } catch (Exception e) {
                        Session.getLog().error("Error during XMLManager.run()", e);
                    }
                    Connection con = Session.getDb();
                    PreparedStatement ps = con.prepareStatement("UPDATE last_start SET value = ?  WHERE name = ?");
                    ps.setTimestamp(1, new Timestamp(this.lastLaunch));
                    ps.setString(2, this.dbMark);
                    int count = ps.executeUpdate();
                    if (count < 1) {
                        ps.close();
                        ps = con.prepareStatement("INSERT INTO last_start(name,value)  VALUES(?, ?)");
                        ps.setString(1, this.dbMark);
                        ps.setTimestamp(2, new Timestamp(this.lastLaunch));
                        ps.executeUpdate();
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Throwable e2) {
                    this.lastLaunch = lastLaunchOld;
                    Session.getLog().error("Exception during cron: ", e2);
                }
            }
            long finish = System.currentTimeMillis();
            long nextLaunch = (this.sleepingTime - finish) + this.lastLaunch;
            if (nextLaunch > 0) {
                try {
                    sleep(this.sleepingTime);
                } catch (InterruptedException e3) {
                }
            }
        }
        Session.getLog().debug("XMLManager has been interrupted");
        Session.getLog().debug("XMLManager has been stopped");
    }
}
