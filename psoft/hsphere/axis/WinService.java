package psoft.hsphere.axis;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPMessage;
import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.EngineConfigurationFactoryFinder;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.encoding.Base64;
import org.apache.axis.encoding.ser.JAFDataHandlerDeserializerFactory;
import org.apache.axis.encoding.ser.JAFDataHandlerSerializerFactory;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.PrefixedQName;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPHeaderElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.axis.tcp.TCPSender;
import psoft.hsphere.axis.tcp.TCPStreamHandler;
import psoft.hsphere.axis.tcp.TCPTransport;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/axis/WinService.class */
public class WinService {
    protected String userName;
    protected String userPassword;
    protected String serviceName;
    protected URL endpoint;
    protected boolean isConnected = false;
    protected boolean isAuthorized = false;
    protected Call call = null;
    protected Service service = null;
    protected TCPSender sender = null;
    protected boolean busy;
    protected String serviceIP;
    protected long idleTime;
    public static final String WSSE_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/04/secext";
    public static final String WSU_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String PSOFT_NAMESPACE = "http://www.psoft.net";

    public WinService(String ip, int port, String login, String password) throws Exception {
        this.idleTime = 0L;
        this.serviceIP = ip;
        this.userName = login;
        this.userPassword = password;
        String url = "tcp://" + this.serviceIP + ":" + port;
        TCPStreamHandler handler = new TCPStreamHandler();
        this.endpoint = new URL((URL) null, url, handler);
        this.idleTime = TimeUtils.currentTimeMillis();
    }

    protected boolean openConnection() throws Exception {
        if (!this.isConnected) {
            Call.setTransportForProtocol("tcp", TCPTransport.class);
            EngineConfiguration defaultConfig = EngineConfigurationFactoryFinder.newFactory().getClientEngineConfig();
            SimpleProvider config = new SimpleProvider(defaultConfig);
            this.sender = new TCPSender();
            SimpleTargetedChain chain = new SimpleTargetedChain(this.sender);
            config.deployTransport("tcp", chain);
            this.service = new Service(config);
            this.call = this.service.createCall();
            this.call.setTransport(new TCPTransport());
            this.call.setTargetEndpointAddress(this.endpoint);
            this.isConnected = true;
        }
        return this.isConnected;
    }

    public SOAPEnvelope invokeMethod(String method, String[][] params) throws Exception {
        return invokeMethod(this.serviceName, method, params);
    }

    public List getChildElementValues(MessageElement parent, String name) {
        ArrayList children = new ArrayList();
        do {
            Iterator i = parent.getChildElements();
            if (!i.hasNext()) {
                break;
            }
            while (i.hasNext()) {
                MessageElement element = null;
                while (i.hasNext()) {
                    Object obj = i.next();
                    if (obj instanceof MessageElement) {
                        element = (MessageElement) obj;
                        if (name.equals(element.getName())) {
                            children.add(element.getValue());
                        }
                    }
                }
                parent = element;
            }
        } while (parent != null);
        return children;
    }

    public boolean isMatch(String ip) {
        return this.serviceIP.equals(ip);
    }

