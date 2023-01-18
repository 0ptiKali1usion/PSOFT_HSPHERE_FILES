package psoft.hsphere.axis.tcp;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.axis.AxisEngine;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.client.Transport;

/* loaded from: hsphere.zip:psoft/hsphere/axis/tcp/TCPTransport.class */
public class TCPTransport extends Transport {
    public static String HOST = "tcp.host";
    public static String PORT = "tcp.port";
    protected String host;
    protected String port;

    public TCPTransport() {
        this.transportName = "tcp";
    }

    public TCPTransport(String host, String port) {
        this.transportName = "tcp";
        this.host = host;
        this.port = port;
    }

    public void setupMessageContextImpl(MessageContext mc, Call call, AxisEngine engine) {
        try {
            String urlString = mc.getStrProp("transport.url");
            if (urlString != null) {
                TCPStreamHandler handler = new TCPStreamHandler();
                URL url = new URL((URL) null, urlString, handler);
                this.host = url.getHost();
                this.port = new Integer(url.getPort()).toString();
            }
        } catch (MalformedURLException e) {
        }
        if (this.host != null) {
            mc.setProperty(HOST, this.host);
        }
        if (this.port != null) {
            mc.setProperty(PORT, this.port);
        }
    }
}
