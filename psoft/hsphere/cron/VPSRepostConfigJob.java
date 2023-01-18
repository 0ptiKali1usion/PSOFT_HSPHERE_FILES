package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.admin.NetworkGatewayManager;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/cron/VPSRepostConfigJob.class */
public class VPSRepostConfigJob extends BackgroundJob {
    private ArrayList newGateways;
    private ArrayList delGateways;
    TemplateList subnets;
    ArrayList checkAddr;

    public VPSRepostConfigJob(C0004CP cp) throws Exception {
        super(cp);
        this.subnets = new TemplateList();
        this.checkAddr = new ArrayList();
    }

    public VPSRepostConfigJob(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
        this.subnets = new TemplateList();
        this.checkAddr = new ArrayList();
    }

    public VPSRepostConfigJob(C0004CP cp, String dbMark, long jobProgressCheckPeriod) throws Exception {
        super(cp, dbMark, jobProgressCheckPeriod);
        this.subnets = new TemplateList();
        this.checkAddr = new ArrayList();
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        Thread.sleep(350000L);
        Date startDate = new Date(TimeUtils.currentTimeMillis());
        getLog().debug("VPSRepostConfigJob - BEGIN " + startDate);
        try {
            processNewGateways();
        } catch (Exception e) {
            getLog().error("Error occured while processing New Gateways ", e);
        }
        try {
            processDelGateways();
        } catch (Exception e2) {
            getLog().error("Error occured while processing Deleted Gateways ", e2);
        }
        long timeFinished = TimeUtils.currentTimeMillis();
        getLog().debug("VPSRepostConfigJob - FINISHED " + new Date(timeFinished));
        long timeDiff = timeFinished - startDate.getTime();
        getLog().debug("VPSRepostConfigJob process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec.");
    }

    private void processNewGateways() throws Exception {
        boolean wasNewGatewayAdded = false;
        if (this.newGateways == null) {
            this.newGateways = NetworkGatewayManager.getManager().getNewNetworkGateways();
        } else {
            this.newGateways.addAll(NetworkGatewayManager.getManager().getNewNetworkGateways());
        }
        List<HostEntry> hosts = HostManager.getHostsByGroupType(12);
        if (this.newGateways == null || this.newGateways.isEmpty()) {
            return;
        }
        do {
            for (HostEntry server : hosts) {
                getSubnetsFromServer(server);
                Iterator i = this.newGateways.iterator();
                while (i.hasNext()) {
                    TemplateModel templateModel = (TemplateHash) i.next();
                    if (!this.checkAddr.contains(templateModel.get("addr"))) {
                        this.subnets.add(templateModel);
                        wasNewGatewayAdded = true;
                    }
                }
                if (wasNewGatewayAdded) {
                    saveSubnetsOnServer(server);
                }
                ArrayList list = NetworkGatewayManager.getManager().getNewNetworkGateways();
                if (list != null) {
                    this.newGateways.addAll(list);
                }
            }
            this.newGateways.clear();
            ArrayList newNetworkGateways = NetworkGatewayManager.getManager().getNewNetworkGateways();
            this.newGateways = newNetworkGateways;
            if (newNetworkGateways == null) {
                return;
            }
        } while (!this.newGateways.isEmpty());
    }

    private void getSubnetsFromServer(HostEntry server) throws Exception {
        Collection<String> col = server.exec("vps-subnets-xml-get.pl", new String[0]);
        StringBuffer response = new StringBuffer();
        for (String s : col) {
            response.append(s);
        }
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new ByteArrayInputStream(response.toString().getBytes())));
        Document doc = parser.getDocument();
        NodeList nodeList = doc.getElementsByTagName("subnet");
        int count = nodeList.getLength();
        for (int i = 0; i < count; i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            String attrAddr = attributes.getNamedItem("addr").getNodeValue();
            Node nd = attributes.getNamedItem("device");
            String attrDevice = nd != null ? nd.getNodeValue() : "";
            String attrGateway = attributes.getNamedItem("gateway").getNodeValue();
            String attrMask = attributes.getNamedItem("mask").getNodeValue();
            TemplateModel templateHash = new TemplateHash();
            templateHash.put("addr", attrAddr);
            templateHash.put("device", attrDevice);
            templateHash.put("gateway", attrGateway);
            templateHash.put("mask", attrMask);
            this.subnets.add(templateHash);
            this.checkAddr.add(attrAddr);
        }
    }

    private void saveSubnetsOnServer(HostEntry server) throws Exception {
        Template config = Session.getTemplate("eeman/vps.config");
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        SimpleHash root = new SimpleHash();
        root.put("subnets", this.subnets);
        config.process(root, writer);
        writer.flush();
        writer.close();
        server.exec("vps-subnets-xml-put.pl", new String[0], out.toString());
    }

    private void processDelGateways() throws Exception {
        if (this.delGateways == null) {
            this.delGateways = NetworkGatewayManager.getManager().getDelNetworkGateways();
        } else {
            this.delGateways.addAll(NetworkGatewayManager.getManager().getDelNetworkGateways());
        }
        if (this.delGateways == null || this.delGateways.isEmpty()) {
            return;
        }
        do {
            Iterator servers = HostManager.getHostsByGroupType(12).iterator();
            while (servers.hasNext()) {
                HostEntry server = (HostEntry) servers.next();
                Iterator ig = this.delGateways.iterator();
                while (ig.hasNext()) {
                    server.exec("vps-subnets-del.pl", new String[]{(String) ig.next()});
                }
            }
            this.delGateways.clear();
            ArrayList delNetworkGateways = NetworkGatewayManager.getManager().getDelNetworkGateways();
            this.delGateways = delNetworkGateways;
            if (delNetworkGateways == null) {
                return;
            }
        } while (!this.delGateways.isEmpty());
    }
}