    public boolean isBusy() {
        return this.busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public static boolean isSOAPSupport() {
        try {
            return Session.getProperty("SOAP_SUPPORT").trim().toLowerCase().equals("true");
        } catch (Exception e) {
            return false;
        }
    }

    public static int getServicePort() {
        try {
            return Integer.parseInt(Session.getProperty("SOAP_PORT"));
        } catch (Exception e) {
            return 10125;
        }
    }

    public SOAPEnvelope invokeMethod(String service, String method, String[][] params) throws Exception {
        return invokeMethod(service, method, params, false);
    }

    public static SOAPElement getChildElement(MessageElement parent, String name) {
        MessageElement child = null;
        while (child == null) {
            Iterator i = parent.getChildElements();
            if (!i.hasNext()) {
                break;
            }
            while (true) {
                if (i.hasNext()) {
                    MessageElement element = (MessageElement) i.next();
                    if (name.equals(element.getName())) {
                        child = element;
                        break;
                    }
                    parent = element;
                }
            }
        }
        return child;
    }

    public static String getFaultDetailValue(AxisFault fault, String name) {
        NodeList list = fault.getFaultDetails()[0].getChildNodes();
        for (int n = 0; n < list.getLength(); n++) {
            Node node = list.item(n);
            if (node.getNodeName().equals(name)) {
                return node.getFirstChild().getNodeValue().trim();
            }
        }
        return "";
    }

    public long getIdleTime() {
        return this.idleTime;
    }

    public void setIdleTime(long time) {
        this.idleTime = time;
    }

    public void free() throws IOException {
        try {
            this.sender.free();
            this.sender = null;
            this.service = null;
            this.call = null;
        } catch (Throwable th) {
            this.sender = null;
            this.service = null;
            this.call = null;
            throw th;
        }
    }

    private SOAPHeaderElement getSecurityHeader() throws Exception {
        SOAPHeaderElement header = new SOAPHeaderElement(new PrefixedQName(WSSE_NAMESPACE, "Security", "wsse"));
        header.addNamespaceDeclaration("wsu", WSU_NAMESPACE);
        SOAPElement userNameToken = header.addChildElement("UsernameToken");
        userNameToken.addChildElement("Username").addTextNode(this.userName);
        MessageDigest md = MessageDigest.getInstance("SHA");
        Random random = new Random(TimeUtils.currentTimeMillis());
        byte[] nonce = new byte[10];
        random.nextBytes(nonce);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String time = format.format(TimeUtils.getDate());
        byte[] timeArray = time.getBytes();
        byte[] passArray = md.digest(this.userPassword.getBytes());
        byte[] password = new byte[nonce.length + timeArray.length + passArray.length];
        int j = 0;
        for (byte b : nonce) {
            int i = j;
            j++;
            password[i] = b;
        }
        for (byte b2 : timeArray) {
            int i2 = j;
            j++;
            password[i2] = b2;
        }
        for (byte b3 : passArray) {
            int i3 = j;
            j++;
            password[i3] = b3;
        }
        SOAPElement pass = userNameToken.addChildElement("Password");
        pass.addAttribute(new PrefixedQName(WSSE_NAMESPACE, "Type", "wsse"), "PasswordDigest");
        pass.addTextNode(Base64.encode(md.digest(password)));
        userNameToken.addChildElement("Nonce").addTextNode(Base64.encode(nonce));
        SOAPElement created = userNameToken.addChildElement("Created", "wsu");
        created.addTextNode(time);
        return header;
    }

    public SOAPEnvelope invokeMethod(String service, String method, String[][] params, boolean withAttach) throws Exception {
        if (!openConnection()) {
            return null;
        }
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        SOAPEnvelope envelope = new SOAPEnvelope();
        if (!this.isAuthorized) {
            envelope.addHeader(getSecurityHeader());
            this.isAuthorized = true;
        }
        SOAPBodyElement body = new SOAPBodyElement(new PrefixedQName(service, method, (String) null));
        if (params != null) {
            int length = withAttach ? params.length - 1 : params.length;
            for (int i = 0; i < length; i++) {
                SOAPElement element = body.addChildElement(params[i][0]);
                element.addTextNode(params[i][1]);
            }
        }
        if (withAttach) {
            QName qnameAttachment = new QName(service, "DataHandler");
            DataHandler dhSource = new DataHandler(new FileDataSource(params[params.length - 1][1]));
            this.call.registerTypeMapping(dhSource.getClass(), qnameAttachment, JAFDataHandlerSerializerFactory.class, JAFDataHandlerDeserializerFactory.class);
            this.call.setProperty("attachment_encapsulation_format", "axis.attachment.style.dime");
            AttachmentPart attachment = new AttachmentPart(dhSource);
            SOAPElement source = body.addChildElement(params[params.length - 1][0], "");
            message.addAttachmentPart(attachment);
            source.addAttribute(envelope.createName("href"), "cid:" + attachment.getContentId());
            attachment.getContentId();
            this.call.addAttachmentPart(attachment);
        }
        envelope.addBodyElement(body);
        try {
            SOAPEnvelope result = this.call.invoke(envelope);
            this.idleTime = TimeUtils.currentTimeMillis();
            return result;
        } catch (AxisFault fault) {
            this.isAuthorized = false;
            if (fault.getMessage().equals(TCPSender.CONNECTION_RESET)) {
                throw new AxisFault(TCPSender.CONNECTION_RESET);
            }
            throw AxisFault.makeFault(fault);
        }
    }
}
