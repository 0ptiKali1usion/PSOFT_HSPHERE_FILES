package psoft.hsphere.monitoring;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/MonitoringServer.class */
public class MonitoringServer extends Thread {
    public static Category LOG;
    public static ResourceBundle config;
    public static Hashtable conf;
    private Set tasks;
    protected Collection hosts;
    protected static Map monitors = new HashMap();
    private static Class[] _argT;

    public static void main(String[] argv) throws Exception {
        config = PropertyResourceBundle.getBundle("psoft_config.hsphere_monitoring");
        conf = new Hashtable();
        conf.put("config", config);
        Session.initMailer();
        Session.setDb(Toolbox.getDB(config));
        PropertyConfigurator.configure(Toolbox.RB2Properties(config));
        LOG = Category.getInstance("MONITORING");
        LOG.info("START MONITORING");
        MonitoringServer main = new MonitoringServer(config);
        main.monitor();
        main.start();
    }

    static {
        monitors.put(new Integer(1), WebServerMonitor.class);
        monitors.put(new Integer(2), DNSMonitor.class);
        monitors.put(new Integer(3), MailServerMonitor.class);
        _argT = new Class[]{HostEntry.class};
    }

    public MonitoringServer(ResourceBundle rb) throws SQLException, ClassNotFoundException {
        config = rb;
        this.tasks = new HashSet();
        this.hosts = HostManager.getHosts();
        for (HostEntry he : this.hosts) {
            Class c = (Class) monitors.get(new Integer(he.getGroupType()));
            Object[] argV = {he};
            if (c != null) {
                try {
                    Monitorable m = (Monitorable) c.getConstructor(_argT).newInstance(argV);
                    this.tasks.add(new Task(m));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
    }

    public void monitor() {
        for (Task task : this.tasks) {
            task.start();
        }
    }
}
